package vn.kozu.data.mirror

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

interface RangeSource {
    fun length(): Long

    fun open(
        offset: Long,
        endInclusive: Long,
    ): InputStream
}

data class RangeTail(
    val start: Long,
    val totalLength: Long,
    val bytes: ByteArray,
)

interface TailRangeSource {
    fun readTail(maxBytes: Long): RangeTail?
}

data class ZipEntryInfo(
    val name: String,
    val method: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
)

object RemoteZip {
    private val audioExtensions = setOf("mp3", "ogg", "wav", "m4a", "flac", "aac", "opus")
    private const val EOCD_SIGNATURE = 0x06054b50
    private const val CENTRAL_SIGNATURE = 0x02014b50
    private const val LOCAL_SIGNATURE = 0x04034b50
    private const val TAIL_BYTES = 66_000L
    private const val MAX_CENTRAL_DIRECTORY_BYTES = 32L * 1024 * 1024
    private const val MAX_EXTRACTED_ENTRY_BYTES = 64L * 1024 * 1024

    fun readEntries(source: RangeSource): List<ZipEntryInfo> {
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
        val cdEnd = cdOffset + cdSize
        if (cdSize <= 0 || cdSize > MAX_CENTRAL_DIRECTORY_BYTES || cdEnd > total) {
            throw IOException("Invalid or oversized central directory")
        }

        val tailEnd = tailStart + tail.size
        val central =
            if (cdOffset >= tailStart && cdEnd <= tailEnd) {
                val start = (cdOffset - tailStart).toInt()
                val end = (cdEnd - tailStart).toInt()
                tail.copyOfRange(start, end)
            } else {
                readFully(source, cdOffset, cdEnd - 1)
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
            val next = p.toLong() + 46L + nameLength + extraLength + commentLength
            if (next > central.size) break
            val name = String(central, p + 46, nameLength, Charsets.UTF_8)
            entries.add(ZipEntryInfo(name, method, compressed, uncompressed, localOffset))
            p = next.toInt()
        }
        return entries
    }

    fun pickAudio(
        entries: List<ZipEntryInfo>,
        preferredName: String? = null,
    ): ZipEntryInfo? {
        val audio = entries.filter { it.name.substringAfterLast('.', "").lowercase() in audioExtensions }
        if (preferredName != null) {
            audio.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }?.let { return it }
        }
        return audio.maxByOrNull { it.uncompressedSize }
    }

    fun openEntryStream(
        source: RangeSource,
        entry: ZipEntryInfo,
    ): InputStream {
        if (entry.compressedSize <= 0L) {
            throw IOException("Empty audio entry")
        }

        val header =
            readFully(
                source,
                entry.localHeaderOffset,
                entry.localHeaderOffset + 29L,
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
            0 -> {
                raw
            }

            8 -> {
                InflaterInputStream(raw, Inflater(true))
            }

            else -> {
                raw.close()
                throw IOException("Unsupported compression method ${entry.method}")
            }
        }
    }

    fun extract(
        source: RangeSource,
        entry: ZipEntryInfo,
        target: File,
        onProgress: (Float) -> Unit = {},
    ) {
        val header = readFully(source, entry.localHeaderOffset, entry.localHeaderOffset + 29)
        if (le32(header, 0) != LOCAL_SIGNATURE) throw IOException("Invalid local header")
        val dataStart = entry.localHeaderOffset + 30 + le16(header, 26) + le16(header, 28)
        if (entry.compressedSize <= 0) throw IOException("Empty entry")
        val dataEnd = dataStart + entry.compressedSize - 1

        val raw = source.open(dataStart, dataEnd)
        val counting =
            CountingInputStream(raw) { read ->
                onProgress((read.toFloat() / entry.compressedSize).coerceIn(0f, 1f))
            }
        val stream: InputStream =
            when (entry.method) {
                0 -> {
                    counting
                }

                8 -> {
                    InflaterInputStream(counting, Inflater(true))
                }

                else -> {
                    raw.close()
                    throw IOException("Unsupported compression method ${entry.method}")
                }
            }
        val expectedSize = entry.uncompressedSize
        if (expectedSize <= 0L || expectedSize > MAX_EXTRACTED_ENTRY_BYTES) {
            stream.close()
            throw IOException("ZIP entry exceeds the extraction size limit")
        }
        target.parentFile?.mkdirs()
        var written = 0L
        try {
            stream.use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        written += n
                        if (written > expectedSize || written > MAX_EXTRACTED_ENTRY_BYTES) {
                            throw IOException("ZIP entry exceeds its declared size")
                        }
                        output.write(buffer, 0, n)
                    }
                }
            }
            if (written != expectedSize || target.length() != expectedSize) {
                throw IOException("Extracted ZIP entry size does not match its directory record")
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun readFully(
        source: RangeSource,
        start: Long,
        endInclusive: Long,
    ): ByteArray {
        val expectedSize = endInclusive - start + 1L
        if (start < 0L || expectedSize <= 0L || expectedSize > MAX_CENTRAL_DIRECTORY_BYTES) {
            throw IOException("Invalid ZIP byte range")
        }
        source.open(start, endInclusive).use { input ->
            val bytes = input.readBytes()
            if (bytes.size.toLong() != expectedSize) throw IOException("Unexpected ZIP range length")
            return bytes
        }
    }

    private fun le16(
        b: ByteArray,
        o: Int,
    ): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun le32(
        b: ByteArray,
        o: Int,
    ): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private class CountingInputStream(
        private val delegate: InputStream,
        private val onRead: (Long) -> Unit,
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

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
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
