package vn.kozu.game.mania.gamestage.chart

/**
 * Thông tin hitsound đã được "giải" cho từng nốt (gộp TimingPoint + hit sample riêng của nốt).
 * Dữ liệu thuần, không đụng tới Android.
 */
class NoteSample(
    /** 1 = normal, 2 = soft, 3 = drum */
    val normalSet: Int,
    val additionSet: Int,
    /** 0/1 = không hậu tố, >= 2 = hậu tố số (vd normal-hitnormal2.wav) */
    val index: Int,
    /** 0..100 */
    val volume: Int,
    /** cờ bit osu!: 1 normal, 2 whistle, 4 finish, 8 clap (0 cũng nghĩa là normal) */
    val hitSound: Int,
    /** tên file riêng khai báo trong hit sample (có thể null) */
    val customFile: String?,
) {
    /**
     * Danh sách tên file (không đuôi) cần phát cho nốt này theo quy ước đặt tên của osu!:
     * `<set>-hitnormal[idx]`, `<set>-hitwhistle[idx]`, ...
     * Nếu có [customFile] thì nó thay cho hitnormal và được trả nguyên tên.
     */
    fun sampleNames(): List<String> {
        val out = ArrayList<String>(3)
        val suffix = if (index > 1) index.toString() else ""
        val custom = customFile?.takeIf { it.isNotBlank() }
        if (hitSound == 0 || (hitSound and 1) != 0) {
            out += custom ?: "${setName(normalSet)}-hitnormal$suffix"
        } else if (custom != null) {
            out += custom
        }
        if ((hitSound and 2) != 0) out += "${setName(additionSet)}-hitwhistle$suffix"
        if ((hitSound and 4) != 0) out += "${setName(additionSet)}-hitfinish$suffix"
        if ((hitSound and 8) != 0) out += "${setName(additionSet)}-hitclap$suffix"
        return out
    }

    private fun setName(set: Int) = when (set) {
        2 -> "soft"
        3 -> "drum"
        else -> "normal"
    }
}

/** Một nốt trong chart. [endTimeMs] == [timeMs] với nốt thường, lớn hơn với nốt giữ (hold). */
class ChartNote(
    val id: Int,
    val column: Int,
    val timeMs: Int,
    val endTimeMs: Int,
    val sample: NoteSample,
) {
    val isHold: Boolean get() = endTimeMs > timeMs
}

/** Chart osu!mania đã phân tích: mọi thứ engine cần để chơi. */
class ManiaChart(
    val keys: Int,
    val overallDifficulty: Double,
    val hpDrain: Double,
    val audioFilename: String,
    val audioLeadInMs: Int,
    /** BPM của timing point gốc đầu tiên (0 nếu không có). */
    val bpm: Double,
    /** Đã sắp theo thời gian rồi theo cột; [ChartNote.id] == vị trí trong danh sách. */
    val notes: List<ChartNote>,
) {
    /** columns[c] = id các nốt của cột c theo thứ tự thời gian. */
    val columns: Array<IntArray>

    /** Thời điểm kết thúc của sự kiện cuối cùng (ms). */
    val lastEventTimeMs: Int

    val holdCount: Int

    val totalNotes: Int get() = notes.size

    init {
        val lists = Array(keys) { ArrayList<Int>() }
        var last = 0
        var holds = 0
        for (n in notes) {
            lists[n.column].add(n.id)
            if (n.endTimeMs > last) last = n.endTimeMs
            if (n.isHold) holds++
        }
        columns = Array(keys) { lists[it].toIntArray() }
        lastEventTimeMs = last
        holdCount = holds
    }
}
