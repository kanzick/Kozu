package vn.kozu.game.mania.gamestage.settings

import vn.kozu.game.mania.gamestage.engine.NoteScroll
import vn.kozu.game.mania.gamestage.playtype.Landscape3DPerspective
import vn.kozu.game.mania.gamestage.playtype.LandscapeVerticalScrolling
import vn.kozu.game.mania.gamestage.playtype.PlayType
import vn.kozu.game.mania.gamestage.playtype.Portrait
import java.io.File
import java.io.IOException
import java.util.Properties
import kotlin.math.roundToInt

enum class GameOrientation {
    PORTRAIT,
    LANDSCAPE,
}

data class GameSettings(
    val musicVolume: Int = 80,
    val hitsoundVolume: Int = 70,
    val speed: Double = 8.0,
    val backgroundDim: Int = 60,
    val offsetMs: Int = 0,
    val playerName: String = DEFAULT_PLAYER_NAME,
    val playType: PlayType = Portrait,
) {

    fun coerced(): GameSettings = copy(
        musicVolume = musicVolume.coerceIn(0, 100),
        hitsoundVolume = hitsoundVolume.coerceIn(0, 100),
        speed = (NoteScroll.clampSpeed(speed) * 10.0).roundToInt() / 10.0,
        backgroundDim = backgroundDim.coerceIn(0, 100),
        offsetMs = offsetMs.coerceIn(OFFSET_MIN, OFFSET_MAX),
        playerName = playerName.trim().take(PLAYER_NAME_MAX).ifEmpty { DEFAULT_PLAYER_NAME },
    )

    companion object {
        const val OFFSET_MIN = -500
        const val OFFSET_MAX = 500
        const val PLAYER_NAME_MAX = 24
        const val DEFAULT_PLAYER_NAME = "Player"
    }
}

class SettingsStore(val file: File) {

    fun load(): GameSettings {
        if (!file.isFile) return GameSettings()
        val p = Properties()
        try {
            file.reader(Charsets.UTF_8).use { p.load(it) }
        } catch (e: IOException) {
            return GameSettings()
        } catch (e: IllegalArgumentException) {
            return GameSettings()
        }
        val d = GameSettings()
        val savedPlayType = PlayType.fromStorageKey(p.getProperty(K_PLAY_TYPE))
            ?: when (p.getProperty(K_ORIENTATION)) {
                "LANDSCAPE" -> when (p.getProperty(K_SCROLL_STYLE)) {
                    "PERSPECTIVE_3D" -> Landscape3DPerspective
                    else -> LandscapeVerticalScrolling
                }
                else -> Portrait
            }
        return GameSettings(
            musicVolume = p.getProperty(K_MUSIC)?.trim()?.toIntOrNull() ?: d.musicVolume,
            hitsoundVolume = p.getProperty(K_HITSOUND)?.trim()?.toIntOrNull() ?: d.hitsoundVolume,
            speed = p.getProperty(K_SPEED)?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: d.speed,
            backgroundDim = p.getProperty(K_DIM)?.trim()?.toIntOrNull() ?: d.backgroundDim,
            offsetMs = p.getProperty(K_OFFSET)?.trim()?.toIntOrNull() ?: d.offsetMs,
            playerName = p.getProperty(K_PLAYER) ?: d.playerName,
            playType = savedPlayType,
        ).coerced()
    }

    fun save(settings: GameSettings) {
        val s = settings.coerced()
        val p = Properties()
        p.setProperty(K_MUSIC, s.musicVolume.toString())
        p.setProperty(K_HITSOUND, s.hitsoundVolume.toString())
        p.setProperty(K_SPEED, "%.1f".format(java.util.Locale.ROOT, s.speed))
        p.setProperty(K_DIM, s.backgroundDim.toString())
        p.setProperty(K_OFFSET, s.offsetMs.toString())
        p.setProperty(K_PLAYER, s.playerName)
        p.setProperty(K_PLAY_TYPE, s.playType.storageKey)

        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs() && !it.isDirectory) throw IOException("Could not create settings folder ${it.path}") }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writer(Charsets.UTF_8).use { p.store(it, "GameStage settings") }
        if (!tmp.renameTo(file)) {

            file.delete()
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }

    private companion object {
        const val K_MUSIC = "audio.music"
        const val K_HITSOUND = "audio.hitsound"
        const val K_SPEED = "speed"
        const val K_DIM = "background.dim"
        const val K_OFFSET = "offset.ms"
        const val K_PLAYER = "player.name"
        const val K_ORIENTATION = "screen.orientation"
        const val K_SCROLL_STYLE = "scroll.style"
        const val K_PLAY_TYPE = "play.type"
    }
}
