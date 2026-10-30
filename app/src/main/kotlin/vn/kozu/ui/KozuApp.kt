package vn.kozu.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import vn.kozu.KozuApplication
import vn.kozu.data.local.DownloadedSet
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.components.DownloadInfo
import vn.kozu.ui.components.LocalDownloads
import vn.kozu.ui.components.LocalFavoriteIds
import vn.kozu.ui.components.LocalIsSupporter
import vn.kozu.ui.components.LocalNowPlaying
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.NowPlayingInfo
import vn.kozu.ui.components.PlaylistPickerSheet
import vn.kozu.ui.components.TrackActions
import vn.kozu.ui.components.TrackMenuSheet
import vn.kozu.ui.explore.AllBeatmapsScreen
import vn.kozu.ui.explore.ExploreScreen
import vn.kozu.ui.game.GameScreen
import vn.kozu.ui.home.HomeScreen
import vn.kozu.ui.library.LibraryScreen
import vn.kozu.ui.navigation.FloatingNavBar
import vn.kozu.ui.navigation.KozuTab
import vn.kozu.ui.player.MiniPlayer
import vn.kozu.ui.player.PlayerScreen
import vn.kozu.ui.search.SearchScreen
import vn.kozu.ui.settings.SettingsScreen
import vn.kozu.ui.theme.ArtworkPalette
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.ui.theme.KozuTheme

/**
 * Root composable of the Kozu application.
 * Manages dependency container initialization, artwork-based dynamic theme seed colors,
 * dark/light/AMOLED mode preferences, and system bar appearance synchronization.
 */
@Composable
fun KozuApp() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as KozuApplication).container }
    val playback = container.playback

    val cachedTheme = remember { container.settings.cachedThemeMode }
    val cachedAmoled = remember { container.settings.cachedAmoled }
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings(themeMode = cachedTheme, amoled = cachedAmoled)
    )
    val state by playback.state.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Extracts seed color from current track artwork
    var artworkSeed by remember { mutableStateOf<Color?>(null) }
    val currentArt = state.current?.thumbUrl
    LaunchedEffect(currentArt, settings.artworkColors) {
        artworkSeed = if (settings.artworkColors) ArtworkPalette.seedColor(currentArt) else null
    }

    val seed = if (state.current != null) artworkSeed ?: settings.accent.color else settings.accent.color

    KozuTheme(seed = seed, darkTheme = dark, amoled = settings.amoled) {
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
        KozuShell(container = container)
    }
}

/**
 * Main application shell containing tab navigation, mini player,
 * floating navigation bar, settings overlay, full player overlay, and bottom sheets.
 */
