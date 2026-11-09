package vn.kozu.ui.player

import android.os.SystemClock
import vn.kozu.ui.components.KozuSheetHandle
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import vn.kozu.R
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.player.RepeatState
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.EqualizerBars
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.i18n.KozuText as Text
import vn.kozu.ui.i18n.kozuUiString

/**
 * "Continue playing" queue, laid out like ArchiveTune's: the current track on top, a toolbar with
 * the queue lock, shuffle / repeat / autoplay keys, then the list.
 *
 * The lock protects the order. While it is closed rows can only be tapped; open it to drag songs
 * by their handle or swipe them away.
 */
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
    repeat: RepeatState = RepeatState.OFF,
    autoplay: Boolean = false,
    isFavorite: Boolean = false,
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onToggleAutoplay: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onClear: () -> Unit = {},
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onSaveAsPlaylist: (List<Beatmap>) -> Unit = {},
) {
    val settings = LocalAppSettings.current
    val showHistory = settings.showQueueHistory
    val haptics = rememberKozuHaptics()
    val colors = MaterialTheme.colorScheme
    var locked by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }

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
    val current = queue.getOrNull(currentIndex)

    fun removeEntryAt(index: Int) {
        if (index !in entries.indices) return
        entries = entries.toMutableList().also { it.removeAt(index) }
        onRemove(index)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        // While the queue is unlocked it is being edited, so the sheet must not steal the drag.
        sheetGesturesEnabled = locked,
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        containerColor = colors.surface,
    ) {
        Box(Modifier.fillMaxSize()) {
            // ── list ──
            val listState =
                rememberLazyListState(
                    // Start on the header unless the playing song is far down the list.
                    initialFirstVisibleItemIndex =
                        (currentIndex - firstShown).let { if (it <= 3) 0 else 3 + it - 1 },
                )
            val reorderableState =
                rememberReorderableLazyListState(
                    lazyListState = listState,
                    onMove = { from, to ->
                        // Match rows by key: the header items above shift the lazy-list indexes.
                        val fromIndex = entries.indexOfFirst { it.uid == from.key }
                        val toIndex = entries.indexOfFirst { it.uid == to.key }
                        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                            entries = entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                            haptics.tick()
                            onMove(fromIndex, toIndex)
                        }
                    },
                )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "queue-head-0") {
                    Column {
                    // ── now playing header ──
                    if (current != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BeatmapArt(current, Modifier.size(96.dp), shape = RoundedCornerShape(24.dp))
                            Spacer(Modifier.width(20.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = current.displayTitle(settings.preferUnicode),
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = current.displayArtist(settings.preferUnicode),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onFavorite) {
                                Icon(
                                    at(if (isFavorite) R.drawable.at_favorite else R.drawable.at_favorite_border),
                                    contentDescription = kozuUiString("Favorite"),
                                    tint = if (isFavorite) colors.error else colors.onSurface,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    }
                }
                item(key = "queue-head-1") {
                    Column {
                    // ── toolbar: lock, more, clear, summary ──
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = colors.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    haptics.tick()
                                    locked = !locked
                                },
                            ) {
                                Icon(
                                    at(if (locked) R.drawable.at_lock else R.drawable.at_lock_open),
                                    contentDescription = kozuUiString(if (locked) "Unlock queue" else "Lock queue"),
                                    tint = if (locked) colors.onSurface else colors.primary,
                                )
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(at(R.drawable.at_more_vert), contentDescription = kozuUiString("More options"))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Save as playlist") },
                                        enabled = queue.isNotEmpty(),
                                        leadingIcon = { Icon(at(R.drawable.at_playlist_play), contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            onSaveAsPlaylist(queue)
                                        },
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    haptics.press()
                                    onClear()
                                    onDismiss()
                                },
                                enabled = queue.isNotEmpty(),
                            ) {
                                Icon(
                                    at(R.drawable.at_delete),
                                    contentDescription = kozuUiString("Clear queue"),
                                    tint = colors.error,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${queue.size} ${if (queue.size == 1) "song" else "songs"}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    }
                }
                item(key = "queue-head-2") {
                    Column {
                    // ── shuffle · repeat · autoplay ──
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        QueueKey(
                            icon = at(R.drawable.at_shuffle),
                            description = "Shuffle",
                            active = shuffle,
                            shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                            onClick = onShuffle,
                            modifier = Modifier.weight(1f),
                        )
                        QueueKey(
                            icon = at(if (repeat == RepeatState.ONE) R.drawable.at_repeat_one else R.drawable.at_repeat),
                            description = "Repeat",
                            active = repeat != RepeatState.OFF,
                            shape = RoundedCornerShape(10.dp),
                            onClick = onRepeat,
                            modifier = Modifier.weight(1f),
                        )
                        QueueKey(
                            icon = at(R.drawable.at_all_inclusive),
                            description = "Autoplay",
                            active = autoplay,
                            shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                            onClick = onToggleAutoplay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(18.dp))

                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Continue Playing", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = if (autoplay) "Autoplaying similar music" else "Playing from your queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                    }
                }
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
                        val row: @Composable () -> Unit = {
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .alpha(if (isPast && !isDragging) 0.55f else 1f)
                                        .bounceClick(pressedScale = 0.98f) { onSkipTo(index) },
                                shape = RoundedCornerShape(28.dp),
                                shadowElevation = elevation,
                                color = if (isCurrent) colors.primaryContainer else colors.surface,
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                        BeatmapArt(beatmap, Modifier.size(80.dp), shape = RoundedCornerShape(20.dp))
                                        if (isCurrent) {
                                            EqualizerBars(
                                                isPlaying = isPlaying,
                                                color = Color.White,
                                                modifier = Modifier.size(26.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            beatmap.displayTitle(settings.preferUnicode),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (isCurrent) colors.onPrimaryContainer else colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            beatmap.displayArtist(settings.preferUnicode) +
                                                if (beatmap.mapper.isNotBlank()) " • ${beatmap.mapper}" else "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    QueueRowMenu(
                                        canRemove = !isCurrent,
                                        onRemove = { removeEntryAt(index) },
                                    )
                                    if (!locked) {
                                        Icon(
                                            at(R.drawable.at_drag_handle),
                                            contentDescription = kozuUiString("Reorder song"),
                                            tint = colors.onSurfaceVariant,
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
                                                    ).padding(horizontal = 12.dp, vertical = 14.dp)
                                                    .size(28.dp),
                                        )
                                    }
                                }
                            }
                        }
                        row()
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueKey(
    icon: ImageVector,
    description: String,
    active: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = rememberKozuHaptics()
    Surface(
        modifier =
            modifier
                .height(72.dp)
                .bounceClick(pressedScale = 0.97f) {
                    haptics.tick()
                    onClick()
                },
        shape = shape,
        color = if (active) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun QueueRowMenu(
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                at(R.drawable.at_more_vert),
                contentDescription = kozuUiString("More options"),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Remove from queue") },
                enabled = canRemove,
                leadingIcon = { Icon(at(R.drawable.at_delete), contentDescription = null) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun at(id: Int): ImageVector = ImageVector.vectorResource(id)

private class QueueEntry(
    val uid: Long,
    val beatmap: Beatmap,
)
