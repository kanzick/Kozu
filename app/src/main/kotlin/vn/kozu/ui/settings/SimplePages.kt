package vn.kozu.ui.settings

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.AppStyle
import vn.kozu.domain.model.NavigationStyle
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.StartTab
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.StatusFilter
import vn.kozu.domain.model.ThemeMode
import vn.kozu.player.EqualizerProfile
import java.util.Locale

@Composable
fun AppearancePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(title = "Appearance", onBack = onBack) {
        SettingsGroup(
            title = "Theme",
            items = listOf(
                {
                    ChoiceRow(
                        title = "App Theme",
                        subtitle = "Switch between light, dark, or follow system appearance.",
                        options = ThemeMode.entries.toList(),
                        selected = settings.themeMode,
                        label = { it.label },
                        onSelect = viewModel::setThemeMode,
                        icon = Icons.Rounded.WbSunny
                    )
                },
                {
                    SwitchRow(
                        title = "Pure black",
                        subtitle = "Black surfaces for OLED screens",
                        checked = settings.amoled,
                        onChange = viewModel::setAmoled
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Style",
            items = listOf({
                ChoiceRow(
                    title = "App style",
                    subtitle = "Choose how Kozu looks and feels.",
                    options = AppStyle.entries.toList(),
                    selected = settings.appStyle,
                    label = { it.label },
                    onSelect = viewModel::setAppStyle,
                    icon = Icons.Rounded.AutoAwesome
                )
            })
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Colors",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Accent color",
                        subtitle = "Used when nothing is playing",
                        options = AccentColor.entries.toList(),
                        selected = settings.accent,
                        label = { it.label },
                        onSelect = viewModel::setAccent,
                        icon = Icons.Rounded.Palette,
                        leading = { accent ->
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = CircleShape,
                                color = accent.color
                            ) {}
                        }
                    )
                },
                {
                    SwitchRow(
                        title = "Artwork colors",
                        subtitle = "Match the colors to album art",
                        checked = settings.artworkColors,
                        onChange = viewModel::setArtworkColors
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Home",
            items = listOf({
                var name by remember(settings.displayName) { mutableStateOf(settings.displayName) }
                Column {
                    Text("Display name", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && name != settings.displayName) {
                                    viewModel.setDisplayName(name)
                                }
                            },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text("Leave blank to use your osu! username") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.setDisplayName(name) })
                    )
                }
            })
        )
    }
}

