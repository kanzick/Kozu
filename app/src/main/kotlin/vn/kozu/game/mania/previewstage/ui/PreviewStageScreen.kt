package vn.kozu.game.mania.previewstage.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.bounceClick
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.model.StartupReport
import java.util.Locale

@Composable
fun PreviewStageScreen(
    module: PreviewStageModule,
    onAddMaps: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onDifficultyClick: ((SongEntry, DifficultyRecord) -> Unit)? = null,
    loadBestScores: (suspend (SongEntry) -> Map<String, ScoreRecord>)? = null,
    scoresRefreshKey: Int = 0,
) {
    var report by remember { mutableStateOf<StartupReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SongEntry?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deletingSongId by remember { mutableStateOf<Long?>(null) }
    var previewError by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val songs by module.catalog.entries.collectAsState()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var bestScoresBySong by remember { mutableStateOf<Map<Long, Map<String, ScoreRecord>>>(emptyMap()) }
    var scoreErrors by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var loadingScoresFor by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val playPreview: (SongEntry, DifficultyRecord) -> Unit = { song, difficulty ->
        previewError = null
        module.playPreview(song, difficulty) { message ->
            previewError = song.songId to message
        }
    }
    val visibleSongs = remember(songs, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) songs else songs.filter { song ->
            song.displayTitle.contains(query, ignoreCase = true) ||
                song.difficulties.any { difficulty ->
                    difficulty.title.contains(query, ignoreCase = true) ||
                        difficulty.titleUnicode.contains(query, ignoreCase = true) ||
                        difficulty.artist.contains(query, ignoreCase = true) ||
                        difficulty.artistUnicode.contains(query, ignoreCase = true) ||
                        difficulty.creator.contains(query, ignoreCase = true) ||
                        difficulty.version.contains(query, ignoreCase = true)
                }
        }
    }

    LaunchedEffect(module) {
        try {
            report = module.start()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load Mania charts"
        }
    }
    LaunchedEffect(expandedId, scoresRefreshKey, songs, loadBestScores) {
        val songId = expandedId ?: return@LaunchedEffect
        val loader = loadBestScores ?: return@LaunchedEffect
        val song = songs.firstOrNull { it.songId == songId } ?: return@LaunchedEffect
        loadingScoresFor = songId
        try {
            bestScoresBySong = bestScoresBySong + (songId to loader(song))
            scoreErrors = scoreErrors - songId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            scoreErrors = scoreErrors + (songId to (e.message ?: "Could not load scores"))
        } finally {
            if (loadingScoresFor == songId) loadingScoresFor = null
        }
    }
    DisposableEffect(module) { onDispose { module.stopPreview() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hero") {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(38.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "MANIA",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Ready to play", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your downloaded charts, all in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                    }
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        item(key = "header") {
            SectionHeader(
                title = "Your charts",
                subtitle = report?.let {
                    "${songs.size} maps · ${it.loadedDifficulties} difficulties"
                } ?: "Loading your Mania charts"
            )
        }

        if (report != null && error == null) {
            item(key = "search-and-random") {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text("Search maps") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                    )
                    Button(
                        enabled = visibleSongs.any { it.difficulties.isNotEmpty() } && onDifficultyClick != null,
                        onClick = {
                            val candidates = visibleSongs.flatMap { song ->
                                song.difficulties.map { difficulty -> song to difficulty }
                            }
                            candidates.randomOrNull()?.let { (song, difficulty) ->
                                playPreview(song, difficulty)
                                onDifficultyClick?.invoke(song, difficulty)
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Random")
                    }
                }
            }
        }

        deleteError?.let { message ->
            item(key = "delete-error") {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        report?.failed?.takeIf { it.isNotEmpty() }?.let { failures ->
            item(key = "import-failures") {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Some archives could not be imported", style = MaterialTheme.typography.titleSmall)
                        failures.forEach { failure ->
                            Text(
                                "${failure.fileName}: ${failure.reason}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        when {
            error != null -> item(key = "error") {
                EmptyState(
                    icon = Icons.Rounded.SportsEsports,
                    title = "Charts could not be loaded",
                    message = error.orEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            report == null -> item(key = "loading") {
                Text(
                    "Loading charts…",
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            songs.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Download,
                    title = "No Mania charts yet",
                    message = "Add a map to build your personal play list.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    action = {
                        Button(onClick = onAddMaps) { Text("Add maps") }
                    }
                )
            }
            visibleSongs.isEmpty() -> item(key = "no-search-results") {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No matching maps",
                    message = "Try another title, artist, mapper, or difficulty name.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> items(visibleSongs, key = { it.songId }) { song ->
                ChartSetCard(
                    song = song,
                    expanded = expandedId == song.songId,
                    onToggle = {
                        if (expandedId == song.songId) {
                            expandedId = null
                            previewError = null
                            module.stopPreview()
                        } else {
                            expandedId = song.songId
                            previewError = null
                            song.defaultPreview?.let { difficulty ->
                                playPreview(song, difficulty)
                            }
                        }
                    },
                    onPreview = { song.defaultPreview?.let { playPreview(song, it) } },
                    previewError = previewError?.takeIf { it.first == song.songId }?.second,
                    bestScores = bestScoresBySong[song.songId],
                    scoreError = scoreErrors[song.songId],
                    scoresLoading = loadingScoresFor == song.songId,
                    onDifficultyClick = { difficulty ->
                        playPreview(song, difficulty)
                        onDifficultyClick?.invoke(song, difficulty)
                    },
                    onDelete = { deleteTarget = song },
                    deleting = deletingSongId == song.songId,
                )
            }
        }
    }

    deleteTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { if (deletingSongId == null) deleteTarget = null },
            title = { Text("Delete map?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remove \"${song.displayTitle.ifBlank { "Beatmap ${song.songId}" }}\" and its chart files from this device?")
                    deleteError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deletingSongId == null,
                    onClick = {
                        deleteError = null
                        deletingSongId = song.songId
                        scope.launch {
                            try {
                                module.deleteSong(song.songId)
                                if (expandedId == song.songId) expandedId = null
                                deleteTarget = null
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                deleteError = e.message ?: "Could not delete this map"
                            } finally {
                                deletingSongId = null
                            }
                        }
                    },
                ) {
                    Text(if (deletingSongId == song.songId) "Deleting…" else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingSongId == null,
                    onClick = { deleteTarget = null },
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ChartSetCard(
    song: SongEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit,
    previewError: String?,
    bestScores: Map<String, ScoreRecord>?,
    scoreError: String?,
    scoresLoading: Boolean,
    onDifficultyClick: (DifficultyRecord) -> Unit,
    onDelete: () -> Unit,
    deleting: Boolean,
) {
    val title = song.displayTitle.ifBlank { "Beatmap ${song.songId}" }
    val artist = song.displayArtist.ifBlank { "Unknown artist" }
    val beatmap = remember(song.songId, title, artist) {
        Beatmap(song.songId, title, artist)
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(pressedScale = 0.98f, onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapArt(beatmap, Modifier.size(60.dp), shape = RoundedCornerShape(20.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    song.difficulties
                        .map { it.starRating }
                        .filter { it > 0.0 }
                        .takeIf { it.isNotEmpty() }
                        ?.let { ratings ->
                            val minimum = ratings.minOrNull() ?: return@let
                            val maximum = ratings.maxOrNull() ?: return@let
                            Text(
                                "${String.format(Locale.US, "%.2f★", minimum)} – ${String.format(Locale.US, "%.2f★", maximum)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                }
                IconButton(onClick = onDelete, enabled = !deleting) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete map",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(
                    onClick = onPreview,
                    enabled = song.defaultPreview != null,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play preview",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            previewError?.let {
                Text(
                    "Preview unavailable: $it",
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (scoresLoading && bestScores == null) {
                        Text(
                            "Loading personal scores…",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    scoreError?.let {
                        Text(
                            "Could not load personal scores: $it",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    song.difficulties.forEach { difficulty ->
                        DifficultyRow(
                            difficulty = difficulty,
                            score = bestScores?.get(difficulty.version),
                            onClick = { onDifficultyClick(difficulty) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyRow(
    difficulty: DifficultyRecord,
    score: ScoreRecord?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    difficulty.version,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildList {
                        if (difficulty.starRating > 0.0) {
                            add(String.format(Locale.US, "%.2f★", difficulty.starRating))
                        }
                        add("CS ${fmt(difficulty.circleSize)}")
                        add("OD ${fmt(difficulty.overallDifficulty)}")
                        add("HP ${fmt(difficulty.hpDrainRate)}")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                score?.let {
                    Text(
                        "${it.rank}  ·  %.2f%%  ·  %,d pts  ·  %s/%s combo".format(
                            java.util.Locale.ROOT,
                            it.accuracy,
                            it.score,
                            it.trueCombo?.toString() ?: "—",
                            it.maxCombo?.toString() ?: "—",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${difficulty.circleSize.toInt()}K",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${difficulty.beatmapId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun fmt(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString()
    else "%.1f".format(java.util.Locale.ROOT, value)
