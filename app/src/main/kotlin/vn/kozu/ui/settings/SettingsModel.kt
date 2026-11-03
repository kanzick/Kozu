package vn.kozu.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class SettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    ROOT("Settings", "", Icons.Rounded.Tune),
    ACCOUNT("Accounts", "Manage osu! sign-in and API credentials", Icons.Rounded.AccountCircle),
    MUSIC("Music", "Browse defaults, library sync and artwork", Icons.Rounded.LibraryMusic),
    CONTENT("Content", "Catalog, titles, privacy and network", Icons.Rounded.Language),
    APPEARANCE("Appearance", "Themes, density, shapes and sliders", Icons.Rounded.Palette),
    TYPOGRAPHY("Typography", "Font, text size, weight and width", Icons.Rounded.TextFields),
    PLAYER_STYLE("Player & mini player", "Customize the player bar and full player", Icons.Rounded.PlayCircle),
    PLAYBACK("Playback", "Audio behavior, crossfade, and background play", Icons.Rounded.MusicNote),
    BEHAVIOR("Behavior", "Gestures, haptics, and navigation behavior", Icons.Rounded.TouchApp),
    BACKUP("Backup & Restore", "Export and recover your personal app data", Icons.Rounded.Upload),
    DEVELOPER("Developer Options", "Maintenance and diagnostics", Icons.Rounded.DeveloperMode),
    STORAGE("Storage Sense", "Manage caches, artwork and old downloads", Icons.Rounded.Storage),
    EQUALIZER("Equalizer", "Adjust audio frequencies and presets", Icons.Rounded.GraphicEq),
    DEVICE("Device Capabilities", "Audio specs, codecs, and decoder info", Icons.Rounded.DeveloperBoard),
    DOWNLOADS("Downloads", "Manage offline beatmaps and storage", Icons.Rounded.CloudDownload),
    SOURCES("Audio sources", "Stream mirrors and full-song access", Icons.Rounded.Dns),
    STATS("Listening history", "Plays, favorites and top artists", Icons.Rounded.BarChart),
    ABOUT("About", "App info, version, and credits", Icons.Rounded.Info),
    LICENSES("Licenses & attribution", "Open source libraries and services", Icons.Rounded.Layers),
}

