package vn.kozu.ui.news

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.net.UnknownHostException
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import retrofit2.HttpException
import vn.kozu.R
import vn.kozu.data.news.NewsFeed
import vn.kozu.data.news.NewsFeedPage
import vn.kozu.data.news.NewsPost
import vn.kozu.data.news.NewsReadState
import vn.kozu.ui.explore.FilterAction
import vn.kozu.di.AppContainer
import vn.kozu.ui.components.KozuSearchField
import vn.kozu.ui.components.KozuSheetHandle
import vn.kozu.ui.components.LocalOverlayBottomInset
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.softTopEdge

private sealed interface NewsState {
    data object Loading : NewsState
    data class Ready(val page: NewsFeedPage) : NewsState
    data class Failed(val message: String) : NewsState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val feed = container.news

    var reload by remember { mutableIntStateOf(0) }
    var retryMore by remember { mutableIntStateOf(0) }
    var retryArchive by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    // 0 means every year. Archive selection is applied only when the filter sheet chooses a year.
    var selectedYear by remember { mutableIntStateOf(0) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(0) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var detail by remember(openId) { mutableStateOf<NewsPost?>(null) }
    var detailLoading by remember(openId) { mutableStateOf(false) }
    var detailError by remember(openId) { mutableStateOf<String?>(null) }
    var retryDetail by remember(openId) { mutableIntStateOf(0) }
    var state by remember(feed) { mutableStateOf<NewsState>(NewsState.Loading) }
    var refreshing by remember { mutableStateOf(false) }
    var archivePosts by remember { mutableStateOf<List<NewsPost>?>(null) }
    var archiveCursor by remember { mutableStateOf<String?>(null) }
    var archiveLoading by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<String?>(null) }
    var lastPageRequestAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(reload) {
        val hadContent = state is NewsState.Ready
        refreshing = hadContent
        if (!hadContent) state = NewsState.Loading
        try {
            val page = feed.load()
            NewsReadState.updateLatest(context, page.posts)
            state = NewsState.Ready(page)
        } catch (error: Exception) {
            if (!hadContent && state !is NewsState.Ready) {
                state = NewsState.Failed(newsErrorMessage(context, error))
            } else {
                (state as? NewsState.Ready)?.let { previous ->
                    state = NewsState.Ready(previous.page)
                }
            }
        } finally {
            refreshing = false
        }
    }

    val page = (state as? NewsState.Ready)?.page
    // Use the exact archive years returned by osu!, not a locally fabricated range.
    val years = remember(page?.years) { page?.years.orEmpty().distinct().sortedDescending() }
    val effectiveYear = selectedYear.takeIf { it > 0 }

    LaunchedEffect(years, selectedYear) {
        if (selectedYear != 0 && years.isNotEmpty() && selectedYear !in years) {
            selectedYear = 0
            selectedMonth = 0
        }
    }

    LaunchedEffect(effectiveYear, reload, retryArchive) {
        if (effectiveYear != null) {
            archivePosts = emptyList()
            archiveCursor = null
            archiveError = null
            archiveLoading = true
            try {
                val archive = feed.loadYear(effectiveYear)
                archivePosts = archive.posts
                archiveCursor = archive.cursorString
            } catch (error: Exception) {
                archiveError = newsErrorMessage(context, error)
            } finally {
                archiveLoading = false
            }
        } else {
            archivePosts = null
            archiveCursor = null
            archiveError = null
            archiveLoading = false
        }
    }

    val posts = if (effectiveYear == null) page?.posts.orEmpty() else archivePosts.orEmpty()
    val nextCursor = if (effectiveYear == null) page?.cursorString else archiveCursor
    val visible = remember(posts, query, selectedMonth) {
        val searchTerm = query.trim()
        posts.filter { post ->
            val matchesSearch = searchTerm.isBlank() ||
                post.title.contains(searchTerm, ignoreCase = true) ||
                post.author.contains(searchTerm, ignoreCase = true) ||
                post.summary.contains(searchTerm, ignoreCase = true)
            val matchesMonth = selectedMonth == 0 || post.yearMonth()?.monthValue == selectedMonth
            matchesSearch && matchesMonth
        }
    }

    val opened = posts.firstOrNull { it.id == openId }

    LaunchedEffect(openId, opened?.slug, retryDetail) {
        if (opened != null) {
            NewsReadState.markSeen(context, opened.id)
            detail = opened
            detailError = null
            detailLoading = true
            try {
                detail = feed.loadDetail(opened)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                detailError = newsErrorMessage(context, error)
            } finally {
                detailLoading = false
            }
        } else {
            detail = null
            detailError = null
            detailLoading = false
        }
    }

    BackHandler {
        if (openId != null) openId = null else onBack()
    }

