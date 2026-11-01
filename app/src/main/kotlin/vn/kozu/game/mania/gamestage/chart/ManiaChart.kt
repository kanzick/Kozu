package vn.kozu.game.mania.gamestage.chart

class NoteSample(

    val normalSet: Int,
    val additionSet: Int,

    val index: Int,

    val volume: Int,

    val hitSound: Int,

    val customFile: String?,
) {

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

class ChartNote(
    val id: Int,
    val column: Int,
    val timeMs: Int,
    val endTimeMs: Int,
    val sample: NoteSample,
) {
    val isHold: Boolean get() = endTimeMs > timeMs
}

class ManiaChart(
    val keys: Int,
    val overallDifficulty: Double,
    val hpDrain: Double,
    val audioFilename: String,
    val audioLeadInMs: Int,

    val bpm: Double,

    val notes: List<ChartNote>,
) {

    val columns: Array<IntArray>

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
