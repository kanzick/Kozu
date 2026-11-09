package vn.kozu.ui.settings

import vn.kozu.ui.i18n.kozuUiString
import vn.kozu.ui.components.KozuSheetHandle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.local.DownloadedSet

private const val FORGOTTEN_DAYS = 60L
private const val DAY_MS = 24L * 60L * 60L * 1000L

private enum class StorageConfirm { HISTORY, FORGOTTEN, CLEAN_ALL }

@Composable
fun StoragePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = viewModel.appContainer
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val streamBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val history by container.history.history.collectAsStateWithLifecycle(initialValue = vn.kozu.data.local.History())
    val searches by container.settings.recentSearches.collectAsStateWithLifecycle(initialValue = emptyList())

    var artworkTick by remember { mutableIntStateOf(0) }
    val artworkBytes by produceState(0L, artworkTick) {
        value = withContext(Dispatchers.IO) { SingletonImageLoader.get(context).diskCache?.size ?: 0L }
    }

    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    val lastPlayed = remember(history) {
        history.events.groupBy { it.beatmapId }.mapValues { entry -> entry.value.maxOf { it.at } }
    }
    val forgotten = remember(downloads, lastPlayed) {
        val cutoff = System.currentTimeMillis() - FORGOTTEN_DAYS * DAY_MS
        downloads
            .map { it to (lastPlayed[it.beatmap.id] ?: it.downloadedAt) }
            .filter { it.second < cutoff }
            .sortedBy { it.second }
    }
    val forgottenBytes = forgotten.sumOf { it.first.sizeBytes }
    val slimmableBytes by produceState(0L, downloads) {
        value = withContext(Dispatchers.IO) { container.downloads.slimmableBytes() }
    }
    val downloadBytes = downloads.sumOf { it.sizeBytes }
    val reclaimable = streamBytes + artworkBytes + forgottenBytes

    var confirm by remember { mutableStateOf<StorageConfirm?>(null) }
    var showForgotten by remember { mutableStateOf(false) }

    fun refreshSoon() {
        scope.launch {
            artworkTick++
            delay(900)
            viewModel.refreshCacheSize()
            artworkTick++
        }
    }

    fun clearArtwork() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val loader = SingletonImageLoader.get(context)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
            artworkTick++
        }
    }

    SettingsScaffold(title = "Storage Sense", onBack = onBack) {
        SettingsSection(null) {
            custom { shape ->
                ReclaimCard(shape, reclaimable, streamBytes, artworkBytes, forgottenBytes)
            }
        }
        SettingsSection("App cache & storage") {
            dropdown(
                Setting.STREAM_CACHE,
                settings.streamCacheMb,
                listOf(128, 256, 512, 1024, 2048),
                { sizeLabel(it) },
                description = "Using ${formatBytes(streamBytes)} for cached audio streams."
            ) { value -> viewModel.save { setStreamCacheMb(value) } }
            action(
                Setting.CLEAR_STREAM_CACHE,
                "Clear",
                description = "Delete cached audio streams. ${formatBytes(streamBytes)} in use.",
                enabled = streamBytes > 0L
            ) {
                viewModel.clearStreamCache()
                refreshSoon()
            }
            action(
                Setting.CLEAR_IMAGE_CACHE,
                "Clear",
                description = "Delete cached cover images. ${formatBytes(artworkBytes)} in use, limit ${settings.imageCacheMb} MB (set in Music).",
                enabled = artworkBytes > 0L
            ) { clearArtwork() }
            action(
                Setting.CLEAR_SEARCHES,
                "Clear",
                description = "${searches.size} saved ${if (searches.size == 1) "search" else "searches"} on this device.",
                enabled = searches.isNotEmpty(),
                destructive = true
            ) { viewModel.clearSearches() }
            action(
                Setting.CLEAR_HISTORY,
                "Clear",
                description = "${history.events.size} recorded ${if (history.events.size == 1) "play" else "plays"}. Removes your listening statistics too.",
                enabled = history.events.isNotEmpty() || history.dailyMs.isNotEmpty(),
                destructive = true
            ) { confirm = StorageConfirm.HISTORY }
        }
        SettingsSection("Downloads") {
            info(
                Setting.DOWNLOAD_LIMIT.icon,
                "Used space",
                "${formatBytes(downloadBytes)}",
                "${downloads.size} offline ${if (downloads.size == 1) "song" else "songs"} stored on this device."
            )
            dropdown(
                Setting.DOWNLOAD_LIMIT,
                settings.downloadLimitMb,
                listOf(0, 256, 512, 1024, 2048, 5120),
                { if (it == 0) "Unlimited" else sizeLabel(it) },
                description = "When a new download would exceed the limit, the least recently played downloads are removed first."
            ) { value -> viewModel.save { setDownloadLimitMb(value) } }
            action(
                Setting.SLIM_DOWNLOADS,
                "Slim down",
                description = if (slimmableBytes > 0L) {
                    "Remove hitsounds, images and storyboards from offline songs. Frees ${formatBytes(slimmableBytes)}; audio, cover and charts are kept."
                } else {
                    "Offline songs already contain only audio, cover and charts."
                },
                enabled = slimmableBytes > 0L
            ) { scope.launch { container.downloads.slimDownloads() } }
            navigation(
                Setting.FORGOTTEN_TRACKS,
                value = if (forgotten.isEmpty()) "None" else "${forgotten.size} · ${formatBytes(forgottenBytes)}",
                description = "Offline songs you have not played in $FORGOTTEN_DAYS days."
            ) { showForgotten = true }
        }
        SettingsSection("Automatic cleanup") {
            switch(Setting.AUTO_CLEAN, settings.autoCleanCache) { value ->
                viewModel.save { setAutoCleanCache(value) }
            }
            action(
                Setting.CLEAN_ALL,
                "Clean all",
                description = "Clear the stream and artwork caches in one step. Downloads, favourites and history are kept.",
                enabled = streamBytes + artworkBytes > 0L
            ) { confirm = StorageConfirm.CLEAN_ALL }
        }
    }

    if (showForgotten) {
        ForgottenSheet(
            items = forgotten,
            onDelete = { id -> scope.launch { container.downloads.delete(id) } },
            onDeleteAll = { confirm = StorageConfirm.FORGOTTEN },
            onDismiss = { showForgotten = false }
        )
    }

    when (confirm) {
        StorageConfirm.HISTORY -> ConfirmDialog(
            title = "Clear listening history?",
            text = "Every recorded play and your listening statistics will be removed from this device.",
            action = "Clear",
            onConfirm = {
                viewModel.clearHistory()
                confirm = null
            },
            onDismiss = { confirm = null }
        )
        StorageConfirm.FORGOTTEN -> ConfirmDialog(
            title = "Delete forgotten tracks?",
            text = "${forgotten.size} offline songs (${formatBytes(forgottenBytes)}) will be removed. You can download them again anytime.",
            action = "Delete all",
            onConfirm = {
                val ids = forgotten.map { it.first.beatmap.id }
                scope.launch { ids.forEach { container.downloads.delete(it) } }
                confirm = null
                showForgotten = false
            },
            onDismiss = { confirm = null }
        )
        StorageConfirm.CLEAN_ALL -> ConfirmDialog(
            title = "Clean caches?",
            text = "${formatBytes(streamBytes + artworkBytes)} of cached streams and artwork will be deleted.",
            action = "Clean",
            onConfirm = {
                viewModel.clearStreamCache()
                clearArtwork()
                refreshSoon()
                confirm = null
            },
            onDismiss = { confirm = null }
        )
        null -> Unit
    }
}

