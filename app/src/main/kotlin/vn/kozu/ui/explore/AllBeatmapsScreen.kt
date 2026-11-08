package vn.kozu.ui.explore

import vn.kozu.ui.i18n.kozuUiString

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficultyInfo
import vn.kozu.domain.model.BeatmapSetInfo
import vn.kozu.game.ui.game.GameMapDownloadState
import vn.kozu.game.ui.game.GameViewModel
import vn.kozu.ui.components.CenteredHeader
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.LocalDownloads
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.components.LocalOverlayBottomInset
import vn.kozu.util.formatTime
import java.util.Locale

private const val BEATMAPS_PER_PAGE = 30

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllBeatmapsScreen(
    onBack: () -> Unit,
    onRetry: () -> Unit,
    isAppRefreshing: Boolean,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory),
    formgamepage: Boolean = false,
    gameViewModel: GameViewModel? = null,
    onPlayGameMap: (Long) -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val gameDownloadsFlow = remember(gameViewModel) {
        gameViewModel?.downloads ?: MutableStateFlow(emptyMap<Long, GameMapDownloadState>())
    }
    val gameLocalMapIdsFlow = remember(gameViewModel) {
        gameViewModel?.localMapIds ?: MutableStateFlow(emptySet<Long>())
    }
    val gameDownloads by gameDownloadsFlow.collectAsStateWithLifecycle()
    val gameLocalMapIds by gameLocalMapIdsFlow.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var gameCatalogError by remember(gameViewModel) { mutableStateOf<String?>(null) }
    val refreshing = ui.isRefreshing || isAppRefreshing
    // The floating mini player is drawn by the app shell on top of this screen; leave room for it.
    val listBottom = if (WindowInsets.isImeVisible) 16.dp else 12.dp + LocalOverlayBottomInset.current
    val pageStart = currentPage * BEATMAPS_PER_PAGE
    val pageItems = ui.items.drop(pageStart).take(BEATMAPS_PER_PAGE)
    val loadedPages = ((ui.items.size + BEATMAPS_PER_PAGE - 1) / BEATMAPS_PER_PAGE).coerceAtLeast(1)
    val estimatedPages = ui.total?.let { ((it + BEATMAPS_PER_PAGE - 1) / BEATMAPS_PER_PAGE).coerceAtLeast(1) }
    val pageCount = if (ui.hasMore) {
        maxOf(loadedPages + 1, estimatedPages ?: 0)
    } else {
        loadedPages
    }

    LaunchedEffect(viewModel, formgamepage) { viewModel.onEnter(formgamepage) }
    LaunchedEffect(gameViewModel, formgamepage) {
        if (formgamepage && gameViewModel == null) {
            gameCatalogError = "Game session is unavailable. Return to Preview and open the catalog again."
        } else if (formgamepage && gameViewModel != null) {
            gameCatalogError = null
            try {
                gameViewModel.ensureCatalogLoaded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                gameCatalogError = e.message ?: "Could not load local Mania maps"
            }
        }
    }
    LaunchedEffect(ui.filters) {
        currentPage = 0
        listState.scrollToItem(0)
    }
    LaunchedEffect(currentPage) { listState.scrollToItem(0) }
    LaunchedEffect(currentPage, ui.items.size, ui.hasMore, ui.isLoadingMore, ui.loadMoreError) {
        val requiredItems = (currentPage + 1) * BEATMAPS_PER_PAGE
        if (ui.items.size < requiredItems && ui.hasMore && !ui.isLoadingMore && ui.loadMoreError == null) {
            viewModel.loadMore()
        } else if (ui.items.size < requiredItems && !ui.hasMore && !ui.isLoadingMore && currentPage > 0) {
            currentPage = ((ui.items.size - 1).coerceAtLeast(0) / BEATMAPS_PER_PAGE)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        CenteredHeader(
            title = "All beatmaps",
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = kozuUiString("Back"),
                    onClick = onBack,
                    startPadding = 0.dp,
                )
            },
            trailing = {
                FilterAction(
                    active = ui.filters.hasActiveFilters,
                    onClick = {
                        keyboard?.hide()
                        focus.clearFocus()
                        showFilters = true
                    },
                )
            },
        )

        BrowseSearchField(
            query = ui.filters.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                viewModel.submitQuery()
                keyboard?.hide()
                focus.clearFocus()
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        BrowseSummary(ui, Modifier.padding(horizontal = 24.dp))
        if (ui.filters.hasActiveFilters) {
            Spacer(Modifier.height(4.dp))
            ActiveFilterChips(
                filters = ui.filters.activeFilters,
                onRemove = { filter -> viewModel.updateFilters(filter.clear) },
                onReset = viewModel::resetFilters,
            )
        }
        BrowseSourceNote(ui, Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Spacer(Modifier.height(4.dp))

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                if (!isAppRefreshing && !ui.isRefreshing) {
                    currentPage = 0
                    viewModel.refresh()
                    onRetry()
                }
            },
            state = pullState,
            modifier = Modifier.weight(1f),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = listBottom + 18.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (formgamepage) {
                    gameCatalogError?.let { message ->
                        item(key = "game-catalog-error") {
                            Text(
                                "Local Mania maps could not be loaded: $message",
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                when {
                    ui.isLoading && ui.items.isEmpty() -> item(key = "browse-loading") {
                        KozuLoading()
                    }

                    ui.error != null && ui.items.isEmpty() -> item(key = "browse-error") {
                        EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "Catalog unavailable",
                            message = ui.error.orEmpty(),
                            action = { TextButton(onClick = viewModel::retry) { Text("Retry") } },
                        )
                    }

                    ui.items.isEmpty() -> item(key = "browse-empty") {
                        EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = "No beatmaps found",
                            message = "Try another search or filter.",
                            action = if (ui.filters.hasActiveFilters) {
                                { TextButton(onClick = viewModel::resetFilters) { Text("Reset filters") } }
                            } else {
                                null
                            },
                        )
                    }

                    else -> {
                        if (ui.error != null) {
                            item(key = "browse-refresh-error") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = ui.error.orEmpty(),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        TextButton(onClick = viewModel::retry) { Text("Retry") }
                                    }
                                }
                            }
                        }
                        itemsIndexed(
                            items = pageItems,
                            key = { _, beatmap -> "browse-${beatmap.id}" },
                        ) { index, beatmap ->
                            BrowseRow(
                                beatmap = beatmap,
                                index = index,
                                count = pageItems.size,
                                onPlay = { actions.play(ui.items, pageStart + index) },
                                loadInfo = viewModel::loadBeatmapInfo,
                                formgamepage = formgamepage,
                                isGameMapAdded = beatmap.id in gameLocalMapIds,
                                gameDownload = gameDownloads[beatmap.id],
                                onGameDownload = {
                                    if (gameViewModel == null) {
                                        gameCatalogError = "Game session is unavailable. Return to Preview and open the catalog again."
                                    } else {
                                        gameViewModel.download(beatmap)
                                    }
                                },
                                onPlayGameMap = onPlayGameMap,
                            )
                        }
                        item(key = "browse-pagination") {
                            BrowsePagination(
                                currentPage = currentPage,
                                pageCount = pageCount,
                                hasMore = ui.hasMore,
                                loadingMore = ui.isLoadingMore,
                                loadMoreError = ui.loadMoreError,
                                onSelect = { page -> currentPage = page },
                                onRetry = viewModel::loadMore,
                            )
                        }
                    }
                }
            }
        }

    }

    if (showFilters) {
        BrowseFilterSheet(
            initial = ui.filters,
            isLoggedIn = ui.isLoggedIn,
            modeLocked = formgamepage,
            onDismiss = { showFilters = false },
            onApply = {
                currentPage = 0
                viewModel.applyFilters(it)
                showFilters = false
            },
        )
    }
}

