package vn.kozu.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.LibraryDensity
import vn.kozu.domain.model.LibraryNavigationMode
import vn.kozu.domain.model.Playlist
import vn.kozu.ui.components.CoverCard
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.FitText
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalNowPlaying
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.NamePlaylistDialog
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.settings.formatBytes
import vn.kozu.ui.theme.KozuCookies
import vn.kozu.ui.theme.isCompactLayout
import kotlin.math.PI
import kotlin.math.sin

/** Pages in the infinite tab pager. Large and even, so the start page maps to any tab index. */
private const val PAGER_PAGES = 1_000_000
private const val PULL_REFRESH_MIN_VISIBLE_MS = 900L
private const val PULL_REFRESH_MAX_VISIBLE_MS = 1_500L

/** Corner used on the outside of a connected button group, and on the inside where buttons meet. */
private val GroupOuter = 24.dp
private val GroupInner = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onSyncError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val nowPlaying = LocalNowPlaying.current
    val appSettings = LocalAppSettings.current
    val compactNavigation = appSettings.libraryNavigationMode == LibraryNavigationMode.COMPACT
    val compactRows = appSettings.libraryDensity == LibraryDensity.COMPACT || isCompactLayout
    val tabCount = ui.tabOrder.size.coerceAtLeast(1)

    val initialIndex = ui.tabOrder.indexOf(ui.tab).coerceAtLeast(0)
    val pagerState =
        rememberPagerState(initialPage = PAGER_PAGES / 2 - (PAGER_PAGES / 2) % tabCount + initialIndex) { PAGER_PAGES }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }
    var showReorderTabs by remember { mutableStateOf(false) }

    // The tab shown right now follows the pager while swiping, so the header reacts immediately.
    val shownIndex = pagerState.currentPage.mod(tabCount)
    val shownTab = ui.tabOrder.getOrNull(shownIndex) ?: ui.tab

    // Jump to a tab picked from the menu by the shortest way around the infinite pager.
    LaunchedEffect(ui.tab, ui.tabOrder) {
        val target = ui.tabOrder.indexOf(ui.tab)
        if (target < 0) return@LaunchedEffect
        val current = pagerState.settledPage.mod(tabCount)
        if (target != current) {
            var delta = (target - current).mod(tabCount)
            if (delta > tabCount / 2) delta -= tabCount
            pagerState.animateScrollToPage(pagerState.settledPage + delta)
        }
    }

    // Only react once the pager has settled, otherwise a long jump would select every tab on the way.
    LaunchedEffect(pagerState, ui.tabOrder) {
        // After the tab order changes the same page index points at another tab, so realign first.
        val aligned = ui.tabOrder.indexOf(ui.tab)
        if (aligned >= 0 && aligned != pagerState.settledPage.mod(tabCount)) {
            var delta = (aligned - pagerState.settledPage.mod(tabCount)).mod(tabCount)
            if (delta > tabCount / 2) delta -= tabCount
            pagerState.scrollToPage(pagerState.settledPage + delta)
        }
        snapshotFlow { pagerState.settledPage }.collect { page ->
            ui.tabOrder.getOrNull(page.mod(tabCount))?.let { tab ->
                if (tab != ui.tab) viewModel.selectTab(tab)
            }
        }
    }

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

    val surface = MaterialTheme.colorScheme.surface
    val headerTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f).compositeOver(surface)

    Column(modifier.fillMaxSize().background(headerTint)) {
        LibraryHeader(
            compact = compactNavigation,
            tab = shownTab,
            sheetOpen = showTabMenu,
            isSyncing = isSyncing,
            onOpenTabs = { showTabMenu = true },
            onRefresh = refreshLibrary,
            onOpenSettings = onOpenSettings,
        )

        if (compactNavigation && ui.openedPlaylist == null) {
            PageIndicator(
                selectedIndex = shownIndex,
                pageCount = ui.tabOrder.size,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }

        // The content sits on a rounded surface that overlaps the tinted header, like PixelMusic.
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                SyncWave(visible = isSyncing && !isPullRefreshing)

                PullToRefreshBox(
                    isRefreshing = isPullRefreshing,
                    onRefresh = refreshLibrary,
                    state = pullState,
                    modifier = Modifier.weight(1f),
                    indicator = {
                        RefreshIndicator(
                            state = pullState,
                            isRefreshing = isPullRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    },
                ) {
                    val opened = ui.openedPlaylist
                    if (opened != null) {
                        PlaylistDetail(
                            playlist = opened,
                            bottomPadding = bottomPadding,
                            compactRows = compactRows,
                            onBack = viewModel::closePlaylist,
                            onRename = { viewModel.renamePlaylist(opened.id, it) },
                            onDelete = { viewModel.deletePlaylist(opened.id) },
                            onRemoveTrack = { viewModel.removeFromPlaylist(opened.id, it) },
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (!compactNavigation) {
                                LibraryTabRow(
                                    selected = ui.tab,
                                    order = ui.tabOrder,
                                    onSelect = viewModel::selectTab,
                                    onReorder = { showReorderTabs = true },
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                                )
                            }

                            val pageListStates =
                                remember(ui.tabOrder, listState) {
                                    ui.tabOrder.associateWith { tab ->
                                        if (tab == LibraryTab.SONGS) listState else LazyListState()
                                    }
                                }
                            val activeList = if (ui.tab == LibraryTab.LIKED) ui.liked else ui.songs
                            val locateIndex =
                                if ((ui.tab == LibraryTab.SONGS || ui.tab == LibraryTab.LIKED) && !ui.grid) {
                                    activeList.indexOfFirst { it.id == nowPlaying.beatmapId }.takeIf { it >= 0 }
                                } else {
                                    null
                                }

                            LibraryActionRow(
                                ui = ui,
                                canLocate = locateIndex != null,
                                onLocate = {
                                    val state = pageListStates[ui.tab]
                                    if (state != null && locateIndex != null) {
                                        scope.launch { state.animateScrollToItem(locateIndex) }
                                    }
                                },
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
                                onDownloadLiked = { actions.downloadAll(ui.liked) },
                            )
                            Spacer(Modifier.height(10.dp))

                            val renderTab: @Composable (LibraryTab) -> Unit = { tab ->
                                when (tab) {
                                    LibraryTab.LIKED ->
                                        TrackCollection(
                                            tracks = ui.liked,
                                            grid = ui.grid,
                                            compactRows = compactRows,
                                            listState = pageListStates.getValue(tab),
                                            bottomPadding = bottomPadding,
                                            emptyIcon = Icons.Rounded.FavoriteBorder,
                                            emptyTitle = "Nothing liked yet",
                                            emptyMessage = "Tap the heart on any song and it will live here.",
                                        )

                                    LibraryTab.SONGS ->
                                        TrackCollection(
                                            tracks = ui.songs,
                                            grid = ui.grid,
                                            compactRows = compactRows,
                                            listState = pageListStates.getValue(tab),
                                            bottomPadding = bottomPadding,
                                            emptyIcon = Icons.Rounded.History,
                                            emptyTitle = "No songs played yet",
                                            emptyMessage = "Everything you listen to will be collected here.",
                                        )

                                    LibraryTab.PLAYLISTS ->
                                        PlaylistGrid(
                                            playlists = ui.playlists,
                                            bottomPadding = bottomPadding,
                                            onOpen = viewModel::openPlaylist,
                                            onCreate = { showCreate = true },
                                        )

                                    LibraryTab.DOWNLOADS ->
                                        DownloadsPane(
                                            ui = ui,
                                            bottomPadding = bottomPadding,
                                            onDelete = viewModel::deleteDownload,
                                            onDismiss = viewModel::dismissDownload,
                                            onCancel = viewModel::cancelDownload,
                                        )
                                }
                            }

                            if (compactNavigation) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.weight(1f),
                                ) { page ->
                                    ui.tabOrder.getOrNull(page.mod(tabCount))?.let { tab -> renderTab(tab) }
                                }
                            } else {
                                renderTab(ui.tab)
                            }
                        }
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
            onDismiss = { showTabMenu = false },
        )
    }

    if (showReorderTabs) {
        ReorderLibraryTabsSheet(
            order = ui.tabOrder,
            onSave = { newOrder ->
                viewModel.saveTabOrder(newOrder)
                showReorderTabs = false
            },
            onDismiss = { showReorderTabs = false },
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
            },
        )
    }
}

