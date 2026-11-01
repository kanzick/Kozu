package vn.kozu.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.CenteredHeader
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape
import java.util.Locale

private const val PREFETCH_DISTANCE = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllBeatmapsScreen(
    bottomPadding: Dp,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    isAppRefreshing: Boolean,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val listBottom = if (WindowInsets.isImeVisible) 16.dp else bottomPadding
    val refreshing = ui.isRefreshing || isAppRefreshing

    LaunchedEffect(viewModel) { viewModel.onEnter() }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, ui.items.size, ui.hasMore, ui.isLoadingMore, ui.loadMoreError) {
        if (nearEnd && ui.hasMore && !ui.isLoadingMore && ui.loadMoreError == null) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        CenteredHeader(
            title = "All beatmaps",
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    startPadding = 0.dp
                )
            },
            trailing = {
                FilterAction(
                    active = ui.filters.hasActiveFilters,
                    onClick = {
                        keyboard?.hide()
                        focus.clearFocus()
                        showFilters = true
                    }
                )
            }
        )

        BrowseSearchField(
            query = ui.filters.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                viewModel.submitQuery()
                keyboard?.hide()
                focus.clearFocus()
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))
        BrowseSummary(ui, Modifier.padding(horizontal = 24.dp))
        if (ui.filters.hasActiveFilters) {
            Spacer(Modifier.height(4.dp))
            ActiveFilterChips(
                filters = ui.filters.activeFilters,
                onRemove = { filter -> viewModel.updateFilters(filter.clear) },
                onReset = viewModel::resetFilters
            )
        }
        BrowseSourceNote(ui, Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Spacer(Modifier.height(4.dp))

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                if (!isAppRefreshing && !ui.isRefreshing) {
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
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = listBottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            action = {
                                TextButton(onClick = viewModel::retry) { Text("Retry") }
                            }
                        )
                    }

                    ui.items.isEmpty() -> item(key = "browse-empty") {
                        EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = "No beatmaps found",
                            message = "Try a different search or loosen the filters.",
                            action = if (ui.filters.hasActiveFilters) {
                                {
                                    TextButton(onClick = viewModel::resetFilters) { Text("Reset filters") }
                                }
                            } else {
                                null
                            }
                        )
                    }

                    else -> {
                        if (ui.error != null) {
                            item(key = "browse-refresh-error") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ui.error.orEmpty(),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        TextButton(onClick = viewModel::retry) { Text("Retry") }
                                    }
                                }
                            }
                        }
                        itemsIndexed(
                            items = ui.items,
                            key = { _, beatmap -> "browse-${beatmap.id}" }
                        ) { index, beatmap ->
                            BrowseRow(
                                beatmap = beatmap,
                                index = index,
                                count = ui.items.size,
                                onClick = { actions.play(ui.items, index) }
                            )
                        }
                        item(key = "browse-footer") {
                            BrowseFooter(ui = ui, onRetry = viewModel::loadMore)
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
                viewModel.applyFilters(it)
                showFilters = false
            }
        )
    }
}

@Composable
private fun BrowseRow(
    beatmap: Beatmap,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(78.dp)) {
            Text(
                text = beatmap.status.ifBlank { "Map" }.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            starRange(beatmap)?.let { range ->
                Text(
                    text = range,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TrackRow(
            beatmap = beatmap,
            shape = segmentedShape(index, count, large = 22.dp),
            modifier = Modifier.weight(1f),
            onClick = onClick
        )
    }
}

@Composable
private fun BrowseFooter(ui: BrowseUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            ui.isLoadingMore -> KozuLoading()
            ui.loadMoreError != null -> {
                Text(
                    text = ui.loadMoreError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onRetry) { Text("Retry loading more") }
            }

            !ui.hasMore -> Text(
                text = "You've reached the end",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun starRange(beatmap: Beatmap): String? {
    if (beatmap.maxStars <= 0.0) return null
    val low = String.format(Locale.US, "%.1f", beatmap.minStars)
    val high = String.format(Locale.US, "%.1f", beatmap.maxStars)
    return if (low == high) "★ $high" else "★ $low–$high"
}
