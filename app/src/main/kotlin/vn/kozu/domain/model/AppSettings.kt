package vn.kozu.domain.model

import androidx.compose.ui.graphics.Color

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AccentColor(val key: String, val label: String, val color: Color) {
    PINK("pink", "Pink", Color(0xFFFF66AA)),
    PURPLE("purple", "Purple", Color(0xFF8C66FF)),
    BLUE("blue", "Blue", Color(0xFF3FA9F5)),
    TEAL("teal", "Teal", Color(0xFF2BC4A8)),
    GREEN("green", "Green", Color(0xFF7ACB3F)),
    YELLOW("yellow", "Yellow", Color(0xFFFFB800)),
    ORANGE("orange", "Orange", Color(0xFFFF8A4C)),
    RED("red", "Red", Color(0xFFFF5C5C));

    companion object {
        fun fromKey(key: String?): AccentColor = entries.firstOrNull { it.key == key } ?: PINK
    }
}

enum class StreamMode(val key: String, val label: String) {
    FULL("full", "Full song"),
    PREVIEW("preview", "Preview only");

    companion object {
        fun fromKey(key: String?): StreamMode = entries.firstOrNull { it.key == key } ?: FULL
    }
}

enum class ModeFilter(val key: String, val label: String, val apiValue: Int?) {
    ANY("any", "Any", null),
    OSU("osu", "osu!", 0),
    TAIKO("taiko", "Taiko", 1),
    CATCH("catch", "Catch", 2),
    MANIA("mania", "Mania", 3);

    companion object {
        fun fromKey(key: String?): ModeFilter = entries.firstOrNull { it.key == key } ?: ANY
    }
}

enum class StatusFilter(val key: String, val label: String, val apiValue: String?) {
    DEFAULT("default", "Default", null),
    RANKED("ranked", "Ranked", "ranked"),
    LOVED("loved", "Loved", "loved"),
    ANY("any", "Everything", "any");

    companion object {
        fun fromKey(key: String?): StatusFilter = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val artworkColors: Boolean = true,
    val accent: AccentColor = AccentColor.PINK,
    val displayName: String = "",
    val osuClientId: String = "",
    val osuClientSecret: String = "",
    val streamMode: StreamMode = StreamMode.FULL,
    val wifiOnlyStreaming: Boolean = false,
    val streamCacheMb: Int = 512,
    val skipSilence: Boolean = false,
    val officialDownload: Boolean = true,
    val disabledMirrors: Set<String> = emptySet(),
    val customMirrors: List<Mirror> = emptyList(),
    val keepFullBeatmap: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val autoUpdateDownloads: Boolean = true,
    val showExplicit: Boolean = true,
    val modeFilter: ModeFilter = ModeFilter.ANY,
    val statusFilter: StatusFilter = StatusFilter.DEFAULT,
    val lastSyncAt: Long = 0L
) {
    val hasCredentials: Boolean get() = osuClientId.isNotBlank() && osuClientSecret.isNotBlank()
    val activeMirrors: List<Mirror> get() = Mirrors.active(customMirrors, disabledMirrors)
}