    val openExternal: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        ),
                    ),
                )
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopIcon(R.drawable.at_arrow_back, stringResource(R.string.back)) {
                    if (openId != null) openId = null else onBack()
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.news_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (openId != null) {
                    TopIcon(R.drawable.at_share, stringResource(R.string.open_on_osu)) {
                        detail?.url?.let(openExternal)
                    }
                }
            }
            if (openId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KozuSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        placeholder = stringResource(R.string.news_search_placeholder),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterAction(
                        active = selectedYear != 0 || selectedMonth != 0,
                        onClick = { showFilters = true },
                    )
                }
            }
        }

        AnimatedContent(
            targetState = openId != null,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 6 } + fadeOut(tween(160)))
            },
            label = "news-page-content",
            modifier = Modifier.fillMaxSize(),
        ) { showDetail ->
            if (showDetail) {
                val article = detail
                if (article == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    NewsDetail(
                        post = article,
                        isLoading = detailLoading,
                        error = detailError,
                        onRetry = { retryDetail++ },
                        onOpenUrl = openExternal,
                    )
                }
            } else {
                when (val current = state) {
                    NewsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is NewsState.Failed -> NewsMessage(
                        title = stringResource(R.string.news_load_failed),
                        message = current.message,
                        onRetry = { reload++ },
                        onOpenSite = { openExternal(NewsFeed.SITE_URL) },
                    )
                    is NewsState.Ready -> {
                        val listState = rememberLazyListState()
                        val pullState = rememberPullToRefreshState()
                        LaunchedEffect(listState, nextCursor, effectiveYear, archiveLoading, retryMore) {
                            snapshotFlow {
                                val info = listState.layoutInfo
                                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                                info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 6
                            }.collect { nearEnd ->
                                if (nearEnd && nextCursor != null && !archiveLoading && !loadingMore) {
                                    loadingMore = true
                                    loadMoreError = null
                                    try {
                                        val elapsed = SystemClock.elapsedRealtime() - lastPageRequestAt
                                        val waitForRateLimit = 1_000L - elapsed
                                        if (waitForRateLimit > 0L) delay(waitForRateLimit)
                                        lastPageRequestAt = SystemClock.elapsedRealtime()
                                        val batch = if (effectiveYear == null) {
                                            feed.loadNextPage(year = null, cursorString = nextCursor)
                                        } else {
                                            feed.loadYear(year = effectiveYear, cursorString = nextCursor)
                                        }
                                        if (effectiveYear == null) {
                                            val currentPage = (state as? NewsState.Ready)?.page
                                            if (currentPage != null) {
                                                val merged = (currentPage.posts + batch.posts)
                                                    .distinctBy { it.id }
                                                    .sortedByDescending { it.dateMillis }
                                                val updated = currentPage.copy(
                                                    posts = merged,
                                                    cursorString = batch.cursorString?.takeIf { it != nextCursor },
                                                )
                                                state = NewsState.Ready(updated)
                                                NewsReadState.updateLatest(context, merged)
                                            }
                                        } else {
                                            archivePosts = (archivePosts.orEmpty() + batch.posts)
                                                .distinctBy { it.id }
                                                .sortedByDescending { it.dateMillis }
                                            archiveCursor = batch.cursorString?.takeIf { it != nextCursor }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        loadMoreError = newsErrorMessage(context, error)
                                    } finally {
                                        loadingMore = false
                                    }
                                }
                            }
                        }
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { reload++ },
                            state = pullState,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                RefreshIndicator(
                                    state = pullState,
                                    isRefreshing = refreshing,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                )
                            },
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().softTopEdge(listState),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 24.dp + LocalOverlayBottomInset.current,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (archiveLoading) {
                                    item(key = "news-archive-loading") {
                                        Box(
                                            Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                archiveError?.let { error ->
                                    item(key = "news-archive-error") {
                                        NewsMessage(
                                            title = stringResource(R.string.news_load_failed),
                                            message = error,
                                            onRetry = { retryArchive++ },
                                            onOpenSite = { openExternal("${NewsFeed.SITE_URL}?year=$effectiveYear") },
                                        )
                                    }
                                }
                                if (!archiveLoading && visible.isEmpty() && archiveError == null) {
                                    item(key = "news-empty") {
                                        Text(
                                            text = stringResource(
                                                if (query.isBlank()) R.string.no_news_yet else R.string.nothing_matches_search,
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(24.dp),
                                        )
                                    }
                                }
                                items(visible, key = { it.id }) { post ->
                                    NewsCard(post) { openId = post.id }
                                }
                                if (loadingMore) {
                                    item(key = "news-load-more-progress") {
                                        Box(
                                            Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                }
                                loadMoreError?.let { message ->
                                    item(key = "news-load-more-error") {
                                        Column(
                                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            androidx.compose.material3.TextButton(onClick = { retryMore++ }) {
                                                Text(stringResource(R.string.retry))
                                            }
                                        }
                                    }
                                }
                                item(key = "news-site-link") {
                                    FilledTonalButton(
                                        onClick = { openExternal(NewsFeed.SITE_URL) },
                                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                                    ) {
                                        Text(stringResource(R.string.read_more_osu))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        NewsYearFilterSheet(
            years = years,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onDismiss = { showFilters = false },
            onApply = { year, month ->
                selectedYear = year
                selectedMonth = month
                if (year != 0 && state !is NewsState.Ready) {
                    // Allow an archive request to render even when the latest-feed request failed.
                    state = NewsState.Ready(NewsFeedPage(emptyList(), years, year))
                } else if (year == 0 && (state as? NewsState.Ready)?.page?.posts.isNullOrEmpty()) {
                    state = NewsState.Loading
                    reload++
                }
                showFilters = false
            },
        )
    }
}

private fun NewsPost.yearMonth(): YearMonth? {
    if (dateMillis <= 0L) return null
    return runCatching {
        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault())
        YearMonth.of(date.year, date.monthValue)
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewsYearFilterSheet(
    years: List<Int>,
    selectedYear: Int,
    selectedMonth: Int,
    onDismiss: () -> Unit,
    onApply: (year: Int, month: Int) -> Unit,
) {
    var draftYear by remember(selectedYear) { mutableIntStateOf(selectedYear) }
    var draftMonth by remember(selectedMonth) { mutableIntStateOf(selectedMonth) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.filters),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.news_archive_year),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NewsFilterChip(
                        label = stringResource(R.string.news_archive_all_years),
                        selected = draftYear == 0,
                        onClick = { draftYear = 0 },
                    )
                    years.forEach { year ->
                        NewsFilterChip(
                            label = year.toString(),
                            selected = year == draftYear,
                            onClick = { draftYear = year },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.news_archive_month),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NewsFilterChip(
                        label = stringResource(R.string.news_archive_all),
                        selected = draftMonth == 0,
                        onClick = { draftMonth = 0 },
                    )
                    (1..12).forEach { month ->
                        NewsFilterChip(
                            label = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            selected = month == draftMonth,
                            onClick = { draftMonth = month },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = {
                        draftYear = 0
                        draftMonth = 0
                    },
                    enabled = draftYear != 0 || draftMonth != 0,
                ) { Text(stringResource(R.string.reset)) }
                Button(
                    onClick = { onApply(draftYear, draftMonth) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.apply)) }
            }
        }
    }
}

@Composable
private fun NewsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TopIcon(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(46.dp).clip(CircleShape).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            ImageVector.vectorResource(icon),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun NewsCard(
    post: NewsPost,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.99f, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHigh,
    ) {
        Column {
            post.imageUrl?.let { url ->
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 5.6f)) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    )
                    if (post.dateLabel.isNotBlank()) {
                        NewsDateBadge(
                            label = post.dateLabel,
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (post.imageUrl == null && post.dateLabel.isNotBlank()) {
                    NewsDateBadge(post.dateLabel)
                }
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (post.summary.isNotBlank()) {
                    Text(
                        text = post.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (post.author.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.news_by_author, post.author),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsDateBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.62f),
        contentColor = Color.White,
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun NewsDetail(
    post: NewsPost,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().softTopEdge(listState),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp + LocalOverlayBottomInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "article-title") {
            // The article body already contains its lead artwork. Do not repeat the card
            // thumbnail/date/author header above the article content.
            Text(post.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (!post.contentHtml.isNullOrBlank()) {
            item(key = "article-body") {
                NewsHtmlContent(post.contentHtml, onOpenUrl)
            }
        }
        if (isLoading && post.contentHtml.isNullOrBlank()) {
            item(key = "article-loading") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (error != null && post.contentHtml.isNullOrBlank()) {
            item(key = "article-error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.retry))
                        }
                        androidx.compose.material3.TextButton(onClick = { onOpenUrl(post.url) }) {
                            Text(stringResource(R.string.open_osu))
                        }
                    }
                }
            }
        }
        item(key = "article-open-site") {
            FilledTonalButton(onClick = { onOpenUrl(post.url) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_osu))
            }
        }
    }
}

private fun newsErrorMessage(context: android.content.Context, error: Throwable): String {
    val causes = generateSequence(error) { it.cause }.toList()
    val isDnsFailure = causes.any { it is UnknownHostException }
    val isForbidden = causes.any { cause ->
        (cause as? HttpException)?.code() == 403 ||
            cause.message?.contains("HTTP 403", ignoreCase = true) == true
    }
    return when {
        isDnsFailure -> context.getString(R.string.news_connection_error)
        isForbidden -> context.getString(R.string.news_forbidden_error)
        else -> error.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.news_load_failed)
    }
}

@Composable
private fun NewsMessage(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onOpenSite: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onOpenSite) { Text(stringResource(R.string.open_osu_site)) }
    }
}
