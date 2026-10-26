package vn.kozu.data.download

import vn.kozu.game.OsuBeatmapParser
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

data class ExtractResult(
    val audioFile: String,
    val backgroundFile: String,
    val sizeBytes: Long
)

object OszExtractor {

    private val videoExtensions = setOf("mp4", "avi", "flv", "mov", "wmv", "mkv", "webm", "m4v", "mpg", "mpeg")
    private val audioExtensions = setOf("mp3", "ogg", "wav", "m4a", "flac", "aac", "opus")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp")

    fun isZip(file: File): Boolean {
        if (!file.isFile || file.length() < 22) return false
        file.inputStream().use { input ->
            val magic = ByteArray(4)
            if (input.read(magic) != 4) return false
            return magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                (magic[2] == 0x03.toByte() || magic[2] == 0x05.toByte())
        }
    }

    fun extract(osz: File, destination: File, keepFull: Boolean): ExtractResult {
        val charsets = listOf(Charsets.UTF_8, Charset.forName("windows-1252"), Charset.forName("Shift_JIS"))
        var failure: Exception? = null
        for (charset in charsets) {
            try {
                destination.deleteRecursively()
                destination.mkdirs()
                return extractWith(osz, destination, keepFull, charset)
            } catch (e: IllegalArgumentException) {
                failure = e
            } catch (e: ZipException) {
                failure = e
            }
        }
        throw IOException("Could not read beatmap archive", failure)
    }

    private fun extractWith(osz: File, destination: File, keepFull: Boolean, charset: Charset): ExtractResult {
        ZipFile(osz, charset).use { zip ->
            val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
            if (entries.isEmpty()) throw IOException("Empty archive")

            val osuEntries = entries.filter { it.name.lowercase().endsWith(".osu") }
            var audioName = ""
            var backgroundName = ""
            for (entry in osuEntries) {
                val text = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                val parsed = OsuBeatmapParser.parse(entry.name, text)
                if (audioName.isEmpty() && parsed.audioFile.isNotEmpty()) audioName = parsed.audioFile
                if (backgroundName.isEmpty() && !parsed.background.isNullOrEmpty()) backgroundName = parsed.background
                if (audioName.isNotEmpty() && backgroundName.isNotEmpty()) break
            }

            val audioEntry = findEntry(entries, audioName)
                ?: entries.filter { extension(it.name) in audioExtensions }.maxByOrNull { it.size }
                ?: throw IOException("No audio file in beatmap")
            val backgroundEntry = findEntry(entries, backgroundName)
                ?: entries.filter { extension(it.name) in imageExtensions }.maxByOrNull { it.size }

            val normalizedAudio = normalize(audioEntry.name)
            val normalizedBackground = backgroundEntry?.let { normalize(it.name) }.orEmpty()

            var total = 0L
            for (entry in entries) {
                val ext = extension(entry.name)
                val isEssential = entry == audioEntry || entry == backgroundEntry || ext == "osu"
                if (!isEssential) {
                    if (!keepFull) continue
                    if (ext in videoExtensions) continue
                }
                val relative = normalize(entry.name)
                val target = File(destination, relative)
                val canonicalRoot = destination.canonicalPath + File.separator
                if (!target.canonicalPath.startsWith(canonicalRoot)) continue
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().buffered().use { output -> total += input.copyTo(output) }
                }
            }

            return ExtractResult(normalizedAudio, normalizedBackground, total)
        }
    }

    private fun findEntry(entries: List<ZipEntry>, name: String): ZipEntry? {
        if (name.isBlank()) return null
        val wanted = normalize(name).lowercase()
        return entries.firstOrNull { normalize(it.name).lowercase() == wanted }
    }

    private fun normalize(name: String): String =
        name.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." && it != ".." }.joinToString("/")

    private fun extension(name: String): String = name.substringAfterLast('.', "").lowercase()
}
