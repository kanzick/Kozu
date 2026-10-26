package vn.kozu.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuMotion

enum class SettingsPage { ROOT, ACCOUNT, STATS, APPEARANCE, PLAYBACK, OFFLINE, SOURCES, CONTENT, ABOUT }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    BackHandler(enabled = page != SettingsPage.ROOT) { page = SettingsPage.ROOT }
    val close = { page = SettingsPage.ROOT }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState != SettingsPage.ROOT) {
                    (slideInHorizontally(KozuMotion.spatial()) { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally(KozuMotion.spatial()) { -it / 6 } + fadeOut())
                } else {
                    (slideInHorizontally(KozuMotion.spatial()) { -it / 6 } + fadeIn()) togetherWith
                        (slideOutHorizontally(KozuMotion.spatial()) { it / 4 } + fadeOut())
                }
            },
            label = "settingsPages"
        ) { current ->
            when (current) {
                SettingsPage.ROOT -> SettingsRoot(onBack = onBack, onOpen = { page = it })
                SettingsPage.ACCOUNT -> AccountPage(viewModel, close)
                SettingsPage.STATS -> StatsPage(viewModel, close)
                SettingsPage.APPEARANCE -> AppearancePage(viewModel, close)
                SettingsPage.PLAYBACK -> PlaybackPage(viewModel, close)
                SettingsPage.OFFLINE -> OfflinePage(viewModel, close)
                SettingsPage.SOURCES -> SourcesPage(viewModel, close)
                SettingsPage.CONTENT -> ContentPage(viewModel, close)
                SettingsPage.ABOUT -> AboutPage(close)
            }
        }
    }
}

private data class Entry(
    val page: SettingsPage,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tone: Tone
)

private enum class Tone { PRIMARY, SECONDARY, TERTIARY }

@Composable
private fun SettingsRoot(onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
    val top = listOf(
        Entry(SettingsPage.ACCOUNT, Icons.Rounded.AccountCircle, "Account", "Sign in with osu! and see your profile", Tone.PRIMARY),
        Entry(SettingsPage.STATS, Icons.Rounded.BarChart, "Listening Stats", "Your top songs, artists and mappers", Tone.PRIMARY)
    )
    val rest = listOf(
        Entry(SettingsPage.APPEARANCE, Icons.Rounded.Palette, "Appearance", "Themes, colors and display name", Tone.SECONDARY),
        Entry(SettingsPage.PLAYBACK, Icons.Rounded.MusicNote, "Playback", "Streaming quality, cache and audio", Tone.TERTIARY),
        Entry(SettingsPage.OFFLINE, Icons.Rounded.Storage, "Offline", "Downloads, storage and sync", Tone.TERTIARY),
        Entry(SettingsPage.SOURCES, Icons.Rounded.Dns, "Sources", "osu! mirrors for streaming and downloads", Tone.SECONDARY),
        Entry(SettingsPage.CONTENT, Icons.Rounded.Public, "Content", "Game mode, status and explicit filters", Tone.PRIMARY),
        Entry(SettingsPage.ABOUT, Icons.Rounded.Info, "About", "Version, credits and legal", Tone.SECONDARY)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .bounceClick(onClick = onBack),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Settings", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(22.dp))

        EntryGroup(top, onOpen)
        Spacer(Modifier.height(18.dp))
        EntryGroup(rest, onOpen)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EntryGroup(entries: List<Entry>, onOpen: (SettingsPage) -> Unit) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        entries.forEachIndexed { index, entry ->
            val colors = MaterialTheme.colorScheme
            val (container: Color, content: Color) = when (entry.tone) {
                Tone.PRIMARY -> colors.primaryContainer to colors.onPrimaryContainer
                Tone.SECONDARY -> colors.secondaryContainer to colors.onSecondaryContainer
                Tone.TERTIARY -> colors.tertiaryContainer to colors.onTertiaryContainer
            }
            SettingsEntry(
                icon = entry.icon,
                container = container,
                content = content,
                title = entry.title,
                subtitle = entry.subtitle,
                index = index,
                count = entries.size,
                onClick = { onOpen(entry.page) }
            )
        }
    }
}
