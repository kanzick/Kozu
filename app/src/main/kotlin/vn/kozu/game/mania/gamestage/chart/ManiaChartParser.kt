package vn.kozu.game.mania.gamestage.chart

class ChartParseException(message: String) : Exception(message)

/**
 * Parses a mania-mode (Mode: 3) .osu file into [ManiaChart]. Plain Kotlin/JVM, so it can be tested locally.
 *
 * osu! rules:
 *  - number of columns = CircleSize
 *  - note column = floor(x * keys / 512)
 *  - type bit 0 = regular note, bit 7 (128) = hold; a hold's endTime is the first field in the extras
 *  - note hitsound comes from its own hit sample; fields set to 0 fall back to the active TimingPoint
 */
object ManiaChartParser {
    const val MAX_KEYS = 20

    private class Timing(
        val time: Double,
        val beatLength: Double,
        val sampleSet: Int,
        val sampleIndex: Int,
        val volume: Int,
        val uninherited: Boolean,
    )

    fun parse(text: String): ManiaChart {
        val general = HashMap<String, String>()
        val difficulty = HashMap<String, String>()
        val timingLines = ArrayList<String>()
        val objectLines = ArrayList<String>()

        var section: String? = null
        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("//")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length - 1)
                continue
            }
            when (section) {
                "General" -> putKeyValue(general, line)
                "Difficulty" -> putKeyValue(difficulty, line)
                "TimingPoints" -> timingLines += line
                "HitObjects" -> objectLines += line
            }
        }

        val mode = general["Mode"]?.toDoubleOrNull()?.toInt() ?: 0
        if (mode != 3) throw ChartParseException("This is not a Mania chart (Mode=$mode)")

        val keys = (difficulty["CircleSize"]?.toDoubleOrNull() ?: 4.0).toInt().coerceIn(1, MAX_KEYS)
        val od = (difficulty["OverallDifficulty"]?.toDoubleOrNull() ?: 5.0).coerceIn(0.0, 10.0)
        val hp = (difficulty["HPDrainRate"]?.toDoubleOrNull() ?: 5.0).coerceIn(0.0, 10.0)
        val defaultSet = when (general["SampleSet"]?.trim()?.lowercase()) {
            "soft" -> 2
            "drum" -> 3
            else -> 1
        }

        val timings = parseTimings(timingLines, defaultSet)
        val notes = parseObjects(objectLines, keys, timings, defaultSet)
        if (notes.isEmpty()) throw ChartParseException("This chart contains no notes")

        val firstBpmPoint = timings.firstOrNull { it.uninherited && it.beatLength > 0 }
        return ManiaChart(
            keys = keys,
            overallDifficulty = od,
            hpDrain = hp,
            audioFilename = general["AudioFilename"].orEmpty(),
            audioLeadInMs = general["AudioLeadIn"]?.toDoubleOrNull()?.toInt() ?: 0,
            bpm = firstBpmPoint?.let { 60000.0 / it.beatLength } ?: 0.0,
            notes = notes,
        )
    }

    private fun parseTimings(lines: List<String>, defaultSet: Int): List<Timing> {
        val out = ArrayList<Timing>()
        for (line in lines) {
            val p = line.split(',')
            val time = p.getOrNull(0)?.trim()?.toDoubleOrNull() ?: continue
            val beat = p.getOrNull(1)?.trim()?.toDoubleOrNull() ?: continue
            val set = p.getOrNull(3)?.trim()?.toIntOrNull()?.takeIf { it in 1..3 } ?: defaultSet
            val index = p.getOrNull(4)?.trim()?.toIntOrNull() ?: 0
            val volume = p.getOrNull(5)?.trim()?.toIntOrNull()?.coerceIn(0, 100) ?: 100
            val uninherited = p.getOrNull(6)?.trim()?.let { it != "0" } ?: (beat > 0)
            out += Timing(time, beat, set, index, volume, uninherited)
        }
        // sortedBy is stable: for equal timestamps, the later entry in the file takes precedence.
        return out.sortedBy { it.time }
    }

    /** Timing point active at [time]; before the first point, use the first one (matching osu!). */
    private fun timingAt(timings: List<Timing>, time: Double): Timing? {
        if (timings.isEmpty()) return null
        var lo = 0
        var hi = timings.size - 1
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (timings[mid].time <= time) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return timings[ans]
    }

    private class RawNote(val column: Int, val time: Int, val end: Int, val sample: NoteSample)

    private fun parseObjects(
        lines: List<String>,
        keys: Int,
        timings: List<Timing>,
        defaultSet: Int,
    ): List<ChartNote> {
        val raws = ArrayList<RawNote>(lines.size)
        for (line in lines) {
            val p = line.split(',')
            if (p.size < 5) continue
            val x = p[0].trim().toDoubleOrNull() ?: continue
            val time = p[2].trim().toDoubleOrNull()?.toInt() ?: continue
            val type = p[3].trim().toIntOrNull() ?: continue
            val hitSound = p[4].trim().toIntOrNull() ?: 0
            val isHold = (type and 128) != 0
            val isNote = (type and 1) != 0
            if (!isHold && !isNote) continue

            val extras = p.getOrNull(5)?.split(':').orEmpty()
            val end: Int
            val fields: List<String>
            if (isHold) {
                end = extras.getOrNull(0)?.trim()?.toDoubleOrNull()?.toInt() ?: time
                fields = extras.drop(1)
            } else {
                end = time
                fields = extras
            }

            val column = ((x * keys / 512.0).toInt()).coerceIn(0, keys - 1)
            val t = timingAt(timings, time.toDouble())
            val normalSet = fields.intAt(0)?.takeIf { it != 0 } ?: t?.sampleSet ?: defaultSet
            val additionSet = fields.intAt(1)?.takeIf { it != 0 } ?: normalSet
            val index = fields.intAt(2)?.takeIf { it != 0 } ?: t?.sampleIndex ?: 0
            val volume = fields.intAt(3)?.takeIf { it != 0 } ?: t?.volume ?: 100
            val custom = fields.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }
            raws += RawNote(
                column = column,
                time = time,
                end = if (end > time) end else time,
                sample = NoteSample(
                    normalSet = normalSet.coerceIn(1, 3),
                    additionSet = additionSet.coerceIn(1, 3),
                    index = index,
                    volume = volume.coerceIn(0, 100),
                    hitSound = hitSound,
                    customFile = custom,
                ),
            )
        }

        raws.sortWith(compareBy<RawNote>({ it.time }, { it.column }))
        return raws.mapIndexed { i, r -> ChartNote(i, r.column, r.time, r.end, r.sample) }
    }

    private fun List<String>.intAt(i: Int): Int? = getOrNull(i)?.trim()?.toIntOrNull()

    private fun putKeyValue(into: MutableMap<String, String>, line: String) {
        val i = line.indexOf(':')
        if (i <= 0) return
        into[line.substring(0, i).trim()] = line.substring(i + 1).trim()
    }
}
