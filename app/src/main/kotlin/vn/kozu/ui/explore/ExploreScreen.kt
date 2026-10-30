package vn.kozu.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.CoverCard
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SearchAction
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAllBeatmaps: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val pullState = rememberPullToRefreshState()
    val haptics = LocalHapticFeedback.current
    val refreshNow = {
        if (!ui.isRefreshing) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onRefresh()
        }
    }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = { HeaderTitle("Explore") },
            actions = {
                HeaderAction(
                    icon = Icons.Rounded.AutoAwesome,
                    contentDescription = "Mix all beatmaps",
                    onClick = {
                        if (ui.surprisePool.isNotEmpty()) {
                            actions.playShuffled(ui.surprisePool.shuffled())
                        }
                        // Keep expanding the Ranked + Loved pool between Mix sessions.
                        viewModel.loadMoreMix()
                    }
                )
                SearchAction(onOpenSearch)
                SettingsAction(onOpenSettings)
            }
        )

        AnimatedVisibility(
            visible = ui.isRefreshing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Refreshing osu! catalog…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = refreshNow,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = ui.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = bottomPadding)
            ) {
                item(key = "library") {
                    Column {
                        SectionHeader(title = "Your Library", onAction = onOpenLibrary)
                        Spacer(Modifier.height(14.dp))
                        if (ui.collections.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .bounceClick(pressedScale = 0.98f, onClick = onOpenSearch),
                                shape = RoundedCornerShape(36.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    title = "Start your collection",
                                    message = "Like songs or build a playlist and it will live here.",
                                    shape = KozuCookies.Soft9
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(ui.collections, key = { _, c -> c.id }) { _, collection ->
                                    CollectionCard(collection) {
                                        if (collection.tracks.isNotEmpty()) actions.play(collection.tracks, 0)
                                    }
                                }
                            }
                        }
                    }
                }

                if (ui.newReleases.isNotEmpty()) {
                    item(key = "new") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(title = "New Releases")
                            Spacer(Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(ui.newReleases, key = { _, b -> "new-${b.id}" }) { index, beatmap ->
                                    CoverCard(
                                        beatmap = beatmap,
                                        size = 164.dp,
                                        shape = RoundedCornerShape(34.dp),
                                        badge = beatmap.status.ifBlank { "New" },
                                        showPlay = true,
                                        onClick = { actions.play(ui.newReleases, index) }
                                    )
                                }
                            }
                        }
                    }
                } else if (ui.isLoading) {
                    item(key = "loading") { KozuLoading() }
                }

                item(key = "all-beatmaps") {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        SectionHeader(
                            title = "All beatmaps",
                            subtitle = "Browse Ranked + Loved maps across all game modes",
                            onAction = onOpenAllBeatmaps
                        )
                        Spacer(Modifier.height(14.dp))
                        if (ui.mix.isEmpty()) {
                            if (ui.isLoading) {
                                KozuLoading()
                            } else {
                                Text(
                                    "The catalog is unavailable right now.",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val preview = ui.mix.sortedByDescending { it.updatedAt }.take(10)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    preview,
                                    key = { _, beatmap -> "all-preview-${beatmap.id}" }) { index, beatmap ->
                                    CoverCard(
                                        beatmap = beatmap,
                                        size = 142.dp,
                                        shape = KozuCookies.forIndex(index),
                                        badge = beatmap.status.ifBlank { "Beatmap" },
                                        onClick = { actions.play(preview, index) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (ui.listenAgain.isNotEmpty()) {
                    item(key = "again") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(title = "Listen again")
                            Spacer(Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(ui.listenAgain, key = { _, b -> "again-${b.id}" }) { index, beatmap ->
                                    CoverCard(
                                        beatmap = beatmap,
                                        size = 140.dp,
                                        shape = KozuCookies.forIndex(index),
                                        onClick = { actions.play(ui.listenAgain, index) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (ui.trending.isNotEmpty()) {
                    item(key = "trending-header") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(title = "Trending now", subtitle = "Most played ranked maps")
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                    val chart = ui.trending.take(6)
                    itemsIndexed(chart, key = { _, b -> "chart-${b.id}" }) { index, beatmap ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                modifier = Modifier.width(40.dp),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            TrackRow(
                                beatmap = beatmap,
                                shape = segmentedShape(index, chart.size, large = 24.dp),
                                modifier = Modifier.weight(1f),
                                onClick = { actions.play(chart, index) }
                            )
                        }
                    }
                }

                if (ui.loved.isNotEmpty()) {
                    item(key = "loved") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(title = "Loved by the community")
                            Spacer(Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(ui.loved, key = { _, b -> "loved-${b.id}" }) { index, beatmap ->
                                    CoverCard(
                                        beatmap = beatmap,
                                        size = 150.dp,
                                        shape = KozuCookies.forIndex(index + 2),
                                        onClick = { actions.play(ui.loved, index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CollectionCard(collection: LibraryCollection, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(310.dp)
            .bounceClick(pressedScale = 0.97f, onClick = onClick),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MosaicCover(collection.tracks)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.tracks.size} ${if (collection.tracks.size == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = collection.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play ${collection.title}",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
