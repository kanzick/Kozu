package vn.kozu.game.mania.previewstage.osu

import java.io.File

/**
 * Tìm file theo tên khai báo trong .osu. File osu! tạo trên Windows (không phân biệt hoa/thường)
 * còn Android phân biệt, nên có fallback so khớp không phân biệt hoa thường. Chặn ".." thoát khỏi thư mục.
 */
object FileResolver {
    fun resolve(baseDir: File, declaredName: String): File? {
        var current = baseDir
        val segments = declaredName.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
        if (segments.isEmpty()) return null
        for (seg in segments) {
            if (seg == "..") return null
            val exact = File(current, seg)
            current = if (exact.exists()) exact
            else current.listFiles()?.firstOrNull { it.name.equals(seg, ignoreCase = true) } ?: return null
        }
        return current.takeIf { it.isFile }
    }
}