@Composable
private fun BrowseRow(
    beatmap: Beatmap,
    index: Int,
    count: Int,
    onPlay: () -> Unit,
    loadInfo: suspend (Long) -> BeatmapSetInfo?,
    formgamepage: Boolean,
    isGameMapAdded: Boolean,
    gameDownload: GameMapDownloadState?,
    onGameDownload: () -> Unit,
    onPlayGameMap: (Long) -> Unit,
) {
    val actions = LocalTrackActions.current
    val downloads = LocalDownloads.current
    var expanded by rememberSaveable(beatmap.id) { mutableStateOf(false) }
    var attempt by remember(beatmap.id) { mutableIntStateOf(0) }
    var difficultyInfo by remember(beatmap.id) { mutableStateOf<BeatmapSetInfo?>(null) }
    var loadingDifficulties by remember(beatmap.id) { mutableStateOf(false) }
    var difficultyError by remember(beatmap.id) { mutableStateOf<String?>(null) }
    val isDownloaded = beatmap.id in downloads.downloaded
    val isOutdated = beatmap.id in downloads.outdated
    val isDownloading = beatmap.id in downloads.active

    LaunchedEffect(beatmap.id, expanded, attempt) {
        if (expanded && difficultyInfo == null) {
            loadingDifficulties = true
            difficultyError = null
            try {
                difficultyInfo = loadInfo(beatmap.id)
                if (difficultyInfo == null) difficultyError = "Could not load difficulty details."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                difficultyError = e.message ?: "Could not load difficulty details."
            } finally {
                loadingDifficulties = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackRow(
                beatmap = beatmap,
                shape = segmentedShape(index, count, large = 22.dp),
                modifier = Modifier.weight(1f),
                onClick = { expanded = !expanded },
                showDownloadBadge = !formgamepage,
                details = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = beatmap.status.ifBlank { "Map" }.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            starRange(beatmap)?.let { range ->
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = if (expanded) "Hide difficulties" else "Show difficulties",
                                    modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!formgamepage) {
                                IconButton(onClick = onPlay) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "Play beatmap",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        if (formgamepage) {
                            gameDownload?.error?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            gameDownload?.ratingWarning?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (gameDownload != null && gameDownload.error == null && !isGameMapAdded) {
                                val progress = gameDownload.progress
                                Text(
                                    if (progress != null && progress >= 1f) "Importing map…"
                                    else if (progress == null || progress < 0f) "Downloading…"
                                    else "Downloading ${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { actions.openInfo(beatmap) }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Beatmap info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (formgamepage) {
                    when {
                        isGameMapAdded -> IconButton(onClick = { onPlayGameMap(beatmap.id) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Go to map", tint = MaterialTheme.colorScheme.primary)
                        }
                        gameDownload != null && gameDownload.error == null -> {
                            if (gameDownload.progress == null || gameDownload.progress < 0f) {
                                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                            } else {
                                CircularProgressIndicator(
                                    progress = { gameDownload.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        else -> IconButton(onClick = onGameDownload) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = if (gameDownload?.error != null) "Retry map download" else "Download and import Mania map",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { actions.download(beatmap, force = isOutdated) },
                        enabled = !isDownloading && (!isDownloaded || isOutdated),
                    ) {
                        Icon(
                            imageVector = if (isDownloaded && !isOutdated) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = when {
                                isDownloading -> "Downloading"
                                isOutdated -> "Update offline copy"
                                isDownloaded -> "Downloaded"
                                else -> "Download for offline"
                            },
                            tint = if (isDownloaded && !isOutdated) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            DifficultyExpansion(
                info = difficultyInfo,
                loading = loadingDifficulties,
                error = difficultyError,
                onRetry = { attempt += 1 },
            )
        }
    }
}

@Composable
private fun DifficultyExpansion(
    info: BeatmapSetInfo?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (info == null) "Difficulties" else "Difficulties (${info.difficulties.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                )
                error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                info?.difficulties.isNullOrEmpty() -> Text(
                    "No difficulty details are available for this map.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.difficulties.forEach { difficulty ->
                        DifficultyDetail(difficulty)
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyDetail(difficulty: BeatmapDifficultyInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = difficulty.version,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (difficulty.lengthSeconds > 0) {
                        formatTime(difficulty.lengthSeconds * 1000L)
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CS ${compact(difficulty.circleSize)} · AR ${compact(difficulty.approachRate)} · " +
                        "OD ${compact(difficulty.overallDifficulty)} · HP ${compact(difficulty.hpDrain)}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "★ ${String.format(Locale.US, "%.2f", difficulty.stars)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BrowsePagination(
    currentPage: Int,
    pageCount: Int,
    hasMore: Boolean,
    loadingMore: Boolean,
    loadMoreError: String?,
    onSelect: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val pages = visiblePages(currentPage + 1, pageCount).map { it?.minus(1) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (loadingMore) {
            KozuLoading()
        } else if (loadMoreError != null) {
            Text(loadMoreError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        } else if (!hasMore && currentPage + 1 >= pageCount) {
            Text("End of results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (pageCount > 1 || currentPage > 0 || hasMore) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = { onSelect(currentPage - 1) },
                    enabled = currentPage > 0 && !loadingMore,
                    modifier = Modifier.width(44.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("‹") }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        pages.forEach { page ->
                            if (page == null) {
                                Text("…", modifier = Modifier.padding(horizontal = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Surface(
                                    onClick = { onSelect(page) },
                                    enabled = page != currentPage && !loadingMore,
                                    shape = CircleShape,
                                    color = if (page == currentPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (page == currentPage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Text(
                                        text = (page + 1).toString(),
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { onSelect(currentPage + 1) },
                    enabled = hasMore && !loadingMore,
                    modifier = Modifier.width(44.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("›") }
            }
        }
        if (pageCount > 1) {
            Text("Page ${currentPage + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun visiblePages(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (1..total).map { it }
    val pages = linkedSetOf<Int?>()
    pages.add(1)
    val start = (current - 1).coerceAtLeast(2)
    val end = (current + 1).coerceAtMost(total - 1)
    if (start > 2) pages.add(null)
    (start..end).forEach { pages.add(it) }
    if (end < total - 1) pages.add(null)
    pages.add(total)
    return pages.toList()
}

private fun starRange(beatmap: Beatmap): String? {
    if (beatmap.maxStars <= 0.0) return null
    val low = String.format(Locale.US, "%.1f", beatmap.minStars)
    val high = String.format(Locale.US, "%.1f", beatmap.maxStars)
    return if (low == high) "★ $high" else "★ $low–$high"
}

private fun compact(value: Double): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")
