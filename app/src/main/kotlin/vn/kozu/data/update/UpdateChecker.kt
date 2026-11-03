package vn.kozu.data.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.UpdateChannel
import java.io.IOException

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

    suspend fun check(channel: UpdateChannel = UpdateChannel.STABLE): UpdateResult =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(RELEASES_URL_API)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 403 || response.code == 429 -> {
                            UpdateResult.Failed("GitHub is rate limiting requests. Try again later.")
                        }

                        !response.isSuccessful -> {
                            UpdateResult.Failed("GitHub returned HTTP ${response.code}")
                        }

                        else -> parseReleases(response.body?.string().orEmpty(), channel)
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

    suspend fun recentCommits(
        channel: UpdateChannel,
        page: Int = 1,
    ): RecentCommitsResult =
        withContext(Dispatchers.IO) {
            val requestedBranch = channel.branch
            loadChronologicalPage(requestedBranch, page)?.let { return@withContext it }

            if (requestedBranch == DEFAULT_BRANCH) {
                return@withContext RecentCommitsResult(
                    branch = DEFAULT_BRANCH,
                    commits = emptyList(),
                    note = "Commit history is unavailable. Try again later.",
                    page = page,
                )
            }

            loadChronologicalPage(DEFAULT_BRANCH, page)?.copy(
                note = "The '$requestedBranch' branch is not published. Showing '$DEFAULT_BRANCH'.",
            ) ?: RecentCommitsResult(
                branch = DEFAULT_BRANCH,
                commits = emptyList(),
                note = "Commit history is unavailable. Check your connection.",
                page = page,
            )
        }

    /** GitHub returns commits newest-first; reverse both page order and item order. */
    private fun loadChronologicalPage(
        branch: String,
        requestedPage: Int,
    ): RecentCommitsResult? {
        val newestPage = fetchCommits(branch, apiPage = 1) ?: return null
        val totalPages = newestPage.totalPages.coerceAtLeast(1)
        val page = requestedPage.coerceIn(1, totalPages)
        val apiPage = totalPages - page + 1
        val selected = if (apiPage == 1) newestPage else fetchCommits(branch, apiPage) ?: return null
        return RecentCommitsResult(
            branch = branch,
            commits = selected.commits.asReversed(),
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
                val items = JSONArray(response.body?.string().orEmpty())
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

    private fun parseReleases(body: String, channel: UpdateChannel): UpdateResult {
        val releases =
            try {
                JSONArray(body)
            } catch (_: Exception) {
                return UpdateResult.Failed("Unexpected response from GitHub")
            }
        val release =
            (0 until releases.length())
                .mapNotNull { releases.optJSONObject(it) }
                .firstOrNull { candidate ->
                    !candidate.optBoolean("draft") && candidate.optBoolean("prerelease") == channel.isPrerelease
                }
                ?: return UpdateResult.UpToDate(currentVersion)

        val tag = release.optString("tag_name").trim()
        if (tag.isEmpty()) return UpdateResult.Failed("GitHub returned a release without a version tag")
        val latest = tag.removePrefix("v").removePrefix("V")
        val url = release.optString("html_url").ifBlank { RELEASES_URL }
        return if (compareVersions(latest, currentVersion) > 0) {
            UpdateResult.Available(latest, url, release.optString("body").trim())
        } else {
            UpdateResult.UpToDate(currentVersion)
        }
    }

    companion object {
        const val RELEASES_URL_API = "https://api.github.com/repos/kanzick/Kozu/releases?per_page=50"
        const val COMMITS_URL_API = "https://api.github.com/repos/kanzick/Kozu/commits"
        const val COMMITS_PER_PAGE = 30
        const val MAX_COMMIT_PAGES = 1_000
        const val REPOSITORY_URL = "https://github.com/kanzick/Kozu"
        const val RELEASES_URL = "https://github.com/kanzick/Kozu/releases"
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
