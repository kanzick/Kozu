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

private class Source(
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

            val sources = buildSources(current.officialDownload, current.activeMirrors.map {
                Source(it.name, it.downloadFor(id), it.referer)
            }, id)

            val temp = File(appContext.cacheDir, "downloads/$id.osz.part")
            temp.parentFile?.mkdirs()
            var usedSource = ""
            var lastError = "No download source is enabled"
            for (source in sources) {
                setStatus(id, DownloadStatus.Running(0f, source.name))
                try {
                    fetch(source, temp) { progress -> setStatus(id, DownloadStatus.Running(progress, source.name)) }
                    if (!OszExtractor.isZip(temp)) throw IOException("not a beatmap archive")
                    usedSource = source.name
                    break
                } catch (e: CancellationException) {
                    temp.delete()
                    throw e
                } catch (e: Exception) {
                    lastError = "${source.name}: ${e.message ?: "failed"}"
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
            val result = OszExtractor.extract(temp, staging, current.keepFullBeatmap)
            destination.deleteRecursively()
            if (!staging.renameTo(destination)) throw IOException("Could not store the beatmap files")
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
            _events.tryEmit("Downloaded ${beatmap.title}")
        } catch (e: CancellationException) {
            clearStatus(id)
            throw e
        } catch (e: Exception) {
            fail(id, e.message ?: "Download failed")
        } finally {
            jobs.remove(id)
        }
    }

    private suspend fun buildSources(official: Boolean, mirrors: List<Source>, id: Long): List<Source> {
        val list = ArrayList<Source>()
        if (official) {
            val token = try {
                auth.userBearer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (token != null) {
                list.add(
                    Source(
                        name = "osu! (official)",
                        url = Mirrors.OFFICIAL_DOWNLOAD.replace("{id}", id.toString()),
                        authorization = "Bearer $token"
                    )
                )
            }
        }
        list.addAll(mirrors)
        return list
    }

    private suspend fun fetch(source: Source, target: File, onProgress: (Float) -> Unit) {
        val builder = Request.Builder().url(source.url)
        if (source.referer.isNotBlank()) builder.header("Referer", source.referer)
        if (source.authorization.isNotBlank()) builder.header("Authorization", source.authorization)
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val type = response.header("Content-Type").orEmpty().lowercase()
            if (type.contains("text/html") || type.contains("json")) throw IOException("unexpected response ($type)")
            val body = response.body
            val total = body.contentLength()
            var read = 0L
            var lastReport = 0L
            body.byteStream().use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 150L) {
                            lastReport = now
                            onProgress(if (total > 0L) (read.toFloat() / total).coerceIn(0f, 0.97f) else -1f)
                        }
                    }
                }
            }
        }
    }

    private fun fail(id: Long, message: String) {
        setStatus(id, DownloadStatus.Failed(message))
        _events.tryEmit("Couldn't download ${titles[id].orEmpty().ifBlank { "song" }}")
    }

    private fun setStatus(id: Long, status: DownloadStatus) {
        _status.update { it + (id to status) }
    }

    private fun clearStatus(id: Long) {
        _status.update { it - id }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(appContext, Intent(appContext, DownloadService::class.java))
        } catch (e: Exception) {
        }
    }
}
