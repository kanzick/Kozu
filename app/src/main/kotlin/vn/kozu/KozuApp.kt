package vn.kozu

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.local.FavoritesStore
import vn.kozu.domain.model.Beatmap
import vn.kozu.player.MusicPlayerManager
import vn.kozu.ui.KozuTab
import vn.kozu.ui.KozuThemeMode
import vn.kozu.ui.demoBeatmaps
import vn.kozu.ui.theme.KozuDarkColorScheme
import vn.kozu.ui.theme.KozuLightColorScheme
import vn.kozu.ui.theme.Typography
import vn.kozu.ui.explore.ExploreScreen
import vn.kozu.ui.home.HomeScreen
import vn.kozu.ui.library.LibraryScreen
import vn.kozu.ui.navigation.FloatingBottomBar
import vn.kozu.ui.player.PlayerScreen
import vn.kozu.ui.player.MiniPlayer
import vn.kozu.ui.search.SearchScreen
import vn.kozu.ui.settings.SettingsScreen

private const val KOZU_SETTINGS_PREFS = "kozu_settings"
private const val KOZU_THEME_MODE_KEY = "theme_mode"

private suspend fun extractCoverColor(
    imageUrl: String?
): Color? = withContext(Dispatchers.IO) {
    if (imageUrl.isNullOrBlank()) return@withContext null

    try {
        val connection = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.doInput = true

        try {
            connection.connect()
            val bitmap = connection.inputStream.use { stream -> BitmapFactory.decodeStream(stream) }
                ?: return@withContext null
            try {
                val palette = Palette.from(bitmap).generate()
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
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun KozuApp() {
    var selectedTab by remember { mutableStateOf(KozuTab.HOME) }
    var selectedBeatmap by remember { mutableStateOf<Beatmap?>(null) }
    var recentlyPicked by remember { mutableStateOf<List<Beatmap>>(emptyList()) }
    var isPlayerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    // Keep playback position state out of KozuApp's read scope. Only the mini-player
    // or full player observes these values, so the whole screen tree does not recompose
    // on every playback tick (which previously made tab transitions feel choppy).
    val currentPositionState = remember { mutableStateOf(0L) }
    val durationState = remember { mutableStateOf(0L) }
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
    val kozuColors = if (isDarkTheme) KozuDarkColorScheme else KozuLightColorScheme

    LaunchedEffect(selectedBeatmap?.coverUrl) {
        albumProgressColor = extractCoverColor(selectedBeatmap?.coverUrl)
    }

    val playerManager = remember(context) { MusicPlayerManager(context.applicationContext) }

    DisposableEffect(playerManager) {
        onDispose { playerManager.release() }
    }

    LaunchedEffect(playerManager) {
        while (true) {
            val player = playerManager.player
            currentPositionState.value = player.currentPosition.coerceAtLeast(0L)
            durationState.value = player.duration.takeIf { it > 0L } ?: 0L
            isPlaying = player.isPlaying
            delay(400)
        }
    }

    val favoritesStore = remember(context) { FavoritesStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val favorites by favoritesStore.favoriteIds.collectAsState(initial = emptySet())

    fun selectBeatmap(beatmap: Beatmap) {
        selectedBeatmap = beatmap
        recentlyPicked = (listOf(beatmap) + recentlyPicked.filterNot { it.id == beatmap.id }).take(5)
        isPlaying = playerManager.player.isPlaying
    }

    fun togglePlayback() {
        if (playerManager.player.isPlaying) {
            playerManager.pause()
            isPlaying = false
        } else {
            if (playerManager.player.currentMediaItem == null) {
                playerManager.playLocalResource(R.raw.test)
            } else {
                playerManager.resume()
            }
            isPlaying = playerManager.player.isPlaying
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

    MaterialTheme(colorScheme = kozuColors, typography = Typography) {
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                val statusColor = if (!isPlayerOpen && !isSettingsOpen &&
                    selectedTab in setOf(
                        KozuTab.HOME,
                        KozuTab.EXPLORE,
                        KozuTab.SEARCH,
                        KozuTab.LIBRARY
                    )
                ) {
                    kozuColors.primaryContainer
                } else {
                    kozuColors.background
                }
                window.statusBarColor = statusColor.toArgb()
                window.navigationBarColor = kozuColors.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }

        Scaffold(
            bottomBar = {
                if (!isPlayerOpen && !isSettingsOpen) {
                    Column {
                        selectedBeatmap?.let { beatmap ->
                            MiniPlayer(
                                beatmap = beatmap,
                                isPlaying = isPlaying,
                                currentPositionState = currentPositionState,
                                durationState = durationState,
                                progressColor = albumProgressColor ?: kozuColors.primary,
                                onSeek = { position ->
                                    playerManager.player.seekTo(position)
                                    currentPositionState.value = position
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
                    .padding(innerPadding)
            ) {
                if (isPlayerOpen && selectedBeatmap != null) {
                    PlayerScreen(
                        beatmap = selectedBeatmap!!,
                        isPlaying = isPlaying,
                        isFavorite = selectedBeatmap!!.id in favorites,
                        currentPositionState = currentPositionState,
                        durationState = durationState,
                        progressColor = albumProgressColor ?: kozuColors.primary,
                        onBack = { isPlayerOpen = false },
                        onSeek = { position ->
                            playerManager.player.seekTo(position)
                            currentPositionState.value = position
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
                } else if (isSettingsOpen) {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            themePreferences.edit().putString(KOZU_THEME_MODE_KEY, mode.preferenceValue).apply()
                        },
                        onBackToHome = { isSettingsOpen = false }
                    )
                } else {
                    Crossfade(targetState = selectedTab, label = "kozuScreenTransition") { tab ->
                        when (tab) {
                            KozuTab.HOME -> HomeScreen(
                                favorites = favorites,
                                recentlyPicked = recentlyPicked,
                                onExplore = { selectedTab = KozuTab.EXPLORE },
                                onSearch = { selectedTab = KozuTab.SEARCH },
                                onSettings = { isSettingsOpen = true },
                                onBeatmapSelected = ::selectBeatmap,
                                onFavorite = { beatmap ->
                                    scope.launch { favoritesStore.toggleFavorite(beatmap.id) }
                                }
                            )

                            KozuTab.EXPLORE -> ExploreScreen(
                                recentlyPicked = recentlyPicked,
                                favorites = demoBeatmaps.filter { it.id in favorites },
                                onSearch = { selectedTab = KozuTab.SEARCH },
                                onOpenLibrary = { selectedTab = KozuTab.LIBRARY },
                                onBeatmapSelected = ::selectBeatmap
                            )

                            KozuTab.SEARCH -> SearchScreen(onBeatmapSelected = ::selectBeatmap)

                            KozuTab.LIBRARY -> LibraryScreen(
                                favorites = demoBeatmaps.filter { it.id in favorites },
                                allBeatmaps = demoBeatmaps,
                                onBeatmapSelected = ::selectBeatmap,
                                onFavorite = { beatmap ->
                                    scope.launch { favoritesStore.toggleFavorite(beatmap.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
