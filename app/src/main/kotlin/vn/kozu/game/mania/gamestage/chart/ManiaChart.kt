package vn.kozu.game.mania.gamestage.chart

/**
 * Resolved hitsound information for each note (merged from the TimingPoint and the note's own hit sample).
 * Plain data, with no Android dependencies.
 */
class NoteSample(
    /** 1 = normal, 2 = soft, 3 = drum */
    val normalSet: Int,
    val additionSet: Int,
    /** 0/1 = no suffix, >= 2 = numbered suffix (e.g. normal-hitnormal2.wav). */
    val index: Int,
    /** 0..100 */
    val volume: Int,
    /** osu! bit flags: 1 normal, 2 whistle, 4 finish, 8 clap (0 also means normal). */
    val hitSound: Int,
    /** Custom filename declared in the hit sample (may be null). */
    val customFile: String?,
) {
    /**
     * Filenames (without extensions) to play for this note, following osu!'s naming convention:
     * `<set>-hitnormal[idx]`, `<set>-hitwhistle[idx]`, ...
     * If [customFile] is present, it replaces hitnormal and is returned unchanged.
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

/** A chart note. [endTimeMs] == [timeMs] for a regular note, and is greater for a hold note. */
class ChartNote(
    val id: Int,
    val column: Int,
    val timeMs: Int,
    val endTimeMs: Int,
    val sample: NoteSample,
) {
    val isHold: Boolean get() = endTimeMs > timeMs
}

/** Parsed osu!mania chart: everything the engine needs to play. */
class ManiaChart(
    val keys: Int,
    val overallDifficulty: Double,
    val hpDrain: Double,
    val audioFilename: String,
    val audioLeadInMs: Int,
    /** BPM of the first uninherited timing point (0 if absent). */
    val bpm: Double,
    /** Sorted by time, then column; [ChartNote.id] is the position in the list. */
    val notes: List<ChartNote>,
) {
    /** columns[c] = note IDs in column c, ordered by time. */
    val columns: Array<IntArray>

    /** End time of the last event (ms). */
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
