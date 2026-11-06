package vn.kozu.ui.player

import vn.kozu.ui.i18n.kozuUiString

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.EqualizerBars
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.rememberKozuHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Beatmap>,
    currentIndex: Int,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    shuffle: Boolean = false,
    onClear: () -> Unit = {},
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onSaveAsPlaylist: (List<Beatmap>) -> Unit = {},
) {
    val showHistory = LocalAppSettings.current.showQueueHistory
    val haptics = rememberKozuHaptics()

    // The list is edited locally while a song is being dragged so it follows the finger at once;
    // every step is also pushed to the real queue so playback order changes with it.
    val uidCounter = remember { LongArray(1) { queue.size.toLong() } }
    var entries by remember { mutableStateOf(queue.mapIndexed { i, b -> QueueEntry(i.toLong(), b) }) }
    var currentUid by remember { mutableLongStateOf(entries.getOrNull(currentIndex)?.uid ?: -1L) }
    var dragging by remember { mutableStateOf(false) }
    var droppedAt by remember { mutableLongStateOf(0L) }
    var frozenFirst by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(queue, currentIndex, dragging) {
        if (dragging) return@LaunchedEffect
        // Give the player a moment to apply the last move so we do not briefly snap back.
        val wait = 350L - (SystemClock.uptimeMillis() - droppedAt)
        if (wait > 0) delay(wait)
        val same = entries.size == queue.size && entries.indices.all { entries[it].beatmap.id == queue[it].id }
        if (!same) {
            val pool = entries.toMutableList()
            entries =
                queue.map { beatmap ->
                    val found = pool.indexOfFirst { it.beatmap.id == beatmap.id }
                    if (found >= 0) pool.removeAt(found) else QueueEntry(uidCounter[0]++, beatmap)
                }
        }
        currentUid = entries.getOrNull(currentIndex)?.uid ?: -1L
        frozenFirst = null
    }

    val baseFirst = if (shuffle || showHistory || currentIndex < 0) 0 else currentIndex.coerceAtMost(entries.size)
    val firstShown = frozenFirst ?: baseFirst
    val shownCount = (entries.size - firstShown).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = !dragging,
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = "Up next",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text(
                text = "${queue.size} ${if (queue.size == 1) "track" else "tracks"} in queue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        haptics.tick()
                        onSaveAsPlaylist(queue)
                    },
                    enabled = queue.isNotEmpty(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save as playlist")
                }
                TextButton(
                    onClick = {
                        haptics.press()
                        onClear()
                        onDismiss()
                    },
                    enabled = queue.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear queue")
                }
            }
            Spacer(Modifier.height(4.dp))

            val listState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = (currentIndex - firstShown - 1).coerceAtLeast(0),
                )
            val reorderableState =
                rememberReorderableLazyListState(
                    lazyListState = listState,
                    onMove = { from, to ->
                        val fromIndex = firstShown + from.index
                        val toIndex = firstShown + to.index
                        if (fromIndex in entries.indices && toIndex in entries.indices && fromIndex != toIndex) {
                            entries = entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                            haptics.tick()
                            onMove(fromIndex, toIndex)
                        }
                    },
                )
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(
                    count = shownCount,
                    key = { offset -> entries.getOrNull(firstShown + offset)?.uid ?: (-1L - offset) },
                ) { offset ->
                    val index = firstShown + offset
                    val entry = entries.getOrNull(index) ?: return@items
                    val beatmap = entry.beatmap
                    val isCurrent = entry.uid == currentUid
                    val isPast = !shuffle && showHistory && index < currentIndex
                    ReorderableItem(reorderableState, key = entry.uid) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "queueDragElevation")
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .alpha(if (isPast && !isDragging) 0.55f else 1f)
                                    .bounceClick(pressedScale = 0.98f) { onSkipTo(index) },
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = elevation,
                            color =
                                if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    BeatmapArt(beatmap, Modifier.size(48.dp), shape = RoundedCornerShape(15.dp))
                                    if (isCurrent) {
                                        EqualizerBars(
                                            isPlaying = isPlaying,
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        beatmap.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        beatmap.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!isCurrent) {
                                    IconButton(onClick = { onRemove(index) }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = kozuUiString("Remove from queue"),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (entries.size > 1) {
                                    Icon(
                                        Icons.Rounded.DragIndicator,
                                        contentDescription = kozuUiString("Reorder song"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                            Modifier
                                                .draggableHandle(
                                                    onDragStarted = {
                                                        frozenFirst = baseFirst
                                                        dragging = true
                                                        haptics.press()
                                                    },
                                                    onDragStopped = {
                                                        droppedAt = SystemClock.uptimeMillis()
                                                        dragging = false
                                                    },
                                                ).padding(horizontal = 8.dp, vertical = 12.dp)
                                                .size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private class QueueEntry(
    val uid: Long,
    val beatmap: Beatmap,
)
