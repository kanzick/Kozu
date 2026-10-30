package vn.kozu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.ThemeMode

@Composable
fun AppearancePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(title = "Appearance", onBack = onBack) {
        SettingsGroup(
            title = "Theme",
            items = listOf(
                {
                    ChoiceRow(
                        title = "Mode",
                        options = ThemeMode.entries.toList(),
                        selected = settings.themeMode,
                        label = { it.label },
                        onSelect = viewModel::setThemeMode
                    )
                },
                {
                    SwitchRow(
                        title = "Pure black",
                        subtitle = "True black backgrounds in dark mode (AMOLED)",
                        checked = settings.amoled,
                        onChange = viewModel::setAmoled
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Colors",
            items = listOf(
                {
                    Column {
                        Text("Accent color", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Used when nothing is playing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AccentColor.entries.forEach { accent ->
                                val selected = accent == settings.accent
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .clickable { viewModel.setAccent(accent) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = accent.label, tint = androidx.compose.ui.graphics.Color.White)
                                    }
                                }
                            }
                        }
                    }
                },
                {
                    SwitchRow(
                        title = "Artwork colors",
                        subtitle = "While a song plays, tint the whole app with its cover",
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
                        subtitle = "Full song fetches the real audio from osu! mirrors and may take a few seconds to start. Preview is instant but short.",
                        options = StreamMode.entries.toList(),
                        selected = settings.streamMode,
                        label = { it.label },
                        onSelect = viewModel::setStreamMode
                    )
                },
                {
                    SwitchRow(
                        title = "Wi-Fi only",
                        subtitle = "Don't stream songs on mobile data (offline songs still play)",
                        checked = settings.wifiOnlyStreaming,
                        onChange = viewModel::setWifiOnlyStreaming
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Audio",
            items = listOf({
                SwitchRow(
                    title = "Skip silence",
                    subtitle = "Trim silent parts at the start and end of songs",
                    checked = settings.skipSilence,
                    onChange = viewModel::setSkipSilence
                )
            })
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
