package vn.kozu.game.mania.previewstage.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.domain.model.Beatmap
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.model.StartupReport
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.R
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.KozuSearchField
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.domain.model.LibraryDensity

private data class DifficultyEntry(val song: SongEntry, val difficulty: DifficultyRecord)

private sealed interface ChartListItem {
    val key: String

    data class GroupHeader(
        override val key: String,
        val title: String,
        val count: Int,
    ) : ChartListItem

    data class Song(val song: SongEntry, val groupKey: String? = null) : ChartListItem {
        override val key = "song:${song.songId}:${groupKey.orEmpty()}"
    }

    data class Difficulty(val entry: DifficultyEntry, val groupKey: String? = null) : ChartListItem {
        override val key = "difficulty:${entry.song.songId}:${entry.difficulty.osuFile}:${groupKey.orEmpty()}"
    }
}

@Composable
fun PreviewStageScreen(
    module: PreviewStageModule,
    onAddMaps: () -> Unit,
    customOrderIds: List<Long> = emptyList(),
    onReorderCharts: (List<Long>) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onDifficultyClick: ((SongEntry, DifficultyRecord) -> Unit)? = null,
    loadBestScores: (suspend (SongEntry) -> Map<String, ScoreRecord>)? = null,
    loadLatestPlayedAt: (suspend (SongEntry) -> Map<String, Long>)? = null,
    loadRankedAt: (suspend (SongEntry) -> Long)? = null,
    scoresRefreshKey: Int = 0,
    osuUsername: String = "",
    scorePlayerKey: String = "",
    openSongId: Long? = null,
    onOpenSongHandled: (Long) -> Unit = {},
) {
    val listState = rememberLazyListState()
    var report by remember { mutableStateOf<StartupReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SongEntry?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deletingSongId by remember { mutableStateOf<Long?>(null) }
    var previewError by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var localCustomOrder by remember(customOrderIds) { mutableStateOf(customOrderIds) }
    val compactRows = LocalAppSettings.current.libraryDensity == LibraryDensity.COMPACT
    val context = LocalContext.current
    val resources = context.resources
    val songs by module.catalog.entries.collectAsState()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var requestedScrollSongId by remember { mutableStateOf<Long?>(null) }
    var sort by remember { mutableStateOf(MapSort.DEFAULT) }
    var group by remember { mutableStateOf(MapGroup.NONE) }
    var showSortOptions by remember(compactRows) { mutableStateOf(!compactRows) }
    var showGroupOptions by remember(compactRows) { mutableStateOf(!compactRows) }
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bpms by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var lengths by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var rankedDates by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var metadataErrors by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var bestScoresBySong by remember { mutableStateOf<Map<Long, Map<String, ScoreRecord>>>(emptyMap()) }
    var latestPlayedAtBySong by remember { mutableStateOf<Map<Long, Map<String, Long>>>(emptyMap()) }
    var scoreErrors by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var loadingScoresFor by remember { mutableStateOf<Long?>(null) }
    var collectionTarget by remember { mutableStateOf<SongEntry?>(null) }
    var collectionOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var collectionDraft by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newCollection by remember { mutableStateOf("") }
    var collectionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val playPreview: (SongEntry, DifficultyRecord) -> Unit = { song, difficulty ->
        previewError = null
        module.playPreview(song, difficulty) { message ->
            previewError = song.songId to message
        }
    }
    fun openCollectionEditor(song: SongEntry) {
        collectionTarget = song
        collectionDraft = song.collections.toSet()
        newCollection = ""
        collectionError = null
        scope.launch {
            try {
                collectionOptions = module.collectionNames()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                collectionError = e.message ?: context.getString(R.string.game_load_collections_failed)
            }
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

    val listItems = remember(
        resources, visibleSongs, sort, group, expandedGroups, bpms, lengths, rankedDates,
        bestScoresBySong, latestPlayedAtBySong, osuUsername, localCustomOrder,
    ) {
        buildChartListItems(
            songs = visibleSongs,
            sort = sort,
            customOrderIds = localCustomOrder,
            group = group,
            expandedGroups = expandedGroups,
            bpms = bpms,
            lengths = lengths,
            rankedDates = rankedDates,
            bestScores = bestScoresBySong,
            latestPlayedAt = latestPlayedAtBySong,
            osuUsername = osuUsername,
            resources = resources,
        )
    }

    LaunchedEffect(customOrderIds) {
        localCustomOrder = customOrderIds
        if (customOrderIds.isNotEmpty() && sort == MapSort.DEFAULT) {
            sort = MapSort.CUSTOM
        }
    }

    // Drop metadata for charts removed from disk so a long-lived Game screen does not retain stale entries.
    LaunchedEffect(songs, report?.loadedSongs) {
        val validSongIds = songs.mapTo(mutableSetOf()) { it.songId }
        val validDifficultyKeys = songs.asSequence()
            .flatMap { song -> song.difficulties.asSequence().map { bpmKey(it) } }
            .toHashSet()
        bpms = bpms.filterKeys { it in validDifficultyKeys }
        lengths = lengths.filterKeys { it in validDifficultyKeys }
        rankedDates = rankedDates.filterKeys { it in validSongIds }
        metadataErrors = metadataErrors.filterKeys { it in validSongIds }
        bestScoresBySong = bestScoresBySong.filterKeys { it in validSongIds }
        latestPlayedAtBySong = latestPlayedAtBySong.filterKeys { it in validSongIds }
        scoreErrors = scoreErrors.filterKeys { it in validSongIds }
        if (expandedId != null && expandedId !in validSongIds) {
            expandedId = null
            previewError = null
            module.stopPreview()
        }
        requestedScrollSongId?.let { if (it !in validSongIds) requestedScrollSongId = null }
        loadingScoresFor?.let { if (it !in validSongIds) loadingScoresFor = null }
        collectionTarget?.let { if (it.songId !in validSongIds) collectionTarget = null }
        if (report != null) {
            val retainedOrder = localCustomOrder.filter { it in validSongIds }
            if (retainedOrder != localCustomOrder) {
                localCustomOrder = retainedOrder
                onReorderCharts(retainedOrder)
            }
        }
    }

    val canReorderSongs = group == MapGroup.NONE && searchQuery.isBlank() &&
        listItems.isNotEmpty() && listItems.all { it is ChartListItem.Song }
    val chartItemsStartIndex = 1 +
        (if (report != null && error == null) 1 else 0) +
        (if (report != null && error == null && songs.isNotEmpty()) 1 else 0) +
        (if (deleteError != null) 1 else 0) +
        (if (metadataErrors.values.distinct().isNotEmpty()) 1 else 0) +
        (if (scoreErrors.values.distinct().isNotEmpty()) 1 else 0) +
        (if (report?.failed?.isNotEmpty() == true) 1 else 0)
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            if (!canReorderSongs) return@rememberReorderableLazyListState
            val fromIndex = from.index - chartItemsStartIndex
            val toIndex = to.index - chartItemsStartIndex
            val currentIds = listItems.filterIsInstance<ChartListItem.Song>().map { it.song.songId }
            if (fromIndex in currentIds.indices && toIndex in currentIds.indices && fromIndex != toIndex) {
                val reordered = currentIds.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                localCustomOrder = reordered
                if (sort != MapSort.CUSTOM) {
                    sort = MapSort.CUSTOM
                    expandedId = null
                    module.stopPreview()
                }
                onReorderCharts(reordered)
            }
        },
    )

    LaunchedEffect(openSongId, songs) {
        val songId = openSongId ?: return@LaunchedEffect
        val song = songs.firstOrNull { it.songId == songId } ?: return@LaunchedEffect
        searchQuery = ""
        sort = MapSort.DATE_ADDED
        group = MapGroup.DATE_ADDED
        showSortOptions = true
        showGroupOptions = true
        expandedGroups = setOf("added:${dateBucket(song.importedAt)}")
        expandedId = song.songId
        requestedScrollSongId = song.songId
        previewError = null
        song.defaultPreview?.let { playPreview(song, it) }
        onOpenSongHandled(song.songId)
    }

    LaunchedEffect(
        requestedScrollSongId,
        listItems,
        report,
        error,
        deleteError,
        metadataErrors,
        scoreErrors,
        sort,
        group,
        searchQuery,
        songs,
    ) {
        val songId = requestedScrollSongId ?: return@LaunchedEffect
        if (
            sort != MapSort.DATE_ADDED ||
            group != MapGroup.DATE_ADDED ||
            searchQuery.isNotBlank() ||
            report == null ||
            error != null ||
            songs.isEmpty()
        ) return@LaunchedEffect

        val rowIndex = listItems.indexOfFirst { item ->
            item is ChartListItem.Song && item.song.songId == songId
        }
        if (rowIndex < 0) return@LaunchedEffect

        val chartListStartIndex = 1 +
            (if (report != null && error == null) 1 else 0) +
            (if (report != null && error == null && songs.isNotEmpty()) 1 else 0) +
            (if (deleteError != null) 1 else 0) +
            (if (metadataErrors.values.distinct().isNotEmpty()) 1 else 0) +
            (if (scoreErrors.values.distinct().isNotEmpty()) 1 else 0) +
            (if (report?.failed?.isNotEmpty() == true) 1 else 0)
        listState.animateScrollToItem(chartListStartIndex + rowIndex)
        requestedScrollSongId = null
    }

    LaunchedEffect(module) {
        try {
            report = module.start()
            collectionOptions = module.collectionNames()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: context.getString(R.string.game_load_charts_failed)
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
            scoreErrors = scoreErrors + (songId to (e.message ?: context.getString(R.string.game_load_scores_failed)))
        } finally {
            if (loadingScoresFor == songId) loadingScoresFor = null
        }
    }
    LaunchedEffect(songs, sort == MapSort.BPM || group == MapGroup.BPM) {
        if (sort != MapSort.BPM && group != MapGroup.BPM) return@LaunchedEffect
        val missing = songs.flatMap { s -> s.difficulties.map { s to it } }.filter { (_, d) -> bpmKey(d) !in bpms }
        if (missing.isEmpty()) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            val loaded = mutableMapOf<String, Double>()
            val failures = mutableMapOf<Long, String>()
            missing.forEach { (song, difficulty) ->
                try {
                    readBpm(song.folder, difficulty)?.let { loaded[bpmKey(difficulty)] = it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures[song.songId] = "${song.displayTitle}: ${e.message ?: context.getString(R.string.game_read_bpm_failed)}"
                }
            }
            loaded to failures
        }
        bpms = bpms + result.first
        metadataErrors = metadataErrors + result.second
    }
    LaunchedEffect(songs, sort == MapSort.LENGTH || group == MapGroup.LENGTH) {
        if (sort != MapSort.LENGTH && group != MapGroup.LENGTH) return@LaunchedEffect
        val missing = songs.flatMap { s -> s.difficulties.map { s to it } }.filter { (_, d) -> bpmKey(d) !in lengths }
        if (missing.isEmpty()) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            val loaded = mutableMapOf<String, Long>()
            val failures = mutableMapOf<Long, String>()
            missing.forEach { (song, difficulty) ->
                try {
                    loaded[bpmKey(difficulty)] = readLengthMs(song.folder, difficulty)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures[song.songId] = "${song.displayTitle}: ${e.message ?: context.getString(R.string.game_read_length_failed)}"
                }
            }
            loaded to failures
        }
        lengths = lengths + result.first
        metadataErrors = metadataErrors + result.second
    }
    LaunchedEffect(songs, sort == MapSort.DATE_RANKED, loadRankedAt) {
        if (sort != MapSort.DATE_RANKED) return@LaunchedEffect
        val loader = loadRankedAt ?: return@LaunchedEffect
        for (song in songs.filter { it.songId !in rankedDates }) {
            try {
                rankedDates = rankedDates + (song.songId to loader(song))
                metadataErrors = metadataErrors - song.songId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                metadataErrors = metadataErrors + (song.songId to (e.message ?: context.getString(R.string.game_load_ranked_date_failed)))
            }
        }
    }
    LaunchedEffect(
        songs, sort.perDifficulty || group.perDifficulty,
        sort == MapSort.LAST_PLAYED || group == MapGroup.LAST_PLAYED,
        scoresRefreshKey, scorePlayerKey, loadBestScores, loadLatestPlayedAt,
    ) {
        val needsBest = sort.perDifficulty || group.perDifficulty
        val needsLatest = sort == MapSort.LAST_PLAYED || group == MapGroup.LAST_PLAYED
        for (s in songs) {
            try {
                if (needsBest) {
                    val loader = loadBestScores ?: continue
                    bestScoresBySong = bestScoresBySong + (s.songId to loader(s))
                }
                if (needsLatest) {
                    val loader = loadLatestPlayedAt ?: continue
                    latestPlayedAtBySong = latestPlayedAtBySong + (s.songId to loader(s))
                }
                scoreErrors = scoreErrors - s.songId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                scoreErrors = scoreErrors + (s.songId to (e.message ?: context.getString(R.string.game_load_scores_failed)))
            }
        }
    }
    DisposableEffect(module) { onDispose { module.stopPreview() } }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = if (compactRows) 8.dp else 12.dp,
            bottom = 24.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(if (compactRows) 6.dp else 12.dp)
    ) {
        item(key = "header") {
            SectionHeader(
                title = stringResource(R.string.your_charts),
                subtitle = report?.let {
                    stringResource(R.string.game_maps_and_difficulties, songs.size, it.loadedDifficulties)
                } ?: stringResource(R.string.loading_your_mania_charts)
            )
        }

        if (report != null && error == null) {
            item(key = "search-and-random") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KozuSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { /* Local filtering is immediate. */ },
                        placeholder = stringResource(R.string.search_maps),
                        modifier = Modifier.weight(1f),
                    )
                    HeaderAction(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.random),
                        onClick = {
                            val candidates = visibleSongs.flatMap { song ->
                                song.difficulties.map { difficulty -> song to difficulty }
                            }
                            candidates.randomOrNull()?.let { (song, difficulty) ->
                                playPreview(song, difficulty)
                                onDifficultyClick?.invoke(song, difficulty)
                            }
                        },
                        startPadding = 0.dp,
                        enabled = visibleSongs.any { it.difficulties.isNotEmpty() } && onDifficultyClick != null,
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = MaterialTheme.colorScheme.onSurface,
                    )
                    HeaderAction(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_maps),
                        onClick = onAddMaps,
                        startPadding = 0.dp,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (report != null && error == null && songs.isNotEmpty()) {
            item(key = "sort-controls") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            .bounceClick(pressedScale = 0.99f) { showSortOptions = !showSortOptions },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.sort), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(if (showSortOptions) R.string.hide else R.string.show), style = MaterialTheme.typography.labelMedium)
                    }
                    AnimatedVisibility(visible = showSortOptions) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapSort.entries.forEach { option ->
                                FilterChip(
                                    selected = sort == option,
                                    onClick = {
                                        if (sort != option) {
                                            expandedId = null
                                            expandedGroups = emptySet()
                                            module.stopPreview()
                                            sort = option
                                        }
                                    },
                                    label = { Text(stringResource(option.labelRes)) }
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            .bounceClick(pressedScale = 0.99f) { showGroupOptions = !showGroupOptions },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.group), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(if (showGroupOptions) R.string.hide else R.string.show), style = MaterialTheme.typography.labelMedium)
                    }
                    AnimatedVisibility(visible = showGroupOptions) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapGroup.entries.forEach { option ->
                                FilterChip(
                                    selected = group == option,
                                    onClick = {
                                        group = option
                                        expandedId = null
                                        expandedGroups = emptySet()
                                    },
                                    label = { Text(stringResource(option.labelRes)) }
                                )
                            }
                        }
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
        metadataErrors.values.distinct().takeIf { it.isNotEmpty() }?.let { messages ->
            item(key = "metadata-errors") {
                Text(messages.joinToString("\n"),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        scoreErrors.values.distinct().takeIf { it.isNotEmpty() }?.let { messages ->
            item(key = "score-errors") {
                Text(messages.joinToString("\n") { context.getString(R.string.game_scores_unavailable, it) },
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
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
                        Text(stringResource(R.string.some_archives_could_not_be_imported), style = MaterialTheme.typography.titleSmall)
                        failures.forEach { failure ->
                            Text("${failure.fileName}: ${failure.reason}",
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
                    title = stringResource(R.string.charts_could_not_be_loaded),
                    message = error.orEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            report == null -> item(key = "loading") {
                Text(stringResource(R.string.loading_charts),
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            songs.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.no_mania_charts_yet),
                    message = stringResource(R.string.game_no_charts_message),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    action = {
                        Button(onClick = onAddMaps) { Text(stringResource(R.string.add_maps)) }
                    }
                )
            }
            visibleSongs.isEmpty() -> item(key = "no-search-results") {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.game_no_matching_maps),
                    message = stringResource(R.string.try_another_title_artist_mapper_or_difficulty_name),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> items(listItems, key = { it.key }) { item ->
                ReorderableItem(reorderableState, key = item.key) { isDragging ->
                    when (item) {
                        is ChartListItem.GroupHeader -> Surface(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .bounceClick(pressedScale = 0.99f) {
                                    expandedGroups = if (item.key in expandedGroups) {
                                        expandedGroups - item.key
                                    } else {
                                        expandedGroups + item.key
                                    }
                                },
                            shape = RoundedCornerShape(if (compactRows) 14.dp else 18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                Modifier.padding(horizontal = if (compactRows) 12.dp else 16.dp, vertical = if (compactRows) 8.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text("${item.count}  ${if (item.key in expandedGroups) "−" else "+"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        is ChartListItem.Song -> {
                            val song = item.song
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
                                        song.defaultPreview?.let { playPreview(song, it) }
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
                                onManageCollections = { openCollectionEditor(song) },
                                onDelete = { deleteTarget = song },
                                deleting = deletingSongId == song.songId,
                                compactRows = compactRows,
                                showDragHandle = canReorderSongs,
                                dragHandleModifier = if (canReorderSongs) Modifier.draggableHandle() else Modifier,
                                isDragging = isDragging,
                            )
                        }
                        is ChartListItem.Difficulty -> {
                            val song = item.entry.song
                            val difficulty = item.entry.difficulty
                            DifficultyCard(
                                song = song,
                                difficulty = difficulty,
                                bpm = bpms[bpmKey(difficulty)],
                                lengthMs = lengths[bpmKey(difficulty)],
                                score = bestScoresBySong[song.songId]?.get(difficulty.version),
                                onDelete = { deleteTarget = song },
                                onClick = {
                                    playPreview(song, difficulty)
                                    onDifficultyClick?.invoke(song, difficulty)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { if (deletingSongId == null) deleteTarget = null },
            title = { Text(stringResource(R.string.delete_map)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.remove_chart_files,
                            song.displayTitle.ifBlank { stringResource(R.string.beatmap_number, song.songId) },
                        ),
                    )
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
                                deleteError = e.message ?: context.getString(R.string.game_could_not_delete_map)
                            } finally {
                                deletingSongId = null
                            }
                        }
                    },
                ) {
                    Text(if (deletingSongId == song.songId) stringResource(R.string.game_deleting) else stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingSongId == null,
                    onClick = { deleteTarget = null },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    collectionTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { collectionTarget = null },
            title = { Text(stringResource(R.string.collections)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (collectionOptions.isEmpty()) {
                        Text(stringResource(R.string.create_collection_hint))
                    }
                    collectionOptions.forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = name in collectionDraft,
                                onCheckedChange = { checked ->
                                    collectionDraft = if (checked) collectionDraft + name else collectionDraft - name
                                },
                            )
                            Text(name)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCollection,
                            onValueChange = { newCollection = it.take(40) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.new_collection)) },
                        )
                        TextButton(
                            onClick = {
                                val name = newCollection.trim()
                                if (name.isNotEmpty()) {
                                    collectionOptions = (collectionOptions + name).distinctBy { it.lowercase(Locale.ROOT) }
                                    collectionDraft = collectionDraft + name
                                    newCollection = ""
                                }
                            },
                        ) { Text(stringResource(R.string.add)) }
                    }
                    collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = collectionDraft + newCollection.trim().takeIf { it.isNotEmpty() }.orEmpty()
                        scope.launch {
                            try {
                                module.setSongCollections(song.songId, selected.toList())
                                collectionOptions = module.collectionNames()
                                collectionTarget = null
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                collectionError = e.message ?: context.getString(R.string.game_save_collections_failed)
                            }
                        }
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { collectionTarget = null }) { Text(stringResource(R.string.cancel)) }
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
    onManageCollections: () -> Unit,
    onDelete: () -> Unit,
    deleting: Boolean,
    compactRows: Boolean,
    showDragHandle: Boolean,
    dragHandleModifier: Modifier,
    isDragging: Boolean,
) {
    val title = song.displayTitle.ifBlank { stringResource(R.string.beatmap_number, song.songId) }
    val artist = song.displayArtist.ifBlank { stringResource(R.string.game_unknown_artist) }
    val beatmap = remember(song.songId, title, artist) {
        Beatmap(song.songId, title, artist)
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(if (compactRows) 16.dp else 24.dp),
        color = if (isDragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(pressedScale = 0.98f, onClick = onToggle)
                    .padding(if (compactRows) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapArt(
                    beatmap,
                    Modifier.size(if (compactRows) 52.dp else 60.dp),
                    shape = RoundedCornerShape(if (compactRows) 14.dp else 20.dp),
                )
                Spacer(Modifier.width(if (compactRows) 10.dp else 14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(artist,
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
                            Text("${String.format(Locale.US, "%.2f★", minimum)} – ${String.format(Locale.US, "%.2f★", maximum)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                }
                if (showDragHandle) {
                    Icon(
                        Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(R.string.reorder_song),
                        modifier = dragHandleModifier.padding(horizontal = 4.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete, enabled = !deleting, modifier = Modifier.size(if (compactRows) 40.dp else 48.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete_map_text),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(
                    onClick = onPreview,
                    enabled = song.defaultPreview != null,
                    modifier = Modifier.size(if (compactRows) 40.dp else 48.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_preview),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Surface(
                    modifier = Modifier.size(if (compactRows) 36.dp else 44.dp),
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
                Text(stringResource(R.string.preview_unavailable, it),
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = if (compactRows) 8.dp else 14.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactRows) 4.dp else 6.dp)
                ) {
                    if (scoresLoading && bestScores == null) {
                        Text(stringResource(R.string.loading_personal_scores),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    scoreError?.let {
                        Text(stringResource(R.string.personal_scores_error, it),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onManageCollections) { Text(stringResource(R.string.collections)) }
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(difficulty.version,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(buildList {
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
                    Text("${it.rank}  ·  %.2f%%  ·  %,d pts  ·  %s/%s combo".format(
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
                Text("${difficulty.circleSize.toInt()}K",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${difficulty.beatmapId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
}

private fun fmt(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString()
    else "%.1f".format(java.util.Locale.ROOT, value)

private enum class MapSort(@StringRes val labelRes: Int, val perDifficulty: Boolean = false) {
    DEFAULT(R.string.game_sort_default),
    CUSTOM(R.string.custom),
    TITLE(R.string.title),
    ARTIST(R.string.artist),
    CREATOR(R.string.mapper),
    BPM(R.string.game_bpm, perDifficulty = true),
    LENGTH(R.string.game_length, perDifficulty = true),
    DIFFICULTY(R.string.difficulty, perDifficulty = true),
    DATE_ADDED(R.string.date_added),
    DATE_RANKED(R.string.game_date_ranked),
    RANK_ACHIEVED(R.string.rank_achieved, perDifficulty = true),
    LAST_PLAYED(R.string.game_last_played, perDifficulty = true),
}

private enum class MapGroup(@StringRes val labelRes: Int, val perDifficulty: Boolean = false) {
    NONE(R.string.game_group_none),
    DIFFICULTY(R.string.difficulty, perDifficulty = true),
    ARTIST(R.string.artist),
    TITLE(R.string.title),
    CREATOR(R.string.mapper),
    BPM(R.string.game_bpm, perDifficulty = true),
    LENGTH(R.string.game_length, perDifficulty = true),
    DATE_ADDED(R.string.date_added),
    RANK_ACHIEVED(R.string.rank_achieved, perDifficulty = true),
    LAST_PLAYED(R.string.game_last_played, perDifficulty = true),
    COLLECTION(R.string.collections),
    MY_MAPS(R.string.game_my_maps),
}

private fun bpmKey(difficulty: DifficultyRecord) = "${difficulty.songId}|${difficulty.osuFile}"


private fun readBpm(folder: java.io.File, difficulty: DifficultyRecord): Double? =
    run {
        var inTiming = false
        java.io.File(folder, difficulty.osuFile).useLines(Charsets.UTF_8) { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.startsWith("[")) {
                    inTiming = line == "[TimingPoints]"
                    continue
                }
                if (!inTiming || line.isEmpty()) continue
                val parts = line.split(",")
                val beatLength = parts.getOrNull(1)?.toDoubleOrNull() ?: continue
                val uninherited = parts.getOrNull(6)?.trim() != "0"
                if (uninherited && beatLength > 0) return@useLines 60000.0 / beatLength
            }
            null
        }
    }

private fun readLengthMs(folder: java.io.File, difficulty: DifficultyRecord): Long {
    var inHitObjects = false
    var lastObjectTime = 0L
    java.io.File(folder, difficulty.osuFile).useLines(Charsets.UTF_8) { lines ->
        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("[")) {
                inHitObjects = line == "[HitObjects]"
                continue
            }
            if (inHitObjects) {
                val parts = line.split(",")
                val start = parts.getOrNull(2)?.toLongOrNull() ?: continue
                val type = parts.getOrNull(3)?.toIntOrNull() ?: 0
                val end = if (type and 128 != 0) {
                    parts.getOrNull(5)?.substringBefore(':')?.toLongOrNull() ?: start
                } else {
                    start
                }
                lastObjectTime = maxOf(lastObjectTime, end)
            }
        }
    }
    return lastObjectTime
}

private fun applyGameCustomOrder(songs: List<SongEntry>, orderedIds: List<Long>): List<SongEntry> {
    if (songs.size < 2 || orderedIds.isEmpty()) return songs
    val byId = songs.associateBy { it.songId }
    val seen = orderedIds.toHashSet()
    return buildList(songs.size) {
        orderedIds.forEach { id -> byId[id]?.let(::add) }
        songs.filterNot { it.songId in seen }.forEach(::add)
    }
}

private fun buildChartListItems(
    songs: List<SongEntry>,
    sort: MapSort,
    customOrderIds: List<Long>,
    group: MapGroup,
    expandedGroups: Set<String>,
    bpms: Map<String, Double>,
    lengths: Map<String, Long>,
    rankedDates: Map<Long, Long>,
    bestScores: Map<Long, Map<String, ScoreRecord>>,
    latestPlayedAt: Map<Long, Map<String, Long>>,
    osuUsername: String,
    resources: Resources,
): List<ChartListItem> {
    val sortedSongs = when (sort) {
        MapSort.CUSTOM -> applyGameCustomOrder(songs, customOrderIds)
        MapSort.TITLE -> songs.sortedBy { it.displayTitle.lowercase(Locale.ROOT) }
        MapSort.ARTIST -> songs.sortedBy { it.displayArtist.lowercase(Locale.ROOT) }
        MapSort.CREATOR -> songs.sortedBy { it.difficulties.firstOrNull()?.creator.orEmpty().lowercase(Locale.ROOT) }
        MapSort.DATE_ADDED -> songs.sortedByDescending { it.importedAt }
        MapSort.DATE_RANKED -> songs.sortedByDescending { rankedDates[it.songId] ?: 0L }
        else -> songs
    }
    val showDifficulties = sort.perDifficulty || group.perDifficulty
    val diffEntries = if (showDifficulties) {
        songs.flatMap { song -> song.difficulties.map { DifficultyEntry(song, it) } }
    } else {
        emptyList()
    }
    val sortedDiffs = when (sort) {
        MapSort.TITLE -> diffEntries.sortedBy { it.song.displayTitle.lowercase(Locale.ROOT) }
        MapSort.ARTIST -> diffEntries.sortedBy { it.song.displayArtist.lowercase(Locale.ROOT) }
        MapSort.CREATOR -> diffEntries.sortedBy { it.difficulty.creator.lowercase(Locale.ROOT) }
        MapSort.BPM -> diffEntries.sortedBy { bpms[bpmKey(it.difficulty)] ?: 0.0 }
        MapSort.LENGTH -> diffEntries.sortedBy { lengths[bpmKey(it.difficulty)] ?: 0L }
        MapSort.DIFFICULTY -> diffEntries.sortedBy { it.difficulty.starRating }
        MapSort.DATE_ADDED -> diffEntries.sortedByDescending { it.song.importedAt }
        MapSort.DATE_RANKED -> diffEntries.sortedByDescending { rankedDates[it.song.songId] ?: 0L }
        MapSort.RANK_ACHIEVED -> diffEntries.sortedWith(
            compareBy<DifficultyEntry> {
                rankSortValue(bestScores[it.song.songId]?.get(it.difficulty.version)?.rank)
            }.thenByDescending {
                bestScores[it.song.songId]?.get(it.difficulty.version)?.score ?: 0L
            }
        )
        MapSort.LAST_PLAYED -> diffEntries.sortedByDescending {
            latestPlayedAt[it.song.songId]?.get(it.difficulty.version) ?: 0L
        }
        MapSort.DEFAULT, MapSort.CUSTOM -> diffEntries
    }
    val rows = if (showDifficulties) {
        sortedDiffs.map { ChartListItem.Difficulty(it) }
    } else {
        sortedSongs.map { ChartListItem.Song(it) }
    }
    if (group == MapGroup.NONE) return rows

    fun values(row: ChartListItem): List<Pair<String, String>> {
        val song = when (row) {
            is ChartListItem.Song -> row.song
            is ChartListItem.Difficulty -> row.entry.song
            is ChartListItem.GroupHeader -> return emptyList()
        }
        val diff = (row as? ChartListItem.Difficulty)?.entry?.difficulty
        val score = diff?.let { bestScores[song.songId]?.get(it.version) }
        val latest = diff?.let { latestPlayedAt[song.songId]?.get(it.version) }
        return when (group) {
            MapGroup.NONE -> emptyList()
            MapGroup.DIFFICULTY -> {
                val stars = diff?.starRating ?: 0.0
                val floor = stars.toInt().coerceAtLeast(0)
                if (stars <= 0.0) listOf("star:unknown" to resources.getString(R.string.game_unrated))
                else listOf("star:$floor" to "$floor★ – ${floor + 1}★")
            }
            MapGroup.ARTIST -> listOf("artist:${song.displayArtist}" to song.displayArtist.ifBlank { resources.getString(R.string.game_unknown_artist) })
            MapGroup.TITLE -> listOf("title:${song.displayTitle}" to song.displayTitle.ifBlank { resources.getString(R.string.game_untitled) })
            MapGroup.CREATOR -> {
                val creator = diff?.creator ?: song.difficulties.firstOrNull()?.creator.orEmpty()
                listOf("creator:$creator" to creator.ifBlank { resources.getString(R.string.game_unknown_mapper) })
            }
            MapGroup.BPM -> {
                val bpm = diff?.let { bpms[bpmKey(it)] }
                val low = bpm?.let { (it.toInt() / 20) * 20 }
                listOf(
                    "bpm:${low ?: -1}" to
                        (low?.let { resources.getString(R.string.game_bpm_range, it, it + 19) }
                            ?: resources.getString(R.string.game_unknown_bpm)),
                )
            }
            MapGroup.LENGTH -> {
                val duration = diff?.let { lengths[bpmKey(it)] }
                val bucket = duration?.let { (it / 120_000L).toInt() }
                listOf(
                    "length:${bucket ?: -1}" to
                        (bucket?.let { resources.getString(R.string.game_length_range, it * 2, it * 2 + 2) }
                            ?: resources.getString(R.string.game_unknown_length)),
                )
            }
            MapGroup.DATE_ADDED -> listOf("added:${dateBucket(song.importedAt)}" to dateBucketLabel(song.importedAt, resources))
            MapGroup.RANK_ACHIEVED -> {
                val rank = score?.rank ?: resources.getString(R.string.unplayed)
                listOf("rank:$rank" to rank)
            }
            MapGroup.LAST_PLAYED -> listOf(
                "played:${dateBucket(latest ?: 0L)}" to
                    if (latest == null) resources.getString(R.string.game_never_played) else dateBucketLabel(latest, resources),
            )
            MapGroup.COLLECTION -> song.collections.ifEmpty { listOf(resources.getString(R.string.game_uncategorized)) }
                .map { "collection:$it" to it }
            MapGroup.MY_MAPS -> {
                val creator = diff?.creator ?: song.difficulties.firstOrNull()?.creator.orEmpty()
                val mine = osuUsername.isNotBlank() && creator.equals(osuUsername, ignoreCase = true)
                listOf(
                    if (mine) {
                        "mine" to resources.getString(R.string.game_my_maps)
                    } else {
                        "others" to resources.getString(R.string.game_other_maps)
                    },
                )
            }
        }
    }

    val grouped = LinkedHashMap<String, Pair<String, MutableList<ChartListItem>>>()
    for (row in rows) {
        for ((key, label) in values(row)) {
            val bucket = grouped.getOrPut(key) { label to mutableListOf() }
            bucket.second += row
        }
    }
    val orderedGroups = grouped.entries.sortedWith(
        compareBy<Map.Entry<String, Pair<String, MutableList<ChartListItem>>>> {
            when {
                group == MapGroup.MY_MAPS && it.key == "mine" -> 0
                group == MapGroup.MY_MAPS -> 1
                else -> 0
            }
        }.thenBy { groupOrderValue(group, it.key, it.value.first) }
    )
    return buildList {
        orderedGroups.forEach { (key, value) ->
            add(ChartListItem.GroupHeader(key, "${value.first} (${value.second.size})", value.second.size))
            if (key in expandedGroups) {
                addAll(value.second.map { row ->
                    when (row) {
                        is ChartListItem.Song -> row.copy(groupKey = key)
                        is ChartListItem.Difficulty -> row.copy(groupKey = key)
                        is ChartListItem.GroupHeader -> row
                    }
                })
            }
        }
    }
}

private fun groupOrderValue(group: MapGroup, key: String, label: String): String = when (group) {
    MapGroup.DIFFICULTY, MapGroup.BPM, MapGroup.LENGTH ->
        key.substringAfter(':').toIntOrNull()?.let {
            if (it < 0) "99999999" else "%08d".format(it + 10_000)
        } ?: "99999999"
    MapGroup.RANK_ACHIEVED -> "%02d".format(rankSortValue(label))
    MapGroup.DATE_ADDED, MapGroup.LAST_PLAYED -> when (key.substringAfter(':')) {
        "today" -> "0"
        "yesterday" -> "1"
        "this-week" -> "2"
        "unknown" -> "9"
        else -> "3${key.substringAfter(':').let { value ->
            val parts = value.split('-')
            if (parts.size == 2) "%04d-%02d".format(9999 - (parts[0].toIntOrNull() ?: 0), 12 - (parts[1].toIntOrNull() ?: 0)) else value
        }}"
    }
    else -> label.lowercase(Locale.ROOT)
}

private fun rankSortValue(rank: String?): Int = when (rank?.uppercase(Locale.ROOT)) {
    "XH", "SSH" -> 0
    "X", "SS" -> 1
    "SH" -> 2
    "S" -> 3
    "A" -> 4
    "B" -> 5
    "C" -> 6
    "D" -> 7
    else -> 8
}

private fun dateBucket(timestamp: Long): String {
    if (timestamp <= 0L) return "unknown"
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val startToday = today.timeInMillis
    val startYesterday = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.timeInMillis
    val startWeek = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    val date = calendar.timeInMillis
    return when {
        date >= startToday -> "today"
        date >= startYesterday -> "yesterday"
        date >= startWeek -> "this-week"
        else -> SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date(date))
    }
}

private fun dateBucketLabel(timestamp: Long, resources: Resources): String = when (dateBucket(timestamp)) {
    "today" -> resources.getString(R.string.game_today)
    "yesterday" -> resources.getString(R.string.game_yesterday)
    "this-week" -> resources.getString(R.string.game_this_week)
    "unknown" -> resources.getString(R.string.game_unknown_date)
    else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun DifficultyCard(
    song: SongEntry,
    difficulty: DifficultyRecord,
    bpm: Double?,
    lengthMs: Long?,
    score: ScoreRecord?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val title = song.displayTitle.ifBlank { stringResource(R.string.beatmap_number, song.songId) }
    val artist = song.displayArtist.ifBlank { stringResource(R.string.game_unknown_artist) }
    val beatmap = remember(song.songId, title, artist) { Beatmap(song.songId, title, artist) }
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BeatmapArt(beatmap, Modifier.size(60.dp), shape = RoundedCornerShape(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(buildList {
                        if (difficulty.starRating > 0.0) {
                            add(String.format(Locale.US, "%.2f★", difficulty.starRating))
                        }
                        add(artist)
                        add(stringResource(R.string.mapped_by, difficulty.creator))
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(buildList {
                        add("[${difficulty.version}]")
                        add("${difficulty.circleSize.toInt()}K")
                        add(bpm?.let { stringResource(R.string.game_bpm_value, it.toInt()) } ?: stringResource(R.string.game_unknown_bpm))
                        lengthMs?.let { add(formatDuration(it)) }
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                score?.let {
                    Text("${it.rank}  ·  %.2f%%  ·  %,d pts".format(Locale.ROOT, it.accuracy, it.score),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete_map_text),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