enum class Setting(
    val page: SettingsPage,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val keywords: String = "",
) {
    LOGIN(
        SettingsPage.ACCOUNT,
        Icons.Rounded.AccountCircle,
        "osu! account",
        "Sign in with your osu! account to sync favourites and profile.",
        "login sign in oauth profile",
    ),
    OSU_API(
        SettingsPage.ACCOUNT,
        Icons.Rounded.Code,
        "osu! API credentials",
        "Client ID and secret used for the official osu! catalog.",
        "client id secret api key",
    ),

    DEFAULT_STATUS(
        SettingsPage.MUSIC,
        Icons.Rounded.FilterAlt,
        "Default status filter",
        "Which beatmap status the catalog shows first.",
        "ranked loved filter category",
    ),
    DEFAULT_MODE(
        SettingsPage.MUSIC,
        Icons.Rounded.Explore,
        "Default game mode",
        "Limit the catalog to a single ruleset by default.",
        "osu taiko catch mania ruleset",
    ),
    SYNC_NOW(
        SettingsPage.MUSIC,
        Icons.Rounded.Sync,
        "Sync library",
        "Refresh favourites, playlists and downloads now.",
        "refresh update library",
    ),
    AUTO_SYNC(
        SettingsPage.MUSIC,
        Icons.Rounded.Cached,
        "Sync on launch",
        "Refresh the library automatically when the app starts.",
        "automatic",
    ),
    ARTWORK_WIFI(
        SettingsPage.MUSIC,
        Icons.Rounded.Image,
        "Wi-Fi artwork quality",
        "Resolution for covers on Wi-Fi.",
        "cover image resolution",
    ),
    ARTWORK_MOBILE(
        SettingsPage.MUSIC,
        Icons.Rounded.Image,
        "Mobile artwork quality",
        "Resolution for covers on mobile data.",
        "cover image resolution data saver",
    ),
    IMAGE_CACHE(
        SettingsPage.MUSIC,
        Icons.Rounded.Storage,
        "Artwork cache limit",
        "Maximum disk space used by cached covers.",
        "image cache size",
    ),

    PREFER_UNICODE(
        SettingsPage.CONTENT,
        Icons.Rounded.TextFields,
        "Prefer original titles",
        "Show the original (unicode) song title and artist when available.",
        "unicode romanised japanese",
    ),
    SHOW_MAPPER(SettingsPage.CONTENT, Icons.Rounded.Star, "Show mapper names", "Display who mapped each beatmap.", "creator"),
    TOP_SIZE(SettingsPage.CONTENT, Icons.Rounded.BarChart, "My top size", "Number of songs in Top stats lists.", "top stats"),
    SHOW_EXPLICIT(
        SettingsPage.CONTENT,
        Icons.Rounded.Security,
        "Show explicit content",
        "Include explicit beatmaps in search and All beatmaps.",
        "nsfw adult mature",
    ),
    SHOW_MIX_CARD(SettingsPage.CONTENT, Icons.Rounded.Shuffle, "Show Your Mix", "Display the shuffle mix card on Home.", "collage home"),
    DISPLAY_NAME(SettingsPage.CONTENT, Icons.Rounded.AccountCircle, "Display name", "Name used in the Home greeting.", "greeting username"),
    PROXY(
        SettingsPage.CONTENT,
        Icons.Rounded.Cloud,
        "Proxy server",
        "Route network traffic through an HTTP or SOCKS5 proxy.",
        "network vpn socks http",
    ),
    CLEAR_HISTORY(
        SettingsPage.CONTENT,
        Icons.Rounded.History,
        "Clear listening history",
        "Remove every play recorded on this device.",
        "privacy stats delete",
    ),
    CLEAR_SEARCHES(
        SettingsPage.CONTENT,
        Icons.Rounded.DeleteSweep,
        "Clear recent searches",
        "Forget your recent search terms.",
        "privacy search history delete",
    ),

    APP_THEME(
        SettingsPage.APPEARANCE,
        Icons.Rounded.WbSunny,
        "App theme",
        "Switch between light, dark, or follow system appearance.",
        "dark light mode",
    ),
    PURE_BLACK(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Palette,
        "Pure black",
        "Pure black surfaces to save battery on OLED screens.",
        "amoled oled dark",
    ),
    APP_FONT(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatSize,
        "App font",
        "Google Sans Flex, Google Sans, Quicksand, Roboto or the system font.",
        "typeface quicksand roboto google sans flex",
    ),
    TEXT_SIZE(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatSize,
        "Text size",
        "Scale all text in the app. Layouts adapt so nothing overflows.",
        "font scale bigger smaller large",
    ),
    FONT_WEIGHT(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatBold,
        "Text weight",
        "Make text lighter or bolder across the app.",
        "bold thin heavy light font",
    ),
    FONT_WIDTH(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.SwapHoriz,
        "Letter width",
        "Narrow or widen letters. Available for Google Sans Flex.",
        "condensed expanded stretch font width",
    ),
    FONT_ROUNDNESS(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.RoundedCorner,
        "Letter roundness",
        "Soften letter ends from sharp to fully rounded. Available for Google Sans Flex.",
        "rounded soft font",
    ),
    RESET_TYPOGRAPHY(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.RestartAlt,
        "Reset typography",
        "Back to the default font, size, weight and width.",
        "default font reset",
    ),
    UI_DENSITY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCompact,
        "Layout density",
        "Comfortable, compact or dense spacing. Applies to every screen in the app.",
        "compact spacing size ui scale padding",
    ),
    COMPACT_MODE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Compress,
        "Compact mode",
        "Tighter spacing and smaller controls across the whole app.",
        "dense small spacing",
    ),
    ARTWORK_SHAPE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Category,
        "Album art shape",
        "Shape of covers everywhere: lists, cards, player and mini player.",
        "cover circle cookie square rounded",
    ),
    SLIDER_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.LinearScale,
        "Slider style",
        "Look of every slider in settings, equalizer and game options.",
        "seek bar wavy expressive thumb",
    ),
    ICON_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Widgets,
        "Player icon style",
        "Rounded, outlined or sharp playback icons.",
        "icons skip shuffle repeat",
    ),
    PLAYER_PAGE_ENTRY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.PlayCircle,
        "Player & mini player",
        "Style, colors, progress, artwork and gestures of the player bar.",
        "mini player bar now playing",
    ),
    TYPOGRAPHY_ENTRY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.TextFields,
        "Typography",
        "Font family, text size, weight, width and roundness.",
        "font text size",
    ),
    MINI_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.ViewCarousel,
        "Mini player style",
        "Floating pill, docked bar, expressive card or minimal.",
        "player bar layout",
    ),
    MINI_COLORS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Palette,
        "Mini player colors",
        "Accent gradient, tonal, solid accent or neutral.",
        "player bar color theme",
    ),
    MINI_PROGRESS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.LinearScale,
        "Mini player progress",
        "Ring around the artwork, thin line, wavy line or hidden.",
        "progress bar ring wavy",
    ),
    MINI_ART_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Image,
        "Mini player artwork",
        "Shape of the cover in the player bar.",
        "cover circle cookie",
    ),
    MINI_CORNER(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.RoundedCorner,
        "Mini player corner radius",
        "Adjust how round the player bar is.",
        "rounded shape corner",
    ),
    MINI_CONTROLS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.SkipNext,
        "Skip buttons",
        "Show previous and next buttons in the player bar.",
        "previous next controls",
    ),
    MINI_ARTIST(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Person,
        "Show artist",
        "Show the artist under the title in the player bar.",
        "subtitle",
    ),
    MINI_SPIN(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Autorenew,
        "Spinning artwork",
        "Rotate round artwork while a song plays.",
        "rotate vinyl",
    ),
    MINI_SWIPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Swipe,
        "Swipe to skip",
        "Swipe the player bar sideways to change track. Swipe down to dismiss.",
        "gesture next previous",
    ),
    SEEK_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.GraphicEq,
        "Seek bar style",
        "Wavy, flat, thick pill or thin line in the full player.",
        "progress slider wavy",
    ),
    PLAYER_ART_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Image,
        "Player artwork shape",
        "Shape of the large cover in the full player.",
        "cover circle cookie",
    ),
    TRANSPORT_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.PlayCircle,
        "Control buttons",
        "Shape of play, previous and next buttons.",
        "transport pill round square",
    ),
    TOGGLE_ROW(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Shuffle,
        "Shuffle, repeat and like row",
        "Show the row of toggles below the playback controls.",
        "shuffle repeat favorite",
    ),
    RESET_PLAYER_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.RestartAlt,
        "Reset player style",
        "Restore the default player bar and full player look.",
        "default reset",
    ),
    PLAYBACK_ICON(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.PlayCircle,
        "Playback icon",
        "Choose the play symbol used across player and library controls.",
        "play pause control icon",
    ),
    LIBRARY_LAYOUT(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCarousel,
        "Library navigation",
        "Choose compact swipe navigation or a visible tab row.",
        "library tabs compact layout",
    ),
    LIBRARY_DENSITY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewAgenda,
        "Library rows",
        "Show denser track rows in library lists. Compact mode already applies this everywhere.",
        "library compact dense spacing",
    ),
    APP_ICON(SettingsPage.APPEARANCE, Icons.Rounded.AutoAwesome, "App icon", "Select your preferred app icon.", "launcher logo"),
    APP_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.AutoAwesome,
        "App style",
        "Choose how Kozu looks and feels.",
        "expressive classic material",
    ),
    ACCENT_COLOR(SettingsPage.APPEARANCE, Icons.Rounded.Palette, "Accent color", "Used when nothing is playing.", "theme color seed"),
    ARTWORK_COLORS(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Image,
        "Artwork colors",
        "Match the colors to the current album art.",
        "dynamic color cover",
    ),
    PALETTE_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Palette,
        "Palette style",
        "How strongly colors are generated from the seed.",
        "tonal vibrant expressive neutral monochrome",
    ),
    HIGH_REFRESH(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Speed,
        "High refresh rate",
        "Use the fastest display mode for smoother scrolling and animations.",
        "120hz fps smooth",
    ),
    PERFORMANCE_MODE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Bolt,
        "Performance mode",
        "Reduce animations and blurs to keep the app smooth and save battery.",
        "low end battery saver",
    ),
    BLUR_PLAYER(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.BlurOn,
        "Blurred player background",
        "Blend the album art behind the full player.",
        "backdrop",
    ),
    BLUR_INTENSITY(SettingsPage.PLAYER_STYLE, Icons.Rounded.BlurOn, "Blur intensity", "How soft the player backdrop looks.", "radius"),
    SHOW_FILE_INFO(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Info,
        "Show player file info",
        "Show codec, bitrate, and sample rate in the player.",
        "format audio quality",
    ),
    NAV_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCarousel,
        "Navigation bar style",
        "Choose the appearance for the navigation bar.",
        "pill floating classic",
    ),
    COMPACT_NAV(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Compress,
        "Icons-only navigation",
        "Hide navigation labels and reduce the navigation bar height.",
        "labels compact nav",
    ),
    NAV_RADIUS(
        SettingsPage.APPEARANCE,
        Icons.Rounded.RoundedCorner,
        "Navigation bar corner radius",
        "Adjust the corner radius of the floating navigation bar.",
        "rounded shape",
    ),
    DEFAULT_TAB(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Home,
        "Default tab",
        "Choose the screen Kozu opens with.",
        "start launch home explore library game",
    ),

    KEEP_PLAYING(
        SettingsPage.PLAYBACK,
        Icons.Rounded.MusicNote,
        "Keep playing after closing",
        "If off, removing the app from recents stops playback.",
        "background service recents",
    ),
    BATTERY_OPT(
        SettingsPage.PLAYBACK,
        Icons.Rounded.BatteryChargingFull,
        "Battery optimization",
        "Disable battery optimization to prevent playback interruptions.",
        "doze background",
    ),
    RESUME_HEADSET(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Headphones,
        "Resume when headphones reconnect",
        "Resume playback automatically when headphones reconnect.",
        "bluetooth wired",
    ),
    PAUSE_HEADSET(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Headphones,
        "Pause when headphones disconnect",
        "Pause playback when the audio output is unplugged.",
        "noisy bluetooth wired",
    ),
    CROSSFADE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.GraphicEq,
        "Crossfade",
        "Enable smooth transition between songs.",
        "fade transition gapless",
    ),
    HIFI(
        SettingsPage.PLAYBACK,
        Icons.Rounded.HighQuality,
        "Hi-Fi mode",
        "Float 32-bit audio output. Disable if playback stutters on your device.",
        "float pcm lossless",
    ),
    OFFLOAD(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Memory,
        "Hardware audio offload",
        "Use the hardware DSP for audio decoding to save battery.",
        "dsp",
    ),
    REMEMBER_SHUFFLE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Shuffle,
        "Persistent shuffle",
        "Remember shuffle and repeat settings even after closing the app.",
        "repeat",
    ),
    SHOW_QUEUE_HISTORY(
        SettingsPage.PLAYBACK,
        Icons.AutoMirrored.Rounded.QueueMusic,
        "Show queue history",
        "Show previously played songs in the queue.",
        "previous",
    ),
    AUTOPLAY(
        SettingsPage.PLAYBACK,
        Icons.AutoMirrored.Rounded.QueueMusic,
        "Dynamic queue (Autoplay)",
        "Automatically refill the queue with related beatmaps when it runs out.",
        "radio similar endless",
    ),
    AVOID_REPEATS(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Shuffle,
        "Avoid repetitive songs",
        "Skip recently played tracks when refilling the dynamic queue.",
        "recent history",
    ),
    DEDUPE_QUEUE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.FilterAlt,
        "Deduplicate queue entries",
        "Remove duplicate songs from the queue.",
        "duplicate",
    ),
    PRELOAD_NEXT(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Download,
        "Queue preloading",
        "Preload the next song so playback starts instantly.",
        "buffer gapless",
    ),
    STREAM_QUALITY(SettingsPage.PLAYBACK, Icons.Rounded.Tune, "Stream quality", "Play full songs or short previews.", "preview full"),
    WIFI_ONLY_STREAM(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Wifi,
        "Wi-Fi only streaming",
        "Stream only while connected to Wi-Fi.",
        "mobile data",
    ),
    SKIP_SILENCE(SettingsPage.PLAYBACK, Icons.AutoMirrored.Rounded.VolumeUp, "Skip silence", "Skip quiet sections of a track.", "gap"),

    HAPTICS(SettingsPage.BEHAVIOR, Icons.Rounded.Vibration, "Haptic feedback", "Enable vibration feedback across the app.", "vibration"),
    AUTO_OPEN_PLAYER(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.PlayCircle,
        "Open player on play",
        "Expand the full player when you start a song.",
        "now playing",
    ),
    ARTWORK_SWIPE(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Swipe,
        "Artwork gestures",
        "Swipe the cover to skip tracks or close the player.",
        "swipe",
    ),
    KEEP_SCREEN_ON(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.ScreenLockPortrait,
        "Keep screen on in player",
        "Prevent the screen from sleeping while the full player is open.",
        "awake",
    ),
    DOUBLE_TAP_SEARCH(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Search,
        "Double-tap Search to type",
        "Double-tap the Search tab to focus the search field instantly.",
        "keyboard",
    ),
    OSU_LINKS(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Link,
        "osu! link integration",
        "Open osu.ppy.sh beatmap links directly in Kozu.",
        "intercept deep link url",
    ),

    EQUALIZER_BANDS(
        SettingsPage.EQUALIZER,
        Icons.Rounded.Equalizer,
        "Equalizer",
        "Adjust audio frequencies and presets.",
        "bass treble preset eq",
    ),
    BASS_BOOST(SettingsPage.EQUALIZER, Icons.Rounded.GraphicEq, "Bass boost", "Strengthen low frequencies.", "bass"),
    VIRTUALIZER(
        SettingsPage.EQUALIZER,
        Icons.Rounded.Headphones,
        "Spatial audio",
        "Let Android spatialize audio when the device and output support it.",
        "surround spatial headphones",
    ),
    LOUDNESS(SettingsPage.EQUALIZER, Icons.AutoMirrored.Rounded.VolumeUp, "Loudness", "Boost overall perceived volume.", "gain amplify"),

    STREAM_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.Storage,
        "Stream cache limit",
        "Maximum disk space for cached streams.",
        "playback cache size",
    ),
    CLEAR_STREAM_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        "Clear stream cache",
        "Delete cached audio streams.",
        "free space",
    ),
    CLEAR_IMAGE_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        "Clear artwork cache",
        "Delete cached cover images.",
        "free space thumbnails",
    ),
    AUTO_CLEAN(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        "Automatic cleanup",
        "Clear old stream cache when the app starts and the limit is exceeded.",
        "smart storage",
    ),
    DOWNLOAD_LIMIT(
        SettingsPage.STORAGE,
        Icons.Rounded.Download,
        "Download storage limit",
        "Maximum storage allocated for offline beatmaps.",
        "offline",
    ),
    FORGOTTEN_TRACKS(
        SettingsPage.STORAGE,
        Icons.Rounded.History,
        "Forgotten tracks",
        "Offline songs you have not played in 60 days.",
        "unused old downloads review",
    ),
    CLEAN_ALL(
        SettingsPage.STORAGE,
        Icons.Rounded.DeleteSweep,
        "Clean all",
        "Clear the stream and artwork caches in one step.",
        "free space reclaim",
    ),

    REFRESH_DEVICE(
        SettingsPage.DEVICE,
        Icons.Rounded.Sync,
        "Refresh device info",
        "Read the current audio, memory and storage state again.",
        "reload specs",
    ),

    KEEP_FULL(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Download,
        "Keep full beatmap",
        "Keep the complete .osz so it can be played in the game.",
        "osz",
    ),
    WIFI_ONLY_DOWNLOADS(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Wifi,
        "Wi-Fi only downloads",
        "Download beatmaps on Wi-Fi only.",
        "mobile data",
    ),
    AUTO_UPDATE_DOWNLOADS(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Sync,
        "Auto-update downloads",
        "Re-download beatmaps when osu! publishes an update.",
        "outdated",
    ),

    EXPORT_BACKUP(
        SettingsPage.BACKUP,
        Icons.Rounded.Upload,
        "Export backup",
        "Save settings, favourites, playlists and history to a file.",
        "save",
    ),
    IMPORT_BACKUP(SettingsPage.BACKUP, Icons.Rounded.Restore, "Import backup", "Restore data from a Kozu backup file.", "restore load"),

    RESET_SETTINGS(
        SettingsPage.DEVELOPER,
        Icons.Rounded.RestartAlt,
        "Reset all settings",
        "Restore every preference to its default value.",
        "factory",
    ),
    REFRESH_CATALOG(
        SettingsPage.DEVELOPER,
        Icons.Rounded.Sync,
        "Force catalog refresh",
        "Clear cached catalog pages and reload from osu!.",
        "reload",
    ),
    COPY_DEBUG(
        SettingsPage.DEVELOPER,
        Icons.Rounded.ContentCopy,
        "Copy debug info",
        "Copy device and app details for bug reports.",
        "diagnostics",
    ),
    REBUILD_LIBRARY(
        SettingsPage.DEVELOPER,
        Icons.Rounded.LibraryMusic,
        "Rebuild library",
        "Re-sync favourites, playlists, history and downloads from osu!.",
        "resync repair",
    ),
    REGENERATE_STATS(
        SettingsPage.DEVELOPER,
        Icons.Rounded.BarChart,
        "Regenerate stats",
        "Recompute listening statistics from your play history.",
        "recompute repair",
    ),
    CLEAR_ALL_CACHES(
        SettingsPage.DEVELOPER,
        Icons.Rounded.CleaningServices,
        "Clear all caches",
        "Delete stream, artwork and catalog caches.",
        "free space reset",
    ),

    CHECK_UPDATES(
        SettingsPage.ABOUT,
        Icons.Rounded.SystemUpdate,
        "Check for updates",
        "Look for a newer Kozu release on GitHub.",
        "version release",
    ),
    AUTO_UPDATE_CHECK(
        SettingsPage.ABOUT,
        Icons.Rounded.Sync,
        "Automatic update check",
        "Check for a new release when the app starts.",
        "background",
    ),
    SHARE_APP(SettingsPage.ABOUT, Icons.Rounded.Share, "Share app", "Recommend Kozu to others.", "invite"),
    RATE_APP(SettingsPage.ABOUT, Icons.Rounded.Star, "Rate Kozu", "Star us on GitHub.", "github review"),
    LICENSES_ENTRY(
        SettingsPage.ABOUT,
        Icons.Rounded.Layers,
        "Licenses & attribution",
        "Open source libraries, fonts and services.",
        "credits third party",
    ),
    PRIVACY_ENTRY(SettingsPage.ABOUT, Icons.Rounded.Security, "Privacy policy", "Read our privacy commitment.", "data"),
}

