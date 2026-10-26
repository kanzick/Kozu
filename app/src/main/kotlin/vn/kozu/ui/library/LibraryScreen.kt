package vn.kozu.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import vn.kozu.ui.components.CoverCard
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.NamePlaylistDialog
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies

@Composable
fun LibraryScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    var showCreate by remember { mutableStateOf(false) }

    BackHandler(enabled = ui.openedPlaylist != null) { viewModel.closePlaylist() }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = { HeaderTitle("Library") },
            actions = { SettingsAction(onOpenSettings) }
        )

        val opened = ui.openedPlaylist
        if (opened != null) {
            PlaylistDetail(
                playlist = opened,
                bottomPadding = bottomPadding,
                onBack = viewModel::closePlaylist,
                onRename = { viewModel.renamePlaylist(opened.id, it) },
                onDelete = { viewModel.deletePlaylist(opened.id) },
                onRemoveTrack = { viewModel.removeFromPlaylist(opened.id, it) }
            )
        } else {
            Spacer(Modifier.height(16.dp))
            TabRow(selected = ui.tab, onSelect = viewModel::selectTab)
            Spacer(Modifier.height(14.dp))
            Toolbar(
                ui = ui,
                onShuffle = {
                    val list = if (ui.tab == LibraryTab.SONGS) ui.songs else ui.liked
                    actions.playShuffled(list)
                },
                onToggleGrid = viewModel::toggleGrid,
                onSort = viewModel::selectSort,
                onCreatePlaylist = { showCreate = true },
                onDownloadLiked = { actions.downloadAll(ui.liked) }
            )
            Spacer(Modifier.height(10.dp))

            when (ui.tab) {
                LibraryTab.LIKED -> TrackCollection(
                    tracks = ui.liked,
                    grid = ui.grid,
                    listState = listState,
                    bottomPadding = bottomPadding,
                    emptyIcon = Icons.Rounded.FavoriteBorder,
                    emptyTitle = "Nothing liked yet",
                    emptyMessage = "Tap the heart on any song and it will live here."
                )
                LibraryTab.SONGS -> TrackCollection(
                    tracks = ui.songs,
                    grid = ui.grid,
                    listState = listState,
                    bottomPadding = bottomPadding,
                    emptyIcon = Icons.Rounded.History,
                    emptyTitle = "No songs played yet",
                    emptyMessage = "Everything you listen to will be collected here."
                )
                LibraryTab.PLAYLISTS -> PlaylistGrid(
                    playlists = ui.playlists,
                    bottomPadding = bottomPadding,
                    onOpen = viewModel::openPlaylist,
                    onCreate = { showCreate = true }
                )
                LibraryTab.DOWNLOADS -> DownloadsPane(
                    ui = ui,
                    bottomPadding = bottomPadding,
                    onDelete = viewModel::deleteDownload,
                    onDismiss = viewModel::dismissDownload,
                    onCancel = viewModel::cancelDownload
                )
            }
        }
    }

    if (showCreate) {
        NamePlaylistDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = {
                showCreate = false
                viewModel.createPlaylist(it)
            }
        )
    }
}

