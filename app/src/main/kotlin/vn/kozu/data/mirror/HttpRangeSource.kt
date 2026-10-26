package vn.kozu.data.mirror

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

class HttpRangeSource(
    private val client: OkHttpClient,
    private val url: String,
    private val referer: String = "",
    private val authorization: String = ""
) : RangeSource {

    private var total = -1L

    override fun length(): Long {
        if (total >= 0L) return total
        client.newCall(build("bytes=0-0")).execute().use { response ->
            if (response.code != 206) throw IOException("Range requests not supported (HTTP ${response.code})")
            val header = response.header("Content-Range") ?: throw IOException("Missing Content-Range")
            total = header.substringAfter('/').trim().toLongOrNull() ?: throw IOException("Unknown archive size")
        }
        return total
    }

    override fun open(offset: Long, endInclusive: Long): InputStream {
        val response = client.newCall(build("bytes=$offset-$endInclusive")).execute()
        if (response.code != 206) {
            response.close()
            throw IOException("Range request failed (HTTP ${response.code})")
        }
        return response.body.byteStream()
    }

    private fun build(range: String): Request {
        val builder = Request.Builder().url(url).header("Range", range)
        if (referer.isNotBlank()) builder.header("Referer", referer)
        if (authorization.isNotBlank()) builder.header("Authorization", authorization)
        return builder.build()
    }
}
