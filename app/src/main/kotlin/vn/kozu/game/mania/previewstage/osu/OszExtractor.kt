package vn.kozu.game.mania.previewstage.osu

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/** Giải nén .osz (thực chất là zip) an toàn: chặn zip-slip và zip-bomb. Thuần JVM. */
object OszExtractor {
    private val DRIVE_PATH = Regex("""^[A-Za-z]:($|/)""")

    /** @return số file đã giải nén. Thư mục [dest] được tạo mới (nếu đã có sẽ bị xoá trước). */
    fun extract(osz: File, dest: File, maxBytes: Long = 1L shl 30): Int {
        return try {
            extractWith(osz, dest, maxBytes, Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            // Tên entry không phải UTF-8 hợp lệ (zip cũ) -> thử lại bằng charset 1 byte.
            extractWith(osz, dest, maxBytes, Charset.forName("ISO-8859-1"))
        }
    }

    private fun extractWith(osz: File, dest: File, maxBytes: Long, charset: Charset): Int {
        if (dest.exists()) dest.deleteRecursively()
        if (!dest.mkdirs()) throw IOException("Could not create extraction folder ${dest.path}")
        val destRoot = dest.canonicalFile
        val rootPrefix = destRoot.path + File.separator

        var files = 0
        var total = 0L
        val buffer = ByteArray(64 * 1024)

        ZipInputStream(FileInputStream(osz).buffered(), charset).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (name.isEmpty()) continue
                if (name.startsWith('/') || DRIVE_PATH.matches(name)) {
                    throw IOException("Archive entry is not a relative path: ${entry.name}")
                }

                val out = File(destRoot, name).canonicalFile
                if (!out.path.startsWith(rootPrefix)) {
                    if (entry.isDirectory && out == destRoot) continue
                    throw IOException("Unsafe archive entry was rejected: ${entry.name}")
                }
                if (entry.isDirectory) {
                    if (!out.isDirectory && !out.mkdirs() && !out.isDirectory) {
                        throw IOException("Could not create extraction folder ${out.path}")
                    }
                    continue
                }
                out.parentFile?.mkdirs()
                out.outputStream().buffered().use { os ->
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > maxBytes) throw IOException("The extracted archive exceeds the $maxBytes-byte limit")
                        os.write(buffer, 0, n)
                    }
                }
                files++
            }
        }
        return files
    }
}
