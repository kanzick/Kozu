package vn.kozu.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.ui.components.softTopEdge
import vn.kozu.ui.components.CenteredHeader
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.i18n.KozuText as Text
import vn.kozu.ui.i18n.kozuUiString

/**
 * A page of its own for one artist or mapper, opened from the player. It sits above the player,
 * so going back lands exactly where the listener was.
 */
@Composable
fun PersonScreen(
    kind: PersonKind,
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PersonViewModel =
        viewModel(key = "person:${kind.name}:$name", factory = PersonViewModel.factory(kind, name))
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, ui.items.size, ui.hasMore) {
        if (nearEnd && ui.hasMore && !ui.isLoadingMore) viewModel.loadMore()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // Swallow stray touches so nothing underneath (the player) reacts through this page.
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        CenteredHeader(
            title = kozuUiString(kind.title),
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = kozuUiString("Back"),
                    onClick = onBack,
                    startPadding = 0.dp,
                )
            },
            trailing = {},
        )

        when {
            ui.isLoading -> KozuLoading()

            ui.items.isEmpty() ->
                EmptyState(
                    icon = if (ui.error == null) Icons.Rounded.Search else Icons.Rounded.CloudOff,
                    title = if (ui.error == null) "No beatmaps found" else "Catalog unavailable",
                    message = ui.error ?: "Nothing in the catalog is credited to $name yet.",
                    action =
                        if (ui.error == null) {
                            null
                        } else {
                            ({ TextButton(onClick = viewModel::retry) { Text("Retry") } })
                        },
                )

            else ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().softTopEdge(listState),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "hero") {
                        Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.displaySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text =
                                    "${ui.items.size}${if (ui.hasMore) "+" else ""} " +
                                        if (ui.items.size == 1) "beatmap" else "beatmaps",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (!ui.exact) {
                                Text(
                                    text = "No exact match, showing the closest results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { actions.play(ui.items, 0) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play all")
                                }
                                FilledTonalButton(
                                    onClick = { actions.playShuffled(ui.items) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                    }
                    itemsIndexed(ui.items, key = { _, beatmap -> beatmap.id }) { index, beatmap ->
                        TrackRow(
                            beatmap = beatmap,
                            onClick = { actions.play(ui.items, index) },
                        )
                    }
                    if (ui.isLoadingMore) item(key = "more") { KozuLoading() }
                }
        }
    }
}