@Composable
private fun libraryTabIcon(tab: LibraryTab): ImageVector =
    when (tab) {
        LibraryTab.SONGS -> Icons.Rounded.MusicNote
        LibraryTab.LIKED -> Icons.Rounded.FavoriteBorder
        LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.QueueMusic
        LibraryTab.DOWNLOADS -> Icons.Rounded.Download
    }

private fun libraryTabCount(
    ui: LibraryUiState,
    tab: LibraryTab,
): Int =
    when (tab) {
        LibraryTab.SONGS -> ui.songs.size
        LibraryTab.LIKED -> ui.liked.size
        LibraryTab.PLAYLISTS -> ui.playlists.size
        LibraryTab.DOWNLOADS -> ui.downloads.size
    }

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

/**
 * Library header. In compact mode the title is a split button: the left part shows the current tab and
 * the right part opens the tab sheet, its chevron turning while the sheet is open.
 */
@Composable
private fun LibraryHeader(
    compact: Boolean,
    tab: LibraryTab,
    sheetOpen: Boolean,
    isSyncing: Boolean,
    onOpenTabs: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (compact) {
                SplitTitle(tab = tab, sheetOpen = sheetOpen, onClick = onOpenTabs)
            } else {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        HeaderAction(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Sync library",
            onClick = onRefresh,
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurface,
            rotating = isSyncing,
        )
        SettingsAction(onOpenSettings)
    }
}

