package vn.kozu.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.NamePlaylistDialog
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onSyncError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }
    var showReorderTabs by remember { mutableStateOf(false) }

    BackHandler(enabled = ui.openedPlaylist != null) { viewModel.closePlaylist() }

    LaunchedEffect(syncError) {
        syncError?.let { message ->
            onSyncError(message)
            viewModel.clearSyncError()
        }
    }

    val refreshLibrary: () -> Unit = {
        if (!isPullRefreshing && !isSyncing) {
            isPullRefreshing = true
            viewModel.syncLibrary()
            scope.launch {
                delay(PULL_REFRESH_MIN_VISIBLE_MS)
                if (viewModel.syncing.value) {
                    delay(PULL_REFRESH_MAX_VISIBLE_MS - PULL_REFRESH_MIN_VISIBLE_MS)
                }
                isPullRefreshing = false
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = {
                Surface(
                    modifier = Modifier.bounceClick(pressedScale = 0.97f) { showTabMenu = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(libraryTabIcon(ui.tab), contentDescription = null, modifier = Modifier.size(19.dp))
                            Text(ui.tab.label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Choose library tab")
                        }
                    }
            },
            actions = {
                SettingsAction(onOpenSettings)
            }
        )

        if (isSyncing && !isPullRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }

        PullToRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = refreshLibrary,
            state = pullState,
            modifier = Modifier.weight(1f),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = isPullRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
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
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(10.dp))
                    Toolbar(
                        ui = ui,
                        onPlay = {
                            val list = if (ui.tab == LibraryTab.SONGS) ui.songs else ui.liked
                            if (list.isNotEmpty()) actions.play(list, 0)
                        },
                        onShuffle = {
                            val list = if (ui.tab == LibraryTab.SONGS) ui.songs else ui.liked
                            actions.playShuffled(list)
                        },
                        onToggleGrid = viewModel::toggleGrid,
                        onSort = viewModel::selectSort,
                        onToggleDirection = viewModel::toggleDirection,
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
        }
    }

    if (showTabMenu) {
        LibraryTabsSheet(
            selected = ui.tab,
            order = ui.tabOrder,
            countFor = { tab -> libraryTabCount(ui, tab) },
            onSelect = { tab ->
                showTabMenu = false
                viewModel.selectTab(tab)
            },
            onReorder = {
                showTabMenu = false
                showReorderTabs = true
            },
            onDismiss = { showTabMenu = false }
        )
    }

    if (showReorderTabs) {
        ReorderLibraryTabsSheet(
            order = ui.tabOrder,
            onSave = { newOrder ->
                viewModel.saveTabOrder(newOrder)
                showReorderTabs = false
            },
            onDismiss = { showReorderTabs = false }
        )
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

private const val PULL_REFRESH_MIN_VISIBLE_MS = 900L
private const val PULL_REFRESH_MAX_VISIBLE_MS = 1_500L

@Composable
private fun libraryTabIcon(tab: LibraryTab): ImageVector = when (tab) {
    LibraryTab.SONGS -> Icons.Rounded.MusicNote
    LibraryTab.LIKED -> Icons.Rounded.FavoriteBorder
    LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.QueueMusic
    LibraryTab.DOWNLOADS -> Icons.Rounded.Download
}

private fun libraryTabCount(ui: LibraryUiState, tab: LibraryTab): Int = when (tab) {
    LibraryTab.SONGS -> ui.songs.size
    LibraryTab.LIKED -> ui.liked.size
    LibraryTab.PLAYLISTS -> ui.playlists.size
    LibraryTab.DOWNLOADS -> ui.downloads.size
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTabsSheet(
    selected: LibraryTab,
    order: List<LibraryTab>,
    countFor: (LibraryTab) -> Int,
    onSelect: (LibraryTab) -> Unit,
    onReorder: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Library tabs", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Jump directly to a collection or change the tab order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            order.chunked(2).forEach { rowTabs ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowTabs.forEach { tab ->
                        val isSelected = tab == selected
                        Surface(
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f).height(78.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(libraryTabIcon(tab), contentDescription = null)
                                Spacer(Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tab.label, style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(5.dp))
                                    Text(countFor(tab).toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    if (rowTabs.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Surface(
                onClick = onReorder,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DragHandle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reorder library tabs", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderLibraryTabsSheet(
    order: List<LibraryTab>,
    onSave: (List<LibraryTab>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val listState = rememberLazyListState()
    var orderedTabs by remember(order) { mutableStateOf(order) }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            orderedTabs = orderedTabs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        lazyListState = listState
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f),
        sheetGesturesEnabled = false,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Reorder library tabs",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hold and drag the handle to arrange your collections.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { orderedTabs = LibraryTab.entries.toList() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset tab order"
                                )
                            }
                            Button(
                                onClick = { onSave(orderedTabs.toList()) },
                                modifier = Modifier.heightIn(min = 52.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Done")
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orderedTabs, key = { it.key }) { tab ->
                    ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = if (isDragging) 6.dp else 0.dp,
                            color = if (isDragging) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isDragging) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = "Drag to reorder ${tab.label}",
                                    modifier = Modifier.draggableHandle()
                                )
                                Spacer(Modifier.width(16.dp))
                                Icon(
                                    libraryTabIcon(tab),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    ui: LibraryUiState,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleGrid: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onDownloadLiked: () -> Unit
) {
    var sortOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        when (ui.tab) {
            LibraryTab.PLAYLISTS -> PillButton(Icons.Rounded.Add, "New", onCreatePlaylist)
            LibraryTab.DOWNLOADS -> PillButton(Icons.Rounded.Download, "Download liked", onDownloadLiked)
            LibraryTab.SONGS, LibraryTab.LIKED -> PillButton(Icons.Rounded.Shuffle, "Shuffle", onShuffle)
        }
        Spacer(Modifier.weight(1f))
        when (ui.tab) {
            LibraryTab.SONGS, LibraryTab.LIKED -> {
                SquareButton(Icons.Rounded.PlayArrow, "Play in order", onPlay)
                SquareButton(
                    icon = if (ui.grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                    description = if (ui.grid) "Show as list" else "Show as grid",
                    onClick = onToggleGrid
                )
                SquareButton(Icons.Rounded.Sort, "Sort", onClick = { sortOpen = true })
            }
            LibraryTab.DOWNLOADS -> SquareButton(
                Icons.Rounded.Sort,
                "Sort downloads",
                onClick = { sortOpen = true }
            )
            LibraryTab.PLAYLISTS -> Unit
        }
    }

    if (sortOpen) {
        ModalBottomSheet(
            onDismissRequest = { sortOpen = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp)
            ) {
                Text("Sort by", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !ui.ascending,
                        onClick = { if (ui.ascending) onToggleDirection() },
                        label = { Text("Descending") }
                    )
                    FilterChip(
                        selected = ui.ascending,
                        onClick = { if (!ui.ascending) onToggleDirection() },
                        label = { Text("Ascending") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                LibrarySort.entries.forEachIndexed { index, option ->
                    val selected = option == ui.sort
                    Surface(
                        onClick = {
                            onSort(option)
                            sortOpen = false
                        },
                        shape = segmentedShape(index, LibrarySort.entries.size),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected")
                        }
                    }
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
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .bounceClick(onClick = onClick),
        shape = CircleShape,
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
        RefreshableEmptyState(
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
            columns = GridCells.Adaptive(minSize = 148.dp),
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tracks, key = { _, b -> "row-${b.id}" }) { index, beatmap ->
                TrackRow(
                    beatmap = beatmap,
                    compact = true,
                    shape = RoundedCornerShape(18.dp),
                    onClick = { actions.play(tracks, index) }
                )
            }
        }
    }
}

@Composable
private fun RefreshableEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        EmptyState(
            icon = icon,
            title = title,
            message = message,
            shape = shape,
            modifier = modifier,
            action = action
        )
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
        RefreshableEmptyState(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "No playlists yet",
            message = "Create a playlist, then add beatmaps from the track menu.",
            shape = KozuCookies.Bold6,
            modifier = Modifier.padding(top = 32.dp),
            action = { PillButton(Icons.Rounded.Add, "New playlist", onCreate) }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
            Surface(
                modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f) { onOpen(playlist.id) },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MosaicCover(playlist.tracks, tileSize = 22.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "beatmap" else "beatmaps"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
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
        RefreshableEmptyState(
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playlist.tracks, key = { _, b -> "pl-${b.id}" }) { index, beatmap ->
                TrackRow(
                    beatmap = beatmap,
                    compact = true,
                    shape = RoundedCornerShape(18.dp),
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
        RefreshableEmptyState(
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
                compact = true,
                shape = RoundedCornerShape(18.dp),
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
