package vn.kozu.game.mania.gamestage.media

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Đọc ảnh nền đã thu nhỏ vừa đủ (không nạp ảnh 2560x1440 nguyên bản vào RAM). Gọi trên luồng IO. */
object BackgroundLoader {
    fun load(file: File, maxWidth: Int, maxHeight: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth && bounds.outHeight / (sample * 2) >= maxHeight) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
    }
}
