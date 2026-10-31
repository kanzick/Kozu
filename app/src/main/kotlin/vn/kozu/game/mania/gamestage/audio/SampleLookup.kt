package vn.kozu.game.mania.gamestage.audio

import java.io.File

/**
 * Finds hitsound files in the beatmap directory. osu! creates files on Windows (case-insensitive),
 * while Android is case-sensitive, so matching ignores case. JVM-only.
 */
class SampleLookup(dir: File?) {
    private val files: Map<String, File> =
        dir?.listFiles { f -> f.isFile }?.associateBy { it.name.lowercase() }.orEmpty()

    /** [name] may include an extension (custom file) or omit it (standard osu! name: try .wav, .ogg, .mp3). */
    fun find(name: String): File? {
        val n = name.trim().replace('\\', '/').substringAfterLast('/').lowercase()
        if (n.isEmpty()) return null
        files[n]?.let { return it }
        for (ext in EXTS) files["$n.$ext"]?.let { return it }
        return null
    }

    private companion object { val EXTS = listOf("wav", "ogg", "mp3") }
}
