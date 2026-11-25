package vn.kozu.data.lazer

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.kozu.data.lazer.realm.LazerRealmReader
import vn.kozu.data.lazer.shizuku.ShizukuAccess

data class LazerLibrarySet(
    val key: String,
    val onlineId: Long,
    val title: String,
    val titleUnicode: String,
    val artist: String,
    val artistUnicode: String,
    val author: String,
    val source: String,
    val tags: String,
    val rulesets: Set<Int>,
    val difficulties: Int,
    val files: Map<String, String>,
    val status: Int = -3,
    val addedAt: Long = 0L,
    val stars: Double = 0.0,
) {
    val hasMania: Boolean get() = 3 in rulesets

    val expectedId: Long
        get() = if (onlineId > 0L) onlineId else LazerImporter.localIdFor(artist, title, author)
}

class LazerCollection(
    val name: String,
    val sets: List<LazerLibrarySet>,
)

class LazerLibraryData(
    val sets: List<LazerLibrarySet>,
    val collections: List<LazerCollection>,
)

class LazerLibrary(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val workDir = File(appContext.cacheDir, "lazer-realm")

    fun directSource(): LazerDataSource? = DirectLazerSource.detect()

    suspend fun treeSource(tree: Uri): LazerDataSource = TreeLazerSource.create(appContext, tree)

    suspend fun shizukuSource(): LazerDataSource = ShizukuAccess.open(appContext)

    suspend fun scan(
        source: LazerDataSource,
        onProgress: (Long) -> Unit = {},
    ): LazerLibraryData =
        withContext(Dispatchers.IO) {
            clearWorkDir()
            workDir.mkdirs()
            val copy = File(workDir, "client-${System.nanoTime()}.realm")
            try {
                source.copyDatabase(copy, onProgress)
                if (copy.length() < MIN_DATABASE_BYTES) throw LazerAccessException(LazerAccessProblem.NOT_LAZER)
                LazerRealmReader.ensureLoaded(appContext)
                val data = LazerRealmReader().read(copy)
                LazerLibraryData(
                    sets = data.sets.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() })),
                    collections = data.collections.sortedBy { it.name.lowercase() },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: LazerAccessException) {
                throw e
            } catch (e: Throwable) {
                throw LazerAccessException(LazerAccessProblem.DATABASE, e.message, e)
            } finally {
                clearWorkDir()
            }
        }

    suspend fun writeArchive(
        source: LazerDataSource,
        set: LazerLibrarySet,
        target: File,
    ) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            val buffer = ByteArray(128 * 1024)
            var written = 0
            for ((name, hash) in set.files) {
                val entryName = sanitizeEntryName(name) ?: continue
                val input = source.openFile(hash)
                if (input == null) {
                    if (entryName.endsWith(".osu", ignoreCase = true) || isAudio(entryName)) {
                        throw IOException("Missing file in osu!lazer storage: $name")
                    }
                    continue
                }
                input.use { stream ->
                    zip.setLevel(
                        if (entryName.substringAfterLast('.', "").lowercase() in STORED_EXTENSIONS) {
                            Deflater.NO_COMPRESSION
                        } else {
                            Deflater.DEFAULT_COMPRESSION
                        },
                    )
                    zip.putNextEntry(ZipEntry(entryName))
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        zip.write(buffer, 0, count)
                    }
                    zip.closeEntry()
                    written++
                }
            }
            if (written == 0) throw IOException("No files could be read for ${set.title}")
        }
    }

    fun needsLegacyStoragePermission(): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    private fun clearWorkDir() {
        workDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun sanitizeEntryName(name: String): String? {
        val normalized = name.replace('\\', '/').trimStart('/')
        if (normalized.isBlank()) return null
        if (normalized.split('/').any { it == ".." || it.isEmpty() }) return null
        return normalized
    }

    private fun isAudio(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

    private companion object {
        const val MIN_DATABASE_BYTES = 4096L
        val STORED_EXTENSIONS = setOf("mp3", "ogg", "jpg", "jpeg", "png", "webp", "mp4", "flv", "avi", "m4a", "flac", "aac", "opus")
        val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "wav", "m4a", "flac", "aac", "opus")
    }
}
