package vn.kozu.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.kozu.data.local.DownloadedSet
import vn.kozu.data.news.NewsReadState
import vn.kozu.data.update.checkIfDue
import vn.kozu.data.update.UpdateResult
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import vn.kozu.domain.model.StartTab
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.ThemePreset
import vn.kozu.game.ui.game.GameViewModel
import vn.kozu.KozuApplication
import vn.kozu.R
import vn.kozu.ui.components.BeatmapInfoSheet
import vn.kozu.ui.components.DownloadInfo
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalBackgroundResolver
import vn.kozu.ui.components.LocalDownloads
import vn.kozu.ui.components.LocalFavoriteIds
import vn.kozu.ui.components.LocalIsSupporter
import vn.kozu.ui.components.LocalMetered
import vn.kozu.ui.components.LocalNowPlaying
import vn.kozu.ui.components.LocalOverlayBottomInset
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.NowPlayingInfo
import vn.kozu.ui.components.PlaylistPickerSheet
import vn.kozu.ui.components.rememberIsMetered
import vn.kozu.ui.components.TrackActions
import vn.kozu.ui.components.TrackMenuSheet
import vn.kozu.ui.explore.AllBeatmapsScreen
import vn.kozu.ui.explore.ExploreScreen
import vn.kozu.ui.game.GameScreen
import vn.kozu.ui.home.HomeScreen
import vn.kozu.ui.library.LibraryScreen
import vn.kozu.ui.navigation.FloatingNavBar
import vn.kozu.ui.navigation.KozuTab
import vn.kozu.ui.onboarding.LocalReplayOnboarding
import vn.kozu.ui.onboarding.OnboardingScreen
import vn.kozu.ui.person.PersonKind
import vn.kozu.ui.person.PersonScreen
import vn.kozu.ui.player.MiniPlayer
import vn.kozu.ui.player.PlayerScreen
import vn.kozu.ui.search.SearchScreen
import vn.kozu.ui.settings.SettingsScreen
import vn.kozu.ui.theme.ArtworkPalette
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.ui.theme.KozuTheme

@Composable
fun KozuApp() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as KozuApplication).container }
    val playback = container.playback
    val cachedTheme = remember { container.settings.cachedThemeMode }
    val cachedAccent = remember { container.settings.cachedAccent }
    val cachedAmoled = remember { container.settings.cachedAmoled }
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue =
            AppSettings(
                themeMode = cachedTheme,
                accent = cachedAccent,
                themePreset = container.settings.cachedThemePreset,
                amoled = cachedAmoled,
                startTab = container.settings.cachedStartTab,
                appFont = container.settings.cachedAppFont,
                appearance = container.settings.cachedAppearance,
                paletteStyle = container.settings.cachedPaletteStyle,
                highRefreshRate = container.settings.cachedHighRefreshRate,
                performanceMode = container.settings.cachedPerformanceMode,
            ),
    )

    val state by playback.state.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    var artworkSeed by remember { mutableStateOf<Color?>(null) }
    val currentArt = state.current?.thumbUrl
    LaunchedEffect(currentArt, settings.artworkColors) {
        artworkSeed = if (settings.artworkColors) ArtworkPalette.seedColor(currentArt) else null
    }


    val artwork = if (state.current != null) artworkSeed else null
    val preset = settings.themePreset
    val seed = artwork ?: if (preset == ThemePreset.CUSTOM) settings.accent.color else preset.seed
    val paletteStyle = if (artwork != null || preset == ThemePreset.CUSTOM) settings.paletteStyle else preset.style
    val fixedPreset = artwork == null && preset != ThemePreset.CUSTOM

    KozuTheme(
        seed = seed,
        darkTheme = dark,
        amoled = settings.amoled,
        appStyle = settings.appStyle,
        appFont = settings.appFont,
        appearance = settings.appearance,
        paletteStyle = paletteStyle,
        secondarySeed = if (fixedPreset) preset.secondary else null,
        tertiarySeed = if (fixedPreset) preset.tertiary else null,
        useSystemDynamic = artwork == null && preset == ThemePreset.DYNAMIC,
    ) {
        val view = LocalView.current
        LaunchedEffect(settings.highRefreshRate) {
            (view.context as? vn.kozu.MainActivity)?.applyRefreshRate(settings.highRefreshRate)
        }
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
        KozuShell(container = container, settings = settings)
    }
}

