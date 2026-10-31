package vn.kozu.game.mania.gamestage.engine

import vn.kozu.game.mania.gamestage.chart.ChartNote
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.HitWindows
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import kotlin.math.abs

/**
 * Phán định osu!mania (stable, ScoreV1), thuần Kotlin, không phụ thuộc Android.
 *
 * Mọi thời gian ở đây là "thời gian chart" (ms) - đã trừ offset của người chơi.
 * Thời điểm của cú chạm là thời điểm của SỰ KIỆN (không phải lúc xử lý), do lớp trên quy đổi.
 *
 * Nốt thường (theo wiki):
 *  - nhấn sớm hơn cửa sổ MISS: không có tác dụng
 *  - nhấn trong (MEH, MISS]: tính MISS và tiêu thụ nốt
 *  - không nhấn tới hết cửa sổ OK: MISS (không có MEH muộn)
 *
 * Nốt hold: MỘT phán định duy nhất từ sai lệch đầu (head) và sai lệch nhả (tail):
 *  PERFECT: head <= 16·1.2   và head+tail <= 16·2.4
 *  GREAT:   head <= GREAT·1.1 và head+tail <= GREAT·2.2
 *  GOOD:    head <= GOOD      và head+tail <= GOOD·2
 *  OK:      head <= OK        và head+tail <= OK·2
 *  MEH:     còn lại (không miss)
 *  Nhả phím giữa thân nốt => tối đa MEH. Không giữ phím trong cửa sổ đuôi (từ MEH sớm tới OK muộn) => MISS.
 *
 * Đơn giản hoá có chủ đích: nhấn "trễ" một hold sau khi hết cửa sổ OK của head vẫn cứu được
 * (kết quả tối đa MEH) cho tới khi cửa sổ đuôi bắt đầu.
 */
class JudgementEngine(
    private val chart: ManiaChart,
    private val tracker: ScoreTracker,
    private val listener: Listener,
    val windows: HitWindows = HitWindows(chart.overallDifficulty),
) {
    interface Listener {
        /** Một nốt đã có phán định cuối cùng. [errorMs] = thời điểm thực - thời điểm lý tưởng (âm = sớm). */
        fun onJudgement(note: ChartNote, judgement: Judgement, errorMs: Double)

        /** Nốt vừa được đánh trúng (không phải miss): lúc để phát hitsound. */
        fun onHit(note: ChartNote)
    }

    private val keys = chart.keys
    private val notes = chart.notes

    /** Trạng thái từng nốt, đọc được từ renderer. */
    val state = IntArray(notes.size) // STATE_*
    val broken = BooleanArray(notes.size)
    private val headError = DoubleArray(notes.size)

    /** Chỉ số (trong chart.columns[c]) của nốt đầu tiên chưa DONE. */
    private val pointer = IntArray(keys)

    /** Phím đang được giữ. */
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

            if (err < -windows.miss) return // quá sớm: không có tác dụng

            if (err > windows.ok) {
                // Chỉ có thể là hold đang chờ cứu muộn (nốt thường đã bị advance() xử lý miss).
                if (!n.isHold) { resolve(n, Judgement.MISS, err); k++; continue }
                val next = nextPending(ids, k + 1)
                if (next != null && timeMs >= notes[next].timeMs - windows.miss) {
                    // cú nhấn này thuộc về nốt kế tiếp, hold cũ coi như bỏ lỡ
                    resolve(n, Judgement.MISS, err)
                    k++
                    continue
                }
                headError[id] = windows.ok + 1.0 // head muộn => tối đa MEH
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
        // advance() phải chạy TRƯỚC khi hạ keyDown: hold giữ quá hạn cần biết phím còn đang giữ.
        advance(timeMs)
        keyDown[column] = false

        val id = holdingNote(column) ?: return
        val n = notes[id]
        if (timeMs < n.endTimeMs - windows.meh) {
            broken[id] = true // nhả giữa thân nốt
            return
        }
        val tailError = abs(timeMs - n.endTimeMs)
        resolve(n, holdJudgement(id, tailError), timeMs - n.endTimeMs)
    }

    // ---------------------------------------------------------------- time

    /** Xử lý các nốt đã quá hạn tại thời điểm [timeMs]. Gọi mỗi khung hình. */
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
                    if (n.isHold && t <= n.endTimeMs - windows.meh) return // còn cứu muộn được
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

    /** Kết thúc bài: phán định nốt còn lại. */
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
