package vn.kozu.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class Beatmap(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String = "",
    val beatmapsetId: Long? = null,
    val starRating: Float = 4.2f,
    val bpm: Int = 175,
    val lengthSeconds: Int = 210,
    val genre: String = "Anime",
    val difficultyName: String = "Insane",
    val previewUrl: String? = null
) {
    val coverUrl: String?
        get() {
            val setId = beatmapsetId ?: return null
            return "https://assets.ppy.sh/beatmaps/$setId/covers/cover.jpg"
        }

    val cardUrl: String?
        get() {
            val setId = beatmapsetId ?: return null
            return "https://assets.ppy.sh/beatmaps/$setId/covers/card.jpg"
        }
}

enum class KozuTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    EXPLORE("Explore", Icons.Filled.Explore),
    SEARCH("Search", Icons.Filled.Search),
    LIBRARY("Library", Icons.Filled.LibraryMusic)
}

enum class KozuThemeMode(val label: String, val preferenceValue: String) {
    SYSTEM("System", "system"),
    LIGHT("Light", "light"),
    DARK("Dark", "dark");

    companion object {
        fun fromPreference(value: String?): KozuThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

val demoBeatmaps = listOf(
    Beatmap(
        id = 1L,
        title = "夜に駆ける (Yoru ni Kakeru)",
        artist = "YOASOBI",
        mapper = "Monstrata",
        beatmapsetId = 1184714L,
        starRating = 5.42f,
        bpm = 130,
        lengthSeconds = 261,
        genre = "J-Pop",
        difficultyName = "Racing Into The Night"
    ),
    Beatmap(
        id = 2L,
        title = "Lemon",
        artist = "Kenshi Yonezu",
        mapper = "Tripticon",
        beatmapsetId = 1313117L,
        starRating = 4.15f,
        bpm = 87,
        lengthSeconds = 255,
        genre = "J-Pop",
        difficultyName = "Insane"
    ),
    Beatmap(
        id = 3L,
        title = "Megalovania",
        artist = "Toby Fox",
        mapper = "Kyshiro",
        beatmapsetId = 387700L,
        starRating = 6.28f,
        bpm = 120,
        lengthSeconds = 156,
        genre = "Video Game",
        difficultyName = "Bad Time"
    ),
    Beatmap(
        id = 4L,
        title = "Bad Apple!! feat. nomico",
        artist = "Masayoshi Minoshima",
        mapper = "Pisapou",
        beatmapsetId = 1936770L,
        starRating = 5.12f,
        bpm = 138,
        lengthSeconds = 318,
        genre = "Touhou",
        difficultyName = "Shadow Art"
    ),
    Beatmap(
        id = 5L,
        title = "FREEDOM DiVE",
        artist = "xi",
        mapper = "Nakagawa-Kanon",
        beatmapsetId = 39804L,
        starRating = 7.41f,
        bpm = 222,
        lengthSeconds = 258,
        genre = "Hardcore",
        difficultyName = "FOUR DIMENSIONS"
    ),
    Beatmap(
        id = 6L,
        title = "Kyuukuraphobia",
        artist = "Iyowa",
        mapper = "ponpokona",
        beatmapsetId = 1750112L,
        starRating = 4.86f,
        bpm = 168,
        lengthSeconds = 204,
        genre = "Vocaloid",
        difficultyName = "Extra"
    ),
    Beatmap(
        id = 7L,
        title = "The Pretender",
        artist = "Foo Fighters",
        mapper = "RLC",
        beatmapsetId = 292301L,
        starRating = 5.89f,
        bpm = 173,
        lengthSeconds = 269,
        genre = "Rock",
        difficultyName = "Insane"
    ),
    Beatmap(
        id = 8L,
        title = "Blue Bird",
        artist = "Ikimono Gakari",
        mapper = "Atsuro",
        beatmapsetId = 1017409L,
        starRating = 4.38f,
        bpm = 152,
        lengthSeconds = 216,
        genre = "Anime",
        difficultyName = "Habataku"
    )
)

