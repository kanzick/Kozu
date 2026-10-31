package vn.kozu.game.ui.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficulty
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.SettingsAction
import java.util.Locale

@Composable
fun AddMapScreen(
    viewModel: GameViewModel,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val localMapIds by viewModel.localMapIds.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var maps by remember { mutableStateOf(emptyList<Beatmap>()) }
    var nextPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var moduleError by remember { mutableStateOf<String?>(null) }
    var expandedMapId by remember { mutableStateOf<Long?>(null) }
    var difficulties by remember { mutableStateOf<List<BeatmapDifficulty>?>(null) }
    var difficultiesLoading by remember { mutableStateOf(false) }
    var difficultiesError by remember { mutableStateOf<String?>(null) }
    val previewError by viewModel.previewError.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.stopSearchPreview() }
    }

    LaunchedEffect(expandedMapId) {
        val mapId = expandedMapId ?: return@LaunchedEffect
        difficulties = null
        difficultiesError = null
        difficultiesLoading = true
        try {
            difficulties = viewModel.maniaDifficulties(mapId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            difficultiesError = e.message ?: "Could not load map difficulties"
        } finally {
            difficultiesLoading = false
        }
    }

    LaunchedEffect(viewModel) {
        try {
            viewModel.ensureCatalogLoaded()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            moduleError = e.message ?: "Could not load locally downloaded charts"
        }
    }

    LaunchedEffect(query) {
        expandedMapId = null
        viewModel.stopSearchPreview()
        delay(350)
        listState.scrollToItem(0)
        maps = emptyList()
        hasMore = false
        loading = true
        loadingMore = false
        error = null
        try {
            val result = viewModel.searchMania(query, 0)
            maps = result.items
            hasMore = result.nextCursor != null
            nextPage = 1
            error = result.error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not search the beatmap catalog"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(listState, query) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (
                    lastVisible >= maps.size - 2 &&
                    maps.isNotEmpty() &&
                    hasMore &&
                    !loading &&
                    !loadingMore
                ) {
                    loadingMore = true
                    try {
                        val result = viewModel.searchMania(query, nextPage)
                        val existing = maps.mapTo(HashSet()) { it.id }
                        maps = maps + result.items.filter { existing.add(it.id) }
                        hasMore = result.nextCursor != null
                        nextPage++
                        if (result.error != null && result.items.isEmpty()) error = result.error
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message ?: "Could not load more beatmaps"
                    } finally {
                        loadingMore = false
                    }
                }
            }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to charts")
            }
            Column(Modifier.weight(1f)) {
                Text("Add Mania maps", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Search the osu! catalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingsAction(onOpenSettings)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search songs, artists, or mappers") }
        )
        Text(
            "Downloaded .osz archives are saved in addmap/downloads.",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 20.dp, bottom = bottomPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            moduleError?.let { message ->
                item(key = "module-error") {
                    Text(
                        "Local charts could not be refreshed: $message",
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item(key = "results-heading") {
                SectionHeader(
                    title = "Mania beatmaps",
                    subtitle = "20 results per page · scroll to load more"
                )
            }
            if (loading && maps.isEmpty()) {
                item(key = "loading") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Searching beatmaps…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (maps.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = if (error == null) "No Mania maps found" else "Search is unavailable",
                        message = error ?: "Try another title, artist, or mapper.",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(maps, key = { it.id }) { beatmap ->
                    AddMapCard(
                        beatmap = beatmap,
                        isAdded = beatmap.id in localMapIds,
                        download = downloads[beatmap.id],
                        isExpanded = expandedMapId == beatmap.id,
                        difficulties = if (expandedMapId == beatmap.id) difficulties else null,
                        difficultiesLoading = expandedMapId == beatmap.id && difficultiesLoading,
                        difficultiesError = if (expandedMapId == beatmap.id) difficultiesError else null,
                        previewError = if (expandedMapId == beatmap.id) previewError else null,
                        onClick = {
                            if (expandedMapId == beatmap.id) {
                                expandedMapId = null
                                viewModel.stopSearchPreview()
                            } else {
                                expandedMapId = beatmap.id
                                viewModel.playSearchPreview(beatmap.id)
                            }
                        },
                        onDownload = { viewModel.download(beatmap) }
                    )
                }
                if (loadingMore) {
                    item(key = "loading-more") {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                    }
                }
                error?.let { message ->
                    item(key = "page-error") {
                        Text(
                            message,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMapCard(
    beatmap: Beatmap,
    isAdded: Boolean,
    download: GameMapDownloadState?,
    isExpanded: Boolean,
    difficulties: List<BeatmapDifficulty>?,
    difficultiesLoading: Boolean,
    difficultiesError: String?,
    previewError: String?,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BeatmapArt(beatmap, Modifier.size(64.dp), shape = RoundedCornerShape(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            beatmap.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            beatmap.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (beatmap.mapper.isNotBlank()) {
                            Text(
                                "Mapped by ${beatmap.mapper}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        download?.error?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (download != null && download.error == null && !isAdded) {
                            val progress = download.progress
                            if (progress == null || progress < 0f) {
                                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                when {
                    isAdded -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            "Added",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    download != null && download.error == null -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    else -> Button(
                        onClick = onDownload,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (download?.error != null) "Retry" else "Add")
                    }
                }
            }
            if (isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Mania difficulties",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    when {
                        difficultiesLoading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        difficultiesError != null -> Text(
                            difficultiesError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        difficulties.isNullOrEmpty() -> Text(
                            "No Mania difficulties were found for this map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> difficulties.forEach { difficulty ->
                            DifficultyInfoRow(difficulty)
                        }
                    }
                    previewError?.let {
                        Text(
                            "Preview unavailable: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyInfoRow(difficulty: BeatmapDifficulty) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            difficulty.version,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val details = buildList {
            if (difficulty.difficultyRating > 0.0) {
                add(String.format(Locale.US, "%.2f★", difficulty.difficultyRating))
            }
            if (difficulty.keys > 0.0) add("${difficulty.keys.toInt()}K")
            if (difficulty.bpm > 0.0) add("${difficulty.bpm.toInt()} BPM")
        }
        Text(
            details.joinToString(" · ").ifBlank { "Mania" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
