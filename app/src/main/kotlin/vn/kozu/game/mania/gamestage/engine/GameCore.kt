package vn.kozu.game.mania.gamestage.engine

import kotlin.math.max
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import vn.kozu.game.mania.gamestage.settings.GameSettings

class GameCore(
    val chart: ManiaChart,
    val settings: GameSettings,
    nanoSource: () -> Long = System::nanoTime,
    listener: JudgementEngine.Listener,
) {
    val clock = GameClock(nanoSource)
    val tracker = ScoreTracker(chart.totalNotes)
    val engine = JudgementEngine(chart, tracker, listener)

    val travelMs: Double = NoteScroll.travelTimeMs(settings.speed)

    val prerollMs: Double = (max(chart.audioLeadInMs, 0) + BASE_PREROLL_MS).toDouble()

    private val nodeOffsetMs = settings.nodeOffsetMs.toDouble()
    private val judgmentOffsetMs = settings.judgmentOffsetMs.toDouble()

    fun begin(withAudio: Boolean = true) = clock.start(-prerollMs, withAudio)

    fun chartTimeNow(): Double = clock.now() - nodeOffsetMs

    fun chartTimeAt(nano: Long): Double = clock.timeAt(nano) - nodeOffsetMs

    fun judgmentTimeAt(nano: Long): Double =
        clock.timeAt(nano) - nodeOffsetMs - judgmentOffsetMs

    fun frame(): Double {
        val judgmentTime = clock.now() - nodeOffsetMs - judgmentOffsetMs
        engine.advance(judgmentTime - EVENT_GRACE_MS)
        return chartTimeNow()
    }

    fun press(column: Int, eventNano: Long) = engine.press(column, judgmentTimeAt(eventNano))

    fun release(column: Int, eventNano: Long) = engine.release(column, judgmentTimeAt(eventNano))

    fun isFinished(chartTimeMs: Double = clock.now() - nodeOffsetMs - judgmentOffsetMs): Boolean {
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