@Composable
fun PlaybackPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    SettingsScaffold(title = "Playback", onBack = onBack) {
        SettingsGroup(
            title = "Streaming",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Stream quality",
                        subtitle = "Full tracks or short previews",
                        options = StreamMode.entries.toList(),
                        selected = settings.streamMode,
                        label = { it.label },
                        onSelect = viewModel::setStreamMode
                    )
                },
                {
                    SwitchRow(
                        title = "Wi-Fi only",
                        subtitle = "Stream on Wi-Fi only",
                        checked = settings.wifiOnlyStreaming,
                        onChange = viewModel::setWifiOnlyStreaming
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Audio",
            items = listOf(
                {
                    SwitchRow(
                        title = "Skip silence",
                        subtitle = "Skip quiet sections",
                        checked = settings.skipSilence,
                        onChange = viewModel::setSkipSilence
                    )
                },
                {
                    SwitchRow(
                        title = "Equalizer",
                        subtitle = "Adjust five frequency bands",
                        checked = settings.equalizerEnabled,
                        onChange = viewModel::setEqualizerEnabled
                    )
                },
                {
                    var gains by remember(settings.equalizerGains) {
                        mutableStateOf(settings.equalizerGains)
                    }
                    Column {
                        Text("Sound profile", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Choose a starting point or tune each band. Some devices may not support audio effects.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        val selectedPreset = EqualizerProfile.presets.entries
                            .firstOrNull { gains == it.value }
                            ?.key
                            ?: "Custom"
                        ChoiceRow(
                            title = "Preset",
                            options = EqualizerProfile.presets.keys.toList(),
                            selected = selectedPreset,
                            label = { it },
                            onSelect = { name ->
                                val preset = EqualizerProfile.presets.getValue(name)
                                gains = preset
                                viewModel.setEqualizerGains(preset)
                            }
                        )
                        EqualizerProfile.centerFrequenciesHz.forEachIndexed { index, frequency ->
                            val sliderState = rememberSliderState(
                                value = gains[index],
                                steps = 23,
                                trackRange = -1f..1f
                            )
                            LaunchedEffect(gains[index]) {
                                sliderState.value = gains[index]
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        frequency >= 1_000f -> "${frequency / 1_000f} kHz"
                                        else -> "${frequency.toInt()} Hz"
                                    },
                                    modifier = Modifier.width(72.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                    state = sliderState,
                                    onValueChange = { value ->
                                        gains = gains.toMutableList().also { it[index] = value }
                                    },
                                    onValueChangeFinished = {
                                        viewModel.setEqualizerGains(gains)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%+.1f",
                                        gains[index] * EqualizerProfile.MAX_GAIN_DB
                                    ),
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "Gain in dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Stream cache",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Cache size",
                        subtitle = "Previously streamed audio is cached up to this size and cleared when playback fully exits. Currently ${formatBytes(cacheBytes)}.",
                        options = listOf(128, 256, 512, 1024, 2048),
                        selected = settings.streamCacheMb,
                        label = { if (it >= 1024) "${it / 1024} GB" else "$it MB" },
                        onSelect = viewModel::setStreamCacheMb
                    )
                },
                { ActionRow("Clear stream cache", "Free ${formatBytes(cacheBytes)}", viewModel::clearStreamCache) }
            )
        )
    }
}

@Composable
fun ContentPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsScaffold(title = "Content", onBack = onBack) {
        SettingsGroup(
            title = "Data",
            items = listOf(
                { ActionRow("Clear listening history", "Resets stats, recent songs and most played", viewModel::clearHistory, destructive = true) },
                { ActionRow("Clear recent searches", "Removes the search suggestions", viewModel::clearSearches, destructive = true) }
            )
        )
    }
}

@Composable
fun CatalogPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val sourceDescription = when (catalog.mixSource) {
        vn.kozu.data.repository.CatalogSource.OFFICIAL_OSU ->
            "Connected to osu! API. Beatmap updates use osu!'s official catalog."
        vn.kozu.data.repository.CatalogSource.PUBLIC_MIRROR ->
            "Using a public fallback index. Add osu! API credentials in Profile for official results."
        vn.kozu.data.repository.CatalogSource.MIXED ->
            "Some results use osu!'s official catalog; fallback results are labeled separately."
        vn.kozu.data.repository.CatalogSource.NONE ->
            catalog.error ?: "Connecting to osu!'s official catalog."
    }

    SettingsScaffold(title = "Content", onBack = onBack) {
        SettingsGroup(
            title = "Default filters",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Status",
                        subtitle = "Starting filter for All beatmaps",
                        options = listOf(StatusFilter.DEFAULT, StatusFilter.RANKED, StatusFilter.LOVED),
                        selected = settings.statusFilter,
                        label = { if (it == StatusFilter.DEFAULT) "Ranked + Loved" else it.label },
                        onSelect = viewModel::setStatusFilter
                    )
                },
                {
                    ChoiceRow(
                        title = "Ruleset",
                        subtitle = "Starting ruleset for All beatmaps",
                        options = ModeFilter.entries.toList(),
                        selected = settings.modeFilter,
                        label = { it.label },
                        onSelect = viewModel::setModeFilter
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "osu! source",
            items = listOf({
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        sourceDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryPill(
                        if (catalog.isRefreshing) "Refreshing…" else "Refresh catalog",
                        enabled = !catalog.isRefreshing,
                        onClick = viewModel::refreshCatalog
                    )
                }
            })
        )
    }
}

@Composable
fun NavigationPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScaffold(title = "Behavior", onBack = onBack) {
        SettingsGroup(
            title = "App navigation",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Default tab",
                        subtitle = "Open this section when Kozu starts",
                        options = StartTab.entries.toList(),
                        selected = settings.startTab,
                        label = { it.label },
                        onSelect = viewModel::setStartTab
                    )
                },
                {
                    ChoiceRow(
                        title = "Navigation style",
                        subtitle = "Choose how the app's tabs are displayed",
                        options = NavigationStyle.entries.toList(),
                        selected = settings.navigationStyle,
                        label = { it.label },
                        onSelect = viewModel::setNavigationStyle
                    )
                }
            )
        )
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    SettingsScaffold(title = "About", onBack = onBack) {
        SettingsGroup(
            title = null,
            items = listOf(
                {
                    Column {
                        Text("Kozu", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (version.isBlank()) "Version unknown" else "Version $version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Listen to your favorite osu! songs online or offline, with a Material 3 Expressive design.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                {
                    Column {
                        Text("Data & audio", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Song information comes from the osu! API. Audio is streamed or downloaded from osu! and community mirrors, which are third-party services and may be unavailable at any time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                {
                    Column {
                        Text("Not affiliated with ppy Pty Ltd. osu! is a trademark of its owners.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { uriHandler.openUri("https://osu.ppy.sh") }) { Text("Open osu!") }
                    }
                }
            )
        )
    }
}
