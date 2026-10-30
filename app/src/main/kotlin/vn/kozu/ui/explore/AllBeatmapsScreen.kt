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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.data.repository.CatalogSource
import vn.kozu.data.repository.DiscoverState
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape

@Composable
fun AllBeatmapsScreen(
    state: DiscoverState,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = LocalTrackActions.current
    // Defensive UI filter: even a stale or malformed fallback response must
    // never display Pending, WIP, Graveyard, Approved or Qualified rows here.
    val beatmaps = state.mix
        .filter {
            it.status.equals("ranked", ignoreCase = true) ||
                it.status.equals("loved", ignoreCase = true)
        }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<vn.kozu.domain.model.Beatmap> { it.updatedAt }
                .thenByDescending { it.id }
        )

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
                    text = "Ranked + Loved only · all game modes. osu! official sorts by latest updates; public fallback sorts by recent rank date, not play count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${beatmaps.size} beatmaps loaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
                    title = "No beatmaps yet",
                    message = "Refresh the catalog to try again.",
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
