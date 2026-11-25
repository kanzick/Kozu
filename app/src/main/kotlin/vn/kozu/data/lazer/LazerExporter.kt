package vn.kozu.data.lazer

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import vn.kozu.data.local.DownloadsStore
import vn.kozu.domain.model.LOCAL_BEATMAP_ID_BASE
import vn.kozu.game.mania.previewstage.PreviewStageConfig
import vn.kozu.game.mania.previewstage.PreviewStageModule

const val LAZER_PACKAGE = "sh.ppy.osulazer"

data class ExportCandidate(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String,
    val folder: File,
    val sizeBytes: Long,
    val fromGame: Boolean,
) {
    val isOnline: Boolean get() = id in 1 until LOCAL_BEATMAP_ID_BASE
}

class LazerExporter(
    context: Context,
    private val store: DownloadsStore,
) {
    private val appContext = context.applicationContext
    private val exportDir = File(appContext.cacheDir, EXPORT_DIRECTORY)

    fun isLazerInstalled(): Boolean =
        try {
            appContext.packageManager.getPackageInfo(LAZER_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }

    suspend fun candidates(includeGame: Boolean): List<ExportCandidate> =
        withContext(Dispatchers.IO) {
            val result = LinkedHashMap<Long, ExportCandidate>()
            store.sets.first().forEach { set ->
                val folder = store.folder(set.beatmap.id)
                if (!folder.isDirectory) return@forEach
                result[set.beatmap.id] =
                    ExportCandidate(
                        id = set.beatmap.id,
                        title = set.beatmap.title,
                        artist = set.beatmap.artist,
                        mapper = set.beatmap.mapper,
                        folder = folder,
                        sizeBytes = set.sizeBytes,
                        fromGame = false,
                    )
            }
            if (includeGame) {
                try {
                    val module =
                        PreviewStageModule.get(
                            appContext,
                            PreviewStageConfig(rootDir = PreviewStageModule.defaultRootDir(appContext)),
                        )
                    module.start()
                    module.catalog.all().forEach { song ->
                        if (song.songId in result || !song.folder.isDirectory) return@forEach
                        val first = song.difficulties.firstOrNull()
                        result[song.songId] =
                            ExportCandidate(
                                id = song.songId,
                                title = first?.title.orEmpty().ifBlank { song.displayTitle },
                                artist = first?.artist.orEmpty().ifBlank { song.displayArtist },
                                mapper = first?.creator.orEmpty(),
                                folder = song.folder,
                                sizeBytes = song.folder.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                                fromGame = true,
                            )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
            result.values.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        }

    suspend fun build(candidate: ExportCandidate): File =
        withContext(Dispatchers.IO) {
            exportDir.mkdirs()
            val target = uniqueFile(fileName(candidate))
            val partial = File(exportDir, "${target.name}.part")
            try {
                writeArchive(candidate.folder, partial)
                if (!partial.renameTo(target)) throw IOException("Could not create ${target.name}")
                target
            } finally {
                partial.delete()
            }
        }

    suspend fun saveToFolder(
        tree: Uri,
        candidates: List<ExportCandidate>,
        onProgress: (Int) -> Unit,
    ): Int =
        withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            val parent =
                DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            var saved = 0
            for (candidate in candidates) {
                currentCoroutineContext().ensureActive()
                val archive = build(candidate)
                try {
                    val document =
                        DocumentsContract.createDocument(resolver, parent, "application/octet-stream", archive.name)
                            ?: throw IOException("Could not create ${archive.name}")
                    val output =
                        resolver.openOutputStream(document, "w")
                            ?: throw IOException("Could not write ${archive.name}")
                    output.use { sink -> archive.inputStream().use { it.copyTo(sink, 64 * 1024) } }
                } finally {
                    archive.delete()
                }
                saved++
                onProgress(saved)
            }
            saved
        }

    fun shareIntent(
        files: List<File>,
        chooserTitle: String,
        preferLazer: Boolean,
    ): Intent {
        require(files.isNotEmpty())
        val authority = "${appContext.packageName}.fileprovider"
        val uris = files.map { FileProvider.getUriForFile(appContext, authority, it) }
        val send =
            if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        send.type = "application/zip"
        send.clipData =
            ClipData.newRawUri("beatmaps", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (preferLazer && isLazerInstalled()) {
            return send.setPackage(LAZER_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun launch(
        context: Context,
        intent: Intent,
    ): Boolean =
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }

    fun cleanup(maxAgeMs: Long = 0L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        exportDir.listFiles()?.forEach { if (maxAgeMs <= 0L || it.lastModified() < cutoff) it.delete() }
    }

    private fun writeArchive(
        folder: File,
        target: File,
    ) {
        val files = folder.walkTopDown().filter { it.isFile && !it.name.startsWith(".tmp_") }.toList()
        if (files.isEmpty()) throw IOException("The beatmap folder is empty")
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            val buffer = ByteArray(64 * 1024)
            for (file in files) {
                val name = file.relativeTo(folder).invariantSeparatorsPath
                zip.setLevel(if (file.extension.lowercase() in STORED_EXTENSIONS) Deflater.NO_COMPRESSION else Deflater.DEFAULT_COMPRESSION)
                val entry = ZipEntry(name)
                entry.time = file.lastModified()
                zip.putNextEntry(entry)
                file.inputStream().use { input ->
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        zip.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun uniqueFile(name: String): File {
        var file = File(exportDir, name)
        var index = 2
        while (file.exists()) {
            file = File(exportDir, "${name.removeSuffix(".osz")} ($index).osz")
            index++
        }
        return file
    }

    private fun fileName(candidate: ExportCandidate): String {
        val label =
            listOf(candidate.artist, candidate.title)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { "beatmap ${candidate.id}" }
        val base =
            if (candidate.isOnline) {
                "${candidate.id} $label"
            } else if (candidate.mapper.isNotBlank()) {
                "$label (${candidate.mapper})"
            } else {
                label
            }
        val safe =
            base
                .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_")
                .trim()
                .trimEnd('.')
                .take(120)
                .ifBlank { "beatmap" }
        return "$safe.osz"
    }

    private companion object {
        const val EXPORT_DIRECTORY = "lazer-export"
        val STORED_EXTENSIONS = setOf("mp3", "ogg", "jpg", "jpeg", "png", "webp", "mp4", "flv", "avi", "m4a", "flac", "aac", "opus")
    }
}
