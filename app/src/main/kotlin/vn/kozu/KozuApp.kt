package vn.kozu

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.FavoritesStore
import vn.kozu.model.Beatmap
import vn.kozu.model.KozuTab
import vn.kozu.model.KozuThemeMode
import vn.kozu.model.demoBeatmaps
import vn.kozu.player.MusicPlayerManager
import vn.kozu.ui.components.FloatingBottomBar
import vn.kozu.ui.screens.ExploreScreen
import vn.kozu.ui.screens.HomeScreen
import vn.kozu.ui.screens.LibraryScreen
import vn.kozu.ui.screens.MiniPlayer
import vn.kozu.ui.screens.PlayerScreen
import vn.kozu.ui.screens.SearchScreen
import vn.kozu.ui.screens.SettingsScreen
import vn.kozu.ui.theme.Typography
import vn.kozu.ui.theme.kozuColorScheme

private const val KOZU_SETTINGS_PREFS = "kozu_settings"
private const val KOZU_THEME_MODE_KEY = "theme_mode"

private suspend fun extractCoverColor(imageUrl: String?): Color? = withContext(Dispatchers.IO) {
    if (imageUrl.isNullOrBlank()) return@withContext null

    try {
        val connection = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.doInput = true

        try {
            connection.connect()
            val bitmap = connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = 4
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                )
            } ?: return@withContext null

            try {
                val palette = Palette.from(bitmap).resizeBitmapArea(20_000).generate()
                val rgb = palette.vibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                rgb?.let { Color(it) }
            } finally {
                bitmap.recycle()
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

@Composable
fun KozuApp() {
    var selectedTab by remember { mutableStateOf(KozuTab.HOME) }
    var selectedBeatmap by remember { mutableStateOf<Beatmap?>(null) }
    var recentlyPicked by remember { mutableStateOf<List<Beatmap>>(emptyList()) }
    var isPlayerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val currentPositionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    var albumProgressColor by remember { mutableStateOf<Color?>(null) }

    val context = LocalContext.current
    val view = LocalView.current
    val themePreferences = remember(context) {
        context.getSharedPreferences(KOZU_SETTINGS_PREFS, Context.MODE_PRIVATE)
    }
    var themeMode by remember(themePreferences) {
        mutableStateOf(KozuThemeMode.fromPreference(themePreferences.getString(KOZU_THEME_MODE_KEY, "system")))
    }
    val systemIsDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        KozuThemeMode.SYSTEM -> systemIsDark
        KozuThemeMode.LIGHT -> false
        KozuThemeMode.DARK -> true
    }

    LaunchedEffect(selectedBeatmap?.coverUrl) {
        albumProgressColor = extractCoverColor(selectedBeatmap?.coverUrl)
    }

    val playerManager = remember(context) { MusicPlayerManager(context.applicationContext) }

    DisposableEffect(playerManager) {
        onDispose { playerManager.release() }
    }

    DisposableEffect(playerManager) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val player = playerManager.player
                durationState.longValue = player.duration.takeIf { it > 0L } ?: 0L
                if (playbackState == Player.STATE_ENDED) {
                    currentPositionState.longValue = 0L
                }
            }

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {
                currentPositionState.longValue = 0L
                durationState.longValue = playerManager.player.duration.takeIf { it > 0L } ?: 0L
            }
        }
        playerManager.player.addListener(listener)
        onDispose { playerManager.player.removeListener(listener) }
    }

    LaunchedEffect(playerManager, isPlaying) {
        val player = playerManager.player
        durationState.longValue = player.duration.takeIf { it > 0L } ?: 0L
        if (isPlaying) {
            while (true) {
                val position = player.currentPosition.coerceAtLeast(0L)
                if (currentPositionState.longValue != position) {
                    currentPositionState.longValue = position
                }
                delay(800)
            }
        } else {
            currentPositionState.longValue = player.currentPosition.coerceAtLeast(0L)
        }
    }

    val favoritesStore = remember(context) { FavoritesStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val favorites by favoritesStore.favoriteIds.collectAsState(initial = emptySet())

    fun selectBeatmap(beatmap: Beatmap) {
        selectedBeatmap = beatmap
        recentlyPicked = (listOf(beatmap) + recentlyPicked.filterNot { it.id == beatmap.id }).take(8)
        playerManager.playBeatmap(beatmap, R.raw.test)
        isPlaying = true
    }

    fun togglePlayback() {
        if (playerManager.player.isPlaying) {
            playerManager.pause()
            isPlaying = false
        } else {
            if (playerManager.player.currentMediaItem == null) {
                val toPlay = selectedBeatmap ?: demoBeatmaps.first()
                selectBeatmap(toPlay)
            } else {
                playerManager.resume()
                isPlaying = true
            }
        }
    }

    fun moveTrack(offset: Int) {
        if (demoBeatmaps.isEmpty()) return
        val currentIndex = demoBeatmaps.indexOfFirst { it.id == selectedBeatmap?.id }
        val nextIndex = if (currentIndex < 0) {
            if (offset >= 0) 0 else demoBeatmaps.lastIndex
        } else {
            (currentIndex + offset).mod(demoBeatmaps.size)
        }
        selectBeatmap(demoBeatmaps[nextIndex])
    }

    val activeArtworkColor = if (isPlaying) albumProgressColor else null
    val kozuColors = kozuColorScheme(
        isDarkTheme = isDarkTheme,
        playingArtworkColor = activeArtworkColor
    )

    MaterialTheme(colorScheme = kozuColors, typography = Typography) {
        val activity = context as? Activity
        DisposableEffect(activity, isDarkTheme) {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
            onDispose {}
        }

        if (isPlayerOpen) {
            BackHandler { isPlayerOpen = false }
        } else if (isSettingsOpen) {
            BackHandler { isSettingsOpen = false }
        }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = !isPlayerOpen && !isSettingsOpen,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it }
                ) {
                    Column {
                        selectedBeatmap?.let { beatmap ->
                            MiniPlayer(
                                beatmap = beatmap,
                                isPlaying = isPlaying,
                                currentPositionState = currentPositionState,
                                durationState = durationState,
                                progressColor = kozuColors.primary,
                                onSeek = { position ->
                                    playerManager.player.seekTo(position)
                                    currentPositionState.longValue = position
                                },
                                onPlayPause = ::togglePlayback,
                                onPrevious = { moveTrack(-1) },
                                onNext = { moveTrack(1) },
                                onOpen = { isPlayerOpen = true }
                            )
                        }
                        FloatingBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (!isPlayerOpen && !isSettingsOpen) innerPadding else PaddingValues())
            ) {
                when {
                    isPlayerOpen && selectedBeatmap != null -> {
                        PlayerScreen(
                            beatmap = selectedBeatmap!!,
                            isPlaying = isPlaying,
                            isFavorite = selectedBeatmap!!.id in favorites,
                            currentPositionState = currentPositionState,
                            durationState = durationState,
                            progressColor = kozuColors.primary,
                            onBack = { isPlayerOpen = false },
                            onSeek = { position ->
                                playerManager.player.seekTo(position)
                                currentPositionState.longValue = position
                            },
                            onPlayPause = ::togglePlayback,
                            onPrevious = { moveTrack(-1) },
                            onNext = { moveTrack(1) },
                            onFavorite = {
                                scope.launch {
                                    favoritesStore.toggleFavorite(selectedBeatmap!!.id)
                                }
                            }
                        )
                    }

                    isSettingsOpen -> {
                        SettingsScreen(
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                themeMode = mode
                                themePreferences.edit { putString(KOZU_THEME_MODE_KEY, mode.preferenceValue) }
                            },
                            onBackToHome = { isSettingsOpen = false }
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "tabContentTransition"
                        ) { tab ->
                            when (tab) {
                                KozuTab.HOME -> HomeScreen(
                                    favorites = favorites,
                                    recentlyPicked = recentlyPicked,
                                    onExplore = { selectedTab = KozuTab.EXPLORE },
                                    onSearch = { selectedTab = KozuTab.SEARCH },
                                    onSettings = { isSettingsOpen = true },
                                    onBeatmapSelected = ::selectBeatmap,
                                    onFavorite = { beatmap ->
                                        scope.launch {
                                            favoritesStore.toggleFavorite(beatmap.id)
                                        }
                                    }
                                )

                                KozuTab.EXPLORE -> ExploreScreen(
                                    recentlyPicked = recentlyPicked,
                                    favorites = demoBeatmaps.filter { it.id in favorites },
                                    onSearch = { selectedTab = KozuTab.SEARCH },
                                    onOpenLibrary = { selectedTab = KozuTab.LIBRARY },
                                    onBeatmapSelected = ::selectBeatmap
                                )

                                KozuTab.SEARCH -> SearchScreen(
                                    onBeatmapSelected = ::selectBeatmap
                                )

                                KozuTab.LIBRARY -> LibraryScreen(
                                    favorites = demoBeatmaps.filter { it.id in favorites },
                                    allBeatmaps = demoBeatmaps,
                                    onBeatmapSelected = ::selectBeatmap,
                                    onFavorite = { beatmap ->
                                        scope.launch {
                                            favoritesStore.toggleFavorite(beatmap.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
