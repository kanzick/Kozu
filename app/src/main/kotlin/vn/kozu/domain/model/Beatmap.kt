package vn.kozu.domain.model

data class Beatmap(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String = "",
    val status: String = "",
    val updatedAt: Long = 0L,
    val modes: Set<Int> = emptySet(),
    val titleUnicode: String = "",
    val artistUnicode: String = "",
    val playCount: Long = 0L,
    val favouriteCount: Int = 0,
    val bpm: Double = 0.0,
    val genreId: Int = 0,
    val languageId: Int = 0,
    val hasVideo: Boolean = false,
    val hasStoryboard: Boolean = false,
    val nsfw: Boolean = false,
    val source: String = "",
    val tags: String = "",
    val rankedAt: Long = 0L,
    val submittedAt: Long = 0L,
    val minStars: Double = 0.0,
    val maxStars: Double = 0.0,
    val difficultyStars: List<Double> = emptyList(),
) {
    val thumbUrl: String get() = "https://assets.ppy.sh/beatmaps/$id/covers/list@2x.jpg"
    val artUrl: String get() = "https://assets.ppy.sh/beatmaps/$id/covers/cover@2x.jpg"


    val fullArtUrl: String get() = "https://assets.ppy.sh/beatmaps/$id/covers/raw.jpg"
    val webUrl: String get() = "https://osu.ppy.sh/beatmapsets/$id"
}

fun Beatmap.displayTitle(preferUnicode: Boolean): String = if (preferUnicode && titleUnicode.isNotBlank()) titleUnicode else title

fun Beatmap.displayArtist(preferUnicode: Boolean): String = if (preferUnicode && artistUnicode.isNotBlank()) artistUnicode else artist

data class BeatmapDifficulty(
    val id: Long,
    val version: String,
    val mode: String,
    val difficultyRating: Double,
    val keys: Double,
    val bpm: Double,
    val lengthSeconds: Int,
)
