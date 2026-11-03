package vn.kozu.game.mania.gamestage.engine

import vn.kozu.game.mania.gamestage.chart.ChartNote
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.HitWindows
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import kotlin.math.abs

class JudgementEngine(
    private val chart: ManiaChart,
    private val tracker: ScoreTracker,
    private val listener: Listener,
    val windows: HitWindows = HitWindows(chart.overallDifficulty),
) {
    interface Listener {

        fun onJudgement(note: ChartNote, judgement: Judgement, errorMs: Double)

        fun onHit(note: ChartNote)
    }

    private val keys = chart.keys
    private val notes = chart.notes

    val state = IntArray(notes.size)
    val broken = BooleanArray(notes.size)
    /** True after a note's head was successfully pressed. */
    val hit = BooleanArray(notes.size)
    private val headError = DoubleArray(notes.size)

    private val pointer = IntArray(keys)

    val keyDown = BooleanArray(keys)

    var resolved = 0
        private set

    val isFinished: Boolean get() = resolved >= notes.size

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
            val roundedErr = windows.roundedErrorMs(err)

            if (roundedErr < -windows.miss) return

            if (roundedErr > windows.ok) {

                if (!n.isHold) { resolve(n, Judgement.MISS, err); k++; continue }
                val next = nextPending(ids, k + 1)
                if (next != null &&
                    windows.roundedErrorMs(timeMs - notes[next].timeMs) >= -windows.miss
                ) {

                    resolve(n, Judgement.MISS, err)
                    k++
                    continue
                }
                headError[id] = windows.ok + 1.0
                state[id] = STATE_HOLDING
                hit[id] = true
                listener.onHit(n)
                return
            }

            val judgement = windows.classify(abs(roundedErr).toDouble(), late = roundedErr > 0)
            if (n.isHold) {
                if (judgement == Judgement.MISS) {
                    resolve(n, Judgement.MISS, err)
                } else {
                    headError[id] = abs(roundedErr).toDouble()
                    state[id] = STATE_HOLDING
                    hit[id] = true
                    listener.onHit(n)
                }
            } else {
                resolve(n, judgement, err)
                if (judgement != Judgement.MISS) {
                    hit[id] = true
                    listener.onHit(n)
                }
            }
            return
        }
    }

    fun release(column: Int, timeMs: Double) {
        if (column !in 0 until keys) return

        advance(timeMs)
        keyDown[column] = false

        val id = holdingNote(column) ?: return
        val n = notes[id]
        val err = timeMs - n.endTimeMs
        val roundedErr = windows.roundedErrorMs(err)
        if (roundedErr < -windows.meh) {
            broken[id] = true
            return
        }
        val tailError = abs(roundedErr).toDouble()
        resolve(n, holdJudgement(id, tailError), err)
    }

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
                    if (windows.roundedErrorMs(t - n.timeMs) <= windows.ok) return
                    if (n.isHold && windows.roundedErrorMs(t - n.endTimeMs) <= -windows.meh) return
                    resolve(n, Judgement.MISS, t - n.timeMs)
                }
                else -> {
                    if (windows.roundedErrorMs(t - n.endTimeMs) <= windows.ok) return
                    val j = if (keyDown[c]) holdJudgement(id, windows.ok) else Judgement.MISS
                    resolve(n, j, t - n.endTimeMs)
                }
            }
        }
    }

    fun finish() = advance(Double.MAX_VALUE / 4)

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
