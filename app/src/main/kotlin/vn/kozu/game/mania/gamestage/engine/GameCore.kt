package vn.kozu.game.mania.gamestage.engine

import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import vn.kozu.game.mania.gamestage.settings.GameSettings
import kotlin.math.max

/**
 * The core of a play session, entirely JVM-only: clock + judgement engine + scoring + offset.
 * The Android layer ([vn.kozu.game.mania.gamestage.session.GameSession]) only needs to connect
 * audio, the screen, and touch input here.
 *
 * "Chart time" = song time - offset. A positive offset means notes arrive later than the music.
 * Rendering and judgement use the same chart time, so what players see
 * always matches what is scored.
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

    /** Time for a note to fall across the screen (ms), fixed for the entire song. */
    val travelMs: Double = NoteScroll.travelTimeMs(settings.speed)

    /** Positive lead-in time (ms) before the music starts, honoring the chart's AudioLeadIn. */
    val prerollMs: Double = (max(chart.audioLeadInMs, 0) + BASE_PREROLL_MS).toDouble()

    private val offsetMs = settings.offsetMs.toDouble()

    fun begin(withAudio: Boolean = true) = clock.start(-prerollMs, withAudio)

    /** Current chart time, used for rendering. */
    fun chartTimeNow(): Double = clock.now() - offsetMs

    /** Converts a touch event's system time (nano) to chart time. */
    fun chartTimeAt(nano: Long): Double = clock.timeAt(nano) - offsetMs

    /**
     * Called once per frame. Automatic MISS judgement is delayed by [EVENT_GRACE_MS] so a valid
     * touch delivered slightly late by the system (an event from the previous frame) can still count.
     * @return chart time for rendering this frame.
     */
    fun frame(): Double {
        val t = chartTimeNow()
        engine.advance(t - EVENT_GRACE_MS)
        return t
    }

    fun press(column: Int, eventNano: Long) = engine.press(column, chartTimeAt(eventNano))

    fun release(column: Int, eventNano: Long) = engine.release(column, chartTimeAt(eventNano))

    /** The song is over when every note has been judged (after the last note expires, plus time to view the result). */
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
