package vn.kozu.data.mirror

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class HttpRangeSource(
    private val client: OkHttpClient,
    private val url: String,
    private val referer: String = "",
    private val authorization: String = ""
) : RangeSource, TailRangeSource {

    private var total = -1L

    override fun length(): Long {
        if (total >= 0L) return total
        client.newCall(build("bytes=0-0")).execute().use { response ->
            if (response.code != 206) {
                throw IOException("Range requests not supported (HTTP ${response.code})")
            }
            val header = response.header("Content-Range")
                ?: throw IOException("Missing Content-Range")
            total = header.substringAfter('/').trim().toLongOrNull()
                ?: throw IOException("Unknown archive size")
        }
        return total
    }

    override fun readTail(maxBytes: Long): RangeTail? {
        return try {
            client.newCall(build("bytes=-$maxBytes")).execute().use { response ->
                if (response.code != 206) return null
                val header = response.header("Content-Range") ?: return null
                // Expected form: bytes START-END/TOTAL
                val range = header.removePrefix("bytes ").split('/', limit = 2)
                if (range.size != 2) return null
                val bounds = range[0].split('-', limit = 2)
                if (bounds.size != 2) return null
                val start = bounds[0].toLongOrNull() ?: return null
                val totalLength = range[1].toLongOrNull() ?: return null
                val bytes = response.body?.bytes() ?: return null
                if (bytes.isEmpty() || start < 0L || totalLength < start + bytes.size) {
                    return null
                }
                RangeTail(start, totalLength, bytes)
            }
        } catch (_: IOException) {
            null
        }
    }

    override fun open(offset: Long, endInclusive: Long): InputStream {
        val response = client.newCall(build("bytes=$offset-$endInclusive")).execute()
        if (response.code != 206) {
            response.close()
            throw IOException("Range request failed (HTTP ${response.code})")
        }
        val contentRange = response.header("Content-Range")
        val actualStart = contentRange
            ?.removePrefix("bytes ")
            ?.substringBefore('-')
            ?.toLongOrNull()
        if (actualStart != offset) {
            response.close()
            throw IOException("Mirror returned an unexpected byte range")
        }
        val body = response.body
        if (body == null) {
            response.close()
            throw IOException("Empty range response")
        }
        return object : FilterInputStream(body.byteStream()) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    response.close()
                }
            }
        }
    }

    private fun build(range: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Range", range)
        if (referer.isNotBlank()) builder.header("Referer", referer)
        if (authorization.isNotBlank()) builder.header("Authorization", authorization)
        return builder.build()
    }
}
