package vn.kozu.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import vn.kozu.util.AppIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.data.repository.CatalogSource
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.AppFont
import vn.kozu.domain.model.AppStyle
import vn.kozu.domain.model.ArtworkQuality
import vn.kozu.domain.model.CrossfadeDuration
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.NavigationStyle
import vn.kozu.domain.model.PaletteStyle
import vn.kozu.domain.model.ProxySettings
import vn.kozu.domain.model.QuickPicksLayout
import vn.kozu.domain.model.QuickPicksSource
import vn.kozu.domain.model.StartTab
import vn.kozu.domain.model.StatusFilter
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.TopSize
import java.text.DateFormat
import java.util.Date

@Composable
fun AppearancePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf(AppIcon.current(context)) }
    val floating = settings.navigationStyle == NavigationStyle.FLOATING

    SettingsScaffold(title = "Appearance", onBack = onBack) {
        SettingsSection("Global theme") {
            dropdown(
                Setting.APP_THEME,
                settings.themeMode,
                ThemeMode.entries.toList(),
                { it.label },
                onSelect = viewModel::setThemeMode
            )
            switch(Setting.PURE_BLACK, settings.amoled, onChange = viewModel::setAmoled)
            dropdown(
                Setting.APP_FONT,
                settings.appFont,
                AppFont.entries.toList(),
                { it.label }
            ) { font -> viewModel.save { setAppFont(font) } }
            dropdown(
                Setting.APP_ICON,
                appIcon,
                AppIcon.entries.toList(),
                { it.label },
                leading = { icon ->
                    Surface(
                        Modifier.size(22.dp),
                        shape = CircleShape,
                        color = when (icon) {
                            AppIcon.DEFAULT -> Color(0xFFFF66AA)
                            AppIcon.MIDNIGHT -> Color(0xFF3B2F6B)
                            AppIcon.MINT -> Color(0xFF2BC4A8)
                            AppIcon.VIOLET -> Color(0xFF8C66FF)
                        }
                    ) {}
                }
            ) { chosen ->
                AppIcon.apply(context, chosen)
                appIcon = chosen
            }
            dropdown(
                Setting.APP_STYLE,
                settings.appStyle,
                AppStyle.entries.toList(),
                { it.label },
                onSelect = viewModel::setAppStyle
            )
        }
        SettingsSection("Colors") {
            dropdown(
                Setting.ACCENT_COLOR,
                settings.accent,
                AccentColor.entries.toList(),
                { it.label },
                leading = { accent ->
                    Surface(Modifier.size(22.dp), shape = CircleShape, color = accent.color) {}
                },
                onSelect = viewModel::setAccent
            )
            switch(Setting.ARTWORK_COLORS, settings.artworkColors, onChange = viewModel::setArtworkColors)
            dropdown(
                Setting.PALETTE_STYLE,
                settings.paletteStyle,
                PaletteStyle.entries.toList(),
                { it.label },
                description = "Current: ${settings.paletteStyle.label}. ${settings.paletteStyle.description}."
            ) { style -> viewModel.save { setPaletteStyle(style) } }
        }
        SettingsSection("Display & performance") {
            switch(Setting.HIGH_REFRESH, settings.highRefreshRate) { enabled ->
                viewModel.save { setHighRefreshRate(enabled) }
            }
            switch(Setting.PERFORMANCE_MODE, settings.performanceMode) { enabled ->
                viewModel.save { setPerformanceMode(enabled) }
            }
        }
        SettingsSection("Now playing") {
            switch(Setting.BLUR_PLAYER, settings.blurPlayer, enabled = !settings.performanceMode) { enabled ->
                viewModel.save { setBlurPlayer(enabled) }
            }
            slider(
                Setting.BLUR_INTENSITY,
                settings.blurIntensity.toFloat(),
                0f..120f,
                valueLabel = { "${it.toInt()} dp" },
                steps = 23,
                enabled = settings.blurPlayer && !settings.performanceMode
            ) { value -> viewModel.save { setBlurIntensity(value.toInt()) } }
            switch(Setting.SHOW_FILE_INFO, settings.showFileInfo) { enabled ->
                viewModel.save { setShowFileInfo(enabled) }
            }
        }
        SettingsSection("Navigation bar") {
            dropdown(
                Setting.NAV_STYLE,
                settings.navigationStyle,
                NavigationStyle.entries.toList(),
                { if (it == NavigationStyle.FLOATING) "Expressive pill" else "Classic" },
                onSelect = viewModel::setNavigationStyle
            )
            switch(Setting.COMPACT_NAV, settings.compactNavigation, enabled = floating) { enabled ->
                viewModel.save { setCompactNavigation(enabled) }
            }
            slider(
                Setting.NAV_RADIUS,
                settings.navCornerRadius.toFloat(),
                0f..32f,
                valueLabel = { "${it.toInt()} dp" },
                steps = 15,
                enabled = floating
            ) { value -> viewModel.save { setNavCornerRadius(value.toInt()) } }
        }
        SettingsSection("App navigation") {
            dropdown(
                Setting.DEFAULT_TAB,
                settings.startTab,
                StartTab.entries.toList(),
                { it.label },
                onSelect = viewModel::setStartTab
            )
        }
    }
}

