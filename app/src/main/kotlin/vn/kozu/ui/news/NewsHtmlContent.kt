package vn.kozu.ui.news

import android.net.Uri
import android.view.LayoutInflater
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import vn.kozu.R

private sealed interface NewsBlock {
    data class TextBlock(
        val text: AnnotatedString,
        val heading: Int = 0,
        val quote: Boolean = false,
        val code: Boolean = false,
    ) : NewsBlock

    data class ImageBlock(
        val url: String,
        val alt: String,
        val maxWidthDp: Int? = null,
        val maxHeightDp: Int? = null,
    ) : NewsBlock

    data class MediaBlock(val url: String, val thumbnail: String?) : NewsBlock

    data class UnsupportedEmbedBlock(val thumbnail: String?) : NewsBlock

    data class ListBlock(val items: List<AnnotatedString>, val ordered: Boolean) : NewsBlock

    data class TableBlock(val rows: List<TableRow>) : NewsBlock

    data object DividerBlock : NewsBlock
}

private data class TableRow(val cells: List<TableCell>, val header: Boolean)
private data class TableCell(val text: AnnotatedString, val images: List<NewsBlock.ImageBlock>)

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun NewsHtmlContent(
    html: String,
    onOpenUrl: (String) -> Unit,
    compact: Boolean = false,
) {
    val blocks = remember(html) { parseNewsHtml(html) }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is NewsBlock.TextBlock -> {
                    if (block.text.isNotBlank()) {
                        val typography = MaterialTheme.typography
                        val style = when (block.heading) {
                            1 -> typography.headlineSmall
                            2 -> typography.titleLarge
                            3 -> typography.titleMedium
                            4 -> typography.titleSmall
                            else -> typography.bodyLarge
                        }
                        if (block.quote) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = block.text,
                                    style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        } else {
                            Text(
                                text = block.text,
                                style = style.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = if (block.code) FontFamily.Monospace else style.fontFamily,
                                    fontWeight = if (block.heading > 0) FontWeight.SemiBold else style.fontWeight,
                                ),
                            )
                        }
                    }
                }
                is NewsBlock.ImageBlock -> {
                    val imageModifier = if (block.maxWidthDp != null || block.maxHeightDp != null) {
                        Modifier
                            .widthIn(max = (block.maxWidthDp ?: 160).dp)
                            .heightIn(max = (block.maxHeightDp ?: 120).dp)
                    } else {
                        Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    }
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.alt.ifBlank { null },
                        contentScale = ContentScale.Fit,
                        modifier = imageModifier.clip(RoundedCornerShape(if (block.maxWidthDp != null) 8.dp else 18.dp)),
                    )
                }
                is NewsBlock.MediaBlock -> NewsVideoPlayer(
                    url = block.url,
                    poster = block.thumbnail,
                )
                is NewsBlock.UnsupportedEmbedBlock -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            block.thumbnail?.let { image ->
                                AsyncImage(
                                    model = image,
                                    contentDescription = stringResource(R.string.video_preview),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                stringResource(R.string.unsupported_video_format),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                is NewsBlock.ListBlock -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        block.items.forEachIndexed { itemIndex, item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Text(
                                    if (block.ordered) "${itemIndex + 1}." else "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                is NewsBlock.TableBlock -> NewsArticleTable(block)
                NewsBlock.DividerBlock -> androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (compact && index < blocks.lastIndex && block is NewsBlock.ImageBlock) Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun NewsArticleTable(block: NewsBlock.TableBlock) {
    Column(Modifier.fillMaxWidth()) {
        block.rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.cells.forEachIndexed { cellIndex, cell ->
                    val weight = when {
                        row.cells.size == 2 && cellIndex == 0 -> 0.30f
                        row.cells.size == 2 -> 0.70f
                        else -> 1f / row.cells.size.coerceAtLeast(1)
                    }
                    Column(
                        modifier = Modifier.weight(weight),
                        horizontalAlignment = if (row.cells.size == 2 && cellIndex == 0) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (cell.text.isNotBlank()) {
                            Text(
                                text = cell.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (row.header) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (cell.images.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                cell.images.forEach { image ->
                                    AsyncImage(
                                        model = image.url,
                                        contentDescription = image.alt.ifBlank { null },
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.sizeIn(maxWidth = 92.dp, maxHeight = 76.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (rowIndex < block.rows.lastIndex) {
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun NewsVideoPlayer(
    url: String,
    poster: String?,
) {
    val context = LocalContext.current
    val activePlayer = remember(context, url) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Referer" to "https://osu.ppy.sh/",
                ),
            )
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply { volume = 0f }
    }

    var previewReady by remember(url) { mutableStateOf(false) }
    var playbackError by remember(url) { mutableStateOf<String?>(null) }
    var userStartedPlayback by remember(url) { mutableStateOf(false) }

    DisposableEffect(activePlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.errorCodeName.ifBlank { error.message ?: "Playback failed" }
                activePlayer.pause()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {


                if (isPlaying && previewReady && !userStartedPlayback) {
                    userStartedPlayback = true
                    activePlayer.volume = 1f
                }
            }
        }
        val frameListener = object : AnalyticsListener {
            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long,
            ) {
                previewReady = true
                if (!userStartedPlayback) {
                    activePlayer.volume = 0f
                    activePlayer.pause()
                }
            }
        }

        activePlayer.addListener(playerListener)
        activePlayer.addAnalyticsListener(frameListener)
        onDispose {
            activePlayer.removeListener(playerListener)
            activePlayer.removeAnalyticsListener(frameListener)
            activePlayer.release()
        }
    }

    LaunchedEffect(activePlayer, url) {
        playbackError = null
        activePlayer.setMediaItem(MediaItem.fromUri(url))
        activePlayer.prepare()


        activePlayer.volume = 0f
        activePlayer.playWhenReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (!poster.isNullOrBlank()) {
            AsyncImage(
                model = poster,
                contentDescription = stringResource(R.string.video_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AndroidView(
            factory = { viewContext ->
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.news_video_player, null, false) as PlayerView).apply {
                    player = activePlayer
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    (videoSurfaceView as? TextureView)?.isOpaque = false
                }
            },
            update = { view ->
                if (view.player !== activePlayer) view.player = activePlayer
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!previewReady && playbackError == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 3.dp,
                )
                Text(
                    text = stringResource(R.string.loading_video_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        playbackError?.let { message ->
            Surface(
                onClick = {
                    playbackError = null
                    previewReady = false
                    userStartedPlayback = false
                    activePlayer.stop()
                    activePlayer.clearMediaItems()
                    activePlayer.setMediaItem(MediaItem.fromUri(url))
                    activePlayer.volume = 0f
                    activePlayer.prepare()
                    activePlayer.playWhenReady = true
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.could_not_load_video), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.tap_to_retry), style = MaterialTheme.typography.labelMedium)
                    Text(message, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

private fun parseNewsHtml(html: String): List<NewsBlock> {
    if (html.isBlank()) return emptyList()
    val document = Jsoup.parseBodyFragment(html, "https://osu.ppy.sh/")
    val result = mutableListOf<NewsBlock>()

    fun inline(element: Element): AnnotatedString {
        val builder = AnnotatedString.Builder()
        fun append(node: Node) {
            when (node) {
                is TextNode -> builder.append(node.wholeText)
                is Element -> {
                    val tag = node.normalName()
                    if (tag == "br") {
                        builder.append("\n")
                        return
                    }
                    if (tag == "img" || tag == "iframe" || tag == "video" || tag == "source") return
                    val style = when (tag) {
                        "strong", "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "em", "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                        "s", "del", "strike" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                        "code" -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.2f))
                        else -> SpanStyle()
                    }
                    val link = if (tag == "a") node.absUrl("href").ifBlank { node.attr("href") } else ""
                    if (link.isNotBlank()) {
                        builder.pushLink(
                            LinkAnnotation.Url(
                                link,
                                TextLinkStyles(
                                    SpanStyle(color = Color(0xFF4E9AD1), textDecoration = TextDecoration.Underline),
                                ),
                            ),
                        )
                    }
                    if (style != SpanStyle()) builder.pushStyle(style)
                    node.childNodes().forEach(::append)
                    if (style != SpanStyle()) builder.pop()
                    if (link.isNotBlank()) builder.pop()
                }
            }
        }
        element.childNodes().forEach(::append)
        return builder.toAnnotatedString()
    }

    fun imageFrom(node: Element): NewsBlock.ImageBlock? {
        val url = node.absUrl("src").ifBlank { node.attr("src") }
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null
        return NewsBlock.ImageBlock(url = url, alt = node.attr("alt"))
    }

    fun parseTable(node: Element): NewsBlock.TableBlock? {
        val rows = node.select("tr").filter { row ->
            row.parents().firstOrNull { it.normalName() == "table" } == node
        }.mapNotNull { row ->
            val cells = row.children().filter { it.normalName() == "th" || it.normalName() == "td" }.map { cell ->
                val images = cell.select("img[src]").mapNotNull(::imageFrom)
                TableCell(text = inline(cell), images = images)
            }
            cells.takeIf { it.isNotEmpty() }?.let { parsed ->
                TableRow(cells = parsed, header = row.children().any { it.normalName() == "th" })
            }
        }
        return if (rows.isNotEmpty()) NewsBlock.TableBlock(rows) else null
    }

    fun emit(node: Node) {
        when (node) {
            is TextNode -> {
                val text = node.wholeText.trim()
                if (text.isNotBlank()) result += NewsBlock.TextBlock(AnnotatedString(text))
            }
            is Element -> {
                val tag = node.normalName()
                when (tag) {
                    "img" -> imageFrom(node)?.let(result::add)
                    "iframe" -> {
                        val url = node.absUrl("src").ifBlank { node.attr("src") }
                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            if (isDirectVideoUrl(url)) result += NewsBlock.MediaBlock(url, videoThumbnail(url))
                            else result += NewsBlock.UnsupportedEmbedBlock(videoThumbnail(url))
                        }
                    }
                    "video" -> {
                        val source = node.selectFirst("source[src]")
                        val url = node.absUrl("src").ifBlank {
                            source?.absUrl("src").orEmpty().ifBlank { source?.attr("src").orEmpty() }
                        }
                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            val poster = node.absUrl("poster").takeIf { it.isNotBlank() }
                            if (isDirectVideoUrl(url) || source != null) result += NewsBlock.MediaBlock(url, poster)
                            else result += NewsBlock.UnsupportedEmbedBlock(poster)
                        }
                    }
                    "source", "script", "style", "noscript" -> Unit
                    "table" -> parseTable(node)?.let(result::add)
                    "hr" -> result += NewsBlock.DividerBlock
                    "ul", "ol" -> {
                        val items = node.select(":scope > li").map { inline(it) }.filter { it.isNotBlank() }
                        if (items.isNotEmpty()) result += NewsBlock.ListBlock(items, ordered = tag == "ol")
                    }
                    "h1", "h2", "h3", "h4", "h5", "h6", "p", "blockquote", "pre" -> {
                        val embeddedMedia = node.selectFirst("img, iframe, video, table")
                        if (embeddedMedia != null) {
                            node.childNodes().forEach(::emit)
                        } else {
                            val level = tag.removePrefix("h").toIntOrNull() ?: 0
                            val text = inline(node)
                            if (text.isNotBlank()) {
                                result += NewsBlock.TextBlock(
                                    text = text,
                                    heading = level,
                                    quote = tag == "blockquote",
                                    code = tag == "pre",
                                )
                            }
                        }
                    }
                    else -> {

                        val avatarUrl = backgroundImageUrl(node.attr("style"))
                        if (avatarUrl != null) {
                            result += NewsBlock.ImageBlock(avatarUrl, node.attr("aria-label"), maxWidthDp = 40, maxHeightDp = 40)
                        }
                        val children = node.childNodes()
                        if (children.isEmpty()) {
                            val text = node.text().trim()
                            if (text.isNotBlank()) result += NewsBlock.TextBlock(AnnotatedString(text))
                        } else {
                            children.forEach(::emit)
                        }
                    }
                }
            }
        }
    }

    document.body().childNodes().forEach(::emit)
    return result
}

private fun isDirectVideoUrl(url: String): Boolean {
    val path = runCatching { Uri.parse(url).path.orEmpty().lowercase() }.getOrDefault("")
    return listOf(".mp4", ".m3u8", ".webm", ".mpd", ".mov").any { path.endsWith(it, ignoreCase = true) }
}

private fun backgroundImageUrl(style: String): String? {
    val raw = Regex("background-image\\s*:\\s*url\\([\\\"']?([^\\\"')]+)", RegexOption.IGNORE_CASE)
        .find(style)?.groupValues?.getOrNull(1)?.trim() ?: return null
    return if (raw.startsWith("http://") || raw.startsWith("https://")) raw else Uri.parse("https://osu.ppy.sh/").buildUpon().appendEncodedPath(raw.trimStart('/')).build().toString()
}

private fun videoThumbnail(url: String): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val host = uri.host.orEmpty().lowercase()
    val videoId = when {
        host.endsWith("youtu.be") -> uri.lastPathSegment
        host.contains("youtube.com") || host.contains("youtube-nocookie.com") ->
            uri.getQueryParameter("v") ?: uri.pathSegments.indexOf("embed").takeIf { it >= 0 }?.let { index -> uri.pathSegments.getOrNull(index + 1) }
        else -> null
    }
    return videoId?.takeIf { it.isNotBlank() }?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}
