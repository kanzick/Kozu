package vn.kozu.game.mania.previewstage.osu

/** Raw data read from an .osu file: only [General], [Metadata], [Difficulty], and the background image in [Events]. */
data class OsuBeatmap(
    val audioFilename: String,
    val audioLeadIn: Int,
    val previewTime: Int,
    val countdown: Int,
    val sampleSet: String,
    val stackLeniency: Double,
    val mode: Int,
    val letterboxInBreaks: Int,
    val widescreenStoryboard: Int,
    val specialStyle: Int,

    val title: String,
    val titleUnicode: String,
    val artist: String,
    val artistUnicode: String,
    val creator: String,
    val version: String,
    val source: String,
    val tags: String,
    val beatmapId: Long,
    val beatmapSetId: Long,

    val hpDrainRate: Double,
    val circleSize: Double,
    val overallDifficulty: Double,
    val approachRate: Double,
    val sliderMultiplier: Double,
    val sliderTickRate: Double,

    val backgroundFilename: String?,
)

/** Plain Kotlin/JVM parser with no Android dependencies, so it can be tested locally. */
object OsuParser {
    const val MODE_MANIA = 3

    // 0,0,"bg.jpg",0,0   or   Background,0,"bg.jpg",0,0
    private val BACKGROUND = Regex("""^(?:0|Background)\s*,\s*-?\d+\s*,\s*(?:"([^"]*)"|([^,]*))""")

    fun parse(text: String): OsuBeatmap {
        val general = HashMap<String, String>()
        val metadata = HashMap<String, String>()
        val difficulty = HashMap<String, String>()
        var background: String? = null

        var section: String? = null
        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("//")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length - 1)
                continue
            }
            val current = section ?: continue
            when (current) {
                "General" -> putKeyValue(general, line)
                "Metadata" -> putKeyValue(metadata, line)
                "Difficulty" -> putKeyValue(difficulty, line)
                "Events" -> if (background == null) {
                    BACKGROUND.find(line)?.let { m ->
                        val name = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
                        if (name.isNotEmpty()) background = name
                    }
                }
            }
        }

        // Default matches osu!: PreviewTime = -1 means "not set".
        return OsuBeatmap(
            audioFilename = general["AudioFilename"].orEmpty(),
            audioLeadIn = general.int("AudioLeadIn", 0),
            previewTime = general.int("PreviewTime", -1),
            countdown = general.int("Countdown", 0),
            sampleSet = general["SampleSet"].orEmpty(),
            stackLeniency = general.dbl("StackLeniency", 0.7),
            mode = general.int("Mode", 0),
            letterboxInBreaks = general.int("LetterboxInBreaks", 0),
            widescreenStoryboard = general.int("WidescreenStoryboard", 0),
            specialStyle = general.int("SpecialStyle", 0),

            title = metadata["Title"].orEmpty(),
            titleUnicode = metadata["TitleUnicode"].orEmpty(),
            artist = metadata["Artist"].orEmpty(),
            artistUnicode = metadata["ArtistUnicode"].orEmpty(),
            creator = metadata["Creator"].orEmpty(),
            version = metadata["Version"].orEmpty(),
            source = metadata["Source"].orEmpty(),
            tags = metadata["Tags"].orEmpty(),
            beatmapId = metadata.long("BeatmapID", 0),
            beatmapSetId = metadata.long("BeatmapSetID", 0),

            hpDrainRate = difficulty.dbl("HPDrainRate", 5.0),
            circleSize = difficulty.dbl("CircleSize", 5.0),
            overallDifficulty = difficulty.dbl("OverallDifficulty", 5.0),
            // osu!: if ApproachRate is missing, use OverallDifficulty.
            approachRate = difficulty.dbl("ApproachRate", difficulty.dbl("OverallDifficulty", 5.0)),
            sliderMultiplier = difficulty.dbl("SliderMultiplier", 1.4),
            sliderTickRate = difficulty.dbl("SliderTickRate", 1.0),

            backgroundFilename = background,
        )
    }

    private fun putKeyValue(into: MutableMap<String, String>, line: String) {
        val i = line.indexOf(':')
        if (i <= 0) return
        into[line.substring(0, i).trim()] = line.substring(i + 1).trim()
    }

    private fun Map<String, String>.int(key: String, def: Int): Int =
        this[key]?.toDoubleOrNull()?.toInt() ?: def

    private fun Map<String, String>.long(key: String, def: Long): Long =
        this[key]?.toLongOrNull() ?: def

    private fun Map<String, String>.dbl(key: String, def: Double): Double =
        this[key]?.toDoubleOrNull() ?: def
}
