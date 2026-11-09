package vn.kozu.data.news

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import vn.kozu.data.remote.osu.OsuApi
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A native representation of an osu! news article. */
data class NewsPost(
    /** Stable Compose key. For Atom fallback posts this may be generated from the slug. */
    val id: Long,
    val slug: String,
    val title: String,
    val author: String,
    val dateMillis: Long,
    val dateLabel: String,
    val summary: String,
    val imageUrl: String?,
    val url: String,
    val contentHtml: String? = null,
    /** Real osu! news-post database ID; null when only the public Atom feed is available. */
    val commentableId: Long? = id,
)

/**
 * Reads osu! news natively. API v2 is preferred because it supplies the numeric post IDs
 * needed by the comments endpoint. If the API is unavailable (for example HTTP 403),
 * the public Atom feed keeps the news list and article content readable without login.
 */
class NewsFeed(
    private val api: OsuApi,
) {
    suspend fun load(): List<NewsPost> {
        val apiPosts = try {
            api.news(limit = 21).newsPosts.mapNotNull(::toPost)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        if (apiPosts.isNotEmpty()) return apiPosts.sortedByDescending { it.dateMillis }

        val atomPosts = loadAtomFeed()
        if (atomPosts.isNotEmpty()) return atomPosts.sortedByDescending { it.dateMillis }

        throw IOException("osu! returned no news posts from the API or Atom feed")
    }

    suspend fun loadDetail(post: NewsPost): NewsPost {
        // Atom entries often contain the full HTML body, but their IDs may be URLs rather
        // than numeric osu! news-post IDs. Resolve the API detail when the comment ID is absent.
        if (!post.contentHtml.isNullOrBlank() && post.commentableId != null) return post
        return try {
            val detail = api.newsPost(post.slug, listOf("html"))
            val apiPost = toPost(detail)
            if (apiPost == null) {
                post
            } else {
                apiPost.copy(
                    imageUrl = post.imageUrl ?: apiPost.imageUrl,
                    contentHtml = detail.content ?: detail.contentMarkdown?.let(::markdownFallback) ?: post.contentHtml,
                    commentableId = detail.id.takeIf { it > 0L } ?: post.commentableId,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            post
        }
    }

    suspend fun loadComments(
        postId: Long,
        sort: String = "new",
        // osu!'s own comments UI uses parent_id=0 for top-level comments.
        parentId: Long? = 0L,
        after: String? = null,
    ): NewsCommentBundleDto =
        api.newsComments(
            commentableType = "news_post",
            commentableId = postId,
            sort = sort,
            parentId = parentId,
            after = after,
        )

    private suspend fun loadAtomFeed(): List<NewsPost> = withContext(Dispatchers.IO) {
        val connection = URL(FEED_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 18_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", BROWSER_USER_AGENT)
            connection.setRequestProperty("Accept", "application/atom+xml, application/xml;q=0.9, */*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("osu! Atom feed returned HTTP $code")
            }
            val xml = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseAtom(xml)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAtom(xml: String): List<NewsPost> {
        val entries =
            Regex("<entry\\b[^>]*>([\\s\\S]*?)</entry>", RegexOption.IGNORE_CASE)
                .findAll(xml).map { it.groupValues[1] }.toList()
                .ifEmpty {
                    Regex("<item\\b[^>]*>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
                        .findAll(xml).map { it.groupValues[1] }.toList()
                }

        return entries.mapNotNull { entry ->
            val title = tag(entry, "title")?.let(::plainText)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val link =
                Regex("<link\\b[^>]*?href=[\"']([^\"']+)[\"'][^>]*/?>", RegexOption.IGNORE_CASE)
                    .findAll(entry).map { it.groupValues[1] }
                    .firstOrNull { it.contains("/home/news/") }
                    ?: tag(entry, "link")?.let(::plainText)
                    ?: return@mapNotNull null
            val slug = link.substringAfter("/home/news/", "").substringBefore('?').substringBefore('#')
                .takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val rawId = tag(entry, "id")?.let(::plainText).orEmpty()
            val realCommentId = rawId.toLongOrNull()
                ?: Regex("(?:NewsPost|news_posts?|news)[/:](\\d+)", RegexOption.IGNORE_CASE)
                    .find(rawId)?.groupValues?.getOrNull(1)?.toLongOrNull()
            val stableKey = realCommentId ?: (slug.hashCode().toLong() and 0x7fffffffL)
            val author =
                Regex("<author\\b[^>]*>[\\s\\S]*?<name[^>]*>([\\s\\S]*?)</name>[\\s\\S]*?</author>", RegexOption.IGNORE_CASE)
                    .find(entry)?.groupValues?.get(1)?.let(::plainText)
                    ?: tag(entry, "dc:creator")?.let(::plainText)
                    ?: "osu!"
            val dateRaw = tag(entry, "published") ?: tag(entry, "updated") ?: tag(entry, "pubDate")
            val dateMillis = parseDate(dateRaw)
            val rawContent = tag(entry, "content") ?: tag(entry, "summary") ?: tag(entry, "description").orEmpty()
            val html = decodeXmlContent(rawContent)
            val fragment = Jsoup.parseBodyFragment(html, "https://osu.ppy.sh/")
            val textContent = fragment.text().trim()
            val image = fragment.selectFirst("img[src]")?.absUrl("src")?.takeIf { it.isNotBlank() }
                ?: Regex("<media:thumbnail[^>]*?url=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                    .find(entry)?.groupValues?.getOrNull(1)
            val dateLabel = if (dateMillis > 0L) {
                DATE_FORMAT.format(Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()))
            } else ""

            NewsPost(
                id = stableKey,
                slug = slug,
                title = title,
                author = author.ifBlank { "osu!" },
                dateMillis = dateMillis,
                dateLabel = dateLabel,
                summary = textContent.take(240),
                imageUrl = image,
                url = link,
                contentHtml = html.takeIf { it.isNotBlank() },
                commentableId = realCommentId,
            )
        }
    }

    private fun tag(entry: String, name: String): String? =
        Regex("<${Regex.escape(name)}(?:\\s[^>]*)?>([\\s\\S]*?)</${Regex.escape(name)}>", RegexOption.IGNORE_CASE)
            .find(entry)?.groupValues?.get(1)

    private fun plainText(value: String): String =
        Jsoup.parseBodyFragment(decodeXmlContent(value)).text().trim()

    private fun decodeXmlContent(value: String): String =
        value.replace("<![CDATA[", "", ignoreCase = true)
            .replace("]]>", "")
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()

    private fun toPost(dto: NewsPostDto): NewsPost? {
        if (dto.slug.isBlank() || dto.title.isBlank()) return null
        val millis = parseDate(dto.publishedAt)
        val label = if (millis > 0L) {
            DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
        } else ""
        return NewsPost(
            id = dto.id,
            slug = dto.slug,
            title = dto.title,
            author = dto.author.ifBlank { "osu!" },
            dateMillis = millis,
            dateLabel = label,
            summary = dto.preview.orEmpty(),
            imageUrl = dto.firstImage2x ?: dto.firstImage,
            url = "https://osu.ppy.sh/home/news/${dto.slug}",
            contentHtml = dto.content,
            commentableId = dto.id.takeIf { it > 0L },
        )
    }

    private fun parseDate(value: String?): Long = value?.let {
        runCatching { Instant.parse(it).toEpochMilli() }
            .getOrElse { runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrDefault(0L) }
    } ?: 0L

    private fun markdownFallback(markdown: String): String =
        markdown.lineSequence().joinToString("<br>") { line ->
            line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        }

    companion object {
        const val SITE_URL = "https://osu.ppy.sh/home/news"
        const val FEED_URL = "$SITE_URL?format=atom"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    }
}
