package vn.kozu.ui.search

import vn.kozu.ui.i18n.kozuUiString

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import vn.kozu.ui.explore.ActiveFilterChips
import vn.kozu.ui.explore.BrowseFilterSheet
import vn.kozu.ui.explore.FilterAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import vn.kozu.ui.components.softTopEdge
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.KozuSearchField
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    isAppRefreshing: Boolean,
    modifier: Modifier = Modifier,
    focusSignal: Int = 0,
    initialQuery: String? = null,
    onInitialQueryConsumed: () -> Unit = {},
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(initialQuery) {
        val query = initialQuery?.trim()?.takeIf(String::isNotEmpty) ?: return@LaunchedEffect
        viewModel.onQueryChange(query)
        viewModel.remember()
        delay(180)
        searchFocus.requestFocus()
        onInitialQueryConsumed()
    }
    LaunchedEffect(focusSignal) {
        if (focusSignal > 0) {
            delay(180)
            searchFocus.requestFocus()
        }
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Idle (recents / categories) and results are different lists. Sharing one state meant the
    // results list re-opened at whatever offset the previous list had been scrolled to.
    val idleListState = rememberLazyListState()
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val listBottom = if (WindowInsets.isImeVisible) 16.dp else bottomPadding
    // Any edit to the query, filters or a fresh result set starts again from the first hit,
    // exactly like All beatmaps does.
    LaunchedEffect(ui.query, ui.filters) { listState.scrollToItem(0) }
    LaunchedEffect(ui.phase) {
        if (ui.phase != SearchPhase.DONE) listState.scrollToItem(0)
    }
    LaunchedEffect(ui.results.firstOrNull()?.id, ui.phase) {
        if (ui.phase == SearchPhase.DONE && !ui.isLoadingMore) listState.scrollToItem(0)
    }
    // Scrolling the results hides the keyboard so more of the list is visible.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) keyboard?.hide() }
    }
    val refreshNow = {
        if (!isAppRefreshing) {
            if (ui.query.isNotBlank() || ui.filters.hasActiveFilters) viewModel.retry()
            onRefresh()
        }
    }

    PullToRefreshBox(
        isRefreshing = isAppRefreshing,
        onRefresh = refreshNow,
        state = pullState,
        modifier = modifier.fillMaxSize().imePadding(),
        indicator = {
            RefreshIndicator(
                state = pullState,
                isRefreshing = isAppRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
        ScreenHeader(
            leading = {
                KozuSearchField(
                    query = ui.query,
                    placeholder = kozuUiString("Search, or try stars<8 ranked=2025"),
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {
                        viewModel.remember()
                        keyboard?.hide()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocus)
                )
            },
            actions = {
                Spacer(Modifier.width(10.dp))
                FilterAction(
                    active = ui.filters.hasActiveFilters,
                    onClick = {
                        keyboard?.hide()
                        showFilters = true
                    }
                )
                SettingsAction(onOpenSettings, startPadding = 10.dp)
            }
        )

        val isIdle = ui.isIdle
        if (isIdle) {
            LazyColumn(
                state = idleListState,
                modifier = Modifier.fillMaxSize().softTopEdge(idleListState),
                contentPadding = PaddingValues(top = 8.dp, bottom = listBottom)
            ) {
                if (recents.isNotEmpty()) {
                    item(key = "recents") {
                        RecentSearches(
                            recents = recents,
                            onPick = viewModel::useRecent,
                            onRemove = { viewModel.removeRecent(it) },
                            onClear = { viewModel.clearRecents() }
                        )
                    }
                }
                item(key = "browse") {
                    BrowseAll(onPick = viewModel::onCategoryChange)
                }
            }
        } else {
            if (ui.phase == SearchPhase.DONE) {
                Text(
                    text = (ui.total?.let { "%,d results".format(it) } ?: "${ui.results.size} results") +
                        " · " + ui.filters.sort.label,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (ui.filters.hasActiveFilters) {
                ActiveFilterChips(
                    filters = ui.filters.activeFilters,
                    onRemove = { filter -> viewModel.updateFilters(filter.clear) },
                    onReset = viewModel::resetFilters
                )
            }
            vn.kozu.ui.components.SearchSyntaxChips(ui.query)
            val nearEnd by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
                }
            }
            LaunchedEffect(nearEnd, ui.results.size, ui.hasMore) {
                if (nearEnd && ui.hasMore && !ui.isLoadingMore && ui.loadMoreError == null) viewModel.loadMore()
            }
            when (ui.phase) {
                SearchPhase.IDLE -> Unit
                SearchPhase.LOADING -> KozuLoading()
                SearchPhase.DONE -> {
                    if (ui.results.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            title = if (ui.error == null) "No results found" else "Catalog unavailable",
                            message = ui.error ?: "Try a different song, artist or mapper, or loosen the filters.",
                            action = if (ui.error == null) null else ({
                                TextButton(onClick = viewModel::retry) { Text("Retry") }
                            })
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().softTopEdge(listState),
                            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = listBottom),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ui.results, key = { it.id }) { beatmap ->
                                TrackRow(
                                    beatmap = beatmap,
                                    onClick = {
                                        viewModel.remember()
                                        keyboard?.hide()
                                        actions.play(ui.results, ui.results.indexOf(beatmap))
                                    }
                                )
                            }
                            if (ui.isLoadingMore) item(key = "more") { KozuLoading() }
                            if (ui.loadMoreError != null) item(key = "more-error") {
                                TextButton(onClick = viewModel::loadMore) { Text("Retry loading more") }
                            }
                        }
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
private fun RecentSearches(
    recents: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent searches", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            recents.forEachIndexed { index, query ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(pressedScale = 0.98f) { onPick(query) },
                    shape = segmentedShape(index, recents.size),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onRemove(query) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = kozuUiString("Remove recent search"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrowseAll(onPick: (SearchCategory) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Browse all", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        val categories = SearchCategory.entries.chunked(2)
        val colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEachIndexed { colIndex, cat ->
                        val cardColor = colors[(rowIndex * 2 + colIndex) % colors.size]
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .bounceClick(pressedScale = 0.96f) { onPick(cat) },
                            shape = RoundedCornerShape(24.dp),
                            color = cardColor
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cat.emoji,
                                    style = MaterialTheme.typography.headlineLarge,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    sort: SearchSort,
    category: SearchCategory?,
    onSort: (SearchSort) -> Unit,
    onClearCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null) {
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(category.title) },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, Modifier.size(16.dp)) }
            )
        }
        SearchSort.entries.forEach { s ->
            FilterChip(
                selected = sort == s,
                onClick = { onSort(s) },
                label = { Text(s.label) }
            )
        }
    }
}
