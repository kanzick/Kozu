package vn.kozu.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkPalette {
    private const val TAG = "ArtworkPalette"
    private const val MAX_IMAGE_BYTES = 12 * 1024 * 1024
    private const val MAX_BITMAP_DIMENSION = 256
    private val cache = LruCache<String, Int>(64)

    suspend fun seedColor(imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null
        cache.get(imageUrl)?.let { return Color(it) }

        val rgb = withContext(Dispatchers.IO) { extract(imageUrl) } ?: return null
        cache.put(imageUrl, rgb)
        return Color(rgb)
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (
            width / sampleSize > MAX_BITMAP_DIMENSION ||
            height / sampleSize > MAX_BITMAP_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun InputStream.readBounded(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IOException("Artwork image exceeds the download limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun extract(imageUrl: String): Int? {
        return try {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = true
            try {
                connection.connect()
                if (connection.contentLengthLong > MAX_IMAGE_BYTES) return null
                val bytes = connection.inputStream.use { it.readBounded(MAX_IMAGE_BYTES) }
                if (bytes.isEmpty()) return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                val bitmap =
                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply {
                            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                            inPreferredConfig = Bitmap.Config.RGB_565
                        },
                    ) ?: return null
                try {
                    val palette = Palette.from(bitmap).resizeBitmapArea(24_000).generate()
                    palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                } finally {
                    bitmap.recycle()
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract a color from the artwork", e)
            null
        }
    }
}