private fun sizeLabel(mb: Int): String = if (mb >= 1024 && mb % 1024 == 0) "${mb / 1024} GB" else "$mb MB"

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    action: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReclaimCard(shape: Shape, total: Long, stream: Long, artwork: Long, forgotten: Long) {
    val colors = MaterialTheme.colorScheme
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Reclaimable space", style = MaterialTheme.typography.titleSmall, color = colors.primary)
            Text(formatBytes(total), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(14.dp))
            val sum = (stream + artwork + forgotten).coerceAtLeast(1L).toFloat()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceContainerHighest),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(stream to colors.primary, artwork to colors.secondary, forgotten to colors.tertiary).forEach { (size, color) ->
                    if (size > 0L) Box(Modifier.weight(size / sum).height(12.dp).background(color))
                }
            }
            Spacer(Modifier.height(12.dp))
            Legend(colors.primary, "Stream cache", stream)
            Legend(colors.secondary, "Artwork cache", artwork)
            Legend(colors.tertiary, "Forgotten tracks", forgotten)
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, bytes: Long) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgottenSheet(
    items: List<Pair<DownloadedSet, Long>>,
    onDelete: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Forgotten tracks",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                "Offline songs not played in the last $FORGOTTEN_DAYS days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    "Nothing to review. Every offline song has been played recently.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                items.forEachIndexed { index, (set, played) ->
                    ForgottenRow(
                        shape = groupShape(index, items.size),
                        set = set,
                        daysAgo = ((System.currentTimeMillis() - played) / DAY_MS).toInt(),
                        onDelete = { onDelete(set.beatmap.id) }
                    )
                    if (index < items.lastIndex) Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete all forgotten")
                }
            }
        }
    }
}

@Composable
private fun ForgottenRow(shape: Shape, set: DownloadedSet, daysAgo: Int, onDelete: () -> Unit) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    set.beatmap.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${set.beatmap.artist} · ${formatBytes(set.sizeBytes)} · $daysAgo days ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = kozuUiString("Delete ${set.beatmap.title}"), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
