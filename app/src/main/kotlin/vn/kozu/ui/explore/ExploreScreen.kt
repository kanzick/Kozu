package vn.kozu.ui.explore

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies

@Composable
fun ExploreScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = { HeaderTitle("Explore") },
            actions = {
                HeaderAction(
                    icon = Icons.Rounded.AutoAwesome,
                    contentDescription = "Surprise me",
                    onClick = { actions.playShuffled(ui.surprisePool) }
                )
                SettingsAction(onOpenSettings)
            }
        )

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

/** Thẻ bộ sưu tập: lưới ảnh 2x2 + tên + số bài + nút phát. */
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
