package vn.kozu.data.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import vn.kozu.data.local.SettingsStore
import java.io.IOException

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

    val currentVersion: String
        get() = installedVersion(appContext)

    suspend fun check(): UpdateResult =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github+json")
                    .build()
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 404 -> {
                            UpdateResult.UpToDate(currentVersion)
                        }

                        response.code == 403 || response.code == 429 -> {
                            UpdateResult.Failed("GitHub is rate limiting requests. Try again later.")
                        }

                        !response.isSuccessful -> {
                            UpdateResult.Failed("GitHub returned HTTP ${response.code}")
                        }

                        else -> {
                            parse(response.body.string())
                        }
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

    private fun parse(body: String): UpdateResult {
        val root =
            try {
                JSONObject(body)
            } catch (e: Exception) {
                return UpdateResult.Failed("Unexpected response from GitHub")
            }
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return UpdateResult.UpToDate(currentVersion)
        val tag = root.optString("tag_name").trim()
        if (tag.isEmpty()) return UpdateResult.UpToDate(currentVersion)
        val url = root.optString("html_url").ifBlank { RELEASES_URL }
        val latest = tag.removePrefix("v").removePrefix("V")
        return if (compareVersions(latest, currentVersion) > 0) {
            UpdateResult.Available(latest, url, root.optString("body").trim())
        } else {
            UpdateResult.UpToDate(currentVersion)
        }
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/kanzick/Kozu/releases/latest"
        const val RELEASES_URL = "https://github.com/kanzick/Kozu/releases/latest"
        const val DAY_MS = 24L * 60L * 60L * 1000L

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
    val result = check()
    if (result is UpdateResult.Failed) return null
    settings.setLastUpdateCheck(now)
    return result
}
