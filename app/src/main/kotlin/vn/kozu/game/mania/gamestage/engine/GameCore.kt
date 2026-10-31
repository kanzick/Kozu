package vn.kozu.game.mania.gamestage.engine

import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import vn.kozu.game.mania.gamestage.settings.GameSettings
import kotlin.math.max

/**
 * Phần lõi của một lượt chơi, hoàn toàn thuần JVM: đồng hồ + engine phán định + tính điểm + offset.
 * Lớp Android ([vn.kozu.game.mania.gamestage.session.GameSession]) chỉ việc nối
 * âm thanh, màn hình và cảm ứng vào đây.
 *
 * "Thời gian chart" = thời gian nhạc - offset. Offset dương = nốt tới muộn hơn so với nhạc.
 * Cả việc vẽ lẫn việc phán định đều dùng cùng một thời gian chart nên thứ người chơi thấy
 * luôn khớp với thứ được chấm.
 */
class GameCore(
    val chart: ManiaChart,
    val settings: GameSettings,
    nanoSource: () -> Long = System::nanoTime,
    listener: JudgementEngine.Listener,
) {
    val clock = GameClock(nanoSource)
    val tracker = ScoreTracker(chart.totalNotes)
    val engine = JudgementEngine(chart, tracker, listener)

    /** Thời gian một nốt rơi hết màn hình (ms), cố định suốt bài. */
    val travelMs: Double = NoteScroll.travelTimeMs(settings.speed)

    /** Đoạn dạo đầu (ms, dương) trước khi nhạc bắt đầu: tôn trọng AudioLeadIn của chart. */
    val prerollMs: Double = (max(chart.audioLeadInMs, 0) + BASE_PREROLL_MS).toDouble()

    private val offsetMs = settings.offsetMs.toDouble()

    fun begin(withAudio: Boolean = true) = clock.start(-prerollMs, withAudio)

    /** Thời gian chart hiện tại, dùng để vẽ. */
    fun chartTimeNow(): Double = clock.now() - offsetMs

    /** Quy đổi thời điểm hệ thống (nano) của một sự kiện chạm thành thời gian chart. */
    fun chartTimeAt(nano: Long): Double = clock.timeAt(nano) - offsetMs

    /**
     * Gọi mỗi khung hình. Việc tự phán MISS bị trễ [EVENT_GRACE_MS] để một cú chạm hợp lệ
     * nhưng được hệ thống giao hơi muộn (sự kiện cũ hơn một khung hình) vẫn kịp được tính.
     * @return thời gian chart để vẽ khung hình này.
     */
    fun frame(): Double {
        val t = chartTimeNow()
        engine.advance(t - EVENT_GRACE_MS)
        return t
    }

    fun press(column: Int, eventNano: Long) = engine.press(column, chartTimeAt(eventNano))

    fun release(column: Int, eventNano: Long) = engine.release(column, chartTimeAt(eventNano))

    /** Bài đã kết thúc khi mọi nốt có phán định (sau khi quá hạn của nốt cuối + một chút để xem kết quả). */
    fun isFinished(chartTimeMs: Double = chartTimeNow()): Boolean {
        if (engine.isFinished) return chartTimeMs > chart.lastEventTimeMs + END_HOLD_MS
        if (chartTimeMs > chart.lastEventTimeMs + engine.windows.ok + END_HOLD_MS) {
            engine.finish()
            return true
        }
        return false
    }

    fun result(): GameResult {
        engine.finish()
        return tracker.result()
    }

    companion object {
        const val BASE_PREROLL_MS = 2000
        const val EVENT_GRACE_MS = 50.0
        const val END_HOLD_MS = 800.0
    }
}
