package vn.kozu.domain.model

data class BeatmapDifficultyInfo(
    val id: Long,
    val version: String,
    val modeInt: Int,
    val stars: Double,
    val lengthSeconds: Int = 0,
    val drainSeconds: Int = 0,
    val bpm: Double = 0.0,
    val circleSize: Double = 0.0,
    val approachRate: Double = 0.0,
    val overallDifficulty: Double = 0.0,
    val hpDrain: Double = 0.0,
    val circles: Int = 0,
    val sliders: Int = 0,
    val spinners: Int = 0,
    val maxCombo: Int = 0,
    val status: String = "",
    val isConvert: Boolean = false,
)

data class BeatmapSetInfo(
    val beatmap: Beatmap,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val difficulties: List<BeatmapDifficultyInfo> = emptyList(),
    val isReduced: Boolean = false,
)

object GameModes {
    fun label(modeInt: Int): String =
        when (modeInt) {
            0 -> "osu!"
            1 -> "osu!taiko"
            2 -> "osu!catch"
            3 -> "osu!mania"
            else -> "Other"
        }
}