@Composable
private fun KozuShell(container: AppContainer) {
    val context = LocalContext.current
    val playback = container.playback
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val state by playback.state.collectAsStateWithLifecycle()
    val favoriteIds by container.favorites.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val isSupporter by container.accounts.isSupporter.collectAsStateWithLifecycle(initialValue = false)
    val playlists by container.playlists.playlists.collectAsStateWithLifecycle(initialValue = emptyList<Playlist>())
    val downloadedSets by container.downloadsStore.sets.collectAsStateWithLifecycle(initialValue = emptyList<DownloadedSet>())
    val activeDownloads by container.downloads.status.collectAsStateWithLifecycle()
    val discoverState by container.discover.state.collectAsStateWithLifecycle()

    val downloadInfo = remember(downloadedSets, activeDownloads) {
        DownloadInfo(
            downloaded = downloadedSets.map { it.beatmap.id }.toSet(),
            outdated = downloadedSets.filter { it.outdated }.map { it.beatmap.id }.toSet(),
            active = activeDownloads
        )
    }

    var tab by rememberSaveable { mutableStateOf(KozuTab.HOME) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var allBeatmapsOpen by rememberSaveable { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<Beatmap?>(null) }
    var playlistTarget by remember { mutableStateOf<Beatmap?>(null) }

    val density = LocalDensity.current
    var chromeHeightPx by remember { mutableIntStateOf(0) }
    val bottomPadding = with(density) { chromeHeightPx.toDp() } + 16.dp

    val homeList = rememberLazyListState()
    val exploreList = rememberLazyListState()
    val libraryList = rememberLazyListState()

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    fun refreshApp() {
        container.discover.refresh()
        scope.launch {
            container.accounts.refreshIfSignedIn()
        }
    }

    // Common track action handlers provided via CompositionLocal
    val actions = remember(playback, container) {
        object : TrackActions {
            override fun play(queue: List<Beatmap>, index: Int) = playback.playQueue(queue, index)
            override fun playShuffled(queue: List<Beatmap>) = playback.playShuffled(queue)
            override fun toggleFavorite(beatmap: Beatmap) {
                scope.launch { container.favorites.toggle(beatmap) }
            }
            override fun openMenu(beatmap: Beatmap) {
                menuTarget = beatmap
            }
            override fun download(beatmap: Beatmap, force: Boolean) {
                container.downloads.enqueue(beatmap, force)
            }
            override fun downloadAll(beatmaps: List<Beatmap>) {
                container.downloads.enqueueAll(beatmaps)
                if (beatmaps.isNotEmpty()) notify("Downloading ${beatmaps.size} songs")
            }
            override fun removeDownload(id: Long) {
                scope.launch { container.downloads.delete(id) }
            }
        }
    }

    // Event listeners for playback, downloads, account updates, and library sync
    LaunchedEffect(playback) { playback.messages.collect { notify(it) } }
    LaunchedEffect(container) { container.downloads.events.collect { notify(it) } }
    LaunchedEffect(container) { container.accounts.events.collect { notify(it) } }
    LaunchedEffect(container) { container.sync.runIfStale() }
    LaunchedEffect(container) { container.accounts.refreshIfSignedIn() }

    // Requests notification permission for media playback controls on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.current != null) {
        if (state.current != null && !askedNotifications && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        LocalTrackActions provides actions,
        LocalNowPlaying provides NowPlayingInfo(state.current?.id, state.isPlaying),
        LocalFavoriteIds provides favoriteIds,
        LocalIsSupporter provides isSupporter,
        LocalDownloads provides downloadInfo
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TabHost(selected = tab) { current ->
                when (current) {
                    KozuTab.HOME -> HomeScreen(
                        listState = homeList,
                        bottomPadding = bottomPadding,
                        onOpenSettings = { settingsOpen = true },
                        onOpenExplore = { tab = KozuTab.EXPLORE },
                        onOpenLibrary = { tab = KozuTab.LIBRARY },
                        onOpenSearch = { searchOpen = true },
                        onRefresh = ::refreshApp
                    )
                    KozuTab.EXPLORE -> ExploreScreen(
                        listState = exploreList,
                        bottomPadding = bottomPadding,
                        onOpenSettings = { settingsOpen = true },
                        onOpenLibrary = { tab = KozuTab.LIBRARY },
                        onOpenSearch = { searchOpen = true },
                        onOpenAllBeatmaps = { allBeatmapsOpen = true },
                        onRefresh = ::refreshApp
                    )
                    KozuTab.LIBRARY -> LibraryScreen(
                        listState = libraryList,
                        bottomPadding = bottomPadding,
                        onOpenSettings = { settingsOpen = true },
                        onOpenSearch = { searchOpen = true }
                    )
                    KozuTab.GAME -> GameScreen(
                        bottomPadding = bottomPadding,
                        onOpenSettings = { settingsOpen = true },
                        onOpenLibrary = { tab = KozuTab.LIBRARY },
                        onOpenSearch = { searchOpen = true }
                    )
                }
            }

            // Bottom bar holding mini player and floating navigation bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { chromeHeightPx = it.height }
            ) {
                AnimatedVisibility(
                    visible = state.current != null,
                    enter = slideInVertically(KozuMotion.spatial()) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    state.current?.let { beatmap ->
                        MiniPlayer(
                            beatmap = beatmap,
                            isPlaying = state.isPlaying,
                            position = playback.position,
                            duration = playback.duration,
                            onOpen = { playerOpen = true },
                            onPlayPause = playback::togglePlayPause,
                            onPrevious = playback::previous,
                            onNext = playback::next,
                            onDismiss = playback::clearQueue
                        )
                    }
                }
                FloatingNavBar(selected = tab, onSelect = { tab = it })
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding)
            )

            // Settings overlay screen
            AnimatedVisibility(
                visible = settingsOpen,
                enter = slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(180)),
                exit = slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(150))
            ) {
                SettingsScreen(onBack = { settingsOpen = false })
            }

            // Search overlay screen
            AnimatedVisibility(
                visible = searchOpen,
                enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut()
            ) {
                SearchScreen(onBack = { searchOpen = false }, onOpenSettings = { settingsOpen = true })
            }
            if (searchOpen && !settingsOpen) BackHandler { searchOpen = false }

            // Full catalog browser. This is a separate screen rather than an
            // inert section header; it shares the paginated catalog state.
            AnimatedVisibility(
                visible = allBeatmapsOpen,
                enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut()
            ) {
                AllBeatmapsScreen(
                    state = discoverState,
                    bottomPadding = bottomPadding,
                    onBack = { allBeatmapsOpen = false },
                    onLoadMore = container.discover::loadMoreMix,
                    onRetry = container.discover::refresh
                )
            }
            if (allBeatmapsOpen && !settingsOpen) BackHandler { allBeatmapsOpen = false }

            // Full-screen player screen overlay
            AnimatedVisibility(
                visible = playerOpen && state.current != null,
                enter = slideInVertically(KozuMotion.spatial()) { it } + fadeIn(),
                exit = slideOutVertically(tween(260)) { it } + fadeOut()
            ) {
                state.current?.let { beatmap ->
                    PlayerScreen(
                        beatmap = beatmap,
                        state = state,
                        position = playback.position,
                        duration = playback.duration,
                        isFavorite = beatmap.id in favoriteIds,
                        onClose = { playerOpen = false },
                        onPlayPause = playback::togglePlayPause,
                        onPrevious = playback::previous,
                        onNext = playback::next,
                        onSeek = playback::seekTo,
                        onShuffle = playback::toggleShuffle,
                        onRepeat = playback::cycleRepeat,
                        onFavorite = { actions.toggleFavorite(beatmap) },
                        onMore = { menuTarget = beatmap },
                        onSkipToQueueItem = playback::skipToQueueItem,
                        onRemoveFromQueue = playback::removeFromQueue
                    )
                }
            }
            if (playerOpen && !settingsOpen) BackHandler { playerOpen = false }
        }

        menuTarget?.let { beatmap ->
            TrackMenuSheet(
                beatmap = beatmap,
                isFavorite = beatmap.id in favoriteIds,
                isDownloaded = beatmap.id in downloadInfo.downloaded,
                isOutdated = beatmap.id in downloadInfo.outdated,
                onDismiss = { menuTarget = null },
                onPlayNext = {
                    playback.playNext(beatmap)
                    menuTarget = null
                    notify("Will play next")
                },
                onAddToQueue = {
                    playback.addToQueue(beatmap)
                    menuTarget = null
                    notify("Added to queue")
                },
                onToggleFavorite = {
                    actions.toggleFavorite(beatmap)
                    menuTarget = null
                },
                onAddToPlaylist = {
                    playlistTarget = beatmap
                    menuTarget = null
                },
                onDownload = {
                    actions.download(beatmap, beatmap.id in downloadInfo.outdated)
                    menuTarget = null
                    notify("Downloading ${beatmap.title}")
                },
                onRemoveDownload = {
                    actions.removeDownload(beatmap.id)
                    menuTarget = null
                    notify("Removed offline copy")
                },
                onOpenInOsu = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, beatmap.webUrl.toUri()))
                    menuTarget = null
                },
                onShare = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${beatmap.title} — ${beatmap.artist}\n${beatmap.webUrl}")
                    }
                    context.startActivity(Intent.createChooser(send, null))
                    menuTarget = null
                }
            )
        }

        playlistTarget?.let { beatmap ->
            PlaylistPickerSheet(
                beatmap = beatmap,
                playlists = playlists,
                onDismiss = { playlistTarget = null },
                onPick = { playlist ->
                    scope.launch { container.playlists.addTrack(playlist.id, beatmap) }
                    playlistTarget = null
                    notify("Added to ${playlist.name}")
                },
                onCreate = { name ->
                    scope.launch { container.playlists.create(name, beatmap) }
                    playlistTarget = null
                    notify("Added to $name")
                }
            )
        }
    }
}

@Composable
private fun TabHost(selected: KozuTab, content: @Composable (KozuTab) -> Unit) {
    AnimatedContent(
        targetState = selected,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(tween(170, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(170, easing = FastOutSlowInEasing)) { it / 32 }) togetherWith
                fadeOut(tween(110))
        },
        label = "appTabs"
    ) { tab ->
        content(tab)
    }
}
