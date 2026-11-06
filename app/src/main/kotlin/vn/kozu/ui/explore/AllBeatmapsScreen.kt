package vn.kozu.ui.explore

import vn.kozu.ui.i18n.kozuUiString

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
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.CenteredHeader
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.components.LocalOverlayBottomInset
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
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
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

    LaunchedEffect(viewModel) { viewModel.onEnter() }
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
                                onClick = { actions.play(ui.items, pageStart + index) },
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(78.dp)) {
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
        }
        TrackRow(
            beatmap = beatmap,
            shape = segmentedShape(index, count, large = 22.dp),
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
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
