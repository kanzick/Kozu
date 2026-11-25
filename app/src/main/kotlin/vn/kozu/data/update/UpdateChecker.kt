package vn.kozu.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.UpdateChannel

data class RecentCommit(
    val sha: String,
    val title: String,
    val author: String,
    val date: String,
    val url: String,
    val avatarUrl: String?,
)

data class RecentCommitsResult(
    val branch: String,
    val commits: List<RecentCommit>,
    val note: String? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
)

sealed interface UpdateResult {
    data class Available(
        val version: String,
        val url: String,
        val notes: String,
        val downloadUrl: String? = null,
    ) : UpdateResult

    data class UpToDate(
        val version: String,
    ) : UpdateResult

    data class Failed(
        val message: String,
    ) : UpdateResult
}

class UpdateChecker(
    context: Context,
    private val client: OkHttpClient,
) {
    private val appContext = context.applicationContext
    private val currentVersion: String get() = installedVersion(appContext)

    @Suppress("UNUSED_PARAMETER")
    suspend fun check(channel: UpdateChannel = UpdateChannel.STABLE): UpdateResult =
        withContext(Dispatchers.IO) {
            val viaApi = checkViaApi()
            if (viaApi !is UpdateResult.Failed) return@withContext viaApi
            checkViaFeed() ?: viaApi
        }

    private fun checkViaApi(): UpdateResult {
        val request =
            Request
                .Builder()
                .url(RELEASES_URL_API)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 403 || response.code == 429 -> {
                        UpdateResult.Failed("GitHub is rate limiting requests. Try again later.")
                    }

                    !response.isSuccessful -> {
                        UpdateResult.Failed("GitHub returned HTTP ${response.code}")
                    }

                    else -> parseReleases(response.body.string())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UpdateResult.Failed("Could not reach GitHub. Check your connection.")
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "Update check failed")
        }
    }

    private fun checkViaFeed(): UpdateResult? =
        try {
            val request = Request.Builder().url(RELEASES_FEED_URL).header("Accept", "application/atom+xml").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body.string()
                val entries = Regex("<entry>([\\s\\S]*?)</entry>").findAll(body).map { it.groupValues[1] }.toList()
                val candidates = entries.mapNotNull { entry ->
                    val title = Regex("<title>([^<]*)</title>").find(entry)?.groupValues?.get(1)?.trim()
                    val link = Regex("<link[^>]*href=\"([^\"]*/releases/tag/[^\"]+)\"").find(entry)?.groupValues?.get(1)
                    if (title.isNullOrBlank()) null else Triple(title, entry, link ?: RELEASES_URL)
                }
                val best = candidates.maxWithOrNull { x, y -> compareVersions(x.first, y.first) } ?: return@use null
                val latest = best.first.trim()
                if (compareVersions(latest, currentVersion) > 0) {
                    val notes = Regex("<content[^>]*>([\\s\\S]*?)</content>").find(best.second)?.groupValues?.get(1).orEmpty()
                    val assetUrl = findFallbackAssetUrl(latest)
                    UpdateResult.Available(latest, best.third, cleanReleaseNotes(htmlToText(notes)), assetUrl)
                } else {
                    UpdateResult.UpToDate(currentVersion)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun htmlToText(escaped: String): String =
        escaped
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'").replace("&amp;", "&")
            .replace(Regex("<li[^>]*>"), "• ")
            .replace(Regex("</(p|h[1-6]|li|ul|ol)>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    suspend fun recentCommits(
        channel: UpdateChannel,
        page: Int = 1,
    ): RecentCommitsResult =
        withContext(Dispatchers.IO) {

            @Suppress("UNUSED_VARIABLE") val ignored = channel
            val requestedBranch = DEFAULT_BRANCH
            loadRecentPage(requestedBranch, page)?.let { return@withContext it }

            if (requestedBranch == DEFAULT_BRANCH) {
                return@withContext RecentCommitsResult(
                    branch = DEFAULT_BRANCH,
                    commits = emptyList(),
                    note = "Commit history is unavailable. Try again later.",
                    page = page,
                )
            }

            loadRecentPage(DEFAULT_BRANCH, page)?.copy(
                note = "The '$requestedBranch' branch is not published. Showing '$DEFAULT_BRANCH'.",
            ) ?: RecentCommitsResult(
                branch = DEFAULT_BRANCH,
                commits = emptyList(),
                note = "Commit history is unavailable. Check your connection.",
                page = page,
            )
        }


    private fun loadRecentPage(
        branch: String,
        requestedPage: Int,
    ): RecentCommitsResult? {
        val firstPage = fetchCommits(branch, apiPage = 1) ?: return null
        val totalPages = firstPage.totalPages.coerceAtLeast(1)
        val page = requestedPage.coerceIn(1, totalPages)
        val selected = if (page == 1) firstPage else fetchCommits(branch, apiPage = page) ?: return null
        return RecentCommitsResult(
            branch = branch,
            commits = selected.commits,
            page = page,
            totalPages = totalPages,
        )
    }

    private data class CommitPage(
        val commits: List<RecentCommit>,
        val totalPages: Int,
    )

    private fun fetchCommits(
        branch: String,
        apiPage: Int,
    ): CommitPage? {
        val request =
            Request
                .Builder()
                .url("$COMMITS_URL_API?sha=$branch&per_page=$COMMITS_PER_PAGE&page=$apiPage")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val items = JSONArray(response.body.string())
                val totalPages = parseLastPage(response.header("Link"), apiPage)
                val commits = buildList {
                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val commit = item.optJSONObject("commit") ?: continue
                        val metadata = commit.optJSONObject("author")
                        val account = item.optJSONObject("author")
                        val committer = item.optJSONObject("committer")
                        val title = commit.optString("message").substringBefore('\n').trim()
                        val sha = item.optString("sha").trim()
                        if (title.isBlank() || sha.isBlank()) continue
                        add(
                            RecentCommit(
                                sha = sha,
                                title = title,
                                author = account?.optString("login")?.takeIf(String::isNotBlank)
                                    ?: metadata?.optString("name")?.takeIf(String::isNotBlank)
                                    ?: "Unknown author",
                                date = metadata?.optString("date").orEmpty(),
                                url = item.optString("html_url").ifBlank { REPOSITORY_URL },
                                avatarUrl = account?.optString("avatar_url")?.takeIf(String::isNotBlank)
                                    ?: committer?.optString("avatar_url")?.takeIf(String::isNotBlank),
                            ),
                        )
                    }
                }
                CommitPage(commits = commits, totalPages = totalPages)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLastPage(
        linkHeader: String?,
        fallbackPage: Int,
    ): Int {
        val links = linkHeader.orEmpty()
        val lastLink = Regex("<[^>]*[?&]page=(\\d+)[^>]*>;\\s*rel=\"last\"")
            .find(links)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val nextLink = Regex("<[^>]*[?&]page=(\\d+)[^>]*>;\\s*rel=\"next\"")
            .find(links)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return (lastLink ?: if (nextLink != null) maxOf(fallbackPage, nextLink) else fallbackPage)
            .coerceIn(1, MAX_COMMIT_PAGES)
    }

    private fun preferredApkNames(): List<String> =
        Build.SUPPORTED_ABIS
            .distinct()
            .map { "kozu-$it.apk" } + "kozu-universal.apk"

    private fun isTrustedReleaseAsset(url: String): Boolean = runCatching {
        val parsed = java.net.URL(url)
        parsed.protocol == "https" &&
            parsed.host.equals("github.com", ignoreCase = true) &&
            parsed.path.startsWith("/kanzick/Kozu/releases/download/")
    }.getOrDefault(false)

    private fun findFallbackAssetUrl(tag: String): String? {
        for (name in preferredApkNames()) {
            val candidate = "$RELEASE_DOWNLOAD_URL/$tag/$name"
            val request = Request.Builder().url(candidate).head().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code in 300..399) return candidate
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun parseReleases(body: String): UpdateResult {
        val releases =
            try {
                JSONArray(body)
            } catch (_: Exception) {
                return UpdateResult.Failed("Unexpected response from GitHub")
            }

        val release =
            (0 until releases.length())
                .mapNotNull { releases.optJSONObject(it) }
                .filter {
                    !it.optBoolean("draft") && it.optString("tag_name").isNotBlank()
                }
                .maxWithOrNull { x, y -> compareVersions(x.optString("tag_name"), y.optString("tag_name")) }
                ?: return UpdateResult.UpToDate(currentVersion)

        val tag = release.optString("tag_name").trim()
        val url = release.optString("html_url").ifBlank { RELEASES_URL }
        val downloadUrl = findApkDownloadUrl(release)
        return if (compareVersions(tag, currentVersion) > 0) {
            UpdateResult.Available(tag, url, cleanReleaseNotes(release.optString("body")), downloadUrl)
        } else {
            UpdateResult.UpToDate(currentVersion)
        }
    }

    private fun findApkDownloadUrl(release: org.json.JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        val urlsByName = buildMap {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name").trim()
                val url = asset.optString("browser_download_url").trim()
                if (name.isNotBlank() && url.isNotBlank()) put(name, url)
            }
        }
        return preferredApkNames().firstNotNullOfOrNull { name ->
            urlsByName[name]?.takeIf(::isTrustedReleaseAsset)
        }
    }

    private fun cleanReleaseNotes(body: String): String {
        val fullChangelog = Regex(
            "(?im)^\\s*(?:#{1,6}\\s*)?(?:📝\\s*)?(?:\\*\\*)?Full Changelog(?:\\*\\*)?(?:\\s*:.*)?$",
        ).find(body)?.range?.first
        val downloads = Regex(
            "(?im)^\\s*(?:#{1,6}\\s*)?(?:📥\\s*)?(?:\\*\\*)?(?:Downloads|Assets)(?:\\*\\*)?(?:\\s*(?:⬇️|↓))?(?:\\s*:.*)?$",
        ).find(body)?.range?.first
        val end = listOfNotNull(fullChangelog, downloads).minOrNull() ?: body.length
        return body.substring(0, end).trim()
    }

    suspend fun downloadApk(
        url: String,
        destination: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val parsedUrl = runCatching { java.net.URL(url) }.getOrElse {
            throw IOException("The download URL is invalid.", it)
        }
        if (parsedUrl.protocol != "https" || parsedUrl.host != "github.com" || !isTrustedReleaseAsset(url)) {
            throw IOException("The update download URL is not trusted.")
        }

        val request = Request.Builder().url(url).header("Accept", "application/octet-stream").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GitHub returned HTTP ${response.code} while downloading the update.")
            val body = response.body
            val total = body.contentLength()
            if (total > MAX_UPDATE_SIZE_BYTES) throw IOException("The update APK is larger than expected.")
            destination.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) throw IOException("Could not create the update folder.")
            }

            try {
                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastReported = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            if (downloaded > MAX_UPDATE_SIZE_BYTES) {
                                throw IOException("The update APK is larger than expected.")
                            }
                            if (downloaded - lastReported >= PROGRESS_INTERVAL_BYTES || downloaded == total) {
                                onProgress(downloaded, total)
                                lastReported = downloaded
                            }
                        }
                        output.fd.sync()
                        if (downloaded <= 0L || (total > 0L && downloaded != total)) {
                            throw IOException("The update download was incomplete.")
                        }
                        onProgress(downloaded, total)
                    }
                }
            } catch (error: Exception) {
                destination.delete()
                throw error
            }
        }
        destination
    }

    companion object {
        const val MAX_UPDATE_SIZE_BYTES = 200L * 1024L * 1024L
        private const val PROGRESS_INTERVAL_BYTES = 256L * 1024L
        const val RELEASES_URL_API = "https://api.github.com/repos/kanzick/Kozu/releases?per_page=50"
        const val COMMITS_URL_API = "https://api.github.com/repos/kanzick/Kozu/commits"
        const val COMMITS_PER_PAGE = 30
        const val MAX_COMMIT_PAGES = 1_000
        const val REPOSITORY_URL = "https://github.com/kanzick/Kozu"
        const val RELEASES_URL = "https://github.com/kanzick/Kozu/releases"
        const val RELEASE_DOWNLOAD_URL = "https://github.com/kanzick/Kozu/releases/download"
        const val RELEASES_FEED_URL = "https://github.com/kanzick/Kozu/releases.atom"
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val DEFAULT_BRANCH = "main"

        fun installedVersion(context: Context): String =
            try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty()
                    .ifBlank { "0" }
            } catch (e: PackageManager.NameNotFoundException) {
                "0"
            }

        fun compareVersions(
            a: String,
            b: String,
        ): Int {
            val left = ParsedVersion.of(a)
            val right = ParsedVersion.of(b)
            val size = maxOf(left.numbers.size, right.numbers.size)
            for (index in 0 until size) {
                val diff = left.numbers.getOrElse(index) { 0L }.compareTo(right.numbers.getOrElse(index) { 0L })
                if (diff != 0) return diff
            }
            return when {
                left.pre.isEmpty() && right.pre.isEmpty() -> 0
                left.pre.isEmpty() -> 1
                right.pre.isEmpty() -> -1
                else -> comparePre(left.pre, right.pre)
            }
        }

        private fun comparePre(
            a: List<String>,
            b: List<String>,
        ): Int {
            for (index in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrNull(index) ?: return -1
                val y = b.getOrNull(index) ?: return 1
                val xn = x.toLongOrNull()
                val yn = y.toLongOrNull()
                val diff =
                    when {
                        xn != null && yn != null -> xn.compareTo(yn)
                        xn != null -> -1
                        yn != null -> 1
                        else -> x.compareTo(y)
                    }
                if (diff != 0) return diff
            }
            return 0
        }
    }

    private class ParsedVersion(
        val numbers: List<Long>,
        val pre: List<String>,
    ) {
        companion object {
            fun of(raw: String): ParsedVersion {
                val clean =
                    raw
                        .trim()
                        .removePrefix("v")
                        .removePrefix("V")
                        .substringBefore('+')
                val core = clean.substringBefore('-')
                val pre =
                    if (clean.contains('-')) {
                        clean.substringAfter('-').split('.', '-').filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }
                val numbers = core.split('.').map { part -> part.takeWhile { it.isDigit() }.toLongOrNull() ?: 0L }
                return ParsedVersion(numbers, pre)
            }
        }
    }
}

suspend fun UpdateChecker.checkIfDue(settings: SettingsStore): UpdateResult? {
    val current = settings.settings.first()
    val now = System.currentTimeMillis()
    if (!current.autoUpdateCheck) return null
    if (now - current.lastUpdateCheckAt in 0 until UpdateChecker.DAY_MS) return null
    val result = check(current.updateChannel)
    if (result is UpdateResult.Failed) return null
    settings.setLastUpdateCheck(now)
    return result
}
