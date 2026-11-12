package vn.kozu.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.LibraryDensity
import vn.kozu.domain.model.LibraryNavigationMode
import vn.kozu.domain.model.Playlist
import vn.kozu.R
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.CoverCard
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.FitText
import vn.kozu.ui.components.KozuSheetHandle
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalNowPlaying
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.MosaicCover
import vn.kozu.ui.components.NamePlaylistDialog
import vn.kozu.ui.components.NewsAction
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.softTopEdge
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.settings.formatBytes
import vn.kozu.ui.theme.isCompactLayout
import vn.kozu.ui.theme.KozuCookies

private const val PAGER_PAGES = 1_000_000
private const val PULL_REFRESH_MIN_VISIBLE_MS = 900L
private const val PULL_REFRESH_MAX_VISIBLE_MS = 1_500L


private val GroupOuter = 24.dp
private val GroupInner = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
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


    val shownIndex = pagerState.currentPage.mod(tabCount)
    val shownTab = ui.tabOrder.getOrNull(shownIndex) ?: ui.tab


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


    LaunchedEffect(pagerState, ui.tabOrder) {

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
            onOpenTabs = { showTabMenu = true },
            onOpenSettings = onOpenSettings,
            onOpenNews = onOpenNews,
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
                            sortUi = ui,
                            bottomPadding = bottomPadding,
                            compactRows = compactRows,
                            onBack = viewModel::closePlaylist,
                            onRename = { viewModel.renamePlaylist(opened.id, it) },
                            onDelete = { viewModel.deletePlaylist(opened.id) },
                            onRemoveTrack = { viewModel.removeFromPlaylist(opened.id, it) },
                            onReorder = { ids -> viewModel.reorderTracks(LibraryTab.PLAYLISTS, ids, opened.id) },
                            onSort = viewModel::selectSort,
                            onToggleDirection = viewModel::toggleDirection,
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(8.dp))
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
                                            emptyTitle = stringResource(R.string.nothing_liked_yet),
                                            emptyMessage = stringResource(R.string.tap_the_heart_on_any_song_and_it_will_live_here),
                                            onReorder = { ids -> viewModel.reorderTracks(LibraryTab.LIKED, ids) },
                                        )

                                    LibraryTab.SONGS ->
                                        TrackCollection(
                                            tracks = ui.songs,
                                            grid = ui.grid,
                                            compactRows = compactRows,
                                            listState = pageListStates.getValue(tab),
                                            bottomPadding = bottomPadding,
                                            emptyIcon = Icons.Rounded.History,
                                            emptyTitle = stringResource(R.string.no_songs_played_yet),
                                            emptyMessage = stringResource(R.string.everything_you_listen_to_will_be_collected_here),
                                            onReorder = { ids -> viewModel.reorderTracks(LibraryTab.SONGS, ids) },
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
                                            onReorder = { ids -> viewModel.reorderTracks(LibraryTab.DOWNLOADS, ids) },
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
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
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


@Composable
private fun LibraryHeader(
    compact: Boolean,
    tab: LibraryTab,
    sheetOpen: Boolean,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
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
                Text(stringResource(R.string.library),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        NewsAction(onOpenNews)
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
                    FitText(stringResource(current.labelRes), style = MaterialTheme.typography.titleLarge, color = content, minScale = 0.7f)
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
                    contentDescription = stringResource(R.string.choose_library_tab),
                    modifier = Modifier.graphicsLayer { rotationZ = chevron },
                )
            }
        }
    }
}


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
            LibraryTab.PLAYLISTS -> PillButton(Icons.Rounded.Add, stringResource(R.string.new_label), onCreatePlaylist)
            LibraryTab.DOWNLOADS -> PillButton(Icons.Rounded.Download, stringResource(R.string.download_liked), onDownloadLiked)
            LibraryTab.SONGS, LibraryTab.LIKED ->
                PillButton(
                    icon = Icons.Rounded.Shuffle,
                    label = stringResource(R.string.shuffle),
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
                        if (canLocate) add(GroupButton(Icons.Rounded.MyLocation, stringResource(R.string.show_current_song), onLocate))
                        add(GroupButton(playIcon, stringResource(R.string.play_in_order), onPlay))
                        add(
                            GroupButton(
                                icon = if (ui.grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                description = stringResource(if (ui.grid) R.string.show_as_list else R.string.show_as_grid),
                                onClick = onToggleGrid,
                            ),
                        )
                        add(GroupButton(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort)) { sortOpen = true })
                    }

                LibraryTab.DOWNLOADS -> listOf(GroupButton(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_downloads)) { sortOpen = true })
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
        dragHandle = { KozuSheetHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.sort_by),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )

            if (ui.sort != LibrarySort.CUSTOM) {
                val arrow by animateFloatAsState(
                    targetValue = if (ui.ascending) 0f else 180f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                    label = "sortArrow",
                )
                Surface(
                    onClick = onToggleDirection,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(14.dp),
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
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(stringResource(R.string.order), style = MaterialTheme.typography.labelSmall)
                            Text(if (ui.ascending) stringResource(R.string.ascending) else stringResource(R.string.descending),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.drag_songs_to_set_your_own_order),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LibrarySort.entries.forEachIndexed { index, option ->
                val selected = option == ui.sort
                Surface(
                    onClick = { onSort(option) },
                    shape = segmentedShape(index, LibrarySort.entries.size, large = 20.dp, small = 6.dp),
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(option.labelRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        RadioButton(selected = selected, onClick = { onSort(option) })
                    }
                }
            }
        }
    }
}


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
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f),
        dragHandle = { KozuSheetHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(stringResource(R.string.library_tabs), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.choose_a_tab_or_change_its_order),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }


            order.chunked(2).forEach { rowTabs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowTabs.forEach { tab ->
                        val isSelected = tab == selected
                        val container by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "tabTile",
                        )
                        Surface(
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f).height(96.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = container,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(libraryTabIcon(tab), contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FitText(stringResource(tab.labelRes), style = MaterialTheme.typography.titleSmall, minScale = 0.75f)
                                    Spacer(Modifier.width(5.dp))
                                    Text(countFor(tab).toString(),
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

            Surface(
                onClick = onReorder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reorder_tabs), style = MaterialTheme.typography.titleSmall)
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
        dragHandle = { KozuSheetHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(stringResource(R.string.reorder_library_tabs), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.drag_a_tab_to_change_its_order),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(orderedTabs, key = { it.key }) { tab ->
                    ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                        val container by animateColorAsState(
                            if (isDragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "reorderRow",
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = if (isDragging) 6.dp else 0.dp,
                            color = container,
                            contentColor = if (isDragging) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 54.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.DragIndicator,
                                    contentDescription = stringResource(R.string.drag_reorder, stringResource(tab.labelRes)),
                                    modifier = Modifier.draggableHandle(),
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(libraryTabIcon(tab), contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(tab.labelRes), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.size(46.dp),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.reset_tab_order))
                }
                Button(
                    onClick = { onSave(orderedTabs.toList()) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.done))
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_order)) },
            text = { Text(stringResource(R.string.reset_tab_order_to_the_default)) },
            confirmButton = {
                TextButton(onClick = {
                    orderedTabs = LibraryTab.entries.toList()
                    confirmReset = false
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
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
                label = { Text(stringResource(tab.labelRes)) },
                leadingIcon = { Icon(libraryTabIcon(tab), contentDescription = null) },
            )
        }
        IconButton(onClick = onReorder) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = stringResource(R.string.reorder_library_tabs))
        }
    }
}


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
    onReorder: (List<Long>) -> Unit,
) {
    val actions = LocalTrackActions.current
    var orderedTracks by remember(tracks.map { it.id }) { mutableStateOf(tracks) }
    LaunchedEffect(tracks) { orderedTracks = tracks }

    if (orderedTracks.isEmpty()) {
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
        val gridState = rememberLazyGridState()
        val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
            val fromIndex = from.index
            val toIndex = to.index
            if (fromIndex in orderedTracks.indices && toIndex in orderedTracks.indices && fromIndex != toIndex) {
                orderedTracks = orderedTracks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                onReorder(orderedTracks.map { it.id })
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (compactRows) 148.dp else 164.dp),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(if (compactRows) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactRows) 8.dp else 16.dp),
        ) {
            itemsIndexed(orderedTracks, key = { _, beatmap -> beatmap.id }) { index, beatmap ->
                ReorderableItem(reorderableState, key = beatmap.id) { _ ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CoverCard(
                            beatmap = beatmap,
                            size = if (compactRows) 136.dp else 156.dp,
                            shape = RoundedCornerShape(if (compactRows) 28.dp else 34.dp),
                            onClick = { actions.play(orderedTracks, index) },
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).draggableHandle(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DragIndicator, contentDescription = stringResource(R.string.reorder_song), modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
        }
    } else {
        val reorderableState = rememberReorderableLazyListState(
            lazyListState = listState,
            onMove = { from, to ->
                val fromIndex = from.index
                val toIndex = to.index
                if (fromIndex in orderedTracks.indices && toIndex in orderedTracks.indices && fromIndex != toIndex) {
                    orderedTracks = orderedTracks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                    onReorder(orderedTracks.map { it.id })
                }
            },
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().softTopEdge(listState),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(if (compactRows) 4.dp else 10.dp),
        ) {
            itemsIndexed(orderedTracks, key = { _, beatmap -> beatmap.id }) { index, beatmap ->
                ReorderableItem(reorderableState, key = beatmap.id) { _ ->
                    TrackRow(
                        beatmap = beatmap,
                        compact = compactRows,
                        shape = RoundedCornerShape(if (compactRows) 16.dp else 22.dp),
                        modifier = Modifier,
                        onClick = { actions.play(orderedTracks, index) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.DragIndicator,
                                    contentDescription = stringResource(R.string.reorder_song),
                                    modifier = Modifier.draggableHandle().padding(8.dp).size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(onClick = { actions.openMenu(beatmap) }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                                }
                            }
                        },
                    )
                }
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
            title = stringResource(R.string.no_playlists_yet),
            message = stringResource(R.string.create_a_playlist_then_add_beatmaps_from_the_track_m),
            shape = KozuCookies.Bold6,
            modifier = Modifier.padding(top = 32.dp),
            action = { PillButton(Icons.Rounded.Add, stringResource(R.string.new_playlist), onCreate) },
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
                        Text(playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(pluralStringResource(R.plurals.beatmap_count, playlist.tracks.size, playlist.tracks.size),
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
    sortUi: LibraryUiState,
    bottomPadding: Dp,
    compactRows: Boolean,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
) {
    val actions = LocalTrackActions.current
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var orderedTracks by remember(playlist.id, playlist.tracks.map { it.id }) { mutableStateOf(sortPlaylistTracks(playlist.tracks, sortUi.sort, sortUi.ascending)) }
    LaunchedEffect(playlist.tracks, sortUi.sort, sortUi.ascending) {
        orderedTracks = if (sortUi.sort == LibrarySort.CUSTOM) {
            val latestById = playlist.tracks.associateBy { it.id }
            val currentIds = orderedTracks.map { it.id }
            val retainedIds = currentIds.filter(latestById::containsKey).toSet()
            val mergedOrder = currentIds.filter(latestById::containsKey) +
                playlist.tracks.map { it.id }.filterNot(retainedIds::contains)
            mergedOrder.mapNotNull(latestById::get)
        } else {
            sortPlaylistTracks(playlist.tracks, sortUi.sort, sortUi.ascending)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp).bounceClick(onClick = onBack),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.song_count, playlist.tracks.size, playlist.tracks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { sortOpen = true }) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.sort_playlist))
            }
            IconButton(onClick = { showRename = true }) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.rename_playlist)) }
            IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_playlist_text)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                icon = playbackIconVector(LocalAppSettings.current.playbackIcon, false),
                label = stringResource(R.string.play_playlist),
                onClick = { actions.play(orderedTracks, 0) },
            )
            PillButton(
                icon = Icons.Rounded.Shuffle,
                label = stringResource(R.string.shuffle),
                onClick = { actions.playShuffled(orderedTracks) },
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            PillButton(
                icon = Icons.Rounded.Download,
                label = stringResource(R.string.offline),
                onClick = { actions.downloadAll(orderedTracks) },
            )
        }

        if (orderedTracks.isEmpty()) {
            RefreshableEmptyState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = stringResource(R.string.empty_playlist),
                message = stringResource(R.string.use_the_menu_on_any_song_and_choose_add_to_playlist),
                shape = KozuCookies.Flower8,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(
                lazyListState = listState,
                onMove = { from, to ->
                    if (from.index in orderedTracks.indices && to.index in orderedTracks.indices && from.index != to.index) {
                        orderedTracks = orderedTracks.toMutableList().apply { add(to.index, removeAt(from.index)) }
                        onReorder(orderedTracks.map { it.id })
                    }
                },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(orderedTracks, key = { _, beatmap -> beatmap.id }) { index, beatmap ->
                    ReorderableItem(reorderableState, key = beatmap.id) { _ ->
                        TrackRow(
                            beatmap = beatmap,
                            compact = compactRows,
                            shape = RoundedCornerShape(if (compactRows) 16.dp else 22.dp),
                            modifier = Modifier,
                            onClick = { actions.play(orderedTracks, index) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.DragIndicator,
                                        contentDescription = stringResource(R.string.reorder_song),
                                        modifier = Modifier.draggableHandle().padding(8.dp).size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    IconButton(onClick = { onRemoveTrack(beatmap.id) }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove_from_playlist), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (sortOpen) {
        SortSheet(
            ui = sortUi,
            onSort = onSort,
            onToggleDirection = onToggleDirection,
            onDismiss = { sortOpen = false },
        )
    }
    if (showRename) {
        NamePlaylistDialog(
            title = stringResource(R.string.rename_playlist),
            confirmLabel = stringResource(R.string.save),
            initial = playlist.name,
            onDismiss = { showRename = false },
            onConfirm = { showRename = false; onRename(it) },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_playlist)) },
            text = { Text(stringResource(R.string.remove_playlist_confirmation, playlist.name)) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun sortPlaylistTracks(
    tracks: List<Beatmap>,
    sort: LibrarySort,
    ascending: Boolean,
): List<Beatmap> {
    val sorted = when (sort) {
        LibrarySort.TITLE -> tracks.sortedBy { it.title.lowercase() }
        LibrarySort.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
        LibrarySort.MAPPER -> tracks.sortedBy { it.mapper.lowercase() }
        LibrarySort.RECENT, LibrarySort.CUSTOM -> tracks
    }
    return if (sort != LibrarySort.CUSTOM && ascending) sorted.reversed() else sorted
}

@Composable
private fun DownloadsPane(
    ui: LibraryUiState,
    bottomPadding: Dp,
    onDelete: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val actions = LocalTrackActions.current
    val queue = ui.queue.entries.filter { entry ->
        ui.downloads.none { it.beatmap.id == entry.key } || entry.value !is DownloadStatus.Queued
    }
    var tracks by remember(ui.downloads.map { it.beatmap.id }) { mutableStateOf(ui.downloads.map { it.beatmap }) }
    LaunchedEffect(ui.downloads) { tracks = ui.downloads.map { it.beatmap } }

    if (queue.isEmpty() && tracks.isEmpty()) {
        RefreshableEmptyState(
            icon = Icons.Rounded.Download,
            title = stringResource(R.string.nothing_offline_yet),
            message = stringResource(R.string.download_songs_from_the_menu_to_play_them_without_in),
            shape = KozuCookies.Clover4,
            modifier = Modifier.padding(top = 32.dp),
        )
        return
    }

    val listState = rememberLazyListState()
    val itemsBeforeTracks = 1 + queue.size + if (queue.isNotEmpty() && tracks.isNotEmpty()) 1 else 0
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            val fromIndex = from.index - itemsBeforeTracks
            val toIndex = to.index - itemsBeforeTracks
            if (fromIndex in tracks.indices && toIndex in tracks.indices && fromIndex != toIndex) {
                tracks = tracks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                onReorder(tracks.map { it.id })
            }
        },
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().softTopEdge(listState),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        item(key = "summary") {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DownloadDone, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(pluralStringResource(R.plurals.offline_song_count, ui.downloads.size, ui.downloads.size), style = MaterialTheme.typography.titleMedium)
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
                            ui.queueTitles[entry.key]?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.beatmap_number, entry.key),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when (status) {
                            is DownloadStatus.Queued -> Text(stringResource(R.string.waiting_in_queue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is DownloadStatus.Running -> {
                                Text(stringResource(R.string.download_status, (status.progress * 100).toInt(), status.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                LinearProgressIndicator(progress = { status.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                            }
                            is DownloadStatus.Failed -> Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { if (status is DownloadStatus.Failed) onDismiss(entry.key) else onCancel(entry.key) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(if (status is DownloadStatus.Failed) R.string.dismiss else R.string.cancel_download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (queue.isNotEmpty() && tracks.isNotEmpty()) item(key = "queue-gap") { Spacer(Modifier.height(8.dp)) }
        itemsIndexed(tracks, key = { _, beatmap -> beatmap.id }) { index, beatmap ->
            ReorderableItem(reorderableState, key = beatmap.id) { _ ->
                TrackRow(
                    beatmap = beatmap,
                    compact = isCompactLayout,
                    shape = segmentedShape(index, tracks.size, large = 22.dp, small = 6.dp),
                    modifier = Modifier,
                    onClick = { actions.play(tracks, index) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.DragIndicator,
                                contentDescription = stringResource(R.string.reorder_download),
                                modifier = Modifier.draggableHandle().padding(8.dp).size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(onClick = { onDelete(beatmap.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_download), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
            }
        }
    }
}
