package vn.kozu.game.mania.gamestage.settings

import vn.kozu.game.mania.gamestage.engine.NoteScroll
import java.io.File
import java.io.IOException
import java.util.Properties
import kotlin.math.roundToInt

/**
 * Cài đặt của người chơi, lưu trong MỘT file riêng (xem [SettingsStore]).
 *
 *  - [musicVolume], [hitsoundVolume]: 0..100
 *  - [speed]: tốc độ rơi 0.1..15.0 (bước 0.1), xem [NoteScroll]
 *  - [backgroundDim]: 0 = ảnh nền sáng hoàn toàn, 100 = che kín bằng màu đen
 *  - [offsetMs]: dương = nốt tới muộn hơn nhạc (dùng khi bạn có xu hướng bấm trễ / tai nghe Bluetooth)
 *  - [playerName]: tên ghi vào bảng điểm
 */
data class GameSettings(
    val musicVolume: Int = 80,
    val hitsoundVolume: Int = 70,
    val speed: Double = 8.0,
    val backgroundDim: Int = 60,
    val offsetMs: Int = 0,
    val playerName: String = DEFAULT_PLAYER_NAME,
) {
    /** Đưa mọi giá trị về khoảng hợp lệ (speed làm tròn 1 chữ số thập phân). */
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

/**
 * Đọc/ghi [GameSettings] ra file `.properties` (UTF-8). Ghi theo kiểu "file tạm rồi đổi tên"
 * nên bị ngắt giữa chừng cũng không làm hỏng file cũ. Đọc khoan dung: thiếu/hỏng thì dùng mặc định.
 */
class SettingsStore(val file: File) {

    fun load(): GameSettings {
        if (!file.isFile) return GameSettings()
        val p = Properties()
        try {
            file.reader(Charsets.UTF_8).use { p.load(it) }
        } catch (e: IOException) {
            return GameSettings()
        } catch (e: IllegalArgumentException) {
            return GameSettings() // escape \u hỏng
        }
        val d = GameSettings()
        return GameSettings(
            musicVolume = p.getProperty(K_MUSIC)?.trim()?.toIntOrNull() ?: d.musicVolume,
            hitsoundVolume = p.getProperty(K_HITSOUND)?.trim()?.toIntOrNull() ?: d.hitsoundVolume,
            speed = p.getProperty(K_SPEED)?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: d.speed,
            backgroundDim = p.getProperty(K_DIM)?.trim()?.toIntOrNull() ?: d.backgroundDim,
            offsetMs = p.getProperty(K_OFFSET)?.trim()?.toIntOrNull() ?: d.offsetMs,
            playerName = p.getProperty(K_PLAYER) ?: d.playerName,
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

        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs() && !it.isDirectory) throw IOException("Could not create settings folder ${it.path}") }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writer(Charsets.UTF_8).use { p.store(it, "GameStage settings") }
        if (!tmp.renameTo(file)) {
            // một số hệ thống file không cho đổi tên đè lên file có sẵn
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
    }
}
