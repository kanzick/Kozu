package vn.kozu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import vn.kozu.domain.model.Beatmap

internal enum class KozuTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    EXPLORE("Explore", Icons.Filled.Explore),
    SEARCH("Search", Icons.Filled.Search),
    LIBRARY("Library", Icons.Filled.LibraryMusic)
}

internal enum class KozuThemeMode(val label: String, val preferenceValue: String) {
    SYSTEM("System", "system"),
    LIGHT("Light", "light"),
    DARK("Dark", "dark");

    companion object {
        fun fromPreference(value: String?): KozuThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

internal val demoBeatmaps = listOf(
    Beatmap(
        id = 1L,
        title = "夜に駆ける",
        artist = "YOASOBI",
        mapper = "Demo mapper",
        beatmapsetId = null
    ),
    Beatmap(
        id = 2L,
        title = "Lemon",
        artist = "Kenshi Yonezu",
        mapper = "Tripticon",
        beatmapsetId = 1313117L
    ),
    Beatmap(
        id = 3L,
        title = "Megalovania",
        artist = "Toby Fox",
        mapper = "Kyshiro",
        beatmapsetId = 387700L
    ),
    Beatmap(
        id = 4L,
        title = "Bad Apple!!",
        artist = "Masayoshi Minoshima",
        mapper = "Pisapou",
        beatmapsetId = 1936770L
    )
)
