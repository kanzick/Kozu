package vn.kozu

import android.os.Bundle
import android.app.WallpaperManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.palette.graphics.Palette
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import vn.kozu.player.MusicPlayerManager
import vn.kozu.data.local.FavoritesStore
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.search.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KozuApp()
        }
    }
}


private enum class KozuTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    LIBRARY("Library", Icons.Filled.LibraryMusic),
    SETTINGS("Settings", Icons.Filled.Settings)
}

private val demoBeatmaps = listOf(
    Beatmap(
        id = 1L,
        title = "夜に駆ける",
        artist = "YOASOBI",
        mapper = "Demo mapper",
        beatmapsetId = null
    ),
    Beatmap(
        id = 2L,
        title = "Lemon",
        artist = "Kenshi Yonezu",
        mapper = "Tripticon",
        beatmapsetId = 1313117L
    ),
    Beatmap(
        id = 3L,
        title = "Megalovania",
        artist = "Toby Fox",
        mapper = "Kyshiro",
        beatmapsetId = 387700L
    ),
    Beatmap(
        id = 4L,
        title = "Bad Apple!!",
        artist = "Masayoshi Minoshima",
        mapper = "Pisapou",
        beatmapsetId = 1936770L
    )
)


private fun getWallpaperSeed(context: android.content.Context): Color {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperColors =
                wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)

            val wallpaperColor = wallpaperColors?.primaryColor

            if (wallpaperColor != null) {
                Color(wallpaperColor.toArgb())
            } else {
                Color(0xFFE7EF91)
            }
        } catch (_: Exception) {
            Color(0xFFE7EF91)
        }
    }

    return Color(0xFFE7EF91)
}

private fun makeKozuColorScheme(
    seed: Color,
    dark: Boolean
): androidx.compose.material3.ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = seed,
            secondary = Color(0xFFC6B7FF),
            tertiary = Color(0xFFFF9FCB),
            background = Color(0xFF151019),
            surface = Color(0xFF211A28),
            surfaceVariant = Color(0xFF302638)
        )
    } else {
        lightColorScheme(
            primary = seed,
            secondary = Color(0xFF747A36),
            tertiary = Color(0xFFB85D83),
            background = Color(0xFFFFF9EC),
            surface = Color(0xFFFFFDF7),
            surfaceVariant = Color(0xFFF0E9D9)
        )
    }
}

private suspend fun extractCoverColor(
    context: Context,
    imageUrl: String?
): Color? = withContext(Dispatchers.IO) {
    if (imageUrl.isNullOrBlank()) {
        return@withContext null
    }

    try {
        val connection = java.net.URL(imageUrl)
            .openConnection() as java.net.HttpURLConnection

        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.doInput = true

        try {
            connection.connect()

            val bitmap = connection.inputStream.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val palette = Palette.from(bitmap).generate()

            val rgb = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb

            rgb?.let { Color(it) }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KozuApp() {
    var selectedTab by remember {
        mutableStateOf(KozuTab.HOME)
    }

    var selectedBeatmap by remember {
        mutableStateOf<Beatmap?>(null)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }
    var currentPosition by remember {
        mutableStateOf(0L)
    }

    var duration by remember {
        mutableStateOf(0L)
    }

    val context = LocalContext.current

    val wallpaperSeed = remember(context) {
        getWallpaperSeed(context)
    }

    var albumSeed by remember {
        mutableStateOf<Color?>(null)
    }

    LaunchedEffect(selectedBeatmap?.coverUrl) {
        albumSeed = extractCoverColor(
            context = context,
            imageUrl = selectedBeatmap?.coverUrl
        )
    }
    val isDarkTheme = selectedTab == KozuTab.SEARCH || selectedTab == KozuTab.SETTINGS

    val kozuColors = remember(
        wallpaperSeed,
        albumSeed,
        isDarkTheme
    ) {
        makeKozuColorScheme(
            seed = albumSeed ?: wallpaperSeed,
            dark = isDarkTheme
        )
    }
    val playerManager = remember(context) {
        MusicPlayerManager(context.applicationContext)
    }

    DisposableEffect(playerManager) {
        onDispose {
            playerManager.release()
        }
    }
    LaunchedEffect(playerManager) {
        while (true) {
            val player = playerManager.player

            currentPosition = player.currentPosition.coerceAtLeast(0L)

            duration = player.duration.takeIf {
                it > 0L
            } ?: 0L

            isPlaying = player.isPlaying

            kotlinx.coroutines.delay(500)
        }
    }
    val favoritesStore = remember(context) {
        FavoritesStore(context.applicationContext)
    }

    val scope = rememberCoroutineScope()

    val favorites by favoritesStore.favoriteIds.collectAsState(
        initial = emptySet()
    )

    MaterialTheme(colorScheme = kozuColors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Kozu",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Column {
                    selectedBeatmap?.let { beatmap ->
                        MiniPlayer(
                            beatmap = beatmap,
                            isPlaying = isPlaying,
                            isFavorite = beatmap.id in favorites,
                            currentPosition = currentPosition,
                            duration = duration,
                            onSeek = { position ->
                                playerManager.player.seekTo(position)
                                currentPosition = position
                            },
                            onPlayPause = {
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
                            },
                            onFavorite = {
                                scope.launch {
                                    favoritesStore.toggleFavorite(beatmap.id)
                                }
                            },
                            onOpen = {
                                selectedTab = KozuTab.LIBRARY
                            }
                        )
                    }

                    FloatingBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    KozuTab.HOME -> HomeScreen(
                        favorites = favorites,
                        onBeatmapSelected = { beatmap ->
                            selectedBeatmap = beatmap
                            isPlaying = false
                        },
                        onFavorite = { beatmap ->
                            scope.launch {
                                favoritesStore.toggleFavorite(beatmap.id)
                            }
                        }
                    )

                    KozuTab.SEARCH -> SearchScreen()

                    KozuTab.LIBRARY -> LibraryScreen(
                        favorites = demoBeatmaps.filter {
                            it.id in favorites
                        },
                        selectedBeatmap = selectedBeatmap,
                        onBeatmapSelected = { beatmap ->
                            selectedBeatmap = beatmap
                            isPlaying = false
                        },
                        onFavorite = { beatmap ->
                            scope.launch {
                                favoritesStore.toggleFavorite(beatmap.id)
                            }
                        }
                    )

                    KozuTab.SETTINGS -> PlaceholderScreen(
                        title = "Settings",
                        description = "Customize your listening experience."
                    )
                }
            }
        }
    }
}


