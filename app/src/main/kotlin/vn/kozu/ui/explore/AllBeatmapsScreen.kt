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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import vn.kozu.ui.components.LocalFavoriteIds
import vn.kozu.ui.components.LocalAppSettings
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
    val favorites = LocalFavoriteIds.current
    val showMapper = LocalAppSettings.current.showMapper
    var expanded by rememberSaveable(beatmap.id) { mutableStateOf(false) }
    var attempt by remember(beatmap.id) { mutableIntStateOf(0) }
    var difficultyInfo by remember(beatmap.id) { mutableStateOf<BeatmapSetInfo?>(null) }
    var loadingDifficulties by remember(beatmap.id) { mutableStateOf(false) }
    var difficultyError by remember(beatmap.id) { mutableStateOf<String?>(null) }
    val difficultyStars = remember(beatmap.difficultyStars, difficultyInfo) {
        beatmap.difficultyStars.ifEmpty {
            difficultyInfo?.difficulties.orEmpty().map { it.stars }.filter { it > 0.0 }.sorted()
        }
    }
    val isDownloaded = beatmap.id in downloads.downloaded
    val isOutdated = beatmap.id in downloads.outdated
    val isDownloading = beatmap.id in downloads.active

    LaunchedEffect(beatmap.id, expanded, attempt) {
        if ((expanded || beatmap.difficultyStars.isEmpty()) && difficultyInfo == null) {
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
                onArtworkClick = if (formgamepage) null else onPlay,
                subtitleSuffix = if (showMapper && beatmap.mapper.isNotBlank()) {
                    " · Mapped by ${beatmap.mapper}"
                } else {
                    null
                },
                compact = true,
                showDownloadBadge = false,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { actions.toggleFavorite(beatmap) }) {
                            Icon(
                                imageVector = if (beatmap.id in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (beatmap.id in favorites) "Remove from liked" else "Add to liked",
                                tint = if (beatmap.id in favorites) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { actions.openMenu(beatmap) }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = kozuUiString("More options"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BrowseDownloadButton(
                            formgamepage = formgamepage,
                            isDownloaded = isDownloaded,
                            isOutdated = isOutdated,
                            isDownloading = isDownloading,
                            gameDownload = gameDownload,
                            isGameMapAdded = isGameMapAdded,
                            onDownload = {
                                if (formgamepage) onGameDownload()
                                else actions.download(beatmap, force = isOutdated)
                            },
                            onPlayGameMap = { onPlayGameMap(beatmap.id) },
                        )
                    }
                },
                details = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = beatmap.status.ifBlank { "Map" }.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            DifficultyDots(difficultyStars)
                            Spacer(Modifier.weight(1f))
                        }
                        if (formgamepage) {
                            gameDownload?.error?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            gameDownload?.ratingWarning?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
            )
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
private fun BrowseDownloadButton(
    formgamepage: Boolean,
    isDownloaded: Boolean,
    isOutdated: Boolean,
    isDownloading: Boolean,
    gameDownload: GameMapDownloadState?,
    isGameMapAdded: Boolean,
    onDownload: () -> Unit,
    onPlayGameMap: () -> Unit,
) {
    val gameDownloadInProgress = formgamepage && gameDownload != null && gameDownload.error == null && !isGameMapAdded
    val downloading = isDownloading || gameDownloadInProgress
    val enabled = when {
        formgamepage -> !isGameMapAdded && !gameDownloadInProgress
        else -> !isDownloading && (!isDownloaded || isOutdated)
    }

    IconButton(
        onClick = if (formgamepage && isGameMapAdded) onPlayGameMap else onDownload,
        enabled = if (formgamepage && isGameMapAdded) true else enabled,
    ) {
        when {
            downloading -> {
                val progress = if (formgamepage) gameDownload?.progress else null
                if (progress == null || progress < 0f) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(18.dp), strokeWidth = 2.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.padding(12.dp).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            formgamepage && isGameMapAdded -> Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Go to map",
                tint = MaterialTheme.colorScheme.primary,
            )
            !formgamepage && isDownloaded && !isOutdated -> Icon(
                Icons.Rounded.DownloadDone,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                Icons.Rounded.Download,
                contentDescription = when {
                    formgamepage && gameDownload?.error != null -> "Retry map download"
                    formgamepage -> "Download and import Mania map"
                    isOutdated -> "Update offline copy"
                    else -> "Download for offline"
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DifficultyDots(stars: List<Double>) {
    if (stars.isEmpty()) return
    Row(
        modifier = Modifier.semantics { contentDescription = "${stars.size} difficulties" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stars.forEach { rating ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(osuDifficultyColor(rating), CircleShape),
            )
        }
    }
}

private fun osuDifficultyColor(stars: Double): Color = when {
    stars < 1.5 -> Color(0xFF66CCFF)
    stars < 2.5 -> Color(0xFF66FF66)
    stars < 3.5 -> Color(0xFFFFCC22)
    stars < 4.5 -> Color(0xFFFF8844)
    stars < 5.5 -> Color(0xFFFF6688)
    else -> Color(0xFFCC66FF)
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

private fun compact(value: Double): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")
