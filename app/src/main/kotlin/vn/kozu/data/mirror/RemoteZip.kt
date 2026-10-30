package vn.kozu.data.mirror

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

interface RangeSource {
    fun length(): Long
    fun open(offset: Long, endInclusive: Long): InputStream
}

data class RangeTail(
    val start: Long,
    val totalLength: Long,
    val bytes: ByteArray
)

/** Optional fast path for servers that support suffix byte-range requests. */
interface TailRangeSource {
    fun readTail(maxBytes: Long): RangeTail?
}

data class ZipEntryInfo(
    val name: String,
    val method: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long
)

object RemoteZip {

    private val audioExtensions = setOf("mp3", "ogg", "wav", "m4a", "flac", "aac", "opus")
    private const val EOCD_SIGNATURE = 0x06054b50
    private const val CENTRAL_SIGNATURE = 0x02014b50
    private const val LOCAL_SIGNATURE = 0x04034b50
    private const val TAIL_BYTES = 66_000L

    fun readEntries(source: RangeSource): List<ZipEntryInfo> {
        // A suffix-range request gives us the archive length and the end-of-
        // central-directory record in one round trip. Older mirrors that do
        // not support suffix ranges use the compatible two-request path.
        val rangedTail = (source as? TailRangeSource)?.readTail(TAIL_BYTES)
        val total = rangedTail?.totalLength ?: source.length()
        if (total < 22) throw IOException("Not a zip file")
        val tailStart = rangedTail?.start ?: (total - TAIL_BYTES).coerceAtLeast(0L)
        val tail = rangedTail?.bytes ?: readFully(source, tailStart, total - 1)

        var eocd = -1
        var i = tail.size - 22
        while (i >= 0) {
            if (le32(tail, i) == EOCD_SIGNATURE) {
                eocd = i
                break
            }
            i--
        }
        if (eocd < 0) throw IOException("End of central directory not found")

        val count = le16(tail, eocd + 10)
        val cdSize = le32(tail, eocd + 12).toLong() and 0xFFFFFFFFL
        val cdOffset = le32(tail, eocd + 16).toLong() and 0xFFFFFFFFL
        if (cdOffset == 0xFFFFFFFFL || count == 0xFFFF) throw IOException("Zip64 is not supported")
        if (cdSize <= 0 || cdOffset + cdSize > total) throw IOException("Invalid central directory")

        val central = if (cdOffset >= tailStart) {
            tail.copyOfRange((cdOffset - tailStart).toInt(), (cdOffset - tailStart + cdSize).toInt())
        } else {
            readFully(source, cdOffset, cdOffset + cdSize - 1)
        }

        val entries = ArrayList<ZipEntryInfo>(count)
        var p = 0
        while (p + 46 <= central.size && le32(central, p) == CENTRAL_SIGNATURE) {
            val method = le16(central, p + 10)
            val compressed = le32(central, p + 20).toLong() and 0xFFFFFFFFL
            val uncompressed = le32(central, p + 24).toLong() and 0xFFFFFFFFL
            val nameLength = le16(central, p + 28)
            val extraLength = le16(central, p + 30)
            val commentLength = le16(central, p + 32)
            val localOffset = le32(central, p + 42).toLong() and 0xFFFFFFFFL
            if (p + 46 + nameLength > central.size) break
            val name = String(central, p + 46, nameLength, Charsets.UTF_8)
            entries.add(ZipEntryInfo(name, method, compressed, uncompressed, localOffset))
            p += 46 + nameLength + extraLength + commentLength
        }
        return entries
    }