@Composable
private fun BeatmapCover(
    beatmap: Beatmap,
    modifier: Modifier = Modifier
) {
    val coverUrl = beatmap.coverUrl

    if (coverUrl != null) {
        AsyncImage(
            model = coverUrl,
            contentDescription = "${beatmap.title} cover",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "♫",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    favorites: Set<Long>,
    onBeatmapSelected: (Beatmap) -> Unit,
    onFavorite: (Beatmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = "YOUR MUSIC SPACE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Find your next favorite.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Explore music through osu! beatmaps.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Quick picks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "DEMO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            demoBeatmaps.take(3).forEachIndexed { index, beatmap ->
                QuickPickCard(
                    beatmap = beatmap,
                    index = index,
                    onClick = { onBeatmapSelected(beatmap) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Explore music",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        demoBeatmaps.forEach { beatmap ->
            BeatmapRow(
                beatmap = beatmap,
                isFavorite = beatmap.id in favorites,
                onClick = { onBeatmapSelected(beatmap) },
                onFavorite = { onFavorite(beatmap) }
            )

            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QuickPickCard(
    beatmap: Beatmap,
    index: Int,
    onClick: () -> Unit
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    Card(
        modifier = Modifier
            .width(154.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors[index % colors.size]
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "♫",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = beatmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = beatmap.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun BeatmapRow(
    beatmap: Beatmap,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeatmapCover(
                beatmap = beatmap,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beatmap.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = beatmap.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Toggle favorite",
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}


@Composable
private fun MiniPlayer(
    beatmap: Beatmap,
    isPlaying: Boolean,
    isFavorite: Boolean,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 4.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapCover(
                    beatmap = beatmap,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = beatmap.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text = beatmap.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }

            Slider(
                value = currentPosition
                    .coerceIn(0L, duration.coerceAtLeast(0L))
                    .toFloat(),
                onValueChange = { value ->
                    onSeek(value.toLong())
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0L
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    favorites: List<Beatmap>,
    selectedBeatmap: Beatmap?,
    onBeatmapSelected: (Beatmap) -> Unit,
    onFavorite: (Beatmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${favorites.size} favorite songs",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        if (favorites.isEmpty()) {
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Explore music on Home and tap the heart to save your favorites.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            favorites.forEach { beatmap ->
                BeatmapRow(
                    beatmap = beatmap,
                    isFavorite = true,
                    onClick = { onBeatmapSelected(beatmap) },
                    onFavorite = { onFavorite(beatmap) }
                )

                Spacer(Modifier.height(10.dp))
            }
        }

        selectedBeatmap?.let {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Selected: ${it.title}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FloatingBottomBar(
    selectedTab: KozuTab,
    onTabSelected: (KozuTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 12.dp
            )
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KozuTab.entries.forEach { tab ->
                val selected = selectedTab == tab

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onTabSelected(tab) },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}