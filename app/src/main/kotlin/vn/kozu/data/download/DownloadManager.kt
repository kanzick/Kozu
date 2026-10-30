package vn.kozu.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import vn.kozu.data.local.DownloadedSet
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Mirrors
import vn.kozu.util.NetworkInfo
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

sealed interface DownloadStatus {
    data object Queued : DownloadStatus
    data class Running(val progress: Float, val source: String) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

private data class Source(
    val name: String,
    val url: String,
    val referer: String = "",
    val authorization: String = ""
)

class DownloadManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val settings: SettingsStore,
    private val store: DownloadsStore,
    private val auth: OsuAuth
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(2)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val titles = ConcurrentHashMap<Long, String>()

    private val _status = MutableStateFlow<Map<Long, DownloadStatus>>(emptyMap())
    val status: StateFlow<Map<Long, DownloadStatus>> = _status.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun titleOf(id: Long): String = titles[id].orEmpty()

    fun enqueue(beatmap: Beatmap, force: Boolean = false) {
        if (jobs[beatmap.id]?.isActive == true) return
        titles[beatmap.id] = beatmap.title
        setStatus(beatmap.id, DownloadStatus.Queued)
        startService()
        jobs[beatmap.id] = scope.launch {
            semaphore.withPermit { execute(beatmap, force) }
        }
    }

    fun enqueueAll(beatmaps: List<Beatmap>) {
        beatmaps.forEach { enqueue(it) }
    }

    fun cancel(id: Long) {
        jobs.remove(id)?.cancel()
        clearStatus(id)
    }

    fun dismiss(id: Long) = clearStatus(id)

    suspend fun delete(id: Long) {
        cancel(id)
        store.remove(id)
        store.folder(id).deleteRecursively()
    }

    suspend fun deleteAll() {
        jobs.keys.toList().forEach { cancel(it) }
        val ids = store.sets.first().map { it.beatmap.id }
        store.clear()
        ids.forEach { store.folder(it).deleteRecursively() }
    }

    private suspend fun execute(beatmap: Beatmap, force: Boolean) {
        val id = beatmap.id
        try {
            val existing = store.find(id)
            if (!force && existing != null && !existing.outdated) {
                clearStatus(id)
                return
            }

            val current = settings.settings.first()
            if (current.wifiOnlyDownloads && NetworkInfo.isMetered(appContext)) {
                fail(id, "Waiting for Wi-Fi (downloads are Wi-Fi only)")
                return
            }

            val sources = buildSources(current.activeMirrors.map { mirror ->
                Source(
                    name = mirror.name,
                    url = mirror.downloadFor(id),
                    referer = mirror.referer
                )
            }, id)

            if (sources.isEmpty()) {
                fail(id, "No download source is available")
                return
            }

            val temp = File(appContext.cacheDir, "downloads/$id.osz.part")
            temp.parentFile?.mkdirs()
            var usedSource = ""
            var lastError = "All download sources failed"

            for (source in sources) {
                currentCoroutineContext().ensureActive()
                setStatus(id, DownloadStatus.Running(0f, source.name))
                try {
                    fetch(source, temp) { progress ->
                        setStatus(id, DownloadStatus.Running(progress, source.name))
                    }

                    if (!OszExtractor.isZip(temp)) {
                        throw IOException("The server did not return a valid .osz archive")
                    }

                    usedSource = source.name
                    break
                } catch (e: CancellationException) {
                    temp.delete()
                    throw e
                } catch (e: Exception) {
                    lastError = "${source.name}: ${e.message ?: "download failed"}"
                    temp.delete()
                }
            }

            if (usedSource.isEmpty()) {
                fail(id, lastError)
                return
            }

            setStatus(id, DownloadStatus.Running(1f, "Extracting"))
            val destination = store.folder(id)
            val staging = File(destination.parentFile, "$id.tmp")
            staging.deleteRecursively()

            val result = OszExtractor.extract(temp, staging, current.keepFullBeatmap)
            destination.deleteRecursively()
            if (!staging.renameTo(destination)) {
                throw IOException("Could not store the beatmap files")
            }
            temp.delete()

            store.upsert(
                DownloadedSet(
                    beatmap = beatmap,
                    audioFile = result.audioFile,
                    backgroundFile = result.backgroundFile,
                    sizeBytes = result.sizeBytes,
                    downloadedAt = System.currentTimeMillis(),
                    source = usedSource
                )
            )

            clearStatus(id)
            _events.tryEmit("Downloaded ${beatmap.title} from $usedSource")
        } catch (e: CancellationException) {
            clearStatus(id)
            throw e
        } catch (e: Exception) {
            fail(id, e.message ?: "Download failed")
        } finally {
            jobs.remove(id)
        }
    }

    /**
     * Signed-in users try osu!'s official download endpoint first.
     * Guests go straight to the public mirrors. If the official endpoint
     * rejects the app's OAuth token or is unavailable, public mirrors are
     * always tried afterwards.
     */
    private suspend fun buildSources(mirrors: List<Source>, setId: Long): List<Source> {
        val sources = ArrayList<Source>(mirrors.size + 1)

        val userToken = try {
            auth.userBearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        if (!userToken.isNullOrBlank()) {
            sources += Source(
                name = "osu! Official",
                url = Mirrors.OFFICIAL_DOWNLOAD.replace("{id}", setId.toString()),
                authorization = "Bearer $userToken"
            )
        }

        sources.addAll(mirrors)
        return sources
    }

    private suspend fun fetch(
        source: Source,
        target: File,
        onProgress: (Float) -> Unit
    ) {
        val builder = Request.Builder()
            .url(source.url)
            .header("Accept", "application/octet-stream, application/zip, */*")
            .header("User-Agent", "Kozu-Android")

        if (source.referer.isNotBlank()) {
            builder.header("Referer", source.referer)
        }
        if (source.authorization.isNotBlank()) {
            // This bearer token is sent only to osu.ppy.sh. OkHttp removes
            // Authorization when following a redirect to a different host.
            builder.header("Authorization", source.authorization)
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }

            val type = response.header("Content-Type").orEmpty().lowercase()
            if (type.contains("text/html") || type.contains("json")) {
                throw IOException("Unexpected response type: $type")
            }

            val body = response.body
            val total = body.contentLength()
            var read = 0L
            var lastReport = 0L

            body.byteStream().use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break

                        output.write(buffer, 0, count)
                        read += count

                        val now = System.currentTimeMillis()
                        if (now - lastReport > 150L) {
                            lastReport = now
                            val progress = if (total > 0L) {
                                (read.toFloat() / total).coerceIn(0f, 0.97f)
                            } else {
                                -1f
                            }
                            onProgress(progress)
                        }
                    }
                }
            }

            if (read == 0L) {
                throw IOException("The server returned an empty file")
            }
        }
    }

    private fun fail(id: Long, message: String) {
        setStatus(id, DownloadStatus.Failed(message))
        _events.tryEmit("Couldn't download ${titles[id].orEmpty().ifBlank { "beatmap" }}")
    }

    private fun setStatus(id: Long, status: DownloadStatus) {
        _status.update { it + (id to status) }
    }

    private fun clearStatus(id: Long) {
        _status.update { it - id }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, DownloadService::class.java)
            )
        } catch (_: Exception) {
            // The download remains managed by this process even if foreground
            // service startup is unavailable on the current Android state.
        }
    }
}