@Composable
private fun SplitTitle(
    tab: LibraryTab,
    sheetOpen: Boolean,
    onClick: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    val chevron by animateFloatAsState(
        targetValue = if (sheetOpen) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "libraryChevron",
    )
    val titleSource = remember { MutableInteractionSource() }
    val chevronSource = remember { MutableInteractionSource() }
    val titlePressed by titleSource.collectIsPressedAsState()
    val chevronPressed by chevronSource.collectIsPressedAsState()
    val spec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    // The facing corners of the two halves round up while pressed, like a connected button group.
    val titleInner by animateDpAsState(if (titlePressed) 26.dp else 6.dp, spec, label = "titleInner")
    val chevronInner by animateDpAsState(if (chevronPressed) 26.dp else 6.dp, spec, label = "chevronInner")

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onClick,
            interactionSource = titleSource,
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = titleInner, bottomEnd = titleInner),
            color = container,
            contentColor = content,
            modifier = Modifier.heightIn(min = 52.dp).animateContentSize(),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(170)) + slideInVertically(tween(200)) { it / 3 }) togetherWith
                        (fadeOut(tween(110)) + slideOutVertically(tween(160)) { -it / 3 })
                },
                label = "libraryTitle",
            ) { current ->
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(libraryTabIcon(current), contentDescription = null, modifier = Modifier.size(22.dp))
                    FitText(current.label, style = MaterialTheme.typography.titleLarge, color = content, minScale = 0.7f)
                }
            }
        }
        Surface(
            onClick = onClick,
            interactionSource = chevronSource,
            shape = RoundedCornerShape(topStart = chevronInner, bottomStart = chevronInner, topEnd = 28.dp, bottomEnd = 28.dp),
            color = container,
            contentColor = content,
            modifier = Modifier.size(width = 52.dp, height = 52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Choose library tab",
                    modifier = Modifier.graphicsLayer { rotationZ = chevron },
                )
            }
        }
    }
}

