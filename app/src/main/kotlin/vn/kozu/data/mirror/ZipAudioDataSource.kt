package vn.kozu.data.mirror

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList

@UnstableApi
class ZipAudioDataSource(
    private val resolver: StreamResolver,
) : DataSource {
    private val listeners = CopyOnWriteArrayList<TransferListener>()

    private var openedUri: Uri? = null
    private var input: java.io.InputStream? = null
    private var output: OutputStream? = null
    private var temporaryFile: java.io.File? = null
    private var targetFile: java.io.File? = null
    private var setId: Long = -1L
    private var sourceId: String = ""
    private var expectedLength: Long = C.LENGTH_UNSET.toLong()
    private var requestedLength: Long = C.LENGTH_UNSET.toLong()
    private var startPosition: Long = 0L
    private var bytesRead: Long = 0L
    private var opened = false

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
    }

    override fun open(dataSpec: DataSpec): Long {
        if (opened) close()

        val uri = dataSpec.uri
        if (uri.scheme != StreamResolver.ZIP_AUDIO_SCHEME) {
            throw IOException("Unsupported ZIP audio URI")
        }

        setId = uri.host?.toLongOrNull()
            ?: throw IOException("Invalid beatmapset id in ZIP audio URI")
        sourceId = uri
            .getQueryParameter("source")
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("Missing archive source id")

        val session = resolver.openArchiveAudio(setId, sourceId)
        val position = dataSpec.position
        var cacheOutput: OutputStream? = null
        try {
            if (position < 0L || position > session.expectedLength) {
                throw IOException("Invalid audio seek position")
            }
            if (position > 0L) {
                skipFully(session.input, position)
            } else if (dataSpec.length == C.LENGTH_UNSET.toLong() || dataSpec.length >= session.expectedLength) {
                session.temporaryFile.parentFile?.mkdirs()
                cacheOutput = session.temporaryFile.outputStream().buffered()
            }
        } catch (e: Exception) {
            runCatching { cacheOutput?.close() }
            runCatching { session.input.close() }
            session.temporaryFile.delete()
            throw e
        }

        input = session.input
        expectedLength = session.expectedLength
        temporaryFile = session.temporaryFile
        targetFile = session.cacheFile
        openedUri = uri
        startPosition = position
        requestedLength = dataSpec.length
        bytesRead = 0L
        output = cacheOutput
        opened = true
        listeners.forEach { listener ->
        }

        val available = expectedLength - startPosition
        return if (requestedLength == C.LENGTH_UNSET.toLong()) {
            available
        } else {
            minOf(available, requestedLength)
        }
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (length == 0) return 0
        val stream = input ?: return C.RESULT_END_OF_INPUT

        val alreadyRead = startPosition + bytesRead
        val remainingInEntry = expectedLength - alreadyRead
        val remainingInRequest =
            if (requestedLength == C.LENGTH_UNSET.toLong()) {
                Long.MAX_VALUE
            } else {
                requestedLength - bytesRead
            }
        val remaining = minOf(remainingInEntry, remainingInRequest)

        if (remaining <= 0L) {
            finishCacheIfComplete()
            return C.RESULT_END_OF_INPUT
        }

        val count =
            stream.read(
                buffer,
                offset,
                minOf(length.toLong(), remaining).toInt(),
            )

        if (count < 0) {
            if (alreadyRead < expectedLength && remainingInRequest > 0L) {
                throw IOException("Archive audio stream ended before the full track arrived")
            }
            finishCacheIfComplete()
            return C.RESULT_END_OF_INPUT
        }

        if (count > 0) {
            output?.write(buffer, offset, count)
            bytesRead += count

            if (startPosition + bytesRead >= expectedLength) {
                finishCacheIfComplete()
            }
        }

        return count
    }

    override fun getUri(): Uri? = openedUri

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()

    override fun close() {
        try {
            input?.close()
        } finally {
            input = null
            try {
                output?.flush()
                output?.close()
            } finally {
                output = null
            }
        }

        finishCacheIfComplete()

        if (opened && temporaryFile?.exists() == true && output == null) {
            val temp = temporaryFile
            val expected = expectedLength
            val actual = bytesRead
            if (startPosition != 0L || actual != expected) {
                temp?.delete()
            }
        }

        temporaryFile = null
        targetFile = null
        openedUri = null
        opened = false
        setId = -1L
        sourceId = ""
        expectedLength = C.LENGTH_UNSET.toLong()
        requestedLength = C.LENGTH_UNSET.toLong()
        startPosition = 0L
        bytesRead = 0L
    }

    private fun finishCacheIfComplete() {
        val temp = temporaryFile ?: return
        val target = targetFile ?: return
        if (startPosition != 0L || bytesRead != expectedLength) return

        try {
            output?.flush()
            output?.close()
        } catch (_: Exception) {
            temp.delete()
            output = null
            return
        }
        output = null

        resolver.completeArchiveAudio(
            id = setId,
            sourceId = sourceId,
            temporary = temp,
            target = target,
            bytesWritten = bytesRead,
            expectedLength = expectedLength,
        )
    }

    private fun skipFully(
        stream: java.io.InputStream,
        amount: Long,
    ) {
        var remaining = amount
        val scratch = ByteArray(32 * 1024)
        while (remaining > 0L) {
            val skipped = stream.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read =
                    stream.read(
                        scratch,
                        0,
                        minOf(scratch.size.toLong(), remaining).toInt(),
                    )
                if (read < 0) {
                    throw IOException("Unable to seek within archive audio")
                }
                remaining -= read
            }
        }
    }
}
