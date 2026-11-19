package vn.kozu.data.lazer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.CRC32
import java.util.zip.ZipFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import vn.kozu.R
import vn.kozu.data.download.DownloadManager
import vn.kozu.data.download.OszExtractor
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.LOCAL_BEATMAP_ID_BASE
import vn.kozu.game.OsuBeatmapParser
import vn.kozu.game.mania.previewstage.PreviewStageConfig
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.util.AppLocaleManager

data class LazerImportState(
    val running: Boolean = false,
    val scanning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val current: String = "",
    val imported: Int = 0,
    val replaced: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val problems: List<String> = emptyList(),
    val finished: Boolean = false,
) {
    val progress: Float get() = if (total <= 0) 0f else (processed.toFloat() / total).coerceIn(0f, 1f)
}

internal data class ArchiveInfo(
    val setId: Long,
    val title: String,
    val titleUnicode: String,
    val artist: String,
    val artistUnicode: String,
    val creator: String,
    val source: String,
    val tags: String,
    val modes: Set<Int>,
    val difficulties: Int,
) {
    val hasMania: Boolean get() = 3 in modes
}

class LazerImporter(
    context: Context,
    private val downloads: DownloadManager,
    private val store: DownloadsStore,
    private val settings: SettingsStore,
) {
    private data class Entry(
        val uri: Uri,
        val name: String,
    )

    private sealed interface Outcome {
        data class Imported(val replaced: Boolean) : Outcome

        data object Skipped : Outcome
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val queue = ArrayDeque<Entry>()
    private var worker: Job? = null
    private val workDir = File(appContext.cacheDir, WORK_DIRECTORY)

    private val _state = MutableStateFlow(LazerImportState())
    val state: StateFlow<LazerImportState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        scope.launch { clearStaleFiles() }
    }

    fun importUris(uris: List<Uri>) {
        val entries = uris.distinct().map { Entry(it, displayName(it)) }
        if (entries.isEmpty()) return
        enqueue(entries)
    }

    fun importTree(tree: Uri) {
        _state.update { current ->
            if (current.running) current else LazerImportState(scanning = true)
        }
        scope.launch {
            val found =
                try {
                    listArchives(tree)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not scan the selected folder", e)
                    emptyList()
                }
            if (found.isEmpty()) {
                _state.update { if (it.running) it else LazerImportState(finished = true) }
                _events.tryEmit(localized(R.string.lazer_no_archives_found))
            } else {
                enqueue(found)
            }
        }
    }

    fun clearResult() {
        _state.update { if (it.running || it.scanning) it else LazerImportState() }
    }

    private fun enqueue(entries: List<Entry>) {
        synchronized(lock) {
            queue.addAll(entries)
            val wasRunning = _state.value.running
            _state.update { current ->
                if (current.running) {
                    current.copy(total = current.total + entries.size)
                } else {
                    LazerImportState(running = true, total = entries.size)
                }
            }
            if (!wasRunning) _events.tryEmit(localized(R.string.lazer_import_started, entries.size))
            if (worker == null) worker = scope.launch { drain() }
        }
    }

    private suspend fun drain() {
        while (true) {
            val entry =
                synchronized(lock) {
                    val next = queue.removeFirstOrNull()
                    if (next == null) worker = null
                    next
                } ?: break
            _state.update { it.copy(current = entry.name) }
            val config = settings.settings.first()
            var problem: String? = null
            val outcome =
                try {
                    process(entry, config)
                } catch (e: CancellationException) {
                    synchronized(lock) { worker = null }
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not import ${entry.name}", e)
                    problem = "${entry.name}: ${e.message ?: e.javaClass.simpleName}"
                    null
                }
            val failure: String? = problem
            _state.update { current ->
                val next = current.copy(processed = current.processed + 1)
                when {
                    failure != null ->
                        next.copy(
                            failed = next.failed + 1,
                            problems = (next.problems + failure).takeLast(MAX_PROBLEMS),
                        )

                    outcome is Outcome.Imported ->
                        if (outcome.replaced) {
                            next.copy(replaced = next.replaced + 1)
                        } else {
                            next.copy(imported = next.imported + 1)
                        }

                    else -> next.copy(skipped = next.skipped + 1)
                }
            }
        }
        val summary = _state.updateAndGet { it.copy(running = false, current = "", finished = true) }
        _events.tryEmit(
            localized(
                R.string.lazer_import_summary,
                summary.imported + summary.replaced,
                summary.skipped,
                summary.failed,
            ),
        )
    }

    private suspend fun process(
        entry: Entry,
        config: AppSettings,
    ): Outcome {
        workDir.mkdirs()
        val pending = File(workDir, "pending-${System.nanoTime()}.osz")
        var staged = pending
        try {
            copyToTemp(entry.uri, pending)
            val info = inspect(pending)
            val id = if (info.setId > 0L) info.setId else localId(info)
            val existing = store.find(id)
            val present = existing != null && store.folder(id).isDirectory
            if (present && !config.lazerReplaceExisting) return Outcome.Skipped

            val renamed = File(workDir, "$id.osz")
            renamed.delete()
            if (pending.renameTo(renamed)) staged = renamed

            val previous = existing?.beatmap
            val beatmap =
                Beatmap(
                    id = id,
                    title = info.title.ifBlank { previous?.title.orEmpty().ifBlank { fallbackTitle(entry.name) } },
                    artist = info.artist.ifBlank { previous?.artist.orEmpty() },
                    mapper = info.creator.ifBlank { previous?.mapper.orEmpty() },
                    status = previous?.status.orEmpty(),
                    updatedAt = previous?.updatedAt ?: 0L,
                    modes = info.modes,
                    titleUnicode = info.titleUnicode.ifBlank { previous?.titleUnicode.orEmpty() },
                    artistUnicode = info.artistUnicode.ifBlank { previous?.artistUnicode.orEmpty() },
                    source = info.source,
                    tags = info.tags,
                )
            downloads.installArchive(beatmap, staged, SOURCE_NAME)
            if (config.lazerAddToGame && info.hasMania) addToGame(staged)
            return Outcome.Imported(replaced = present)
        } finally {
            pending.delete()
            staged.delete()
        }
    }

    private suspend fun addToGame(archive: File) {
        try {
            val module =
                PreviewStageModule.get(
                    appContext,
                    PreviewStageConfig(rootDir = PreviewStageModule.defaultRootDir(appContext)),
                )
            module.importArchive(archive)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not add the Mania charts to the game library", e)
        }
    }

    private fun copyToTemp(
        uri: Uri,
        target: File,
    ) {
        val input = appContext.contentResolver.openInputStream(uri) ?: throw IOException("Could not open the file")
        input.use { source ->
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ARCHIVE_BYTES) throw IOException("The archive is larger than 2 GB")
                    output.write(buffer, 0, count)
                }
            }
        }
        if (target.length() == 0L) throw IOException("The file is empty")
    }

    internal fun inspect(file: File): ArchiveInfo {
        if (!OszExtractor.isZip(file)) throw IOException("This is not a beatmap archive")
        var failure: Exception? = null
        for (charset in CHARSETS) {
            try {
                return inspectWith(file, charset)
            } catch (e: IllegalArgumentException) {
                failure = e
            }
        }
        throw IOException("Could not read the beatmap archive", failure)
    }

    private fun inspectWith(
        file: File,
        charset: Charset,
    ): ArchiveInfo {
        ZipFile(file, charset).use { zip ->
            val entries =
                zip
                    .entries()
                    .asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".osu", ignoreCase = true) }
                    .toList()
            if (entries.isEmpty()) throw IOException("No beatmap difficulties were found in this archive")
            val parsed =
                entries.mapNotNull { entry ->
                    val text =
                        zip.getInputStream(entry).use { readLimited(it) }
                            ?: return@mapNotNull null
                    runCatching { OsuBeatmapParser.parse(entry.name, text) }.getOrNull()
                }
            if (parsed.isEmpty()) throw IOException("No readable beatmap difficulties were found")
            fun pick(selector: (vn.kozu.game.OsuDifficulty) -> String) =
                parsed.map(selector).firstOrNull { it.isNotBlank() }.orEmpty()
            return ArchiveInfo(
                setId = parsed.map { it.beatmapSetId }.firstOrNull { it > 0L } ?: 0L,
                title = pick { it.title },
                titleUnicode = pick { it.titleUnicode },
                artist = pick { it.artist },
                artistUnicode = pick { it.artistUnicode },
                creator = pick { it.creator },
                source = pick { it.source },
                tags = pick { it.tags },
                modes = parsed.map { it.mode }.filter { it in 0..3 }.toSet(),
                difficulties = parsed.size,
            )
        }
    }

    private fun readLimited(input: java.io.InputStream): String? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_OSU_BYTES) return null
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun localId(info: ArchiveInfo): Long {
        val crc = CRC32()
        crc.update("${info.artist}|${info.title}|${info.creator}".lowercase().toByteArray(Charsets.UTF_8))
        return LOCAL_BEATMAP_ID_BASE + (crc.value and 0xFFFFFFFFL)
    }

    private fun fallbackTitle(fileName: String): String =
        fileName
            .substringBeforeLast('.')
            .replaceFirst(Regex("^\\d+\\s+"), "")
            .ifBlank { fileName }

    private fun displayName(uri: Uri): String {
        runCatching {
            appContext.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "beatmap.osz"
    }

    private fun listArchives(tree: Uri): List<Entry> {
        val found = ArrayList<Entry>()

        fun walk(
            documentId: String,
            depth: Int,
        ) {
            if (depth > MAX_SCAN_DEPTH || found.size >= MAX_SCAN_RESULTS) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
            val folders = ArrayList<String>()
            appContext.contentResolver
                .query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    while (cursor.moveToNext() && found.size < MAX_SCAN_RESULTS) {
                        val id = cursor.getString(0) ?: continue
                        val name = cursor.getString(1) ?: continue
                        val mime = cursor.getString(2).orEmpty()
                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR -> folders += id
                            name.endsWith(".osz", ignoreCase = true) ->
                                found += Entry(DocumentsContract.buildDocumentUriUsingTree(tree, id), name)
                        }
                    }
                }
            folders.forEach { walk(it, depth + 1) }
        }

        walk(DocumentsContract.getTreeDocumentId(tree), 0)
        return found.sortedBy { it.name.lowercase() }
    }

    private fun clearStaleFiles() {
        val cutoff = System.currentTimeMillis() - STALE_AFTER_MS
        workDir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private fun localized(
        resource: Int,
        vararg args: Any,
    ): String = AppLocaleManager.wrapContext(appContext).getString(resource, *args)

    private companion object {
        const val TAG = "LazerImporter"
        const val SOURCE_NAME = "osu!lazer"
        const val WORK_DIRECTORY = "lazer-import"
        const val MAX_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_OSU_BYTES = 8L * 1024L * 1024L
        const val MAX_PROBLEMS = 6
        const val MAX_SCAN_DEPTH = 6
        const val MAX_SCAN_RESULTS = 5_000
        const val STALE_AFTER_MS = 60L * 60L * 1000L
        val CHARSETS: List<Charset> =
            listOf(Charsets.UTF_8, Charset.forName("windows-1252"), Charset.forName("Shift_JIS"))
    }
}