@Composable
fun PlaybackPage(viewModel: SettingsViewModel, onOpenEqualizer: () -> Unit, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScaffold(title = "Playback", onBack = onBack) {
        SettingsSection("Background playback") {
            switch(Setting.KEEP_PLAYING, settings.keepPlayingAfterClose) { enabled ->
                viewModel.save { setKeepPlayingAfterClose(enabled) }
            }
            action(Setting.BATTERY_OPT, "Open battery settings") {
                openBatterySettings(context)
            }
        }
        SettingsSection("Headphones") {
            switch(Setting.PAUSE_HEADSET, settings.pauseOnHeadsetDisconnect) { enabled ->
                viewModel.save { setPauseOnHeadsetDisconnect(enabled) }
            }
            switch(Setting.RESUME_HEADSET, settings.resumeOnHeadset) { enabled ->
                viewModel.save { setResumeOnHeadset(enabled) }
            }
        }
        SettingsSection("Streaming & data saving") {
            dropdown(
                Setting.STREAM_QUALITY,
                settings.streamMode,
                StreamMode.entries.toList(),
                { it.label },
                onSelect = viewModel::setStreamMode
            )
            switch(Setting.WIFI_ONLY_STREAM, settings.wifiOnlyStreaming, onChange = viewModel::setWifiOnlyStreaming)
            switch(Setting.SKIP_SILENCE, settings.skipSilence, onChange = viewModel::setSkipSilence)
        }
        SettingsSection("Queue and transitions") {
            dropdown(
                Setting.CROSSFADE,
                settings.crossfade,
                CrossfadeDuration.entries.toList(),
                { it.label }
            ) { value -> viewModel.save { setCrossfade(value) } }
            switch(Setting.HIFI, settings.hiFiMode) { enabled -> viewModel.save { setHiFiMode(enabled) } }
            switch(Setting.OFFLOAD, settings.hardwareOffload) { enabled ->
                viewModel.save { setHardwareOffload(enabled) }
            }
            switch(Setting.PRELOAD_NEXT, settings.preloadNext) { enabled ->
                viewModel.save { setPreloadNext(enabled) }
            }
            switch(Setting.REMEMBER_SHUFFLE, settings.rememberShuffle) { enabled ->
                viewModel.save { setRememberShuffle(enabled) }
            }
            switch(Setting.SHOW_QUEUE_HISTORY, settings.showQueueHistory) { enabled ->
                viewModel.save { setShowQueueHistory(enabled) }
            }
            switch(Setting.AUTOPLAY, settings.autoplay) { enabled -> viewModel.save { setAutoplay(enabled) } }
            switch(Setting.AVOID_REPEATS, settings.avoidRepeats, enabled = settings.autoplay) { enabled ->
                viewModel.save { setAvoidRepeats(enabled) }
            }
            switch(Setting.DEDUPE_QUEUE, settings.dedupeQueue) { enabled ->
                viewModel.save { setDedupeQueue(enabled) }
            }
        }
        SettingsSection("Sound") {
            navigation(
                Setting.EQUALIZER_BANDS,
                value = if (settings.equalizerEnabled) "On" else "Off",
                onClick = onOpenEqualizer
            )
        }
    }
}

@Composable
fun BehaviorPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScaffold(title = "Behavior", onBack = onBack) {
        SettingsSection("Player") {
            switch(Setting.ARTWORK_SWIPE, settings.artworkSwipe) { enabled ->
                viewModel.save { setArtworkSwipe(enabled) }
            }
            switch(Setting.AUTO_OPEN_PLAYER, settings.autoOpenPlayer) { enabled ->
                viewModel.save { setAutoOpenPlayer(enabled) }
            }
            switch(Setting.KEEP_SCREEN_ON, settings.keepScreenOn) { enabled ->
                viewModel.save { setKeepScreenOn(enabled) }
            }
        }
        SettingsSection("Search") {
            switch(Setting.DOUBLE_TAP_SEARCH, settings.doubleTapSearch) { enabled ->
                viewModel.save { setDoubleTapSearch(enabled) }
            }
        }
        SettingsSection("Haptics") {
            switch(Setting.HAPTICS, settings.hapticFeedback) { enabled ->
                viewModel.save { setHapticFeedback(enabled) }
            }
        }
        SettingsSection("osu! link integration") {
            action(Setting.OSU_LINKS, "Open system settings") { openDefaultLinkSettings(context) }
        }
    }
}

