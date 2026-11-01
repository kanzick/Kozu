package vn.kozu.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.domain.model.OsuProfile
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.RefreshIndicator

enum class SettingsPage { ROOT, ACCOUNT, CATALOG, STATS, APPEARANCE, NAVIGATION, PLAYBACK, OFFLINE, SOURCES, CONTENT, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    val rootScrollState = rememberScrollState()
    val pullState = rememberPullToRefreshState()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }
    BackHandler(enabled = true) {
        if (page != SettingsPage.ROOT) {
            page = SettingsPage.ROOT
        } else {
            onBack()
        }
    }
    val close = { page = SettingsPage.ROOT }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { if (!isRefreshing) onRefresh() },
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState != SettingsPage.ROOT) {
                    (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 12 } +
                        fadeIn(tween(170))) togetherWith
                        (slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { -it / 20 } +
                            fadeOut(tween(130)))
                } else {
                    (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 20 } +
                        fadeIn(tween(170))) togetherWith
                        (slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { it / 12 } +
                            fadeOut(tween(130)))
                }
            },
            label = "settingsPages"
        ) { current ->
            when (current) {
                SettingsPage.ROOT -> SettingsRoot(
                    onBack = onBack,
                    onOpen = { page = it },
                    scrollState = rootScrollState,
                    profile = profile,
                    loggedIn = loggedIn
                )
                SettingsPage.ACCOUNT -> AccountPage(viewModel, close)
                SettingsPage.CATALOG -> CatalogPage(viewModel, close)
                SettingsPage.STATS -> StatsPage(viewModel, close)
                SettingsPage.APPEARANCE -> AppearancePage(viewModel, close)
                SettingsPage.NAVIGATION -> NavigationPage(viewModel, close)
                SettingsPage.PLAYBACK -> PlaybackPage(viewModel, close)
                SettingsPage.OFFLINE -> OfflinePage(viewModel, close)
                SettingsPage.SOURCES -> SourcesPage(viewModel, close)
                SettingsPage.CONTENT -> ContentPage(viewModel, close)
                SettingsPage.ABOUT -> AboutPage(close)
            }
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
private fun SettingsRoot(
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    profile: OsuProfile?,
    loggedIn: Boolean
) {
    val entries = listOf(
        Entry(SettingsPage.ACCOUNT, Icons.Rounded.AccountCircle, "Account", "osu! profile", Tone.PRIMARY),
        Entry(SettingsPage.CATALOG, Icons.Rounded.Explore, "Music", "Beatmaps and filters", Tone.PRIMARY),
        Entry(SettingsPage.APPEARANCE, Icons.Rounded.Palette, "Appearance", "Theme and colors", Tone.SECONDARY),
        Entry(SettingsPage.PLAYBACK, Icons.Rounded.MusicNote, "Playback", "Audio and equalizer", Tone.TERTIARY),
        Entry(SettingsPage.NAVIGATION, Icons.Rounded.Home, "Behavior", "Home and navigation", Tone.SECONDARY),
        Entry(SettingsPage.OFFLINE, Icons.Rounded.Storage, "Downloads", "Offline music", Tone.TERTIARY),
        Entry(SettingsPage.STATS, Icons.Rounded.BarChart, "Listening history", "Plays and favorites", Tone.PRIMARY),
        Entry(SettingsPage.SOURCES, Icons.Rounded.Dns, "Audio sources", "Stream mirrors", Tone.SECONDARY),
        Entry(SettingsPage.CONTENT, Icons.Rounded.Public, "Privacy", "Data and searches", Tone.PRIMARY),
        Entry(SettingsPage.ABOUT, Icons.Rounded.Info, "About", "Version and credits", Tone.SECONDARY)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(22.dp))

        EntryGroup(entries, profile, loggedIn, onOpen)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EntryGroup(
    entries: List<Entry>,
    profile: OsuProfile?,
    loggedIn: Boolean,
    onOpen: (SettingsPage) -> Unit
) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
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
                avatarUrl = profile?.avatarUrl?.takeIf {
                    loggedIn && entry.page == SettingsPage.ACCOUNT
                },
                onClick = { onOpen(entry.page) }
            )
        }
    }
}