    fun pickAudio(entries: List<ZipEntryInfo>, preferredName: String? = null): ZipEntryInfo? {
        val audio = entries.filter { it.name.substringAfterLast('.', "").lowercase() in audioExtensions }
        if (preferredName != null) {
            audio.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }?.let { return it }
        }
        return audio.maxByOrNull { it.uncompressedSize }
    }

    /**
     * Opens just the audio member inside a remote ZIP. The returned stream
     * inflates deflated members on the fly, so Media3 can play while the bytes
     * are still arriving instead of waiting for a complete local extraction.
     */
    fun openEntryStream(source: RangeSource, entry: ZipEntryInfo): InputStream {
        if (entry.compressedSize <= 0L) {
            throw IOException("Empty audio entry")
        }

        // Read the small local header first so the following Range request
        // starts exactly at the compressed member. Guessing the maximum ZIP
        // extra-field size here can make Range end beyond EOF and cause HTTP 416
        // for valid archives where the audio is the final ZIP entry.
        val header = readFully(
            source,
            entry.localHeaderOffset,
            entry.localHeaderOffset + 29L
        )
        if (le32(header, 0) != LOCAL_SIGNATURE) {
            throw IOException("Invalid local header")
        }

        val flags = le16(header, 6)
        if ((flags and 0x0001) != 0) {
            throw IOException("Encrypted audio entries are not supported")
        }

        val method = le16(header, 8)
        if (method != entry.method) {
            throw IOException("ZIP audio compression method mismatch")
        }

        val nameLength = le16(header, 26)
        val extraLength = le16(header, 28)
        val dataStart = entry.localHeaderOffset + 30L + nameLength + extraLength
        val dataEnd = dataStart + entry.compressedSize - 1L
        if (dataEnd < dataStart) {
            throw IOException("Invalid ZIP audio byte range")
        }

        val raw = source.open(dataStart, dataEnd)
        return when (entry.method) {
            0 -> raw
            8 -> InflaterInputStream(raw, Inflater(true))
            else -> {
                raw.close()
                throw IOException("Unsupported compression method ${entry.method}")
            }
        }
    }

    private fun readFully(input: InputStream, bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            if (read < 0) throw IOException("Unexpected end of ZIP local header")
            offset += read
        }
    }

    private fun skipFully(input: InputStream, amount: Long) {
        var remaining = amount
        val scratch = ByteArray(1024)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
                if (read < 0) throw IOException("Unexpected end of ZIP extra fields")
                remaining -= read
            }
        }
    }

    fun extract(source: RangeSource, entry: ZipEntryInfo, target: File, onProgress: (Float) -> Unit = {}) {
        val header = readFully(source, entry.localHeaderOffset, entry.localHeaderOffset + 29)
        if (le32(header, 0) != LOCAL_SIGNATURE) throw IOException("Invalid local header")
        val dataStart = entry.localHeaderOffset + 30 + le16(header, 26) + le16(header, 28)
        if (entry.compressedSize <= 0) throw IOException("Empty entry")
        val dataEnd = dataStart + entry.compressedSize - 1

        val raw = source.open(dataStart, dataEnd)
        val counting = CountingInputStream(raw) { read ->
            onProgress((read.toFloat() / entry.compressedSize).coerceIn(0f, 1f))
        }
        val stream: InputStream = when (entry.method) {
            0 -> counting
            8 -> InflaterInputStream(counting, Inflater(true))
            else -> {
                raw.close()
                throw IOException("Unsupported compression method ${entry.method}")
            }
        }
        target.parentFile?.mkdirs()
        stream.use { input ->
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                }
            }
        }
        if (target.length() <= 0L) throw IOException("Extracted file is empty")
    }

    private fun readFully(source: RangeSource, start: Long, endInclusive: Long): ByteArray {
        source.open(start, endInclusive).use { input ->
            return input.readBytes()
        }
    }

    private fun le16(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun le32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private class CountingInputStream(
        private val delegate: InputStream,
        private val onRead: (Long) -> Unit
    ) : InputStream() {
        private var total = 0L

        override fun read(): Int {
            val v = delegate.read()
            if (v >= 0) {
                total++
                onRead(total)
            }
            return v
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = delegate.read(b, off, len)
            if (n > 0) {
                total += n
                onRead(total)
            }
            return n
        }

        override fun close() = delegate.close()
    }
}
