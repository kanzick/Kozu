package vn.kozu.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility object that extracts prominent seed colors from track artwork images for dynamic theme generation.
 */
object ArtworkPalette {

    private const val TAG = "ArtworkPalette"
    private val cache = LruCache<String, Int>(64)

    /**
     * Fetches artwork, extracts a vibrant/dominant color, and returns a Compose [Color].
     */
    suspend fun seedColor(imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null
        cache.get(imageUrl)?.let { return Color(it) }

        val rgb = withContext(Dispatchers.IO) { extract(imageUrl) } ?: return null
        cache.put(imageUrl, rgb)
        return Color(rgb)
    }

    private fun extract(imageUrl: String): Int? {
        return try {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            try {
                connection.connect()
                val bitmap = connection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply {
                            inSampleSize = 2
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                    )
                } ?: return null
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
