package vn.kozu.data.news

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.HttpException
import vn.kozu.data.remote.osu.OsuApi

/** A lightweight view model for an osu! news post. Article HTML is fetched only when opened. */
data class NewsPost(
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
)

data class NewsFeedPage(
    val posts: List<NewsPost>,
    val years: List<Int>,
    val currentYear: Int,
    val cursorString: String? = null,
)

data class NewsPostsPage(
    val posts: List<NewsPost>,
    val cursorString: String?,
)

private fun NewsPost.year(): Int? {
    if (dateMillis <= 0L) return null
    return runCatching {
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).year
    }.getOrNull()
}

class NewsFeed(
    private val api: OsuApi,
    private val publicTokenProvider: suspend () -> String? = { null },
) {
    // Serialize first-page refreshes so repeated taps cannot launch overlapping loads.
    private val latestLoadMutex = Mutex()
    private val gson = Gson()

    /** Always requests fresh data. No persistent or in-memory article/feed cache is used. */
    suspend fun load(): NewsFeedPage = latestLoadMutex.withLock {
        val listing = requestListing()
        val newestPosts = listing.newsPosts.mapNotNull(::toPost)
        val sidebarPosts = listing.newsSidebar?.newsPosts.orEmpty().mapNotNull(::toPost)
        val posts = (sidebarPosts + newestPosts)
            .distinctBy { it.id }
            .sortedByDescending { it.dateMillis }
        val currentYear = listing.newsSidebar?.currentYear
            ?.takeIf { it > 0 }
            ?: posts.firstNotNullOfOrNull(NewsPost::year)
            ?: java.time.Year.now().value
        val years = listing.newsSidebar?.years.orEmpty()
            .filter { it in 2007..java.time.Year.now().value + 1 }
            .distinct()
            .sortedDescending()
        NewsFeedPage(
            posts = posts,
            years = years,
            currentYear = currentYear,
            cursorString = listing.cursorString,
        )
    }

    suspend fun loadYear(year: Int, cursorString: String? = null): NewsPostsPage {
        require(year in 2007..java.time.Year.now().value + 1) { "Invalid news archive year" }
        val listing = requestListing(year = year, cursorString = cursorString)
        return NewsPostsPage(
            posts = listing.newsPosts.mapNotNull(::toPost)
                .distinctBy { it.id }
                .sortedByDescending { it.dateMillis },
            cursorString = listing.cursorString,
        )
    }

    suspend fun loadNextPage(year: Int?, cursorString: String): NewsPostsPage {
        val listing = requestListing(year = year, cursorString = cursorString)
        return NewsPostsPage(
            posts = listing.newsPosts.mapNotNull(::toPost)
                .distinctBy { it.id }
                .sortedByDescending { it.dateMillis },
            cursorString = listing.cursorString,
        )
    }

    /** Fetches full article content on demand; only the lightweight list item is kept beforehand. */
    suspend fun loadDetail(post: NewsPost): NewsPost {
        val detail = requestNewsPost(post.slug)
        val content = detail.content
            ?.takeIf { it.isNotBlank() }
            ?: detail.contentMarkdown?.takeIf { it.isNotBlank() }?.let(::markdownFallback)
            ?: throw IOException("osu! returned an empty article")
        val mapped = toPost(detail) ?: throw IOException("osu! returned an invalid article")
        return mapped.copy(
            imageUrl = post.imageUrl ?: mapped.imageUrl,
            summary = post.summary.ifBlank { mapped.summary },
            contentHtml = content,
        )
    }

    private suspend fun requestListing(
        year: Int? = null,
        cursorString: String? = null,
    ): NewsListingDto = try {
        // osu!'s public website embeds the same listing payload and avoids the API 403
        // that this client can receive. Try it first to avoid a slow failed request.
        requestWebListing(year, cursorString)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (webError: Exception) {
        try {
            requestApiListing(year, cursorString)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (apiError: Exception) {
            throw IOException(
                "Unable to load osu! News (website: ${webError.message ?: "unavailable"}; API: ${apiError.message ?: "unavailable"})",
                webError,
            ).also { it.addSuppressed(apiError) }
        }
    }

    private suspend fun requestApiListing(year: Int?, cursorString: String?): NewsListingDto = try {
        api.news(limit = PAGE_SIZE, year = year, cursorString = cursorString)
    } catch (error: HttpException) {
        if (error.code() !in setOf(401, 403)) throw error
        val authorization = publicAuthorization() ?: throw error
        api.news(
            limit = PAGE_SIZE,
            year = year,
            cursorString = cursorString,
            authorization = authorization,
        )
    }

    /** The public HTML page embeds the same listing JSON used by osu!'s website React screen. */
    private suspend fun requestWebListing(year: Int?, cursorString: String?): NewsListingDto =
        withContext(Dispatchers.IO) {
            api.newsWebPage(year = year, cursorString = cursorString).use { response ->
                val document = Jsoup.parse(response.string(), SITE_URL)
                val props = document
                    .selectFirst(".js-react[data-react=news-index][data-props]")
                    ?.attr("data-props")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IOException("osu! News page did not include archive metadata")
                val root = JsonParser.parseString(props).asJsonObject
                val data = root.getAsJsonObject("data") ?: root
                gson.fromJson(data, NewsListingDto::class.java)
                    ?: throw IOException("osu! News page returned invalid listing data")
            }
        }

    private suspend fun requestNewsPost(slug: String): NewsPostDto = try {
        // Fetch the article page directly: its embedded JSON contains the full article
        // body, and avoids waiting for the API endpoint when it responds with 403.
        withContext(Dispatchers.IO) {
            api.newsWebArticle(slug).use { response ->
                val document = Jsoup.parse(response.string(), "$SITE_URL/$slug")
                val json = document.getElementById("json-show")
                    ?.let { it.data().ifBlank { it.html() } }
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IOException("osu! article page did not include article data")
                gson.fromJson(JsonParser.parseString(json), NewsPostDto::class.java)
                    ?: throw IOException("osu! article page returned invalid data")
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (webError: Exception) {
        try {
            requestApiNewsPost(slug)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (apiError: Exception) {
            throw IOException(
                "Unable to load this osu! article (website: ${webError.message ?: "unavailable"}; API: ${apiError.message ?: "unavailable"})",
                webError,
            ).also { it.addSuppressed(apiError) }
        }
    }

    private suspend fun requestApiNewsPost(slug: String): NewsPostDto = try {
        api.newsPost(slug, listOf("html"))
    } catch (error: HttpException) {
        if (error.code() !in setOf(401, 403)) throw error
        val authorization = publicAuthorization() ?: throw error
        api.newsPost(slug, listOf("html"), authorization)
    }

    private suspend fun publicAuthorization(): String? =
        runCatching {
            publicTokenProvider()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        }.getOrNull()

    private fun toPost(dto: NewsPostDto): NewsPost? {
        if (dto.slug.isBlank() || dto.title.isBlank()) return null
        val millis = parseDate(dto.publishedAt)
        val label = if (millis > 0L) {
            DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
        } else ""
        return NewsPost(
            id = dto.id.takeIf { it > 0L } ?: dto.slug.hashCode().toLong(),
            slug = dto.slug,
            title = dto.title,
            author = dto.author.ifBlank { "osu!" },
            dateMillis = millis,
            dateLabel = label,
            summary = dto.preview.orEmpty(),
            imageUrl = dto.firstImage ?: dto.firstImage2x,
            url = "$SITE_URL/${dto.slug}",
            contentHtml = dto.content,
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
        private const val PAGE_SIZE = 21
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    }
}
