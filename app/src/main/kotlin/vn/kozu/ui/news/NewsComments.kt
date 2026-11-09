package vn.kozu.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.kozu.data.news.NewsCommentBundleDto
import vn.kozu.data.news.NewsCommentDto
import vn.kozu.data.news.NewsCommentUserDto
import vn.kozu.data.news.NewsFeed
import vn.kozu.data.news.NewsPost
import vn.kozu.ui.i18n.KozuText as Text

@Composable
internal fun NewsComments(
    post: NewsPost,
    feed: NewsFeed,
    signedIn: Boolean,
    onOpenPost: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sort by remember(post.id) { mutableStateOf("new") }
    var reload by remember(post.id) { mutableIntStateOf(0) }
    var bundle by remember(post.id) { mutableStateOf<NewsCommentBundleDto?>(null) }
    var loading by remember(post.id) { mutableStateOf(true) }
    var loadError by remember(post.id) { mutableStateOf<String?>(null) }
    val replyBundles = remember(post.id) { mutableStateMapOf<Long, NewsCommentBundleDto>() }
    val loadingReplies = remember(post.id) { mutableStateMapOf<Long, Boolean>() }
    var expandedReplies by remember(post.id) { mutableStateOf(setOf<Long>()) }
    var loadingMore by remember(post.id) { mutableStateOf(false) }
    var actionError by remember(post.id) { mutableStateOf<String?>(null) }
    val commentableId = post.commentableId

    LaunchedEffect(post.id, commentableId, sort, reload) {
        loading = true
        loadError = null
        if (commentableId == null) {
            bundle = null
            loadError = "osu! did not provide a comment thread ID in its public news feed. Open the article on osu! to view comments."
            loading = false
        } else {
            try {
                bundle = feed.loadComments(commentableId, sort)
            } catch (error: Exception) {
                loadError = error.message ?: "Couldn't load comments"
            } finally {
                loading = false
            }
        }
    }

    val data = bundle
    val allComments = remember(data) {
        (data?.pinnedComments.orEmpty() + data?.comments.orEmpty() + data?.includedComments.orEmpty()).distinctBy { it.id }
    }
    val topLevel = allComments.filter { it.parentId == null }
    val users = remember(data) { data?.users.orEmpty().associateBy { it.id } }
    val commentCount = data?.let { it.total ?: it.topLevelCount ?: topLevel.size }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                if (commentCount != null) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(commentCount.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (signedIn) {
                        Text(
                            "Comments are shown here, but osu! does not currently allow third-party OAuth apps to post them. Open this article on osu! to write or reply; your browser may ask you to sign in there.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onOpenPost) { Text("Open osu! comments") }
                        }
                    } else {
                        Text("Please sign in to proceed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onOpenPost) { Text("Sign in to comment") }
                        }
                    }
                }
            }

            actionError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SortChoice("Recent", selected = sort == "new") { sort = "new"; replyBundles.clear(); expandedReplies = emptySet() }
                SortChoice("Old", selected = sort == "old") { sort = "old"; replyBundles.clear(); expandedReplies = emptySet() }
                SortChoice("Top", selected = sort == "top") { sort = "top"; replyBundles.clear(); expandedReplies = emptySet() }
            }

            when {
                loading && data == null -> Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                loadError != null && data == null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { reload++ }) { Text("Retry") }
                }
                topLevel.isEmpty() -> Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    topLevel.forEach { comment ->
                        val user = users[comment.userId]
                        NewsCommentRow(
                            comment = comment,
                            user = user,
                            onReply = onOpenPost,
                            onOpenUrl = onOpenUrl,
                        )
                        if (comment.repliesCount > 0) {
                            val expanded = comment.id in expandedReplies
                            TextButton(
                                onClick = {
                                    if (expanded) {
                                        expandedReplies = expandedReplies - comment.id
                                    } else {
                                        expandedReplies = expandedReplies + comment.id
                                        if (replyBundles[comment.id] == null) {
                                            loadingReplies[comment.id] = true
                                            scope.launch {
                                                runCatching { feed.loadComments(commentableId ?: return@launch, sort, comment.id) }
                                                    .onSuccess { replyBundles[comment.id] = it }
                                                    .onFailure { actionError = it.message ?: "Couldn't load replies" }
                                                loadingReplies[comment.id] = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 56.dp),
                            ) {
                                Text(if (expanded) "Hide replies" else "View ${comment.repliesCount} ${if (comment.repliesCount == 1) "reply" else "replies"}")
                            }
                            if (expanded) {
                                if (loadingReplies[comment.id] == true) {
                                    CircularProgressIndicator(Modifier.padding(start = 58.dp).size(18.dp), strokeWidth = 2.dp)
                                }
                                val replyBundle = replyBundles[comment.id]
                                val replyUsers = replyBundle?.users.orEmpty().associateBy { it.id }
                                replyBundle?.comments.orEmpty().forEach { reply ->
                                    NewsCommentRow(
                                        comment = reply,
                                        user = replyUsers[reply.userId],
                                        onReply = onOpenPost,
                                        onOpenUrl = onOpenUrl,
                                        nested = true,
                                    )
                                }
                            }
                        }
                    }
                    val lastLoadedCommentId = topLevel.lastOrNull()?.id
                    if (data?.hasMore == true && lastLoadedCommentId != null) {
                        TextButton(
                            onClick = {
                                if (!loadingMore) {
                                    loadingMore = true
                                    scope.launch {
                                        try {
                                            val next = feed.loadComments(
                                                postId = commentableId ?: return@launch,
                                                sort = sort,
                                                after = lastLoadedCommentId.toString(),
                                            )
                                            val previous = bundle
                                            if (previous != null) {
                                                bundle = previous.copy(
                                                    comments = (previous.comments + next.comments).distinctBy { it.id },
                                                    includedComments = (previous.includedComments + next.includedComments).distinctBy { it.id },
                                                    pinnedComments = (previous.pinnedComments + next.pinnedComments).distinctBy { it.id },
                                                    users = (previous.users + next.users).distinctBy { it.id },
                                                    total = next.total ?: previous.total,
                                                    topLevelCount = next.topLevelCount ?: previous.topLevelCount,
                                                    hasMore = next.hasMore,
                                                    hasMoreId = next.hasMoreId,
                                                )
                                            }
                                        } catch (error: Exception) {
                                            actionError = error.message ?: "Couldn't load more comments"
                                        } finally {
                                            loadingMore = false
                                        }
                                    }
                                }
                            },
                            enabled = !loadingMore,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            if (loadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Load more comments")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun NewsCommentRow(
    comment: NewsCommentDto,
    user: NewsCommentUserDto?,
    onReply: () -> Unit,
    onOpenUrl: (String) -> Unit,
    nested: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (nested) 42.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val avatarUrl = user?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(if (nested) 34.dp else 42.dp).clip(CircleShape),
            )
        } else {
            Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(if (nested) 34.dp else 42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user?.username ?: comment.legacyName ?: "osu! user", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(relativeDate(comment.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (comment.deletedAt != null) {
                Text("This comment has been deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            } else if (!comment.messageHtml.isNullOrBlank()) {
                NewsHtmlContent(comment.messageHtml, onOpenUrl = onOpenUrl, compact = true)
            } else {
                Text(comment.message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${comment.votesCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = onReply) { Text("reply on osu!") }
            }
        }
    }
}

private fun relativeDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val millis = runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
    if (millis <= 0L) return ""
    val elapsed = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "$minutes min ago"
        minutes < 1440L -> "${minutes / 60L} hours ago"
        minutes < 43_200L -> "${minutes / 1440L} days ago"
        else -> java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
    }
}
