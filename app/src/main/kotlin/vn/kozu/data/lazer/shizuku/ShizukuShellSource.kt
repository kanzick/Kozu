package vn.kozu.data.lazer.shizuku

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import vn.kozu.data.lazer.LAZER_DATABASE_NAME
import vn.kozu.data.lazer.LazerAccessException
import vn.kozu.data.lazer.LazerAccessProblem
import vn.kozu.data.lazer.LazerDataSource
import vn.kozu.data.lazer.copyStreamTo
import vn.kozu.data.lazer.lazerFilePath

internal class ShizukuShellSource private constructor(
    private val process: Process,
) : LazerDataSource {
    private val input = BufferedInputStream(process.inputStream, 512 * 1024)
    private val output: OutputStream = process.outputStream
    private val gate = Mutex()

    @Volatile
    private var broken = false

    override suspend fun copyDatabase(
        target: File,
        onProgress: (Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val stream = request(LAZER_DATABASE_NAME) ?: throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND)
        stream.use { copyStreamTo(it, target, onProgress) }
    }

    override suspend fun openFile(hash: String): InputStream? {
        val relative = lazerFilePath(hash) ?: return null
        return withContext(Dispatchers.IO) { request(relative) }
    }

    override fun close() {
        broken = true
        try {
            process.destroy()
        } catch (_: Throwable) {
        }
    }

    private suspend fun request(relative: String): InputStream? {
        gate.lock()
        var handedOver = false
        try {
            if (broken) throw IOException("The Shizuku shell session is no longer available")
            output.write((relative + "\n").toByteArray(Charsets.UTF_8))
            output.flush()
            val size = readHeader()
            if (size < 0L) return null
            handedOver = true
            return Bounded(size)
        } catch (e: Throwable) {
            broken = true
            throw e
        } finally {
            if (!handedOver) gate.unlock()
        }
    }

    private fun readHeader(): Long {
        val line = StringBuilder()
        while (true) {
            val value = input.read()
            if (value < 0) throw IOException("The Shizuku shell closed unexpectedly")
            if (value == '\n'.code) break
            if (value != '\r'.code) line.append(value.toChar())
            if (line.length > 32) throw IOException("Unexpected shell output")
        }
        return line.toString().trim().toLongOrNull() ?: throw IOException("Unexpected shell output")
    }

    private inner class Bounded(
        private var remaining: Long,
    ) : InputStream() {
        private var closed = false

        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = input.read()
            if (value < 0) {
                broken = true
                throw IOException("The Shizuku shell closed unexpectedly")
            }
            remaining--
            return value
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (remaining <= 0L) return -1
            val count = input.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (count < 0) {
                broken = true
                throw IOException("The Shizuku shell closed unexpectedly")
            }
            remaining -= count
            return count
        }

        override fun close() {
            if (closed) return
            closed = true
            try {
                val scratch = ByteArray(64 * 1024)
                while (remaining > 0L) {
                    val count = input.read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
                    if (count < 0) {
                        broken = true
                        break
                    }
                    remaining -= count
                }
            } catch (_: IOException) {
                broken = true
            } finally {
                gate.unlock()
            }
        }
    }

    companion object {
        private const val PACKAGE = "sh.ppy.osulazer"

        suspend fun open(userId: Int): ShizukuShellSource =
            withContext(Dispatchers.IO) {
                val roots =
                    listOf(
                        "/storage/emulated/$userId/Android/data/$PACKAGE/files",
                        "/data/media/$userId/Android/data/$PACKAGE/files",
                        "/data/user/$userId/$PACKAGE/files",
                        "/data/data/$PACKAGE/files",
                    )
                val probe = start("for d in ${roots.joinToString(" ") { "'$it'" }}; do if [ -f \"${'$'}d/$LAZER_DATABASE_NAME\" ]; then echo \"${'$'}d\"; break; fi; done")
                val root =
                    try {
                        probe.inputStream.bufferedReader().readLine()?.trim().orEmpty()
                    } finally {
                        try {
                            probe.destroy()
                        } catch (_: Throwable) {
                        }
                    }
                if (root.isEmpty()) throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND)
                ShizukuShellSource(start(serverScript(root)))
            }

        internal fun fromProcess(process: Process): ShizukuShellSource = ShizukuShellSource(process)

        internal fun serverScript(root: String): String =
            """
            exec 2>/dev/null
            root='$root'
            while IFS= read -r p; do
              f="${'$'}root/${'$'}p"
              if [ -f "${'$'}f" ] && [ -r "${'$'}f" ]; then
                s=${'$'}(wc -c < "${'$'}f")
                echo ${'$'}s
                cat "${'$'}f"
              else
                echo -1
              fi
            done
            """.trimIndent()

        private fun start(script: String): Process {
            try {
                val method =
                    Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java,
                    )
                method.isAccessible = true
                return method.invoke(null, arrayOf("sh", "-c", script), null, null) as Process
            } catch (e: InvocationTargetException) {
                throw e.targetException ?: e
            }
        }
    }
}