@Composable
private fun TabRow(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                modifier = Modifier.bounceClick { onSelect(tab) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = tab.label,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun Toolbar(
    ui: LibraryUiState,
    onShuffle: () -> Unit,
    onToggleGrid: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDownloadLiked: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (ui.tab == LibraryTab.PLAYLISTS) {
            PillButton(Icons.Rounded.Add, "New playlist", onCreatePlaylist)
            return@Row
        }
        if (ui.tab == LibraryTab.DOWNLOADS) {
            PillButton(Icons.Rounded.Download, "Download liked", onDownloadLiked)
            return@Row
        }

        PillButton(Icons.Rounded.Shuffle, "Shuffle", onShuffle)
        Spacer(Modifier.weight(1f))
        SquareButton(
            icon = if (ui.grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
            description = if (ui.grid) "Show as list" else "Show as grid",
            onClick = onToggleGrid
        )
        Spacer(Modifier.width(8.dp))
        Box {
            SquareButton(Icons.AutoMirrored.Rounded.Sort, "Sort", onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                color = if (option == ui.sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSort(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.bounceClick(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrackCollection(
    tracks: List<Beatmap>,
    grid: Boolean,
    listState: LazyListState,
    bottomPadding: Dp,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String
) {
    val actions = LocalTrackActions.current

    if (tracks.isEmpty()) {
        EmptyState(
            icon = emptyIcon,
            title = emptyTitle,
            message = emptyMessage,
            shape = KozuCookies.Soft9,
            modifier = Modifier.padding(top = 32.dp)
        )
        return
    }

    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(tracks, key = { _, b -> "grid-${b.id}" }) { index, beatmap ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoverCard(
                        beatmap = beatmap,
                        size = 156.dp,
                        shape = RoundedCornerShape(34.dp),
                        onClick = { actions.play(tracks, index) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(tracks, key = { _, b -> "row-${b.id}" }) { index, beatmap ->
                TrackRow(
                    beatmap = beatmap,
                    shape = segmentedShape(index, tracks.size),
                    onClick = { actions.play(tracks, index) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    bottomPadding: Dp,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "No playlists yet",
            message = "Create one, then add songs from the ⋮ menu on any track.",
            shape = KozuCookies.Bold6,
            modifier = Modifier.padding(top = 32.dp),
            action = {
                PillButton(Icons.Rounded.Add, "Create playlist", onCreate)
            }
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(pressedScale = 0.96f) { onOpen(playlist.id) },
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MosaicCover(playlist.tracks, tileSize = 58.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: Playlist,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (Long) -> Unit
) {
    val actions = LocalTrackActions.current
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .bounceClick(onClick = onBack),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { showRename = true }) { Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist") }
        IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist") }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PillButton(Icons.Rounded.PlayArrow, "Play") { actions.play(playlist.tracks, 0) }
        PillButton(Icons.Rounded.Shuffle, "Shuffle") { actions.playShuffled(playlist.tracks) }
        PillButton(Icons.Rounded.Download, "Offline") { actions.downloadAll(playlist.tracks) }
    }

    if (playlist.tracks.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "Empty playlist",
            message = "Use the ⋮ menu on any song and choose “Add to playlist”.",
            shape = KozuCookies.Flower8,
            modifier = Modifier.padding(top = 24.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(playlist.tracks, key = { _, b -> "pl-${b.id}" }) { index, beatmap ->
                TrackRow(
                    beatmap = beatmap,
                    shape = segmentedShape(index, playlist.tracks.size),
                    onClick = { actions.play(playlist.tracks, index) },
                    trailing = {
                        IconButton(onClick = { onRemoveTrack(beatmap.id) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove from playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }

    if (showRename) {
        NamePlaylistDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initial = playlist.name,
            onDismiss = { showRename = false },
            onConfirm = {
                showRename = false
                onRename(it)
            }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("“${playlist.name}” will be removed. Your songs are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}


@Composable
private fun DownloadsPane(
    ui: LibraryUiState,
    bottomPadding: Dp,
    onDelete: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onCancel: (Long) -> Unit
) {
    val actions = LocalTrackActions.current
    val queue = ui.queue.entries.filter { entry -> ui.downloads.none { it.beatmap.id == entry.key } || entry.value !is DownloadStatus.Queued }
    val tracks = ui.downloads.map { it.beatmap }

    if (queue.isEmpty() && ui.downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Download,
            title = "Nothing offline yet",
            message = "Download songs from the ⋮ menu to play them without internet and use them in game mode.",
            shape = KozuCookies.Clover4,
            modifier = Modifier.padding(top = 32.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item(key = "summary") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DownloadDone, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "${ui.downloads.size} ${if (ui.downloads.size == 1) "song" else "songs"} offline",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(formatBytes(ui.downloadedBytes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        itemsIndexed(queue, key = { _, entry -> "q-${entry.key}" }) { index, entry ->
            val status = entry.value
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = segmentedShape(index, queue.size),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ui.queueTitles[entry.key].orEmpty().ifBlank { "Song #${entry.key}" },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        when (status) {
                            is DownloadStatus.Running -> {
                                Text(
                                    status.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                if (status.progress < 0f) LinearProgressIndicator(Modifier.fillMaxWidth())
                                else LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
                            }
                            is DownloadStatus.Queued -> Text(
                                "Queued",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            is DownloadStatus.Failed -> Text(
                                status.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { if (status is DownloadStatus.Failed) onDismiss(entry.key) else onCancel(entry.key) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                    }
                }
            }
        }
        if (queue.isNotEmpty()) item(key = "gap") { Spacer(Modifier.height(10.dp)) }
        itemsIndexed(ui.downloads, key = { _, set -> "d-${set.beatmap.id}" }) { index, set ->
            TrackRow(
                beatmap = set.beatmap,
                shape = segmentedShape(index, ui.downloads.size),
                onClick = { actions.play(tracks, index) },
                trailing = {
                    IconButton(onClick = { onDelete(set.beatmap.id) }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "${bytes / 1024} KB"
}