@Composable
fun MusicPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val lastSync = if (settings.lastSyncAt > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(settings.lastSyncAt))
    } else {
        "Never"
    }
    val source = when (catalog.mixSource) {
        CatalogSource.OFFICIAL_OSU -> "Official osu!"
        CatalogSource.PUBLIC_MIRROR -> "Public fallback"
        CatalogSource.MIXED -> "Official + fallback"
        CatalogSource.NONE -> "Connecting"
    }

    SettingsScaffold(title = "Music", onBack = onBack) {
        SettingsSection("Default filters") {
            dropdown(
                Setting.DEFAULT_STATUS,
                settings.statusFilter,
                listOf(StatusFilter.DEFAULT, StatusFilter.RANKED, StatusFilter.LOVED),
                { it.label },
                onSelect = viewModel::setStatusFilter
            )
            dropdown(
                Setting.DEFAULT_MODE,
                settings.modeFilter,
                ModeFilter.entries.toList(),
                { it.label },
                onSelect = viewModel::setModeFilter
            )
        }
        SettingsSection("Sync and library") {
            action(
                Setting.SYNC_NOW,
                label = if (syncing) "Syncing…" else "Sync now",
                description = "Refresh favourites, playlists and downloads. Last sync: $lastSync",
                enabled = !syncing
            ) { viewModel.syncNow() }
            switch(Setting.AUTO_SYNC, settings.autoSyncLibrary) { enabled ->
                viewModel.save { setAutoSyncLibrary(enabled) }
            }
        }
        SettingsSection("osu! catalog") {
            info(
                icon = Setting.DEFAULT_MODE.icon,
                title = "Catalog source",
                value = source,
                description = catalog.error ?: "Beatmap listings and updates come from osu!."
            )
            action(
                Setting.REFRESH_CATALOG,
                label = if (catalog.isRefreshing) "Refreshing…" else "Refresh catalog",
                enabled = !catalog.isRefreshing
            ) { viewModel.refreshCatalog() }
        }
        SettingsSection("Visual quality") {
            dropdown(
                Setting.ARTWORK_WIFI,
                settings.artworkQualityWifi,
                ArtworkQuality.entries.toList(),
                { "${it.label} · ${it.description}" }
            ) { value -> viewModel.save { setArtworkQualityWifi(value) } }
            dropdown(
                Setting.ARTWORK_MOBILE,
                settings.artworkQualityMobile,
                ArtworkQuality.entries.toList(),
                { "${it.label} · ${it.description}" }
            ) { value -> viewModel.save { setArtworkQualityMobile(value) } }
            slider(
                Setting.IMAGE_CACHE,
                settings.imageCacheMb.toFloat(),
                32f..1024f,
                valueLabel = { "${it.toInt()} MB" },
                steps = 30
            ) { value -> viewModel.save { setImageCacheMb(value.toInt()) } }
        }
    }
}

@Composable
fun ContentPage(viewModel: SettingsViewModel, onOpenProxy: () -> Unit, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(title = "Content", onBack = onBack) {
        SettingsSection("Catalog") {
            switch(Setting.SHOW_EXPLICIT, settings.showExplicit) { enabled ->
                viewModel.save { setShowExplicit(enabled) }
            }
        }
        SettingsSection("Titles & cards") {
            switch(Setting.PREFER_UNICODE, settings.preferUnicode) { enabled ->
                viewModel.save { setPreferUnicode(enabled) }
            }
            switch(Setting.SHOW_MAPPER, settings.showMapper) { enabled ->
                viewModel.save { setShowMapper(enabled) }
            }
            dropdown(
                Setting.TOP_SIZE,
                settings.topSize,
                TopSize.entries.toList(),
                { it.label }
            ) { size -> viewModel.save { setTopSize(size) } }
        }
        SettingsSection("Home") {
            switch(Setting.SHOW_MIX_CARD, settings.showMixCard) { enabled ->
                viewModel.save { setShowMixCard(enabled) }
            }
            custom { shape ->
                DisplayNameItem(
                    shape = shape,
                    name = settings.displayName,
                    onCommit = viewModel::setDisplayName
                )
            }
        }
        SettingsSection("Network") {
            navigation(
                Setting.PROXY,
                value = proxySummary(settings.proxy),
                onClick = onOpenProxy
            )
        }
        SettingsSection("Privacy") {
            action(Setting.CLEAR_HISTORY, "Clear history", destructive = true) { viewModel.clearHistory() }
            action(Setting.CLEAR_SEARCHES, "Clear searches", destructive = true) { viewModel.clearSearches() }
        }
    }
}

@Composable
private fun DisplayNameItem(
    shape: androidx.compose.ui.graphics.Shape,
    name: String,
    onCommit: (String) -> Unit
) {
    var draft by remember(name) { mutableStateOf(name) }
    SettingCard(setting = Setting.DISPLAY_NAME, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            ItemText(Setting.DISPLAY_NAME.title, Setting.DISPLAY_NAME.description)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(24) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && draft != name) onCommit(draft)
                    },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                placeholder = { Text("Leave blank to use your osu! username") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCommit(draft) })
            )
        }
    }
}

fun proxySummary(proxy: ProxySettings): String =
    if (proxy.isActive) "${proxy.type.label} · ${proxy.host}:${proxy.port}" else "Disabled"

private fun openBatterySettings(context: android.content.Context) {
    val intents = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
    startFirst(context, intents)
}

private fun openDefaultLinkSettings(context: android.content.Context) {
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    startFirst(context, intents)
}

private fun startFirst(context: android.content.Context, intents: List<Intent>) {
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
}
