package vn.kozu.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.data.repository.CatalogSource
import vn.kozu.data.repository.DiscoverState
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.StatusFilter
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape

@Composable
private fun FilterRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBeatmapsScreen(
    state: DiscoverState,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    isAppRefreshing: Boolean,
    statusFilter: StatusFilter,
    modeFilter: ModeFilter,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onModeFilterChange: (ModeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = LocalTrackActions.current
    val pullState = rememberPullToRefreshState()
    var sortFilter by rememberSaveable { mutableStateOf("updated") }
    val beatmaps = remember(state.mix, statusFilter, modeFilter, sortFilter) {
        state.mix
            .filter { it.status.equals("ranked", true) || it.status.equals("loved", true) }
            .filter {
                statusFilter == StatusFilter.DEFAULT ||
                    statusFilter == StatusFilter.ANY ||
                    it.status.equals(statusFilter.apiValue, true)
            }
            .filter { modeFilter.apiValue == null || modeFilter.apiValue in it.modes }
            .distinctBy { it.id }
            .let { items ->
                when (sortFilter) {
                    "title" -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    "artist" -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
                    else -> items.sortedWith(
                        compareByDescending<vn.kozu.domain.model.Beatmap> { it.updatedAt }
                            .thenByDescending { it.id }
                    )
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ScreenHeader(
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Spacer(Modifier.width(12.dp))
                HeaderTitle("All beatmaps")
            },
            actions = {}
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing || isAppRefreshing,
            onRefresh = { if (!isAppRefreshing) onRetry() },
            state = pullState,
            modifier = Modifier.weight(1f),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing || isAppRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
        Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = when (state.mixSource) {
                        CatalogSource.OFFICIAL_OSU -> "osu! official catalog"
                        CatalogSource.PUBLIC_MIRROR -> "Public fallback · add osu! API credentials for official results"
                        CatalogSource.MIXED -> "osu! official + public fallback"
                        CatalogSource.NONE -> "Connecting to catalog"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Filter by status, ruleset or title.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterRow(
                options = listOf("all" to "All", "ranked" to "Ranked", "loved" to "Loved"),
                selected = when (statusFilter) {
                    StatusFilter.RANKED -> "ranked"
                    StatusFilter.LOVED -> "loved"
                    else -> "all"
                },
                onSelect = {
                    onStatusFilterChange(
                        when (it) {
                            "ranked" -> StatusFilter.RANKED
                            "loved" -> StatusFilter.LOVED
                            else -> StatusFilter.DEFAULT
                        }
                    )
                }
            )
            FilterRow(
                options = listOf(
                    "-1" to "All modes",
                    "0" to "osu!",
                    "1" to "Taiko",
                    "2" to "Catch",
                    "3" to "Mania"
                ),
                selected = (modeFilter.apiValue ?: -1).toString(),
                onSelect = {
                    val mode = it.toInt()
                    onModeFilterChange(
                        if (mode == -1) ModeFilter.ANY
                        else ModeFilter.entries.first { filter -> filter.apiValue == mode }
                    )
                }
            )
            FilterRow(
                options = listOf("updated" to "Latest", "title" to "Title", "artist" to "Artist"),
                selected = sortFilter,
                onSelect = { sortFilter = it }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${beatmaps.size} maps",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        when {
            beatmaps.isEmpty() && state.isLoading -> {
                KozuLoading()
            }

            beatmaps.isEmpty() && state.error != null -> {
                EmptyState(
                    icon = Icons.Rounded.Refresh,
                    title = "Catalog unavailable",
                    message = state.error,
                    action = {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                )
            }

            beatmaps.isEmpty() -> {
                EmptyState(
                    icon = Icons.Rounded.Refresh,
                    title = if (state.mix.isEmpty()) "No beatmaps yet" else "No matching maps",
                    message = if (state.mix.isEmpty()) {
                        "Refresh the catalog to try again."
                    } else {
                        "Try another status or ruleset."
                    },
                    action = {
                        TextButton(onClick = onRetry) {
                            Text("Refresh")
                        }
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = bottomPadding + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = beatmaps,
                        key = { _, beatmap -> "all-map-${beatmap.id}" }
                    ) { index, beatmap ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = beatmap.status.ifBlank { "Map" }
                                    .replaceFirstChar { it.uppercase() },
                                modifier = Modifier.width(78.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TrackRow(
                                beatmap = beatmap,
                                shape = segmentedShape(index, beatmaps.size, large = 22.dp),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    actions.play(beatmaps, index)
                                }
                            )
                        }
                    }

                    item(key = "all-beatmaps-pagination") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.isLoadingMoreMix) {
                                KozuLoading()
                            } else if (state.hasMoreMix) {
                                TextButton(onClick = onLoadMore) {
                                    Text("Load more beatmaps")
                                }
                            } else {
                                Text(
                                    text = "No more pages available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.error != null && beatmaps.isNotEmpty()) {
                                TextButton(onClick = onLoadMore) {
                                    Text("Retry loading more")
                                }
                            }

                        }
                    }
                    }
                    }
                }
            }
        }
    }
}
