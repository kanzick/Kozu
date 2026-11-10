package vn.kozu.ui.news

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.kozu.data.news.NewsFeed
import vn.kozu.data.news.NewsPost
import vn.kozu.data.news.NewsReadState
import vn.kozu.di.AppContainer
import vn.kozu.R
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.KozuSearchField
import vn.kozu.ui.components.LocalOverlayBottomInset
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.softTopEdge

private sealed interface NewsState {
    data object Loading : NewsState
    data class Ready(val posts: List<NewsPost>) : NewsState
    data class Failed(val message: String) : NewsState
}

private enum class NewsHeaderMode { List, Search, Detail }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val feed = container.news
    val signedIn by container.auth.loggedIn.collectAsStateWithLifecycle(initialValue = false)
    val accountProfile by container.accounts.profile.collectAsStateWithLifecycle(initialValue = null)
    var reload by remember { mutableIntStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detail by remember(openId) { mutableStateOf<NewsPost?>(null) }
    var detailLoading by remember(openId) { mutableStateOf(false) }

    var state by remember { mutableStateOf<NewsState>(NewsState.Loading) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        val hadContent = state is NewsState.Ready
        refreshing = hadContent
        if (!hadContent) state = NewsState.Loading
        try {
            val loadedPosts = feed.load()
            NewsReadState.updateLatest(context, loadedPosts)
            state = NewsState.Ready(loadedPosts)
        } catch (error: Exception) {

            if (!hadContent) {
                state = NewsState.Failed(error.message ?: "Could not load the news")
            }
        } finally {
            refreshing = false
        }
    }
    val posts = (state as? NewsState.Ready)?.posts.orEmpty()
    val opened = posts.firstOrNull { it.id == openId }
    val visible =
        remember(posts, query) {
            val q = query.trim()
            if (q.isEmpty()) posts else posts.filter { post ->
                post.title.contains(q, true) || post.author.contains(q, true) || post.summary.contains(q, true)
            }
        }

    LaunchedEffect(openId, opened?.slug) {
        if (opened != null) {
            NewsReadState.markSeen(context, opened.id)
            detailLoading = true
            detail = opened
            try {
                detail = feed.loadDetail(opened)
            } catch (_: Exception) {
                detail = opened
            } finally {
                detailLoading = false
            }
        } else {
            detail = null
            detailLoading = false
        }
    }

    BackHandler {
        when {
            openId != null -> openId = null
            searching -> {
                searching = false
                query = ""
            }
            else -> onBack()
        }
    }

    val openExternal: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }


    val signInToComment: () -> Unit = {
        scope.launch {
            try {
                val authorizeUrl = container.accounts.beginLogin()
                if (authorizeUrl == null) {
                    onOpenAccountSettings()
                } else {
                    uriHandler.openUri(authorizeUrl)
                }
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    error.message ?: "Could not start osu! sign-in",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                        ),
                    ),
                )
                .statusBarsPadding(),
        ) {
            val headerMode = when {
                searching && openId == null -> NewsHeaderMode.Search
                openId != null -> NewsHeaderMode.Detail
                else -> NewsHeaderMode.List
            }
            AnimatedContent(
                targetState = headerMode,
                transitionSpec = {
                    (slideInHorizontally(tween(230)) { it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(tween(180)) { -it / 4 } + fadeOut(tween(140)))
                },
                label = "news-header-transition",
            ) { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopIcon(R.drawable.at_arrow_back, stringResource(R.string.back)) {
                        when (mode) {
                            NewsHeaderMode.Search -> {
                                searching = false
                                query = ""
                            }
                            NewsHeaderMode.Detail -> openId = null
                            NewsHeaderMode.List -> onBack()
                        }
                    }
                    if (mode == NewsHeaderMode.Search) {
                        Spacer(Modifier.width(8.dp))
                        KozuSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = {},
                            placeholder = "Search news",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                        when (mode) {
                            NewsHeaderMode.List -> TopIcon(R.drawable.at_search, stringResource(R.string.search)) { searching = true }
                            NewsHeaderMode.Detail -> TopIcon(R.drawable.at_share, stringResource(R.string.open_on_osu)) {
                                detail?.url?.let(openExternal)
                            }
                            NewsHeaderMode.Search -> Unit
                        }
                    }
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
                if (article == null || (detailLoading && article.contentHtml.isNullOrBlank())) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    NewsDetail(
                        post = article,
                        feed = feed,
                        auth = container.auth,
                        signedIn = signedIn,
                        signedInUsername = accountProfile?.username,
                        onSignIn = signInToComment,
                        onOpenUrl = openExternal,
                    )
                }
            } else {
                when (val current = state) {
                    NewsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is NewsState.Failed -> NewsMessage(
                        title = "Couldn't load the news",
                        message = current.message,
                        onRetry = { reload++ },
                        onOpenSite = { openExternal(NewsFeed.SITE_URL) },
                    )
                    is NewsState.Ready -> {
                        val listState = rememberLazyListState()
                        val pullState = rememberPullToRefreshState()
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
                                    top = 4.dp,
                                    bottom = 24.dp + LocalOverlayBottomInset.current,
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                item(key = "title") {
                                    Text(stringResource(R.string.news_title),
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 10.dp),
                                    )
                                }
                                if (visible.isEmpty()) {
                                    item(key = "empty") {
                                        Text(if (query.isBlank()) stringResource(R.string.no_news_yet) else stringResource(R.string.nothing_matches_search),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(24.dp),
                                        )
                                    }
                                }
                                items(visible, key = { it.id }) { post ->
                                    NewsCard(post) { openId = post.id }
                                }
                                item(key = "site") {
                                    FilledTonalButton(
                                        onClick = { openExternal(NewsFeed.SITE_URL) },
                                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                                    ) { Text(stringResource(R.string.read_more_osu)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopIcon(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ImageVector.vectorResource(icon), contentDescription = description, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun NewsChips(post: NewsPost) {
    val colors = MaterialTheme.colorScheme
    val fresh = post.dateMillis > 0 && System.currentTimeMillis() - post.dateMillis < 7L * 24 * 3600 * 1000
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = if (fresh) colors.tertiary else colors.secondaryContainer,
            contentColor = if (fresh) colors.onTertiary else colors.onSecondaryContainer,
        ) {
            Text(if (fresh) stringResource(R.string.new_label) else "News",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
        Surface(Modifier.weight(1f, fill = false), shape = CircleShape, color = colors.surfaceContainerHighest) {
            Text(listOf(post.author, post.dateLabel).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun NewsCard(
    post: NewsPost,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.985f, onClick = onClick),
        shape = RoundedCornerShape(36.dp),
        color = colors.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            post.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 8f).clip(RoundedCornerShape(24.dp)),
                )
            }
            NewsChips(post)
            Text(post.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (post.summary.isNotBlank()) {
                Text(post.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NewsDetail(
    post: NewsPost,
    feed: NewsFeed,
    auth: vn.kozu.data.remote.osu.OsuAuth,
    signedIn: Boolean,
    signedInUsername: String?,
    onSignIn: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().softTopEdge(listState),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 24.dp + LocalOverlayBottomInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "article-image") {
            post.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 8f).clip(RoundedCornerShape(28.dp)),
                )
            }
        }
        item(key = "article-meta") { NewsChips(post) }
        item(key = "article-title") {
            Text(post.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item(key = "article-body") {
            if (!post.contentHtml.isNullOrBlank()) {
                NewsHtmlContent(post.contentHtml, onOpenUrl)
            } else {
                Text(post.summary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item(key = "comments") {
            NewsComments(
                post = post,
                feed = feed,
                auth = auth,
                signedIn = signedIn,
                signedInUsername = signedInUsername,
                onSignIn = onSignIn,
                onOpenUrl = onOpenUrl,
            )
        }
        item(key = "article-open-site") {
            FilledTonalButton(onClick = { onOpenUrl(post.url) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_osu))
            }
        }
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