/** Segmented dashes under the header; the selected one stretches and takes the accent colour. */
@Composable
private fun PageIndicator(
    selectedIndex: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == selectedIndex.mod(pageCount)
            val width by animateDpAsState(
                targetValue = if (selected) 26.dp else 10.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                label = "dashWidth",
            )
            val color by animateColorAsState(
                targetValue =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                label = "dashColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.5.dp)
                    .size(width = width, height = 4.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Animated wavy line shown under the header while the library syncs. */
@Composable
private fun SyncWave(visible: Boolean) {
    val performance = LocalPerformanceMode.current
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(300))) {
        val transition = rememberInfiniteTransition(label = "syncWave")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "syncWavePhase",
        )
        val color = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val stroke = 3.dp.toPx()
            val amp = 3.dp.toPx()
            val waveLength = 30.dp.toPx()
            val shift = if (performance) 0f else phase
            val path = Path()
            var x = 0f
            path.moveTo(0f, size.height / 2f + amp * sin(shift))
            while (x < size.width) {
                x = (x + 4f).coerceAtMost(size.width)
                path.lineTo(x, size.height / 2f + amp * sin(x / waveLength * 2f * PI.toFloat() - shift))
            }
            drawPath(path, color.copy(alpha = 0.7f), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Action row
// ---------------------------------------------------------------------------------------------

private class GroupButton(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
)

@Composable
private fun LibraryActionRow(
    ui: LibraryUiState,
    canLocate: Boolean,
    onLocate: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleGrid: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onDownloadLiked: () -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    val playIcon = playbackIconVector(LocalAppSettings.current.playbackIcon, false)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (ui.tab) {
            LibraryTab.PLAYLISTS -> PillButton(Icons.Rounded.Add, "New", onCreatePlaylist)
            LibraryTab.DOWNLOADS -> PillButton(Icons.Rounded.Download, "Download liked", onDownloadLiked)
            LibraryTab.SONGS, LibraryTab.LIKED ->
                PillButton(
                    icon = Icons.Rounded.Shuffle,
                    label = "Shuffle",
                    onClick = onShuffle,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
        }
        Spacer(Modifier.weight(1f))

        val buttons =
            when (ui.tab) {
                LibraryTab.SONGS, LibraryTab.LIKED ->
                    buildList {
                        if (canLocate) add(GroupButton(Icons.Rounded.MyLocation, "Show current song", onLocate))
                        add(GroupButton(playIcon, "Play in order", onPlay))
                        add(
                            GroupButton(
                                icon = if (ui.grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                description = if (ui.grid) "Show as list" else "Show as grid",
                                onClick = onToggleGrid,
                            ),
                        )
                        add(GroupButton(Icons.Rounded.Sort, "Sort") { sortOpen = true })
                    }

                LibraryTab.DOWNLOADS -> listOf(GroupButton(Icons.Rounded.Sort, "Sort downloads") { sortOpen = true })
                LibraryTab.PLAYLISTS -> emptyList()
            }
        ConnectedButtonGroup(buttons)
    }

    if (sortOpen) {
        SortSheet(
            ui = ui,
            onSort = {
                onSort(it)
                sortOpen = false
            },
            onToggleDirection = onToggleDirection,
            onDismiss = { sortOpen = false },
        )
    }
}

/** Icon buttons joined into one group: rounded on the outside, tight inside, and each rounds up while pressed. */
@Composable
private fun ConnectedButtonGroup(buttons: List<GroupButton>) {
    if (buttons.isEmpty()) return
    Row(
        modifier = Modifier.animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buttons.forEachIndexed { index, button ->
            key(button.description) {
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val spec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
                val start by animateDpAsState(
                    targetValue = if (pressed || index == 0) GroupOuter else GroupInner,
                    animationSpec = spec,
                    label = "groupStart",
                )
                val end by animateDpAsState(
                    targetValue = if (pressed || index == buttons.lastIndex) GroupOuter else GroupInner,
                    animationSpec = spec,
                    label = "groupEnd",
                )
                Surface(
                    onClick = button.onClick,
                    interactionSource = source,
                    modifier = Modifier.size(width = 46.dp, height = 44.dp),
                    shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(button.icon, contentDescription = button.description, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = Modifier.bounceClick(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    ui: LibraryUiState,
    onSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text("Sort by", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))

            // Big direction tile; the arrow flips with a spring when toggled.
            val arrow by animateFloatAsState(
                targetValue = if (ui.ascending) 0f else 180f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "sortArrow",
            )
            Surface(
                onClick = onToggleDirection,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { rotationZ = arrow },
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Order", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (ui.ascending) "Ascending" else "Descending",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LibrarySort.entries.forEachIndexed { index, option ->
                val selected = option == ui.sort
                Surface(
                    onClick = { onSort(option) },
                    shape = segmentedShape(index, LibrarySort.entries.size, large = 22.dp, small = 6.dp),
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        RadioButton(selected = selected, onClick = { onSort(option) })
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tab sheets
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTabsSheet(
    selected: LibraryTab,
    order: List<LibraryTab>,
    countFor: (LibraryTab) -> Int,
    onSelect: (LibraryTab) -> Unit,
    onReorder: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Library tabs", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Jump directly to any tab or reorder them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            order.chunked(2).forEach { rowTabs ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowTabs.forEach { tab ->
                        val isSelected = tab == selected
                        val container by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "tabTile",
                        )
                        Surface(
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f).heightIn(min = 112.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = container,
                            contentColor =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                    contentColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(libraryTabIcon(tab), contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FitText(tab.label, style = MaterialTheme.typography.titleSmall, minScale = 0.75f)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        countFor(tab).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalContentColor.current.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                    if (rowTabs.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    onClick = onReorder,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reorder tabs", style = MaterialTheme.typography.titleSmall)
                    }
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
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    val listState = rememberLazyListState()
    var orderedTabs by remember(order) { mutableStateOf(order) }
    var confirmReset by remember { mutableStateOf(false) }
    val reorderableState =
        rememberReorderableLazyListState(
            onMove = { from, to ->
                orderedTabs =
                    orderedTabs.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
            },
            lazyListState = listState,
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
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text("Reorder library tabs", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Drag a tab to change the order in the library selector.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            bottomBar = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { confirmReset = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset tab order")
                            }
                            Button(
                                onClick = { onSave(orderedTabs.toList()) },
                                modifier = Modifier.heightIn(min = 52.dp),
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Done")
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(orderedTabs, key = { it.key }) { tab ->
                    ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                        val container by animateColorAsState(
                            if (isDragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "reorderRow",
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = if (isDragging) 6.dp else 0.dp,
                            color = container,
                            contentColor =
                                if (isDragging) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.DragIndicator,
                                    contentDescription = "Drag to reorder ${tab.label}",
                                    modifier = Modifier.draggableHandle(),
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(libraryTabIcon(tab), contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(tab.label, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset order") },
            text = { Text("Reset tab order to the default?") },
            confirmButton = {
                TextButton(onClick = {
                    orderedTabs = LibraryTab.entries.toList()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryTabRow(
    selected: LibraryTab,
    order: List<LibraryTab>,
    onSelect: (LibraryTab) -> Unit,
    onReorder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        order.forEach { tab ->
            FilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
                leadingIcon = { Icon(libraryTabIcon(tab), contentDescription = null) },
            )
        }
        IconButton(onClick = onReorder) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = "Reorder library tabs")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Collections
// ---------------------------------------------------------------------------------------------

@Composable
private fun TrackCollection(
    tracks: List<Beatmap>,
    grid: Boolean,
    compactRows: Boolean,
    listState: LazyListState,
    bottomPadding: Dp,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
) {
    val actions = LocalTrackActions.current

    if (tracks.isEmpty()) {
        RefreshableEmptyState(
            icon = emptyIcon,
            title = emptyTitle,
            message = emptyMessage,
            shape = KozuCookies.Soft9,
            modifier = Modifier.padding(top = 32.dp),
        )
        return
    }

    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (compactRows) 148.dp else 164.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(if (compactRows) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactRows) 8.dp else 16.dp),
        ) {
            itemsIndexed(tracks, key = { _, b -> "grid-${b.id}" }) { index, beatmap ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoverCard(
                        beatmap = beatmap,
                        size = if (compactRows) 136.dp else 156.dp,
                        shape = RoundedCornerShape(if (compactRows) 28.dp else 34.dp),
                        onClick = { actions.play(tracks, index) },
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(if (compactRows) 4.dp else 10.dp),
        ) {
            itemsIndexed(tracks, key = { _, b -> "row-${b.id}" }) { index, beatmap ->
                TrackRow(
                    beatmap = beatmap,
                    compact = compactRows,
                    shape = RoundedCornerShape(if (compactRows) 16.dp else 22.dp),
                    onClick = { actions.play(tracks, index) },
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
    action: @Composable (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        EmptyState(
            icon = icon,
            title = title,
            message = message,
            shape = shape,
            modifier = modifier,
            action = action,
        )
    }
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    bottomPadding: Dp,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
) {
    if (playlists.isEmpty()) {
        RefreshableEmptyState(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "No playlists yet",
            message = "Create a playlist, then add beatmaps from the track menu.",
            shape = KozuCookies.Bold6,
            modifier = Modifier.padding(top = 32.dp),
            action = { PillButton(Icons.Rounded.Add, "New playlist", onCreate) },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
            Surface(
                modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f) { onOpen(playlist.id) },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MosaicCover(playlist.tracks, tileSize = 22.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "beatmap" else "beatmaps"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    compactRows: Boolean,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
) {
    val actions = LocalTrackActions.current
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier =
                    Modifier
                        .size(48.dp)
                        .bounceClick(onClick = onBack),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showRename = true }) { Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist") }
            IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist") }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                icon = playbackIconVector(LocalAppSettings.current.playbackIcon, false),
                label = "Play",
                onClick = { actions.play(playlist.tracks, 0) },
            )
            PillButton(
                icon = Icons.Rounded.Shuffle,
                label = "Shuffle",
                onClick = { actions.playShuffled(playlist.tracks) },
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            PillButton(
                icon = Icons.Rounded.Download,
                label = "Offline",
                onClick = { actions.downloadAll(playlist.tracks) },
            )
        }

        if (playlist.tracks.isEmpty()) {
            RefreshableEmptyState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "Empty playlist",
                message = "Use the \u22EE menu on any song and choose \u201CAdd to playlist\u201D.",
                shape = KozuCookies.Flower8,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(playlist.tracks, key = { _, b -> "pl-${b.id}" }) { index, beatmap ->
                    TrackRow(
                        beatmap = beatmap,
                        compact = compactRows,
                        shape = RoundedCornerShape(if (compactRows) 16.dp else 22.dp),
                        onClick = { actions.play(playlist.tracks, index) },
                        trailing = {
                            IconButton(onClick = { onRemoveTrack(beatmap.id) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove from playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
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
            },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("\u201C${playlist.name}\u201D will be removed. Your songs are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DownloadsPane(
    ui: LibraryUiState,
    bottomPadding: Dp,
    onDelete: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onCancel: (Long) -> Unit,
) {
    val actions = LocalTrackActions.current
    val queue =
        ui.queue.entries.filter { entry ->
            ui.downloads.none { it.beatmap.id == entry.key } || entry.value !is DownloadStatus.Queued
        }
    val tracks = ui.downloads.map { it.beatmap }

    if (queue.isEmpty() && ui.downloads.isEmpty()) {
        RefreshableEmptyState(
            icon = Icons.Rounded.Download,
            title = "Nothing offline yet",
            message = "Download songs from the \u22EE menu to play them without internet and use them in game mode.",
            shape = KozuCookies.Clover4,
            modifier = Modifier.padding(top = 32.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        item(key = "summary") {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DownloadDone, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "${ui.downloads.size} ${if (ui.downloads.size == 1) "song" else "songs"} offline",
                            style = MaterialTheme.typography.titleMedium,
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
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = ui.queueTitles[entry.key]?.takeIf { it.isNotBlank() } ?: "Beatmap #${entry.key}",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when (status) {
                            is DownloadStatus.Queued ->
                                Text(
                                    "Waiting in queue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                            is DownloadStatus.Running -> {
                                Text(
                                    "Downloading \u00B7 ${(status.progress * 100).toInt()}% \u00B7 ${status.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                LinearProgressIndicator(
                                    progress = { status.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                )
                            }

                            is DownloadStatus.Failed ->
                                Text(
                                    status.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (status is DownloadStatus.Failed) onDismiss(entry.key) else onCancel(entry.key)
                        },
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = if (status is DownloadStatus.Failed) "Dismiss" else "Cancel download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (queue.isNotEmpty() && ui.downloads.isNotEmpty()) {
            item(key = "queue-gap") { Spacer(Modifier.height(8.dp)) }
        }
        itemsIndexed(tracks, key = { _, b -> "dl-${b.id}" }) { index, beatmap ->
            TrackRow(
                beatmap = beatmap,
                compact = isCompactLayout,
                shape = segmentedShape(index, tracks.size, large = 22.dp, small = 6.dp),
                onClick = { actions.play(tracks, index) },
                trailing = {
                    IconButton(onClick = { onDelete(beatmap.id) }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}
