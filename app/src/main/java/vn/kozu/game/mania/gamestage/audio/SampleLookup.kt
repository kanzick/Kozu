package vn.kozu.game.mania.gamestage.audio

import java.io.File

/**
 * Tìm file hitsound trong thư mục beatmap. osu! tạo file trên Windows (không phân biệt hoa/thường)
 * còn Android có, nên so khớp không phân biệt hoa thường. Thuần JVM.
 */
class SampleLookup(dir: File?) {
    private val files: Map<String, File> =
        dir?.listFiles { f -> f.isFile }?.associateBy { it.name.lowercase() }.orEmpty()

    /** [name] có thể có đuôi (custom file) hoặc không (tên chuẩn osu!: thử .wav, .ogg, .mp3). */
    fun find(name: String): File? {
        val n = name.trim().replace('\\', '/').substringAfterLast('/').lowercase()
        if (n.isEmpty()) return null
        files[n]?.let { return it }
        for (ext in EXTS) files["$n.$ext"]?.let { return it }
        return null
    }

    private companion object { val EXTS = listOf("wav", "ogg", "mp3") }
}
