package vn.kozu.domain.model

import androidx.compose.ui.graphics.Color

enum class ThemeMode(
    val key: String,
    val label: String,
) {
    SYSTEM("system", "Follow System"),
    LIGHT("light", "Light Theme"),
    DARK("dark", "Dark Theme"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AppStyle(
    val key: String,
    val label: String,
) {
    EXPRESSIVE("expressive", "Expressive"),
    CLASSIC("classic", "Classic"),
    ;

    companion object {
        fun fromKey(key: String?): AppStyle = entries.firstOrNull { it.key == key } ?: EXPRESSIVE
    }
}

enum class NavigationStyle(
    val key: String,
    val label: String,
) {
    FLOATING("floating", "Floating"),
    CLASSIC("classic", "Classic"),
    ;

    companion object {
        fun fromKey(key: String?): NavigationStyle = entries.firstOrNull { it.key == key } ?: FLOATING
    }
}

enum class StartTab(
    val key: String,
    val label: String,
) {
    HOME("home", "Home"),
    EXPLORE("explore", "Explore"),
    LIBRARY("library", "Library"),
    GAME("game", "Game"),
    ;

    companion object {
        fun fromKey(key: String?): StartTab = entries.firstOrNull { it.key == key } ?: HOME
    }
}

enum class AccentColor(
    val key: String,
    val label: String,
    val color: Color,
) {
    PINK("pink", "Pink", Color(0xFFFF66AA)),
    PURPLE("purple", "Purple", Color(0xFF8C66FF)),
    BLUE("blue", "Blue", Color(0xFF3FA9F5)),
    TEAL("teal", "Teal", Color(0xFF2BC4A8)),
    GREEN("green", "Green", Color(0xFF7ACB3F)),
    YELLOW("yellow", "Yellow", Color(0xFFFFB800)),
    ORANGE("orange", "Orange", Color(0xFFFF8A4C)),
    RED("red", "Red", Color(0xFFFF5C5C)),
    ;

    companion object {
        fun fromKey(key: String?): AccentColor = entries.firstOrNull { it.key == key } ?: PINK
    }
}

enum class StreamMode(
    val key: String,
    val label: String,
) {
    FULL("full", "Full song"),
    PREVIEW("preview", "Preview only"),
    ;

    companion object {
        fun fromKey(key: String?): StreamMode = entries.firstOrNull { it.key == key } ?: FULL
    }
}

enum class ModeFilter(
    val key: String,
    val label: String,
    val apiValue: Int?,
) {
    ANY("any", "Any", null),
    OSU("osu", "osu!", 0),
    TAIKO("taiko", "Taiko", 1),
    CATCH("catch", "Catch", 2),
    MANIA("mania", "Mania", 3),
    ;

    companion object {
        fun fromKey(key: String?): ModeFilter = entries.firstOrNull { it.key == key } ?: ANY
    }
}

enum class StatusFilter(
    val key: String,
    val label: String,
    val apiValue: String?,
) {
    DEFAULT("default", "Ranked + Loved", null),
    RANKED("ranked", "Ranked", "ranked"),
    LOVED("loved", "Loved", "loved"),
    ANY("any", "Everything", "any"),
    ;

    companion object {
        fun fromKey(key: String?): StatusFilter = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class AppFont(
    val key: String,
    val label: String,
) {
    GOOGLE_SANS_FLEX("google_sans_flex", "Google Sans Flex (Rounded)"),
    GOOGLE_SANS("google_sans", "Google Sans"),
    KOZU("kozu", "Quicksand"),
    ROBOTO("roboto", "Roboto / Sans Serif"),
    SYSTEM("system", "System default"),
    ;

    /** Only the Flex family exposes the width and roundness axes. */
    val hasWidthAxis: Boolean get() = this == GOOGLE_SANS_FLEX

    companion object {
        fun fromKey(key: String?): AppFont = entries.firstOrNull { it.key == key } ?: GOOGLE_SANS_FLEX
    }
}

enum class LibraryNavigationMode(
    val key: String,
    val label: String,
) {
    COMPACT("compact", "Compact swipe tabs"),
    TAB_ROW("tab_row", "Visible tab row"),
    ;

    companion object {
        fun fromKey(key: String?): LibraryNavigationMode = entries.firstOrNull { it.key == key } ?: COMPACT
    }
}

enum class LibraryDensity(
    val key: String,
    val label: String,
) {
    COMFORTABLE("comfortable", "Comfortable"),
    COMPACT("compact", "Compact"),
    ;

    companion object {
        fun fromKey(key: String?): LibraryDensity = entries.firstOrNull { it.key == key } ?: COMPACT
    }
}

enum class PlaybackIcon(
    val key: String,
    val label: String,
) {
    ARROW("arrow", "Play arrow"),
    CIRCLE("circle", "Play circle"),
    CIRCLE_FILLED("circle_filled", "Filled play circle"),
    CIRCLE_OUTLINE("circle_outline", "Outlined play circle"),
    ;

    companion object {
        fun fromKey(key: String?): PlaybackIcon = entries.firstOrNull { it.key == key } ?: ARROW
    }
}

enum class PaletteStyle(
    val key: String,
    val label: String,
    val description: String,
) {
    TONAL_SPOT("tonal_spot", "Tonal Spot", "Balanced, calm colors"),
    VIBRANT("vibrant", "Vibrant", "Rich and saturated colors"),
    EXPRESSIVE("expressive", "Expressive", "Playful hues with strong contrast"),
    NEUTRAL("neutral", "Neutral", "Muted colors with a hint of tint"),
    MONOCHROME("monochrome", "Monochrome", "Grayscale with a single accent"),
    ;

    companion object {
        fun fromKey(key: String?): PaletteStyle = entries.firstOrNull { it.key == key } ?: TONAL_SPOT
    }
}

enum class CrossfadeDuration(
    val key: String,
    val label: String,
    val millis: Int,
) {
    OFF("off", "Disabled", 0),
    SHORT("3", "3 seconds", 3_000),
    MEDIUM("5", "5 seconds", 5_000),
    LONG("8", "8 seconds", 8_000),
    EXTRA_LONG("12", "12 seconds", 12_000),
    ;

    companion object {
        fun fromKey(key: String?): CrossfadeDuration = entries.firstOrNull { it.key == key } ?: OFF
    }
}

enum class MixSize(
    val count: Int,
    val label: String,
) {
    TWENTY_FIVE(25, "25 songs"),
    FIFTY(50, "50 songs"),
    HUNDRED(100, "100 songs"),
    ;

    companion object {
        fun fromCount(count: Int?): MixSize = entries.firstOrNull { it.count == count } ?: HUNDRED
    }
}

enum class QuickPicksSource(
    val key: String,
    val label: String,
) {
    MIX("mix", "Mixed picks"),
    TRENDING("trending", "Trending (Most played)"),
    NEW_RELEASES("new", "New releases"),
    LOVED("loved", "Loved picks"),
    ;

    companion object {
        fun fromKey(key: String?): QuickPicksSource = entries.firstOrNull { it.key == key } ?: MIX
    }
}

enum class QuickPicksLayout(
    val key: String,
    val label: String,
) {
    CAROUSEL("carousel", "Carousel"),
    COMPACT("compact", "Compact cards"),
    ;

    companion object {
        fun fromKey(key: String?): QuickPicksLayout = entries.firstOrNull { it.key == key } ?: CAROUSEL
    }
}

enum class ArtworkQuality(
    val key: String,
    val label: String,
    val description: String,
) {
    LOW("low", "Low (150px)", "Saves data"),
    MEDIUM("medium", "Medium (300px)", "Balanced"),
    HIGH("high", "High (original)", "Sharpest covers"),
    ;

    companion object {
        fun fromKey(key: String?): ArtworkQuality = entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

enum class ProxyType(
    val key: String,
    val label: String,
) {
    DISABLED("disabled", "Disabled"),
    HTTP("http", "HTTP"),
    SOCKS5("socks5", "SOCKS5"),
    ;

    companion object {
        fun fromKey(key: String?): ProxyType = entries.firstOrNull { it.key == key } ?: DISABLED
    }
}

enum class TopSize(
    val count: Int,
    val label: String,
) {
    TEN(10, "10 songs"),
    TWENTY_FIVE(25, "25 songs"),
    FIFTY(50, "50 songs"),
    HUNDRED(100, "100 songs"),
    ;

    companion object {
        fun fromCount(count: Int?): TopSize = entries.firstOrNull { it.count == count } ?: FIFTY
    }
}

data class ProxySettings(
    val type: ProxyType = ProxyType.DISABLED,
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
) {
    val isActive: Boolean get() = type != ProxyType.DISABLED && host.isNotBlank() && port in 1..65535
}

enum class UpdateChannel(
    val key: String,
    val label: String,
    val description: String,
    val isPrerelease: Boolean,
    val branch: String,
) {
    STABLE("stable", "Stable", "Public releases", false, "main"),
    DEV("dev", "Dev", "Prerelease builds", true, "dev"),
    ;

    companion object {
        fun fromKey(key: String?): UpdateChannel = entries.firstOrNull { it.key == key } ?: STABLE
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appStyle: AppStyle = AppStyle.EXPRESSIVE,
    val navigationStyle: NavigationStyle = NavigationStyle.FLOATING,
    val startTab: StartTab = StartTab.HOME,
    val amoled: Boolean = false,
    val artworkColors: Boolean = true,
    val accent: AccentColor = AccentColor.PINK,
    val displayName: String = "",
    val osuClientId: String = "",
    val osuClientSecret: String = "",
    val streamMode: StreamMode = StreamMode.FULL,
    val wifiOnlyStreaming: Boolean = false,
    val streamCacheMb: Int = 256,
    val skipSilence: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerGains: List<Float> = List(5) { 0f },
    val disabledMirrors: Set<String> = emptySet(),
    val customMirrors: List<Mirror> = emptyList(),
    val keepFullBeatmap: Boolean = false,
    val wifiOnlyDownloads: Boolean = false,
    val autoUpdateDownloads: Boolean = true,
    val showExplicit: Boolean = false,
    val modeFilter: ModeFilter = ModeFilter.ANY,
    val statusFilter: StatusFilter = StatusFilter.DEFAULT,
    val lastSyncAt: Long = 0L,
    val appFont: AppFont = AppFont.GOOGLE_SANS_FLEX,
    val appearance: AppearancePrefs = AppearancePrefs(),
    val libraryNavigationMode: LibraryNavigationMode = LibraryNavigationMode.COMPACT,
    val libraryDensity: LibraryDensity = LibraryDensity.COMPACT,
    val libraryGrid: Boolean = false,
    val playbackIcon: PlaybackIcon = PlaybackIcon.ARROW,
    val paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    val highRefreshRate: Boolean = true,
    val performanceMode: Boolean = false,
    val blurPlayer: Boolean = true,
    val blurIntensity: Int = 72,
    val showFileInfo: Boolean = false,
    val compactNavigation: Boolean = false,
    val navCornerRadius: Int = 32,
    val showMixCard: Boolean = true,
    val artworkQualityWifi: ArtworkQuality = ArtworkQuality.HIGH,
    val artworkQualityMobile: ArtworkQuality = ArtworkQuality.MEDIUM,
    val imageCacheMb: Int = 128,
    val keepPlayingAfterClose: Boolean = true,
    val resumeOnHeadset: Boolean = false,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val crossfade: CrossfadeDuration = CrossfadeDuration.OFF,
    val hiFiMode: Boolean = false,
    val hardwareOffload: Boolean = false,
    val rememberShuffle: Boolean = true,
    val showQueueHistory: Boolean = false,
    val autoplay: Boolean = true,
    val avoidRepeats: Boolean = true,
    val dedupeQueue: Boolean = true,
    val preloadNext: Boolean = true,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val hapticFeedback: Boolean = true,
    val autoOpenPlayer: Boolean = false,
    val artworkSwipe: Boolean = true,
    val keepScreenOn: Boolean = false,
    val doubleTapSearch: Boolean = true,
    val preferUnicode: Boolean = false,
    val showMapper: Boolean = true,
    val quickPicksSource: QuickPicksSource = QuickPicksSource.MIX,
    val quickPicksLayout: QuickPicksLayout = QuickPicksLayout.CAROUSEL,
    val mixSize: MixSize = MixSize.HUNDRED,
    val topSize: TopSize = TopSize.FIFTY,
    val proxy: ProxySettings = ProxySettings(),
    val autoSyncLibrary: Boolean = true,
    val downloadLimitMb: Int = 0,
    val autoCleanCache: Boolean = false,
    val autoUpdateCheck: Boolean = true,
    val lastUpdateCheckAt: Long = 0L,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
) {
    val hasCredentials: Boolean get() = osuClientId.isNotBlank() && osuClientSecret.isNotBlank()
    val activeMirrors: List<Mirror> get() = Mirrors.active(customMirrors, disabledMirrors)
}