private val searchSkip = setOf(Setting.LICENSES_ENTRY)

object SettingsSearch {
    fun search(query: String): List<Setting> {
        val tokens = query.lowercase().split(' ', '-', '_').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return Setting.entries
            .filterNot { it in searchSkip }
            .map { it to score(it, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun score(
        setting: Setting,
        tokens: List<String>,
    ): Int {
        val title = setting.title.lowercase()
        val description = setting.description.lowercase()
        val keywords = setting.keywords.lowercase()
        val page = setting.page.title.lowercase()
        var total = 0
        for (token in tokens) {
            var best = 0
            if (title == token) best = maxOf(best, 100)
            if (title.startsWith(token)) best = maxOf(best, 80)
            if (title.contains(token)) best = maxOf(best, 60)
            if (keywords.split(' ').any { it.startsWith(token) }) best = maxOf(best, 45)
            if (page.contains(token)) best = maxOf(best, 30)
            if (description.contains(token)) best = maxOf(best, 20)
            if (best == 0 && token.length >= 4) {
                val words = (title + " " + keywords).split(' ').filter { it.length >= 3 }
                if (words.any { distance(it.take(token.length + 1), token) <= 1 }) {
                    best = 25
                } else if (isSubsequence(token, title)) {
                    best = 12
                }
            }
            if (best == 0) return 0
            total += best
        }
        return total
    }

    private fun isSubsequence(
        needle: String,
        haystack: String,
    ): Boolean {
        var index = 0
        for (char in haystack) {
            if (index < needle.length && char == needle[index]) index++
        }
        return index == needle.length
    }

    private fun distance(
        a: String,
        b: String,
    ): Int {
        if (a == b) return 0
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
