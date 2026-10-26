package vn.kozu.data.mirror

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.Mirrors
import vn.kozu.domain.model.StreamMode
import vn.kozu.util.NetworkInfo
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ResolvedAudio(val uri: Uri, val label: String)

class StreamResolver(
    private val context: Context,
    client: OkHttpClient,
    private val downloads: DownloadsStore,
    settings: SettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val probeClient = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private val dataClient = client.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    @Volatile private var current = AppSettings()
    @Volatile private var offline: Map<Long, String> = emptyMap()
    private val labels = ConcurrentHashMap<Long, String>()
    private val previews = ConcurrentHashMap<Long, ResolvedAudio>()
    private val streamDir = File(context.cacheDir, "stream").apply { mkdirs() }

    init {
        scope.launch { settings.settings.collect { current = it } }
        scope.launch {
            downloads.sets.collect { sets ->
                offline = sets.associate { it.beatmap.id to downloads.audioPath(it).absolutePath }
            }
        }
    }

    fun labelFor(id: Long): String? = labels[id]

    fun cacheBytes(): Long = streamDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearCache() {
        streamDir.listFiles()?.forEach { it.delete() }
        previews.clear()
    }

    fun resolve(id: Long): ResolvedAudio {
        offline[id]?.let { path ->
            val file = File(path)
            if (file.isFile && file.length() > 0L) return remember(id, ResolvedAudio(Uri.fromFile(file), "Offline"))
        }
        cachedFile(id)?.let { file ->
            file.setLastModified(System.currentTimeMillis())
            return remember(id, ResolvedAudio(Uri.fromFile(file), "Cached"))
        }
        previews[id]?.let { return remember(id, it) }

        val settings = current
        if (settings.wifiOnlyStreaming && NetworkInfo.isMetered(context)) {
            throw IOException("Streaming is set to Wi-Fi only")
        }

        if (settings.streamMode == StreamMode.FULL) {
            fullAudio(id, settings)?.let {
                previews[id] = it
                return remember(id, it)
            }
        }
        val preview = preview(id, settings) ?: throw IOException("No audio source available for this song")
        previews[id] = preview
        return remember(id, preview)
    }

    private fun remember(id: Long, resolved: ResolvedAudio): ResolvedAudio {
        labels[id] = resolved.label
        return resolved
    }

    private fun fullAudio(id: Long, settings: AppSettings): ResolvedAudio? {
        val mirrors = settings.activeMirrors
        val deadline = System.currentTimeMillis() + FULL_AUDIO_BUDGET_MS

        if (mirrors.any { it.id == "sayobot" }) {
            sayobotFull(id)?.let { return it }
        }
        for (mirror in mirrors.take(4)) {
            if (System.currentTimeMillis() > deadline) break
            zipAudio(id, mirror, settings)?.let { return it }
        }
        return null
    }

    private fun sayobotFull(id: Long): ResolvedAudio? {
        return try {
            val request = Request.Builder().url("https://api.sayobot.cn/v2/beatmapinfo?K=$id&T=0").build()
            val body = probeClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body.string()
            }
            val list = JSONObject(body).optJSONObject("data")?.optJSONArray("bid_data") ?: return null
            var audio = ""
            for (i in 0 until list.length()) {
                val name = list.optJSONObject(i)?.optString("audio").orEmpty()
                if (name.isNotBlank()) {
                    audio = name
                    break
                }
            }
            if (audio.isBlank()) return null
            val url = "https://dl.sayobot.cn/beatmaps/files/$id/${Uri.encode(audio)}"
            if (probeAudio(url, "")) ResolvedAudio(Uri.parse(url), "Sayobot · full song") else null
        } catch (e: Exception) {
            Log.d(TAG, "Sayobot full audio unavailable for $id: ${e.message}")
            null
        }
    }

    private fun zipAudio(id: Long, mirror: Mirror, settings: AppSettings): ResolvedAudio? {
        val url = mirror.downloadFor(id)
        return try {
            val probeSource = HttpRangeSource(probeClient, url, mirror.referer)
            val entries = RemoteZip.readEntries(probeSource)
            val audio = RemoteZip.pickAudio(entries) ?: return null
            val extension = audio.name.substringAfterLast('.', "mp3").lowercase()
            val part = File(streamDir, "$id.part")
            RemoteZip.extract(HttpRangeSource(dataClient, url, mirror.referer), audio, part)
            val target = File(streamDir, "$id.$extension")
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) {
                part.delete()
                return null
            }
            prune(settings.streamCacheMb)
            ResolvedAudio(Uri.fromFile(target), "${mirror.name} · full song")
        } catch (e: IOException) {
            Log.d(TAG, "${mirror.name} cannot stream $id: ${e.message}")
            null
        } catch (e: RuntimeException) {
            Log.d(TAG, "${mirror.name} failed for $id: ${e.message}")
            null
        }
    }

    private fun preview(id: Long, settings: AppSettings): ResolvedAudio? {
        val official = Mirrors.OFFICIAL_PREVIEW.replace("{id}", id.toString())
        if (probeAudio(official, "")) return ResolvedAudio(Uri.parse(official), "Preview · osu!")
        for (mirror in settings.activeMirrors) {
            val url = mirror.previewFor(id) ?: continue
            if (probeAudio(url, mirror.referer)) return ResolvedAudio(Uri.parse(url), "Preview · ${mirror.name}")
        }
        return null
    }

    private fun cachedFile(id: Long): File? =
        streamDir.listFiles { file -> file.name.startsWith("$id.") && !file.name.endsWith(".part") }
            ?.firstOrNull { it.length() > 0L }

    private fun prune(limitMb: Int) {
        val limit = (if (limitMb <= 0) 64 else limitMb) * 1024L * 1024L
        val files = streamDir.listFiles { file -> !file.name.endsWith(".part") }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= limit) break
            total -= file.length()
            file.delete()
        }
    }

    companion object {
        private const val TAG = "StreamResolver"
        private const val FULL_AUDIO_BUDGET_MS = 30_000L

        fun probeAudio(client: OkHttpClient, url: String, referer: String): Boolean {
            return try {
                val builder = Request.Builder().url(url).header("Range", "bytes=0-1")
                if (referer.isNotBlank()) builder.header("Referer", referer)
                client.newCall(builder.build()).execute().use { response ->
                    if (response.code != 200 && response.code != 206) return false
                    val type = response.header("Content-Type").orEmpty().lowercase()
                    type.startsWith("audio/") || type.contains("octet-stream") || type.contains("mpeg") || type.contains("ogg")
                }
            } catch (e: IOException) {
                false
            }
        }
    }

    private fun probeAudio(url: String, referer: String): Boolean = probeAudio(probeClient, url, referer)
}
