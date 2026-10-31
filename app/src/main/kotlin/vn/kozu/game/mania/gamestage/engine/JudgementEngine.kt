package vn.kozu.game.mania.gamestage.engine

import vn.kozu.game.mania.gamestage.chart.ChartNote
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.HitWindows
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import kotlin.math.abs

/**
 * osu!mania judgement (stable, ScoreV1), plain Kotlin with no Android dependencies.
 *
 * All times here are "chart time" (ms), with the player's offset already subtracted.
 * A touch is timed at the EVENT time (not the processing time), converted by the caller.
 *
 * Regular notes (per the wiki):
 *  - Pressing earlier than the MISS window has no effect.
 *  - Pressing within (MEH, MISS] results in a MISS and consumes the note.
 *  - If not pressed by the end of the OK window, the note is a MISS (there is no late MEH).
 *
 * Hold notes: a SINGLE judgement based on the head and tail errors:
 *  PERFECT: head <= 16·1.2   and head+tail <= 16·2.4
 *  GREAT:   head <= GREAT·1.1 and head+tail <= GREAT·2.2
 *  GOOD:    head <= GOOD      and head+tail <= GOOD·2
 *  OK:      head <= OK        and head+tail <= OK·2
 *  MEH:     otherwise (not a miss)
 *  Releasing during the hold body gives at most MEH. Not holding through the tail window (from early MEH to late OK) gives MISS.
 *
 * Intentional simplification: a "late" hold press after the head's OK window can still be saved
 * (at most MEH) until the tail window begins.
 */
class JudgementEngine(
    private val chart: ManiaChart,
    private val tracker: ScoreTracker,
    private val listener: Listener,
    val windows: HitWindows = HitWindows(chart.overallDifficulty),
) {
    interface Listener {
        /** A note received its final judgement. [errorMs] = actual time - ideal time (negative = early). */
        fun onJudgement(note: ChartNote, judgement: Judgement, errorMs: Double)

        /** A note was just hit (not missed): time to play the hitsound. */
        fun onHit(note: ChartNote)
    }

    private val keys = chart.keys
    private val notes = chart.notes

    /** State of each note, readable by the renderer. */
    val state = IntArray(notes.size) // STATE_*
    val broken = BooleanArray(notes.size)
    private val headError = DoubleArray(notes.size)

    /** Index (in chart.columns[c]) of the first note that is not DONE. */
    private val pointer = IntArray(keys)

    /** Keys currently held. */
    val keyDown = BooleanArray(keys)

    var resolved = 0
        private set

    val isFinished: Boolean get() = resolved >= notes.size

    // ---------------------------------------------------------------- input

    fun press(column: Int, timeMs: Double) {
        if (column !in 0 until keys) return
        advance(timeMs)
        keyDown[column] = true

        val ids = chart.columns[column]
        var k = pointer[column]
        while (k < ids.size) {
            val id = ids[k]
            if (state[id] != STATE_PENDING) { k++; continue }
            val n = notes[id]
            val err = timeMs - n.timeMs

            if (err < -windows.miss) return // Too early: no effect.

            if (err > windows.ok) {
                // Only a hold can still be saved late (regular notes were already missed by advance()).
                if (!n.isHold) { resolve(n, Judgement.MISS, err); k++; continue }
                val next = nextPending(ids, k + 1)
                if (next != null && timeMs >= notes[next].timeMs - windows.miss) {
                    // This press belongs to the next note; the old hold is considered missed.
                    resolve(n, Judgement.MISS, err)
                    k++
                    continue
                }
                headError[id] = windows.ok + 1.0 // Late head => at most MEH.
                state[id] = STATE_HOLDING
                listener.onHit(n)
                return
            }

            val judgement = windows.classify(abs(err), late = err > 0)
            if (n.isHold) {
                if (judgement == Judgement.MISS) {
                    resolve(n, Judgement.MISS, err)
                } else {
                    headError[id] = abs(err)
                    state[id] = STATE_HOLDING
                    listener.onHit(n)
                }
            } else {
                resolve(n, judgement, err)
                if (judgement != Judgement.MISS) listener.onHit(n)
            }
            return
        }
    }

    fun release(column: Int, timeMs: Double) {
        if (column !in 0 until keys) return
        // advance() must run BEFORE clearing keyDown: an expired hold needs to know whether the key is still held.
        advance(timeMs)
        keyDown[column] = false

        val id = holdingNote(column) ?: return
        val n = notes[id]
        if (timeMs < n.endTimeMs - windows.meh) {
            broken[id] = true // Released during the hold body.
            return
        }
        val tailError = abs(timeMs - n.endTimeMs)
        resolve(n, holdJudgement(id, tailError), timeMs - n.endTimeMs)
    }

    // ---------------------------------------------------------------- time

    /** Processes notes that have expired at [timeMs]. Called once per frame. */
    fun advance(timeMs: Double) {
        for (c in 0 until keys) advanceColumn(c, timeMs)
    }

    private fun advanceColumn(c: Int, t: Double) {
        val ids = chart.columns[c]
        while (pointer[c] < ids.size) {
            val id = ids[pointer[c]]
            val n = notes[id]
            when (state[id]) {
                STATE_DONE -> pointer[c]++
                STATE_PENDING -> {
                    if (t <= n.timeMs + windows.ok) return
                    if (n.isHold && t <= n.endTimeMs - windows.meh) return // Can still be saved late.
                    resolve(n, Judgement.MISS, t - n.timeMs)
                }
                else -> { // HOLDING
                    if (t <= n.endTimeMs + windows.ok) return
                    val j = if (keyDown[c]) holdJudgement(id, windows.ok) else Judgement.MISS
                    resolve(n, j, t - n.endTimeMs)
                }
            }
        }
    }

    /** Ends the song and judges the remaining notes. */
    fun finish() = advance(Double.MAX_VALUE / 4)

    // ---------------------------------------------------------------- helpers

    private fun holdJudgement(id: Int, tailError: Double): Judgement {
        if (broken[id]) return Judgement.MEH
        val head = headError[id]
        val combined = head + tailError
        val w = windows
        return when {
            head <= w.perfect * 1.2 && combined <= w.perfect * 2.4 -> Judgement.PERFECT
            head <= w.great * 1.1 && combined <= w.great * 2.2 -> Judgement.GREAT
            head <= w.good && combined <= w.good * 2.0 -> Judgement.GOOD
            head <= w.ok && combined <= w.ok * 2.0 -> Judgement.OK
            else -> Judgement.MEH
        }
    }

    private fun nextPending(ids: IntArray, from: Int): Int? {
        for (i in from until ids.size) if (state[ids[i]] == STATE_PENDING) return ids[i]
        return null
    }

    private fun holdingNote(column: Int): Int? {
        val ids = chart.columns[column]
        for (i in pointer[column] until ids.size) if (state[ids[i]] == STATE_HOLDING) return ids[i]
        return null
    }

    private fun resolve(n: ChartNote, j: Judgement, errorMs: Double) {
        state[n.id] = STATE_DONE
        resolved++
        tracker.apply(j)
        listener.onJudgement(n, j, errorMs)
        val ids = chart.columns[n.column]
        while (pointer[n.column] < ids.size && state[ids[pointer[n.column]]] == STATE_DONE) pointer[n.column]++
    }

    companion object {
        const val STATE_PENDING = 0
        const val STATE_HOLDING = 1
        const val STATE_DONE = 2
    }
}
