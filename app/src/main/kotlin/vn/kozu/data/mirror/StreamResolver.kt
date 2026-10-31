package vn.kozu.data.mirror

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
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
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Identifies the selected provider so the player can retry without reusing it. */
data class ResolvedAudio(
    val uri: Uri,
    val label: String,
    val sourceId: String = "unknown",
    val requestHeaders: Map<String, String> = emptyMap()
)

private data class ZipCandidate(
    val mirror: Mirror,
    val url: String,
    val audio: ZipEntryInfo
)

data class ArchiveAudioSession(
    val input: java.io.InputStream,
    val expectedLength: Long,
    val temporaryFile: File,
    val cacheFile: File
)

/**
 * Resolves full audio from several independent sources.
 *
 * Hinai is one provider, not the only provider: the resolver also checks
 * Sayobot's direct audio endpoint and the .osz archives from osu.direct,
 * Nekoha, Mirria, osu!dl, Mino/catboy, Sayobot and Beatconnect. Hinai's
 * JSON cascade can also choose a healthy partner URL without proxying all
 * archive bytes through Hinai. ZIP audio is inflated incrementally by Media3.
 */
class StreamResolver(
    private val context: Context,
    client: OkHttpClient,
    private val downloads: DownloadsStore,
    settings: SettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val probeClient = client.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val lookupClient = client.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // The read timeout is an idle-between-reads limit, not a whole-track
    // deadline. Disable callTimeout here: a complete song can legitimately
    // take several minutes to stream and must not be cut off at 60 seconds.
    private val dataClient = client.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // These workers only perform short HTTP Range probes. Daemon threads
    // prevent an app process from being kept alive by an idle executor.
    private val zipProbeExecutor = Executors.newFixedThreadPool(
        MAX_PARALLEL_PROVIDERS
    ) { task ->
        Thread(task, "Kozu-AudioProvider").apply { isDaemon = true }
    }

    @Volatile
    private var current = AppSettings()

    @Volatile
    private var offline: Map<Long, String> = emptyMap()

    @Volatile
    private var preferredZipMirrorId: String? = null

    private val labels = ConcurrentHashMap<Long, String>()
    private val lastResolved = ConcurrentHashMap<Long, ResolvedAudio>()
    private val fullUrlCache = ConcurrentHashMap<Long, ResolvedAudio>()
    private val previewCache = ConcurrentHashMap<Long, ResolvedAudio>()
    private val fullMissUntil = ConcurrentHashMap<Long, Long>()
    private val failedSources = ConcurrentHashMap<Long, MutableSet<String>>()
    private val archiveCandidates = ConcurrentHashMap<String, ZipCandidate>()

    private val streamDir = File(context.cacheDir, "stream").apply { mkdirs() }

    init {
        scope.launch {
            settings.settings.collect { next ->
                val previous = current
                if (
                    previous.streamMode != next.streamMode ||
                    previous.disabledMirrors != next.disabledMirrors ||
                    previous.customMirrors != next.customMirrors
                ) {
                    fullUrlCache.clear()
                    previewCache.clear()
                    fullMissUntil.clear()
                    failedSources.clear()
                    lastResolved.clear()
                    archiveCandidates.clear()
                }
                current = next
            }
        }

        scope.launch {
            downloads.sets.collect { sets ->
                offline = sets.associate {
                    it.beatmap.id to downloads.audioPath(it).absolutePath
                }
            }
        }
    }

    fun labelFor(id: Long): String? = labels[id]

    fun cacheBytes(): Long = streamDir.listFiles()?.sumOf { it.length() } ?: 0L

    /** Clears transient streamed files only; offline downloads live elsewhere. */
    fun clearCache() {
        streamDir.listFiles()?.forEach { it.deleteRecursively() }
        fullUrlCache.clear()
        previewCache.clear()
        fullMissUntil.clear()
        failedSources.clear()
        lastResolved.clear()
        archiveCandidates.clear()
        labels.clear()
    }

    /** Called by PlaybackController after Media3 reports that the chosen source failed. */
    fun markCurrentSourceFailed(id: Long): Boolean {
        val resolved = lastResolved.remove(id) ?: return false
        val failed = failedSources.computeIfAbsent(id) {
            Collections.synchronizedSet(mutableSetOf())
        }
        failed.add(resolved.sourceId)
        fullUrlCache.remove(id)
        archiveCandidates.remove(archiveKey(id, resolved.sourceId))
        fullMissUntil.remove(id)

        if (resolved.uri.scheme == "file") {
            val path = resolved.uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.parentFile?.canonicalFile == streamDir.canonicalFile) {
                    file.delete()
                }
            }
        } else if (resolved.uri.scheme == ZIP_AUDIO_SCHEME) {
            streamDir.listFiles { file ->
                file.name.startsWith("$id-${resolved.sourceId}.")
            }?.forEach { it.delete() }
        }

        Log.w(TAG, "Marking ${resolved.sourceId} as failed for beatmap $id")
        return true
    }

    /**
     * FULL mode returns a real full-length audio source or throws. It never
     * silently switches to the 30-second osu! preview.
     */
    fun resolve(id: Long): ResolvedAudio {
        val settings = current
        if (settings.wifiOnlyStreaming && NetworkInfo.isMetered(context)) {
            throw IOException("Streaming is set to Wi-Fi only")
        }

        if (settings.streamMode == StreamMode.PREVIEW) {
            previewCache[id]?.let { return remember(id, it) }
            val preview = preview(id, settings)
                ?: throw IOException("No preview source is available for this song")
            previewCache[id] = preview
            return remember(id, preview)
        }

        val excluded = failedSources[id] ?: emptySet()

        if ("offline" !in excluded) {
            offline[id]?.let { path ->
                val file = File(path)
                if (file.isFile && file.length() > 0L) {
                    return remember(
                        id,
                        ResolvedAudio(Uri.fromFile(file), "Offline · full song", "offline")
                    )
                }
            }
        }

        fullUrlCache[id]?.let { cached ->
            if (cached.sourceId !in excluded) return remember(id, cached)
        }

        cachedFile(id)?.let { file ->
            val sourceId = sourceIdFromCachedFile(id, file)
            if (sourceId !in excluded) {
                file.setLastModified(System.currentTimeMillis())
                return remember(
                    id,
                    ResolvedAudio(Uri.fromFile(file), "Cached · full song", sourceId)
                )
            }
        }

        val now = System.currentTimeMillis()
        val retryAt = fullMissUntil[id] ?: 0L
        if (now < retryAt) {
            val remaining = ((retryAt - now) / 1000L).coerceAtLeast(1L)
            throw IOException("Full song sources recently failed. Retry in about $remaining seconds.")
        }
        if (retryAt != 0L) {
            fullMissUntil.remove(id)
            failedSources.remove(id)
        }

        val full = fullAudio(id, settings, failedSources[id].orEmpty())
        if (full != null) {
            fullMissUntil.remove(id)
            previewCache.remove(id)
            if (full.uri.scheme != "file") {
                fullUrlCache[id] = full
            }
            return remember(id, full)
        }

        fullMissUntil[id] = System.currentTimeMillis() + FULL_RETRY_COOLDOWN_MS
        throw IOException(
            "Full song is unavailable from the enabled mirrors. Preview was not started."
        )
    }

    fun resolvePreview(id: Long): ResolvedAudio {
        val settings = current
        if (settings.wifiOnlyStreaming && NetworkInfo.isMetered(context)) {
            throw IOException("Streaming is set to Wi-Fi only")
        }
        previewCache[id]?.let { return remember(id, it) }
        val resolved = preview(id, settings)
            ?: throw IOException("No preview source is available for this map")
        previewCache[id] = resolved
        return remember(id, resolved)
    }

    private fun remember(id: Long, resolved: ResolvedAudio): ResolvedAudio {
        labels[id] = resolved.label
        lastResolved[id] = resolved
        return resolved
    }

    private fun fullAudio(
        id: Long,
        settings: AppSettings,
        excluded: Set<String>
    ): ResolvedAudio? {
        val mirrors = settings.activeMirrors.filter { it.id !in excluded }
        if (mirrors.isEmpty() && excluded.isNotEmpty()) return null

        // Race every independent provider together. Previously the resolver
        // spent up to 12 seconds checking direct audio, then another 8 seconds
        // probing ZIP mirrors. This single deadline prevents those budgets from
        // stacking and lets a responsive archive mirror win immediately.
        val resolutionStartedAt = System.nanoTime()
        val tasks = ArrayList<Callable<ResolvedAudio?>>()
        val activeIds = mirrors.mapTo(mutableSetOf()) { it.id }

        if ("hinai-audio" !in excluded && "hinai" in activeIds) {
            tasks += Callable { hinaiCachedFullAudio(id) }
        }
        if ("sayobot-direct" !in excluded && "sayobot" in activeIds) {
            tasks += Callable { sayobotFull(id) }
        }
        // Hinai's unmetered JSON cascade selects a live partner (usually
        // osu.direct, with Nekoha/other partners as fallbacks) and returns
        // its direct .osz URL. We then range-stream that partner URL ourselves.
        // This uses Hinai as a routing hint, not as the only byte-stream host.
        if ("hinai-cascade" !in excluded && "hinai" in activeIds) {
            tasks += Callable { hinaiCascadeFull(id, mirrors) }
        }

        val orderedMirrors = mirrors.sortedBy {
            if (it.id == preferredZipMirrorId) 0 else 1
        }
        orderedMirrors.forEach { mirror ->
            if (mirror.id !in excluded) {
                tasks += Callable {
                    val candidate = probeZipCandidate(id, mirror) ?: return@Callable null
                    archiveCandidates[archiveKey(id, mirror.id)] = candidate
                    ResolvedAudio(
                        uri = Uri.Builder()
                            .scheme(ZIP_AUDIO_SCHEME)
                            .authority(id.toString())
                            .appendQueryParameter("source", mirror.id)
                            .build(),
                        label = "${mirror.name} · streaming full song",
                        sourceId = mirror.id
                    )
                }
            }
        }

        if (tasks.isEmpty()) return null

        val completion = ExecutorCompletionService<ResolvedAudio?>(zipProbeExecutor)
        val futures = tasks.map { completion.submit(it) }
        val deadline = System.nanoTime() +
            TimeUnit.MILLISECONDS.toNanos(PLAYBACK_RESOLVE_BUDGET_MS)
        var completed = 0

        while (completed < futures.size) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) break

            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            completed++

            val result = try {
                finished.get()
            } catch (e: Exception) {
                Log.d(TAG, "Audio provider failed for $id: ${e.message}")
                null
            }

            if (result != null) {
                futures.forEach { if (!it.isDone) it.cancel(true) }
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - resolutionStartedAt
                )
                Log.i(
                    TAG,
                    "Resolved full song $id from ${result.sourceId} in ${elapsedMs}ms"
                )
                return result
            }
        }

        futures.forEach { if (!it.isDone) it.cancel(true) }
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime() - resolutionStartedAt
        )
        Log.w(TAG, "No full-song source resolved for $id within ${elapsedMs}ms")
        return null
    }

    private fun archiveKey(id: Long, sourceId: String): String = "$id:$sourceId"

    /**
     * Opens the selected ZIP member as a streaming input. The compressed audio
     * bytes are fetched with HTTP Range and inflated as Media3 consumes them.
     */
    fun openArchiveAudio(id: Long, sourceId: String): ArchiveAudioSession {
        val candidate = archiveCandidates[archiveKey(id, sourceId)]
            ?: throw IOException("Archive source expired; resolving it again")

        val source = HttpRangeSource(
            dataClient,
            candidate.url,
            candidate.mirror.referer
        )
        val input = RemoteZip.openEntryStream(source, candidate.audio)
        val extension = candidate.audio.name
            .substringAfterLast('.', "mp3")
            .lowercase()
            .ifBlank { "mp3" }
        val temporary = File(streamDir, "$id-$sourceId.stream.part")
        val target = File(streamDir, "$id-$sourceId.$extension")
        temporary.parentFile?.mkdirs()
        temporary.delete()

        return ArchiveAudioSession(
            input = input,
            expectedLength = candidate.audio.uncompressedSize,
            temporaryFile = temporary,
            cacheFile = target
        )
    }

    /** Promote a fully streamed archive member into the local stream cache. */
    fun completeArchiveAudio(
        id: Long,
        sourceId: String,
        temporary: File,
        target: File,
        bytesWritten: Long,
        expectedLength: Long
    ): Boolean {
        if (
            expectedLength <= 0L ||
            bytesWritten != expectedLength ||
            temporary.length() != expectedLength ||
            !isAudioFile(temporary)
        ) {
            temporary.delete()
            return false
        }

        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.delete()
            return false
        }

        target.setLastModified(System.currentTimeMillis())
        preferredZipMirrorId = sourceId
        prune(current.streamCacheMb)
        Log.i(TAG, "Full song $id cached after streaming from $sourceId")
        return true
    }

    /** Hinai Music API is used only when it explicitly confirms a cached full track. */
    private fun hinaiCachedFullAudio(id: Long): ResolvedAudio? {
        return try {
            val request = Request.Builder()
                .url("https://mirror.hinamizawa.ai/v3/osu/music/song/$id")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val metadata = lookupClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string().orEmpty()
            }
            if (metadata.isBlank()) return null

            val root = JSONObject(metadata)
            val song = root.optJSONObject("song")
                ?: root.optJSONObject("data")
                ?: root
            if (!song.optBoolean("audio_cached", false)) return null

            val url = "https://mirror.hinamizawa.ai/v3/osu/music/audio/$id"
            // audio_cached is the authoritative full-track flag. Skip an
            // extra Range probe here: cached Hinai songs are already the fast
            // path and should not pay another network round-trip.
            ResolvedAudio(Uri.parse(url), "Hinai · cached full song", "hinai-audio")
        } catch (e: Exception) {
            Log.d(TAG, "Hinai has no verified cached full audio for $id: ${e.message}")
            null
        }
    }

    /** Sayobot's metadata endpoint returns the real audio filename for a set. */
    private fun sayobotFull(id: Long): ResolvedAudio? {
        return try {
            val request = Request.Builder()
                .url("https://api.sayobot.cn/v2/beatmapinfo?K=$id&T=0")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val body = lookupClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string().orEmpty()
            }

            val list = JSONObject(body)
                .optJSONObject("data")
                ?.optJSONArray("bid_data") ?: return null

            val audio = (0 until list.length())
                .asSequence()
                .mapNotNull { list.optJSONObject(it)?.optString("audio") }
                .firstOrNull { it.isNotBlank() && it != "null" }
                ?: return null

            val url = "https://dl.sayobot.cn/beatmaps/files/$id/${Uri.encode(audio)}"
            // Verify the direct URL actually returns audio (not a CDN error page).
            // This tiny Range probe runs inside the same global provider deadline.
            if (!probeAudio(url, "")) return null
            ResolvedAudio(Uri.parse(url), "Sayobot · full song", "sayobot-direct")
        } catch (e: Exception) {
            Log.d(TAG, "Sayobot direct audio unavailable for $id: ${e.message}")
            null
        }
    }

    private fun hinaiCascadeFull(
        id: Long,
        activeMirrors: List<Mirror>
    ): ResolvedAudio? {
        return try {
            val request = Request.Builder()
                .url("https://mirror.hinamizawa.ai/d/$id?no_video=true")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val json = lookupClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string().orEmpty()
            }
            if (json.isBlank()) return null

            val root = JSONObject(json)
            if (!root.optBoolean("success", false)) return null
            val url = root.optString("download_url").takeIf { it.startsWith("https://") }
                ?: return null
            val reportedSource = root.optString("source").lowercase()
            val sourceId = when (reportedSource) {
                "osu.direct", "osu!direct", "osudirect" -> "osudirect"
                "nekoha" -> "nekoha"
                "lisek", "mirria" -> "mirria"
                "osudl", "osu!dl" -> "osudl"
                "catboy", "mino" -> "catboy"
                "sayobot" -> "sayobot"
                "beatconnect" -> "beatconnect"
                "hinai", "local" -> "hinai"
                else -> return null
            }
            if (sourceId in failedSources[id].orEmpty()) return null
            val mirror = activeMirrors.firstOrNull { it.id == sourceId } ?: return null
            val candidate = probeZipCandidate(id, mirror, url) ?: return null
            archiveCandidates[archiveKey(id, sourceId)] = candidate

            ResolvedAudio(
                uri = Uri.Builder()
                    .scheme(ZIP_AUDIO_SCHEME)
                    .authority(id.toString())
                    .appendQueryParameter("source", sourceId)
                    .build(),
                label = "${mirror.name} · full song",
                sourceId = sourceId
            )
        } catch (e: Exception) {
            Log.d(TAG, "Hinai cascade could not select a stream for $id: ${e.message}")
            null
        }
    }

    private fun probeZipCandidate(id: Long, mirror: Mirror): ZipCandidate? =
        probeZipCandidate(id, mirror, mirror.downloadFor(id))

    private fun probeZipCandidate(
        id: Long,
        mirror: Mirror,
        url: String
    ): ZipCandidate? {
        return try {
            val source = HttpRangeSource(probeClient, url, mirror.referer)
            val entries = RemoteZip.readEntries(source)
            val audio = RemoteZip.pickAudio(entries) ?: return null
            if (audio.compressedSize <= 0L || audio.uncompressedSize <= 0L) return null
            ZipCandidate(mirror, url, audio)
        } catch (e: Exception) {
            Log.d(TAG, "${mirror.name} range probe failed for $id: ${e.message}")
            null
        }
    }

    private fun isAudioFile(file: File): Boolean {
        if (!file.isFile || file.length() < 4L) return false
        val bytes = ByteArray(16)
        val count = try {
            file.inputStream().use { it.read(bytes) }
        } catch (_: IOException) {
            return false
        }
        return hasAudioSignature(bytes, count)
    }

    private fun preview(id: Long, settings: AppSettings): ResolvedAudio? {
        val official = Mirrors.OFFICIAL_PREVIEW.replace("{id}", id.toString())
        if (probeAudio(official, "")) {
            return ResolvedAudio(Uri.parse(official), "Preview · osu!", "osu-preview")
        }

        for (mirror in settings.activeMirrors) {
            val url = mirror.previewFor(id) ?: continue
            if (probeAudio(url, mirror.referer)) {
                val headers = mirror.referer.takeIf { it.isNotBlank() }
                    ?.let { mapOf("Referer" to it) }
                    .orEmpty()
                return ResolvedAudio(
                    Uri.parse(url),
                    "Preview · ${mirror.name}",
                    "preview:${mirror.id}",
                    headers
                )
            }
        }
        return null
    }

    private fun cachedFile(id: Long): File? = streamDir.listFiles { file ->
        (file.name.startsWith("$id-") || file.name.startsWith("$id.")) &&
            !file.name.endsWith(".part") && !file.name.endsWith(".osz")
    }?.firstOrNull { it.isFile && it.length() > 0L }

    private fun sourceIdFromCachedFile(id: Long, file: File): String {
        val prefix = "$id-"
        return if (file.name.startsWith(prefix)) {
            file.name.removePrefix(prefix).substringBeforeLast('.')
        } else {
            "zip-cache"
        }
    }

    private fun prune(limitMb: Int) {
        val limit = (if (limitMb <= 0) 64 else limitMb) * 1024L * 1024L
        val files = streamDir.listFiles { file ->
            !file.name.endsWith(".part") && !file.name.endsWith(".osz")
        }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= limit) break
            total -= file.length()
            file.delete()
        }
    }

    companion object {
        private const val TAG = "StreamResolver"
        private const val USER_AGENT = "Kozu/2.5 (Android; https://github.com/kanzick/Kozu)"
        const val ZIP_AUDIO_SCHEME = "kozu-zipaudio"
        private const val PLAYBACK_RESOLVE_BUDGET_MS = 4_500L
        private const val MAX_PARALLEL_PROVIDERS = 11
        private const val FULL_RETRY_COOLDOWN_MS = 12_000L

        fun probeAudio(client: OkHttpClient, url: String, referer: String): Boolean {
            return try {
                val builder = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-15")
                    .header("Accept", "audio/*, application/octet-stream, */*")
                if (referer.isNotBlank()) builder.header("Referer", referer)

                client.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) return false
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    if (
                        contentType.contains("text/html") ||
                        contentType.contains("application/json") ||
                        contentType.startsWith("text/")
                    ) return false

                    val body = response.body ?: return false
                    val prefix = ByteArray(16)
                    val count = body.byteStream().use { input ->
                        var total = 0
                        while (total < prefix.size) {
                            val read = input.read(prefix, total, prefix.size - total)
                            if (read < 0) break
                            total += read
                        }
                        total
                    }
                    if (count < 4) return false

                    // Do not trust Content-Type alone. A few mirrors return
                    // error pages with an audio MIME type; validate the actual
                    // bytes before exposing that URL to Media3.
                    hasAudioSignature(prefix, count)
                }
            } catch (_: IOException) {
                false
            }
        }

        private fun hasAudioSignature(bytes: ByteArray, count: Int): Boolean {
            if (count < 4) return false
            fun ascii(value: String, offset: Int = 0): Boolean {
                if (count < offset + value.length) return false
                return value.indices.all { bytes[offset + it].toInt().toChar() == value[it] }
            }

            // ID3/MP3, Ogg/Opus, FLAC, WAV, MP4/M4A and AAC/ADTS.
            if (ascii("ID3") || ascii("OggS") || ascii("fLaC") || ascii("RIFF")) return true
            if (ascii("ftyp", 4)) return true
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            return first == 0xFF && (second and 0xE0) == 0xE0
        }
    }

    private fun probeAudio(url: String, referer: String): Boolean =
        probeAudio(probeClient, url, referer)
}
