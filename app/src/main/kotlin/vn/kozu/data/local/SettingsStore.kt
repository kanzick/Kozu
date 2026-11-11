package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.AppearancePrefs
import vn.kozu.domain.model.AppFont
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.AppStyle
import vn.kozu.domain.model.ArtworkQuality
import vn.kozu.domain.model.CrossfadeDuration
import vn.kozu.domain.model.LibraryDensity
import vn.kozu.domain.model.LibraryNavigationMode
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.MixSize
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.NavigationStyle
import vn.kozu.domain.model.PaletteStyle
import vn.kozu.domain.model.PlaybackIcon
import vn.kozu.domain.model.ProxySettings
import vn.kozu.domain.model.ProxyType
import vn.kozu.domain.model.QuickPicksLayout
import vn.kozu.domain.model.QuickPicksSource
import vn.kozu.domain.model.StartTab
import vn.kozu.domain.model.StatusFilter
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.ThemePreset
import vn.kozu.domain.model.TopSize
import vn.kozu.domain.model.UpdateChannel

class SettingsStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val store = appContext.kozuDataStore
    private val prefsCache = appContext.getSharedPreferences("kozu_theme_sync", Context.MODE_PRIVATE)

    val cachedThemeMode: ThemeMode get() = initialThemeMode(appContext)

    val cachedThemePreset: ThemePreset get() =
        prefsCache.getString("theme_preset", null)?.let(ThemePreset::fromKey)
            ?: if (prefsCache.contains("accent")) ThemePreset.CUSTOM else ThemePreset.OSU


    private val onboardingPrefs = appContext.getSharedPreferences("kozu_onboarding", Context.MODE_PRIVATE)
    var onboardingCompleted: Boolean
        get() = onboardingPrefs.getBoolean("completed", false)
        set(value) {
            onboardingPrefs.edit().putBoolean("completed", value).apply()
        }

    val cachedAccent: AccentColor get() =
        AccentColor.fromKey(prefsCache.getString("accent", AccentColor.PINK.key))
    val cachedAmoled: Boolean get() = prefsCache.getBoolean("amoled", false)
    val cachedStartTab: StartTab get() =
        StartTab.fromKey(prefsCache.getString("start_tab", StartTab.HOME.key))
    val cachedAppFont: AppFont get() = AppFont.fromKey(prefsCache.getString("app_font", AppFont.GOOGLE_SANS_FLEX.key))
    val cachedAppearance: AppearancePrefs get() = AppearancePrefs.fromJson(prefsCache.getString("appearance_json", null))
    val cachedPaletteStyle: PaletteStyle get() =
        PaletteStyle.fromKey(prefsCache.getString("palette_style", PaletteStyle.TONAL_SPOT.key))
    val cachedHighRefreshRate: Boolean get() = prefsCache.getBoolean("high_refresh_rate", true)
    val cachedPerformanceMode: Boolean get() = prefsCache.getBoolean("performance_mode", false)

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val appStyle = stringPreferencesKey("app_style")
        val navigationStyle = stringPreferencesKey("navigation_style")
        val startTab = stringPreferencesKey("start_tab")
        val amoled = booleanPreferencesKey("amoled")
        val artworkColors = booleanPreferencesKey("artwork_colors")
        val accent = stringPreferencesKey("accent")
        val themePreset = stringPreferencesKey("theme_preset")
        val displayName = stringPreferencesKey("display_name")
        val clientId = stringPreferencesKey("osu_client_id")
        val clientSecret = stringPreferencesKey("osu_client_secret")
        val recentSearches = stringPreferencesKey("recent_searches")
        val streamMode = stringPreferencesKey("stream_mode")
        val wifiOnlyStreaming = booleanPreferencesKey("wifi_only_streaming")
        val streamCacheMb = intPreferencesKey("stream_cache_mb")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
        val equalizerGains = stringPreferencesKey("equalizer_gains")
        val disabledMirrors = stringPreferencesKey("disabled_mirrors")
        val customMirrors = stringPreferencesKey("custom_mirrors")
        val keepFull = booleanPreferencesKey("keep_full_beatmap")
        val wifiOnlyDownloads = booleanPreferencesKey("wifi_only_downloads")
        val autoUpdate = booleanPreferencesKey("auto_update_downloads")
        val explicit = booleanPreferencesKey("show_explicit")
        val modeFilter = stringPreferencesKey("mode_filter")
        val statusFilter = stringPreferencesKey("status_filter")
        val lastSync = longPreferencesKey("last_sync_at")
        val appFont = stringPreferencesKey("app_font")
        val libraryNavigationMode = stringPreferencesKey("library_navigation_mode")
        val libraryDensity = stringPreferencesKey("library_density")
        val libraryGrid = booleanPreferencesKey("library_grid")
        val playbackIcon = stringPreferencesKey("playback_icon")
        val paletteStyle = stringPreferencesKey("palette_style")
        val highRefreshRate = booleanPreferencesKey("high_refresh_rate")
        val performanceMode = booleanPreferencesKey("performance_mode")
        val blurPlayer = booleanPreferencesKey("blur_player")
        val blurIntensity = intPreferencesKey("blur_intensity")
        val showFileInfo = booleanPreferencesKey("show_file_info")
        val compactNavigation = booleanPreferencesKey("compact_navigation")
        val navCornerRadius = intPreferencesKey("nav_corner_radius")
        val showMixCard = booleanPreferencesKey("show_mix_card")
        val mixSize = intPreferencesKey("mix_size")
        val artworkQualityWifi = stringPreferencesKey("artwork_quality_wifi")
        val artworkQualityMobile = stringPreferencesKey("artwork_quality_mobile")
        val imageCacheMb = intPreferencesKey("image_cache_mb")
        val keepPlayingAfterClose = booleanPreferencesKey("keep_playing_after_close")
        val resumeOnHeadset = booleanPreferencesKey("resume_on_headset")
        val pauseOnHeadsetDisconnect = booleanPreferencesKey("pause_on_headset_disconnect")
        val crossfade = stringPreferencesKey("crossfade")
        val hiFiMode = booleanPreferencesKey("hifi_mode")
        val hardwareOffload = booleanPreferencesKey("hardware_offload")
        val rememberShuffle = booleanPreferencesKey("remember_shuffle")
        val showQueueHistory = booleanPreferencesKey("show_queue_history")
        val autoplay = booleanPreferencesKey("autoplay")
        val avoidRepeats = booleanPreferencesKey("avoid_repeats")
        val dedupeQueue = booleanPreferencesKey("dedupe_queue")
        val preloadNext = booleanPreferencesKey("preload_next")
        val bassBoost = intPreferencesKey("bass_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val loudness = intPreferencesKey("loudness")
        val hapticFeedback = booleanPreferencesKey("haptic_feedback")
        val autoOpenPlayer = booleanPreferencesKey("auto_open_player")
        val artworkSwipe = booleanPreferencesKey("artwork_swipe")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val doubleTapSearch = booleanPreferencesKey("double_tap_search")
        val preferUnicode = booleanPreferencesKey("prefer_unicode")
        val showMapper = booleanPreferencesKey("show_mapper")
        val quickPicksSource = stringPreferencesKey("quick_picks_source")
        val quickPicksLayout = stringPreferencesKey("quick_picks_layout")
        val topSize = intPreferencesKey("top_size")
        val proxyType = stringPreferencesKey("proxy_type")
        val proxyHost = stringPreferencesKey("proxy_host")
        val proxyPort = intPreferencesKey("proxy_port")
        val proxyUser = stringPreferencesKey("proxy_user")
        val proxyPass = stringPreferencesKey("proxy_pass")
        val autoSyncLibrary = booleanPreferencesKey("auto_sync_library")
        val downloadLimitMb = intPreferencesKey("download_limit_mb")
        val autoCleanCache = booleanPreferencesKey("auto_clean_cache")
        val autoUpdateCheck = booleanPreferencesKey("auto_update_check")
        val lastUpdateCheck = longPreferencesKey("last_update_check")
        val updateChannel = stringPreferencesKey("update_channel")
        val libraryTabOrder = stringPreferencesKey("library_tab_order")
        val libraryCustomOrder = stringPreferencesKey("library_custom_order_v1")
        val librarySortMode = stringPreferencesKey("library_sort_mode")
        val librarySortAscending = booleanPreferencesKey("library_sort_ascending")
        val appearance = stringPreferencesKey("appearance_json")
    }

    val libraryTabOrder: Flow<List<String>> =
        store.data.map { prefs ->
            val defaults = listOf("songs", "liked", "playlists", "downloads")
            val saved =
                prefs[Keys.libraryTabOrder]
                    .orEmpty()
                    .split(',')
                    .filter { it in defaults }
            (saved + defaults).distinct()
        }

    suspend fun setLibraryTabOrder(order: List<String>) {
        val allowed = listOf("songs", "liked", "playlists", "downloads")
        val normalized = (order.filter { it in allowed } + allowed).distinct()
        store.edit { it[Keys.libraryTabOrder] = normalized.joinToString(",") }
    }

    val librarySortPreferences: Flow<Pair<String, Boolean>> =
        store.data.map { prefs ->
            (prefs[Keys.librarySortMode] ?: "recent") to (prefs[Keys.librarySortAscending] ?: false)
        }.distinctUntilChanged()

    suspend fun setLibrarySortPreferences(mode: String, ascending: Boolean) {
        val allowed = setOf("recent", "title", "artist", "mapper", "custom")
        store.edit { prefs ->
            prefs[Keys.librarySortMode] = mode.takeIf { it in allowed } ?: "recent"
            prefs[Keys.librarySortAscending] = ascending
        }
    }


    val libraryCustomOrders: Flow<Map<String, List<Long>>> =
        store.data.map { prefs -> decodeLibraryCustomOrders(prefs[Keys.libraryCustomOrder]) }.distinctUntilChanged()

    suspend fun setLibraryCustomOrder(collection: String, beatmapIds: List<Long>) {
        val allowed = setOf("songs", "liked", "downloads", "game")
        if (collection !in allowed) return
        store.edit { prefs ->
            val current = decodeLibraryCustomOrders(prefs[Keys.libraryCustomOrder]).toMutableMap()
            current[collection] = beatmapIds.asSequence().filter { it > 0L }.distinct().take(20_000).toList()
            val json = JSONObject()
            current.forEach { (key, ids) ->
                if (key in allowed) json.put(key, JSONArray(ids))
            }
            prefs[Keys.libraryCustomOrder] = json.toString()
        }
    }

    private fun decodeLibraryCustomOrders(raw: String?): Map<String, List<Long>> =
        try {
            val root = JSONObject(raw.orEmpty())
            listOf("songs", "liked", "downloads", "game").associateWith { key ->
                val array = root.optJSONArray(key) ?: JSONArray()
                List(minOf(array.length(), 20_000)) { index -> array.optLong(index) }.filter { it > 0L }.distinct()
            }.filterValues { it.isNotEmpty() }
        } catch (_: JSONException) {
            emptyMap()
        }

    val settings: Flow<AppSettings> =
        store.data
            .map { prefs ->
                AppSettings(
                    themeMode = ThemeMode.fromKey(prefs[Keys.theme]),
                    appStyle = AppStyle.fromKey(prefs[Keys.appStyle]),
                    navigationStyle = NavigationStyle.fromKey(prefs[Keys.navigationStyle]),
                    startTab = StartTab.fromKey(prefs[Keys.startTab]),
                    amoled = prefs[Keys.amoled] ?: false,
                    artworkColors = prefs[Keys.artworkColors] ?: true,
                    accent = AccentColor.fromKey(prefs[Keys.accent]),
                    themePreset =
                        prefs[Keys.themePreset]?.let(ThemePreset::fromKey)
                            ?: if (prefs[Keys.accent] != null) ThemePreset.CUSTOM else ThemePreset.OSU,
                    displayName = prefs[Keys.displayName].orEmpty(),
                    osuClientId = prefs[Keys.clientId].orEmpty(),
                    osuClientSecret = prefs[Keys.clientSecret].orEmpty(),
                    streamMode = StreamMode.fromKey(prefs[Keys.streamMode]),
                    wifiOnlyStreaming = prefs[Keys.wifiOnlyStreaming] ?: false,
                    streamCacheMb = prefs[Keys.streamCacheMb] ?: 256,
                    skipSilence = prefs[Keys.skipSilence] ?: false,
                    equalizerEnabled = prefs[Keys.equalizerEnabled] ?: false,
                    equalizerGains = decodeEqualizerGains(prefs[Keys.equalizerGains]),
                    disabledMirrors =
                        prefs[Keys.disabledMirrors]
                            .orEmpty()
                            .split(',')
                            .filter { it.isNotBlank() }
                            .toSet(),
                    customMirrors = decodeMirrors(prefs[Keys.customMirrors]),
                    keepFullBeatmap = prefs[Keys.keepFull] ?: false,
                    wifiOnlyDownloads = prefs[Keys.wifiOnlyDownloads] ?: false,
                    autoUpdateDownloads = prefs[Keys.autoUpdate] ?: true,
                    showExplicit = prefs[Keys.explicit] ?: false,
                    modeFilter = ModeFilter.fromKey(prefs[Keys.modeFilter]),
                    statusFilter = StatusFilter.fromKey(prefs[Keys.statusFilter]),
                    lastSyncAt = prefs[Keys.lastSync] ?: 0L,
                    appFont = AppFont.fromKey(prefs[Keys.appFont]),
                    appearance = AppearancePrefs.fromJson(prefs[Keys.appearance]),
                    libraryNavigationMode = LibraryNavigationMode.fromKey(prefs[Keys.libraryNavigationMode]),
                    libraryDensity = LibraryDensity.fromKey(prefs[Keys.libraryDensity]),
                    libraryGrid = prefs[Keys.libraryGrid] ?: false,
                    playbackIcon = PlaybackIcon.fromKey(prefs[Keys.playbackIcon]),
                    paletteStyle = PaletteStyle.fromKey(prefs[Keys.paletteStyle]),
                    highRefreshRate = prefs[Keys.highRefreshRate] ?: true,
                    performanceMode = prefs[Keys.performanceMode] ?: false,
                    blurPlayer = prefs[Keys.blurPlayer] ?: true,
                    blurIntensity = (prefs[Keys.blurIntensity] ?: 72).coerceIn(0, 120),
                    showFileInfo = prefs[Keys.showFileInfo] ?: false,
                    compactNavigation = prefs[Keys.compactNavigation] ?: false,
                    navCornerRadius = (prefs[Keys.navCornerRadius] ?: 32).coerceIn(0, 32),
                    showMixCard = prefs[Keys.showMixCard] ?: true,
                    mixSize = MixSize.fromCount(prefs[Keys.mixSize]),
                    artworkQualityWifi = ArtworkQuality.fromKey(prefs[Keys.artworkQualityWifi] ?: ArtworkQuality.HIGH.key),
                    artworkQualityMobile = ArtworkQuality.fromKey(prefs[Keys.artworkQualityMobile]),
                    imageCacheMb = (prefs[Keys.imageCacheMb] ?: 128).coerceIn(32, 1024),
                    keepPlayingAfterClose = prefs[Keys.keepPlayingAfterClose] ?: true,
                    resumeOnHeadset = prefs[Keys.resumeOnHeadset] ?: false,
                    pauseOnHeadsetDisconnect = prefs[Keys.pauseOnHeadsetDisconnect] ?: true,
                    crossfade = CrossfadeDuration.fromKey(prefs[Keys.crossfade]),
                    hiFiMode = prefs[Keys.hiFiMode] ?: false,
                    hardwareOffload = prefs[Keys.hardwareOffload] ?: false,
                    rememberShuffle = prefs[Keys.rememberShuffle] ?: true,
                    showQueueHistory = prefs[Keys.showQueueHistory] ?: false,
                    autoplay = prefs[Keys.autoplay] ?: true,
                    avoidRepeats = prefs[Keys.avoidRepeats] ?: true,
                    dedupeQueue = prefs[Keys.dedupeQueue] ?: true,
                    preloadNext = prefs[Keys.preloadNext] ?: true,
                    bassBoost = (prefs[Keys.bassBoost] ?: 0).coerceIn(0, 1000),
                    virtualizer = (prefs[Keys.virtualizer] ?: 0).coerceIn(0, 1000),
                    loudness = (prefs[Keys.loudness] ?: 0).coerceIn(0, 1000),
                    hapticFeedback = prefs[Keys.hapticFeedback] ?: true,
                    autoOpenPlayer = prefs[Keys.autoOpenPlayer] ?: false,
                    artworkSwipe = prefs[Keys.artworkSwipe] ?: true,
                    keepScreenOn = prefs[Keys.keepScreenOn] ?: false,
                    doubleTapSearch = prefs[Keys.doubleTapSearch] ?: true,
                    preferUnicode = prefs[Keys.preferUnicode] ?: false,
                    showMapper = prefs[Keys.showMapper] ?: true,
                    quickPicksSource = QuickPicksSource.fromKey(prefs[Keys.quickPicksSource]),
                    quickPicksLayout = QuickPicksLayout.fromKey(prefs[Keys.quickPicksLayout]),
                    topSize = TopSize.fromCount(prefs[Keys.topSize]),
                    proxy =
                        ProxySettings(
                            type = ProxyType.fromKey(prefs[Keys.proxyType]),
                            host = prefs[Keys.proxyHost].orEmpty(),
                            port = prefs[Keys.proxyPort] ?: 0,
                            username = prefs[Keys.proxyUser].orEmpty(),
                            password = prefs[Keys.proxyPass].orEmpty(),
                        ),
                    autoSyncLibrary = prefs[Keys.autoSyncLibrary] ?: true,
                    downloadLimitMb = prefs[Keys.downloadLimitMb] ?: 0,
                    autoCleanCache = prefs[Keys.autoCleanCache] ?: false,
                    autoUpdateCheck = prefs[Keys.autoUpdateCheck] ?: true,
                    lastUpdateCheckAt = prefs[Keys.lastUpdateCheck] ?: 0L,
                    updateChannel = UpdateChannel.fromKey(prefs[Keys.updateChannel]),
                )
            }.distinctUntilChanged()

    val recentSearches: Flow<List<String>> =
        store.data
            .map { prefs ->
                prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() }
            }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putString("theme_mode", mode.key).commit()) {
                "Could not persist the startup theme preference"
            }
        }
        store.edit { it[Keys.theme] = mode.key }
    }

    suspend fun setAmoled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putBoolean("amoled", enabled).commit()) {
                "Could not persist the startup AMOLED preference"
            }
        }
        store.edit { it[Keys.amoled] = enabled }
    }

    suspend fun setAppStyle(style: AppStyle) = store.edit { it[Keys.appStyle] = style.key }

    suspend fun setNavigationStyle(style: NavigationStyle) = store.edit { it[Keys.navigationStyle] = style.key }

    suspend fun setStartTab(tab: StartTab) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putString("start_tab", tab.key).commit()) {
                "Could not persist the startup tab preference"
            }
        }
        store.edit { it[Keys.startTab] = tab.key }
    }

    suspend fun setArtworkColors(enabled: Boolean) = store.edit { it[Keys.artworkColors] = enabled }

    suspend fun setAccent(accent: AccentColor) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putString("accent", accent.key).commit()) {
                "Could not persist the startup accent preference"
            }
        }
        store.edit { it[Keys.accent] = accent.key }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putString("theme_preset", preset.key).commit()) {
                "Could not persist the startup theme preset"
            }
        }
        store.edit { it[Keys.themePreset] = preset.key }
    }

    suspend fun setDisplayName(name: String) = store.edit { it[Keys.displayName] = name.trim().take(24) }

    suspend fun setStreamMode(mode: StreamMode) = store.edit { it[Keys.streamMode] = mode.key }

    suspend fun setWifiOnlyStreaming(enabled: Boolean) = store.edit { it[Keys.wifiOnlyStreaming] = enabled }

    suspend fun setStreamCacheMb(mb: Int) = store.edit { it[Keys.streamCacheMb] = mb }

    suspend fun setSkipSilence(enabled: Boolean) = store.edit { it[Keys.skipSilence] = enabled }

    suspend fun setEqualizerEnabled(enabled: Boolean) = store.edit { it[Keys.equalizerEnabled] = enabled }

    suspend fun setEqualizerGains(gains: List<Float>) {
        require(gains.size == EQUALIZER_BAND_COUNT)
        require(gains.all(Float::isFinite))
        store.edit {
            it[Keys.equalizerGains] =
                gains.joinToString(",") { gain ->
                    gain.coerceIn(-1f, 1f).toString()
                }
        }
    }

    suspend fun setKeepFullBeatmap(enabled: Boolean) = store.edit { it[Keys.keepFull] = enabled }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) = store.edit { it[Keys.wifiOnlyDownloads] = enabled }

    suspend fun setAutoUpdateDownloads(enabled: Boolean) = store.edit { it[Keys.autoUpdate] = enabled }

    suspend fun setShowExplicit(enabled: Boolean) = store.edit { it[Keys.explicit] = enabled }

    suspend fun setModeFilter(filter: ModeFilter) = store.edit { it[Keys.modeFilter] = filter.key }

    suspend fun setStatusFilter(filter: StatusFilter) = store.edit { it[Keys.statusFilter] = filter.key }

    suspend fun setLastSync(timeMillis: Long) = store.edit { it[Keys.lastSync] = timeMillis }

    suspend fun setAppFont(font: AppFont) {
        withContext(Dispatchers.IO) { prefsCache.edit().putString("app_font", font.key).commit() }
        store.edit { it[Keys.appFont] = font.key }
    }


    suspend fun updateAppearance(transform: AppearancePrefs.() -> AppearancePrefs) {
        var json = ""
        store.edit { prefs ->
            val updated = AppearancePrefs.fromJson(prefs[Keys.appearance]).transform()
            json = updated.toJson()
            prefs[Keys.appearance] = json
        }
        withContext(Dispatchers.IO) { prefsCache.edit().putString("appearance_json", json).commit() }
    }

    suspend fun setLibraryNavigationMode(mode: LibraryNavigationMode) = store.edit { it[Keys.libraryNavigationMode] = mode.key }

    suspend fun setLibraryDensity(density: LibraryDensity) = store.edit { it[Keys.libraryDensity] = density.key }

    suspend fun setLibraryGrid(enabled: Boolean) = store.edit { it[Keys.libraryGrid] = enabled }

    suspend fun setPlaybackIcon(icon: PlaybackIcon) = store.edit { it[Keys.playbackIcon] = icon.key }

    suspend fun setPaletteStyle(style: PaletteStyle) {
        withContext(Dispatchers.IO) { prefsCache.edit().putString("palette_style", style.key).commit() }
        store.edit { it[Keys.paletteStyle] = style.key }
    }

    suspend fun setHighRefreshRate(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefsCache.edit().putBoolean("high_refresh_rate", enabled).commit() }
        store.edit { it[Keys.highRefreshRate] = enabled }
    }

    suspend fun setPerformanceMode(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefsCache.edit().putBoolean("performance_mode", enabled).commit() }
        store.edit { it[Keys.performanceMode] = enabled }
    }

    suspend fun setBlurPlayer(enabled: Boolean) = store.edit { it[Keys.blurPlayer] = enabled }

    suspend fun setBlurIntensity(value: Int) = store.edit { it[Keys.blurIntensity] = value.coerceIn(0, 120) }

    suspend fun setShowFileInfo(enabled: Boolean) = store.edit { it[Keys.showFileInfo] = enabled }

    suspend fun setCompactNavigation(enabled: Boolean) = store.edit { it[Keys.compactNavigation] = enabled }

    suspend fun setNavCornerRadius(value: Int) = store.edit { it[Keys.navCornerRadius] = value.coerceIn(0, 32) }

    suspend fun setShowMixCard(enabled: Boolean) = store.edit { it[Keys.showMixCard] = enabled }

    suspend fun setMixSize(size: MixSize) = store.edit { it[Keys.mixSize] = size.count }

    suspend fun setArtworkQualityWifi(quality: ArtworkQuality) = store.edit { it[Keys.artworkQualityWifi] = quality.key }

    suspend fun setArtworkQualityMobile(quality: ArtworkQuality) = store.edit { it[Keys.artworkQualityMobile] = quality.key }

    suspend fun setImageCacheMb(value: Int) = store.edit { it[Keys.imageCacheMb] = value.coerceIn(32, 1024) }

    suspend fun setKeepPlayingAfterClose(enabled: Boolean) = store.edit { it[Keys.keepPlayingAfterClose] = enabled }

    suspend fun setResumeOnHeadset(enabled: Boolean) = store.edit { it[Keys.resumeOnHeadset] = enabled }

    suspend fun setPauseOnHeadsetDisconnect(enabled: Boolean) = store.edit { it[Keys.pauseOnHeadsetDisconnect] = enabled }

    suspend fun setCrossfade(duration: CrossfadeDuration) = store.edit { it[Keys.crossfade] = duration.key }

    suspend fun setHiFiMode(enabled: Boolean) = store.edit { it[Keys.hiFiMode] = enabled }

    suspend fun setHardwareOffload(enabled: Boolean) = store.edit { it[Keys.hardwareOffload] = enabled }

    suspend fun setRememberShuffle(enabled: Boolean) = store.edit { it[Keys.rememberShuffle] = enabled }

    suspend fun setShowQueueHistory(enabled: Boolean) = store.edit { it[Keys.showQueueHistory] = enabled }

    suspend fun setAutoplay(enabled: Boolean) = store.edit { it[Keys.autoplay] = enabled }

    suspend fun setAvoidRepeats(enabled: Boolean) = store.edit { it[Keys.avoidRepeats] = enabled }

    suspend fun setDedupeQueue(enabled: Boolean) = store.edit { it[Keys.dedupeQueue] = enabled }

    suspend fun setPreloadNext(enabled: Boolean) = store.edit { it[Keys.preloadNext] = enabled }

    suspend fun setBassBoost(value: Int) = store.edit { it[Keys.bassBoost] = value.coerceIn(0, 1000) }

    suspend fun setVirtualizer(value: Int) = store.edit { it[Keys.virtualizer] = value.coerceIn(0, 1000) }

    suspend fun setLoudness(value: Int) = store.edit { it[Keys.loudness] = value.coerceIn(0, 1000) }

    suspend fun setHapticFeedback(enabled: Boolean) = store.edit { it[Keys.hapticFeedback] = enabled }

    suspend fun setAutoOpenPlayer(enabled: Boolean) = store.edit { it[Keys.autoOpenPlayer] = enabled }

    suspend fun setArtworkSwipe(enabled: Boolean) = store.edit { it[Keys.artworkSwipe] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) = store.edit { it[Keys.keepScreenOn] = enabled }

    suspend fun setDoubleTapSearch(enabled: Boolean) = store.edit { it[Keys.doubleTapSearch] = enabled }

    suspend fun setPreferUnicode(enabled: Boolean) = store.edit { it[Keys.preferUnicode] = enabled }

    suspend fun setShowMapper(enabled: Boolean) = store.edit { it[Keys.showMapper] = enabled }

    suspend fun setQuickPicksSource(source: QuickPicksSource) = store.edit { it[Keys.quickPicksSource] = source.key }

    suspend fun setQuickPicksLayout(layout: QuickPicksLayout) = store.edit { it[Keys.quickPicksLayout] = layout.key }

    suspend fun setTopSize(size: TopSize) = store.edit { it[Keys.topSize] = size.count }

    suspend fun setAutoSyncLibrary(enabled: Boolean) = store.edit { it[Keys.autoSyncLibrary] = enabled }

    suspend fun setDownloadLimitMb(value: Int) = store.edit { it[Keys.downloadLimitMb] = value.coerceAtLeast(0) }

    suspend fun setAutoCleanCache(enabled: Boolean) = store.edit { it[Keys.autoCleanCache] = enabled }

    suspend fun setAutoUpdateCheck(enabled: Boolean) = store.edit { it[Keys.autoUpdateCheck] = enabled }

    suspend fun setLastUpdateCheck(timeMillis: Long) = store.edit { it[Keys.lastUpdateCheck] = timeMillis }

    suspend fun setUpdateChannel(channel: UpdateChannel) = store.edit {
        if (it[Keys.updateChannel] != channel.key) {
            it[Keys.updateChannel] = channel.key
            it[Keys.lastUpdateCheck] = 0L
        }
    }

    suspend fun setProxy(proxy: ProxySettings) =
        store.edit {
            it[Keys.proxyType] = proxy.type.key
            it[Keys.proxyHost] = proxy.host.trim()
            it[Keys.proxyPort] = proxy.port
            it[Keys.proxyUser] = proxy.username
            it[Keys.proxyPass] = proxy.password
        }

    suspend fun resetAll() {
        withContext(Dispatchers.IO) { prefsCache.edit().clear().commit() }
        store.edit { prefs ->
            val keep = setOf(Keys.clientId.name, Keys.clientSecret.name)
            prefs
                .asMap()
                .keys
                .filter { it.name !in keep }
                .forEach { prefs.remove(it) }
        }
    }

    suspend fun exportPreferences(): Map<String, Any> =
        store.data
            .first()
            .asMap()
            .mapKeys { it.key.name }
            .filterKeys { it !in SENSITIVE_KEYS }
            .mapValues { it.value }

    suspend fun importPreferences(values: Map<String, Any?>) =
        store.edit { prefs ->
            values.forEach { (name, value) ->
                if (name in SENSITIVE_KEYS) return@forEach
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(name)] = value
                    is Int -> prefs[intPreferencesKey(name)] = value
                    is Long -> prefs[longPreferencesKey(name)] = value
                    is String -> prefs[stringPreferencesKey(name)] = value
                }
            }
        }

    fun syncStartupCache(settings: AppSettings) {
        prefsCache
            .edit()
            .putString("theme_mode", settings.themeMode.key)
            .putBoolean("amoled", settings.amoled)
            .putString("accent", settings.accent.key)
            .putString("theme_preset", settings.themePreset.key)
            .putString("start_tab", settings.startTab.key)
            .putString("app_font", settings.appFont.key)
            .putString("palette_style", settings.paletteStyle.key)
            .putString("appearance_json", settings.appearance.toJson())
            .putBoolean("high_refresh_rate", settings.highRefreshRate)
            .putBoolean("performance_mode", settings.performanceMode)
            .apply()
    }

    suspend fun setOsuCredentials(
        clientId: String,
        clientSecret: String,
    ) = store.edit {
        it[Keys.clientId] = clientId.trim()
        it[Keys.clientSecret] = clientSecret.trim()
    }

    suspend fun setMirrorEnabled(
        id: String,
        enabled: Boolean,
    ) = store.edit { prefs ->
        val current =
            prefs[Keys.disabledMirrors]
                .orEmpty()
                .split(',')
                .filter { it.isNotBlank() }
                .toMutableSet()
        if (enabled) current.remove(id) else current.add(id)
        prefs[Keys.disabledMirrors] = current.joinToString(",")
    }

    suspend fun addCustomMirror(mirror: Mirror) =
        store.edit { prefs ->
            val current = decodeMirrors(prefs[Keys.customMirrors]).filterNot { it.id == mirror.id }
            prefs[Keys.customMirrors] = encodeMirrors(current + mirror)
        }

    suspend fun removeCustomMirror(id: String) =
        store.edit { prefs ->
            prefs[Keys.customMirrors] = encodeMirrors(decodeMirrors(prefs[Keys.customMirrors]).filterNot { it.id == id })
        }

    suspend fun addRecentSearch(query: String) {
        val value = query.trim().replace('\n', ' ')
        if (value.isBlank()) return
        store.edit { prefs ->
            val current = prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() }
            val updated = (listOf(value) + current.filterNot { it.equals(value, ignoreCase = true) }).take(8)
            prefs[Keys.recentSearches] = updated.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(query: String) =
        store.edit { prefs ->
            val updated = prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() && it != query }
            prefs[Keys.recentSearches] = updated.joinToString("\n")
        }

    suspend fun clearRecentSearches() = store.edit { it.remove(Keys.recentSearches) }

    private fun decodeEqualizerGains(value: String?): List<Float> {
        val parsed = value.orEmpty().split(',').mapNotNull { it.toFloatOrNull() }
        return if (parsed.size == EQUALIZER_BAND_COUNT && parsed.all(Float::isFinite)) {
            parsed.map { it.coerceIn(-1f, 1f) }
        } else {
            List(EQUALIZER_BAND_COUNT) { 0f }
        }
    }

    private fun encodeMirrors(list: List<Mirror>): String {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("download", it.downloadUrl)
                    .put("preview", it.previewUrl)
                    .put("referer", it.referer),
            )
        }
        return array.toString()
    }

    private fun decodeMirrors(json: String?): List<Mirror> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                Mirror(
                    id = obj.getString("id"),
                    name = obj.optString("name"),
                    downloadUrl = obj.optString("download"),
                    previewUrl = obj.optString("preview"),
                    referer = obj.optString("referer"),
                    builtIn = false,
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    companion object {
        private const val EQUALIZER_BAND_COUNT = 5
        private val SENSITIVE_KEYS = setOf("osu_client_id", "osu_client_secret", "proxy_pass", "proxy_user")

        fun initialThemeMode(context: Context): ThemeMode =
            ThemeMode.fromKey(
                context.applicationContext
                    .getSharedPreferences("kozu_theme_sync", Context.MODE_PRIVATE)
                    .getString("theme_mode", "system"),
            )
    }
}