@Composable
private fun KozuShell(
    container: AppContainer,
    settings: AppSettings,
) {
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

    val downloadInfo =
        remember(downloadedSets, activeDownloads) {
            DownloadInfo(
                downloaded = downloadedSets.map { it.beatmap.id }.toSet(),
                outdated = downloadedSets.filter { it.outdated }.map { it.beatmap.id }.toSet(),
                active = activeDownloads,
            )
        }

    var tab by rememberSaveable { mutableStateOf(settings.startTab.toKozuTab()) }
    var searchFocusSignal by remember { mutableIntStateOf(0) }
    var lastSearchTap by remember { mutableLongStateOf(0L) }
    var lastMainTab by rememberSaveable { mutableStateOf(KozuTab.HOME) }
    LaunchedEffect(tab) { if (tab != KozuTab.SEARCH) lastMainTab = tab }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var statsOpen by rememberSaveable { mutableStateOf(false) }
    var newsOpen by rememberSaveable { mutableStateOf(false) }
    var personName by rememberSaveable { mutableStateOf<String?>(null) }
    var personKindKey by rememberSaveable { mutableStateOf(PersonKind.ARTIST.name) }
    var personOnTopOfPlayer by rememberSaveable { mutableStateOf(false) }
    var lastPersonName by remember { mutableStateOf("") }
    var lastPersonKind by remember { mutableStateOf(PersonKind.ARTIST) }
    LaunchedEffect(personName) {
        personName?.let {
            lastPersonName = it
            lastPersonKind = PersonKind.valueOf(personKindKey)
        }
    }
    val showOnboardingFirst =
        remember { !container.settings.onboardingCompleted && isFreshInstall(context) }
    var onboardingOpen by rememberSaveable { mutableStateOf(showOnboardingFirst) }
    var allBeatmapsOpen by rememberSaveable { mutableStateOf(false) }
    var formgamepage by rememberSaveable { mutableStateOf(false) }
    var gameMapPickerViewModel by remember { mutableStateOf<GameViewModel?>(null) }
    var gamePlayActive by remember { mutableStateOf(false) }

    val settingsVisibility = remember { MutableTransitionState(false) }.apply { targetState = settingsOpen }
    val allBeatmapsVisibility = remember { MutableTransitionState(false) }.apply { targetState = allBeatmapsOpen }
    val statsVisibility = remember { MutableTransitionState(false) }.apply { targetState = statsOpen }
    val newsVisibility = remember { MutableTransitionState(false) }.apply { targetState = newsOpen }
    val playerVisibility = remember { MutableTransitionState(false) }.apply {
        targetState = playerOpen && state.current != null && !personOnTopOfPlayer
    }
    val personVisibility = remember { MutableTransitionState(false) }.apply {
        targetState = personName != null && (!playerOpen || personOnTopOfPlayer)
    }
    val onboardingVisibility = remember { MutableTransitionState(false) }.apply { targetState = onboardingOpen }

    DisposableEffect(playback) {
        vn.kozu.game.mania.previewstage.audio.PreviewPlayer.onAudible = { playback.pause() }
        onDispose { vn.kozu.game.mania.previewstage.audio.PreviewPlayer.onAudible = null }
    }
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) vn.kozu.game.mania.previewstage.PreviewStageModule.peek()?.stopPreview()
    }
    var menuTarget by remember { mutableStateOf<Beatmap?>(null) }
    var playlistTarget by remember { mutableStateOf<Beatmap?>(null) }
    var infoTarget by remember { mutableStateOf<Beatmap?>(null) }
    var isAppRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(settingsOpen, allBeatmapsOpen, statsOpen, newsOpen, personName, onboardingOpen) {
        if (settingsOpen || allBeatmapsOpen || statsOpen || newsOpen || personName != null || onboardingOpen) {
            menuTarget = null
            playlistTarget = null
            infoTarget = null
        }
    }

    val density = LocalDensity.current
    var chromeHeightPx by remember { mutableIntStateOf(0) }
    val bottomPadding = with(density) { chromeHeightPx.toDp() } + 16.dp

    var overlayMiniPx by remember { mutableIntStateOf(0) }
    val overlayScreenOpen = settingsOpen || allBeatmapsOpen || statsOpen || newsOpen
    val mainPageVisible = !(
        settingsVisibility.currentState || settingsVisibility.targetState ||
            allBeatmapsVisibility.currentState || allBeatmapsVisibility.targetState ||
            statsVisibility.currentState || statsVisibility.targetState ||
            newsVisibility.currentState || newsVisibility.targetState ||
            playerVisibility.currentState || playerVisibility.targetState ||
            personVisibility.currentState || personVisibility.targetState ||
            onboardingVisibility.currentState || onboardingVisibility.targetState
    )
    val overlayMiniInset = if (state.current != null) with(density) { overlayMiniPx.toDp() } else 0.dp
    fun closeAllBeatmaps() {
        allBeatmapsOpen = false
        formgamepage = false
    }

    fun closePerson() {
        personName = null
        personOnTopOfPlayer = false
    }

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
        if (isAppRefreshing) return
        isAppRefreshing = true
        container.discover.refresh()
        scope.launch {
            try {
                coroutineScope {
                    val catalogUpdate =
                        async {
                            container.discover.state.first { !it.isRefreshing }
                        }
                    val profileUpdate =
                        async {
                            container.accounts.refreshIfSignedIn(force = true)
                        }
                    val libraryUpdate = async { container.sync.run() }
                    profileUpdate.await()
                    libraryUpdate.await()
                    catalogUpdate.await()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                notify("Some app data could not be refreshed")
            } finally {
                isAppRefreshing = false
            }
        }
    }

    val latestSettings by rememberUpdatedState(settings)
    val actions =
        remember(playback, container) {
            object : TrackActions {
                override fun play(
                    queue: List<Beatmap>,
                    index: Int,
                ) {
                    playback.playQueue(queue, index)
                    if (latestSettings.autoOpenPlayer) {
                        personOnTopOfPlayer = false
                        playerOpen = true
                    }
                }

                override fun playShuffled(queue: List<Beatmap>) {
                    playback.playShuffled(queue)
                    if (latestSettings.autoOpenPlayer) {
                        personOnTopOfPlayer = false
                        playerOpen = true
                    }
                }

                override fun playShuffledWithOrder(queue: List<Beatmap>): List<Beatmap> {
                    val shuffled = playback.playShuffledWithOrder(queue)
                    if (latestSettings.autoOpenPlayer) {
                        personOnTopOfPlayer = false
                        playerOpen = true
                    }
                    return shuffled
                }

                override fun toggleFavorite(beatmap: Beatmap) {
                    scope.launch { container.favorites.toggle(beatmap) }
                }

                override fun openMenu(beatmap: Beatmap) {
                    menuTarget = beatmap
                }

                override fun openInfo(beatmap: Beatmap) {
                    infoTarget = beatmap
                }

                override fun download(
                    beatmap: Beatmap,
                    force: Boolean,
                ) {
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, container) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                NewsReadState.initialize(context)
                scope.launch {
                    runCatching { container.news.load() }
                        .onSuccess { page -> NewsReadState.updateLatest(context, page.posts) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playback) { playback.messages.collect { notify(it) } }
    LaunchedEffect(container) { container.downloads.events.collect { notify(it) } }
    LaunchedEffect(container) { container.accounts.events.collect { notify(it) } }
    LaunchedEffect(container) {
        if (container.settings.settings
                .first()
                .autoSyncLibrary
        ) {
            container.sync.runIfStale()
        }
    }
    LaunchedEffect(container) { container.accounts.refreshIfSignedIn() }
    LaunchedEffect(container) {
        val result = runCatching { container.updates.checkIfDue(container.settings) }.getOrNull()
        if (result is UpdateResult.Available) notify("Kozu ${result.version} is available")
    }

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

    val metered by rememberIsMetered()
    val backgroundResolver =
        remember<suspend (Long, Boolean) -> java.io.File?>(container) {
            { id, allowRemote -> container.streams.resolveBackground(id, allowRemote) }
        }
    CompositionLocalProvider(
        LocalBackgroundResolver provides backgroundResolver,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        LocalAppSettings provides settings,
        LocalMetered provides metered,
        LocalPerformanceMode provides settings.performanceMode,
        LocalTrackActions provides actions,
        LocalNowPlaying provides NowPlayingInfo(state.current?.id, state.isPlaying),
        LocalFavoriteIds provides favoriteIds,
        LocalIsSupporter provides isSupporter,
        LocalDownloads provides downloadInfo,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (mainPageVisible) {
                TabHost(selected = tab) { current ->
                    when (current) {
                        KozuTab.HOME -> {
                            HomeScreen(
                                listState = homeList,
                                bottomPadding = bottomPadding,
                                onOpenSettings = { settingsOpen = true },
                                onOpenExplore = { tab = KozuTab.EXPLORE },
                                onOpenLibrary = { tab = KozuTab.LIBRARY },
                                onOpenStats = { statsOpen = true },
                                onOpenNews = { newsOpen = true },
                                onRefresh = ::refreshApp,
                                isAppRefreshing = isAppRefreshing,
                            )
                        }

                        KozuTab.EXPLORE -> {
                            ExploreScreen(
                                listState = exploreList,
                                bottomPadding = bottomPadding,
                                onOpenSettings = { settingsOpen = true },
                                onOpenNews = { newsOpen = true },
                                onOpenLibrary = { tab = KozuTab.LIBRARY },
                                onOpenSearch = { tab = KozuTab.SEARCH },
                                onOpenAllBeatmaps = {
                                    formgamepage = false
                                    allBeatmapsOpen = true
                                },
                                onRefresh = ::refreshApp,
                                isAppRefreshing = isAppRefreshing,
                            )
                        }

                        KozuTab.LIBRARY -> {
                            LibraryScreen(
                                listState = libraryList,
                                bottomPadding = bottomPadding,
                                onOpenSettings = { settingsOpen = true },
                                onOpenNews = { newsOpen = true },
                                onSyncError = ::notify,
                            )
                        }

                        KozuTab.SEARCH -> {
                            SearchScreen(
                                focusSignal = searchFocusSignal,
                                bottomPadding = bottomPadding,
                                onOpenSettings = { settingsOpen = true },
                                onRefresh = ::refreshApp,
                                isAppRefreshing = isAppRefreshing,
                            )
                        }

                        KozuTab.GAME -> {
                            GameScreen(
                                isSelected = tab == KozuTab.GAME,
                                bottomPadding = bottomPadding,
                                onOpenSettings = { settingsOpen = true },
                                onOpenNews = { newsOpen = true },
                                onGameplayChanged = { active ->
                                    if (active != gamePlayActive) {
                                        gamePlayActive = active
                                        if (active) playback.pause()
                                    }
                                },
                                onOpenAllBeatmapsFromPreview = { gameViewModel ->
                                    gameMapPickerViewModel = gameViewModel
                                    formgamepage = true
                                    allBeatmapsOpen = true
                                },
                            )
                        }
                    }
                }

                if (!gamePlayActive) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.3f to MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        0.5f to MaterialTheme.colorScheme.surface,
                                    ),
                                ).onSizeChanged { chromeHeightPx = it.height },
                    ) {
                        AnimatedVisibility(
                            visible = state.current != null,
                            enter = slideInVertically(KozuMotion.spatial()) { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                        ) {
                            state.current?.let { beatmap ->
                                MiniPlayer(
                                    beatmap = beatmap,
                                    isPlaying = state.isPlaying,
                                    position = playback.position,
                                    duration = playback.duration,
                                    onOpen = {
                                        personOnTopOfPlayer = false
                                        playerOpen = true
                                    },
                                    onPlayPause = playback::togglePlayPause,
                                    onPrevious = playback::previous,
                                    onNext = playback::next,
                                    onDismiss = playback::clearQueue,
                                )
                            }
                        }
                        FloatingNavBar(
                            selected = tab,
                            style = settings.navigationStyle,
                            compact = settings.compactNavigation,
                            cornerRadius = settings.navCornerRadius,
                            onSelect = { target ->
                                if (target == KozuTab.SEARCH && latestSettings.doubleTapSearch) {
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastSearchTap <= 380L) searchFocusSignal++
                                    lastSearchTap = now
                                }
                                tab = target
                            },
                        )
                    }
                }

                if (tab == KozuTab.SEARCH) {
                    BackHandler { tab = lastMainTab }
                }
            }


            CompositionLocalProvider(
                LocalOverlayBottomInset provides overlayMiniInset,
                LocalReplayOnboarding provides {
                    settingsOpen = false
                    onboardingOpen = true
                },
            ) {
                AnimatedVisibility(
                    visibleState = settingsVisibility,
                    enter = slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(180)),
                    exit = slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(150)),
                ) {
                    SettingsScreen(
                        onBack = { settingsOpen = false },
                    )
                }

                AnimatedVisibility(
                    visibleState = allBeatmapsVisibility,
                    enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                    exit = slideOutHorizontally(tween(240)) { it } + fadeOut(),
                ) {
                    AllBeatmapsScreen(
                        onBack = ::closeAllBeatmaps,
                        onRetry = ::refreshApp,
                        isAppRefreshing = isAppRefreshing,
                        formgamepage = formgamepage,
                        gameViewModel = if (formgamepage) gameMapPickerViewModel else null,
                        onPlayGameMap = { beatmapSetId ->
                            gameMapPickerViewModel?.requestOpenMap(beatmapSetId)
                            closeAllBeatmaps()
                        },
                    )
                }


                AnimatedVisibility(
                    visibleState = statsVisibility,
                    enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                    exit = slideOutHorizontally(tween(240)) { it } + fadeOut(),
                ) {
                    val settingsViewModel: vn.kozu.ui.settings.SettingsViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel(factory = vn.kozu.ui.settings.SettingsViewModel.Factory)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        vn.kozu.ui.settings.StatsPage(settingsViewModel) { statsOpen = false }
                    }
                }


                AnimatedVisibility(
                    visibleState = newsVisibility,
                    enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                    exit = slideOutHorizontally(tween(240)) { it } + fadeOut(),
                ) {
                    vn.kozu.ui.news.NewsScreen(
                        onBack = { newsOpen = false },
                        container = container,
                    )
                }
            }
            if (allBeatmapsOpen && !settingsOpen && !statsOpen) BackHandler { closeAllBeatmaps() }
            if (statsOpen && !settingsOpen) BackHandler { statsOpen = false }

            AnimatedVisibility(
                visibleState = playerVisibility,
                enter = slideInVertically(KozuMotion.spatial()) { it } + fadeIn(),
                exit = slideOutVertically(tween(260)) { it } + fadeOut(),
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
                        onToggleAutoplay = {
                            scope.launch { container.settings.setAutoplay(!settings.autoplay) }
                        },
                        onShuffle = playback::toggleShuffle,
                        onRepeat = playback::cycleRepeat,
                        onFavorite = { actions.toggleFavorite(beatmap) },
                        onMore = { menuTarget = beatmap },
                        onArtistClick = { artist ->
                            if (artist.isNotBlank()) {
                                personKindKey = PersonKind.ARTIST.name
                                personOnTopOfPlayer = true
                                personName = artist
                            }
                        },
                        onMapperClick = { mapper ->
                            if (mapper.isNotBlank()) {
                                personKindKey = PersonKind.MAPPER.name
                                personOnTopOfPlayer = true
                                personName = mapper
                            }
                        },
                        onSkipToQueueItem = playback::skipToQueueItem,
                        onRemoveFromQueue = playback::removeFromQueue,
                        audioInfo = playback.audioInfo,
                        sleepTimer = playback.sleepTimer,
                        onSetSleepTimer = playback::setSleepTimer,
                        onSleepAtEndOfTrack = playback::sleepAtEndOfTrack,
                        onCancelSleepTimer = playback::cancelSleepTimer,
                        onClearQueue = playback::clearQueue,
                        onMoveQueueItem = playback::moveQueueItem,
                        onSaveAsPlaylist = { tracks ->
                            val unique = tracks.distinctBy { it.id }
                            if (unique.isNotEmpty()) {
                                scope.launch {
                                    val id = container.playlists.create("Queue (${unique.size} tracks)")
                                    unique.forEach { container.playlists.addTrack(id, it) }
                                    snackbar.showSnackbar(context.getString(R.string.saved_playlist, unique.size))
                                }
                            }
                        },
                    )
                }
            }
            if (playerOpen && !personOnTopOfPlayer) BackHandler { playerOpen = false }


            AnimatedVisibility(
                visibleState = personVisibility,
                enter = slideInHorizontally(KozuMotion.spatial()) { it } + fadeIn(),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut(),
            ) {
                CompositionLocalProvider(LocalOverlayBottomInset provides overlayMiniInset) {
                    PersonScreen(
                        kind = personName?.let { PersonKind.valueOf(personKindKey) } ?: lastPersonKind,
                        name = personName ?: lastPersonName,
                        onBack = ::closePerson,
                    )
                }
            }
            if (personName != null && (!playerOpen || personOnTopOfPlayer)) BackHandler { closePerson() }


            AnimatedVisibility(
                visible =
                    state.current != null &&
                        ((personName != null && (!playerOpen || personOnTopOfPlayer)) ||
                            (overlayScreenOpen && !playerOpen)),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(160)) + slideInVertically(KozuMotion.spatial()) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically { it / 2 },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.3f to MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    0.5f to MaterialTheme.colorScheme.surface,
                                ),
                            ).navigationBarsPadding(),
                ) {
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.onSizeChanged { overlayMiniPx = it.height }) {
                        state.current?.let { beatmap ->
                            MiniPlayer(
                                beatmap = beatmap,
                                isPlaying = state.isPlaying,
                                position = playback.position,
                                duration = playback.duration,
                                onOpen = {
                                    personOnTopOfPlayer = false
                                    playerOpen = true
                                },
                                onPlayPause = playback::togglePlayPause,
                                onPrevious = playback::previous,
                                onNext = playback::next,
                                onDismiss = playback::clearQueue,
                            )
                        }
                    }
                }
            }


            val overlayOpen = settingsOpen || playerOpen || allBeatmapsOpen || statsOpen || newsOpen
            SnackbarHost(
                hostState = snackbar,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .then(
                            if (overlayOpen) {
                                Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp + if (playerOpen) 0.dp else overlayMiniInset)
                            } else {
                                Modifier.padding(bottom = bottomPadding)
                            },
                        ),
            )


            AnimatedVisibility(
                visibleState = onboardingVisibility,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(260)),
            ) {
                OnboardingScreen(
                    container = container,
                    settings = settings,
                    onFinish = {
                        container.settings.onboardingCompleted = true
                        onboardingOpen = false
                    },
                )
            }
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
                onInfo = {
                    infoTarget = beatmap
                    menuTarget = null
                },
                onOpenInOsu = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, beatmap.webUrl.toUri()))
                    menuTarget = null
                },
                onShare = {
                    val send =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${beatmap.title} — ${beatmap.artist}\n${beatmap.webUrl}")
                        }
                    context.startActivity(Intent.createChooser(send, null))
                    menuTarget = null
                },
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
                },
            )
        }

        infoTarget?.let { beatmap ->
            BeatmapInfoSheet(
                beatmap = beatmap,
                loadInfo = container.catalog::setInfo,
                onDismiss = { infoTarget = null },
            )
        }
    }
}


private fun isFreshInstall(context: android.content.Context): Boolean =
    runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)

private fun StartTab.toKozuTab(): KozuTab =
    when (this) {
        StartTab.HOME -> KozuTab.HOME
        StartTab.EXPLORE -> KozuTab.EXPLORE
        StartTab.LIBRARY -> KozuTab.LIBRARY
        StartTab.GAME -> KozuTab.GAME
    }

@Composable
private fun TabHost(
    selected: KozuTab,
    content: @Composable (KozuTab) -> Unit,
) {
    AnimatedContent(
        targetState = selected,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (
                fadeIn(tween(170, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(170, easing = FastOutSlowInEasing)) { it / 32 }
            ) togetherWith
                fadeOut(tween(110))
        },
        label = "appTabs",
    ) { tab ->
        content(tab)
    }
}
