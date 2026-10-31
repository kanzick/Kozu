package vn.kozu.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.remote.osu.BeatmapsetDto
import vn.kozu.data.remote.osu.OsuBeatmapDto
import vn.kozu.data.remote.osu.OsuApi
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficulty
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** One page of a catalog. Official cursor strings are opaque and must be reused unchanged. */
data class CatalogResult(
    val items: List<Beatmap>,
    val error: String? = null,
    val nextCursor: String? = null,
    val source: CatalogSource = CatalogSource.NONE
)

enum class CatalogSource {
    OFFICIAL_OSU,
    PUBLIC_MIRROR,
    MIXED,
    NONE
}

private data class CachedCatalogPage(
    val savedAtMs: Long,
    val result: CatalogResult
)

/**
 * osu! API v2 is the canonical catalog whenever an OAuth public token is
 * available. Public Hinai search is a no-auth fallback for guest browsing.
 * No OAuth secret is embedded in the APK.
 */
class BeatmapRepository(
    private val api: OsuApi,
    private val auth: OsuAuth,
    private val settings: SettingsStore,
    client: OkHttpClient
) {
    private val gson = Gson()

    private val publicClient = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val mixPageCache = ConcurrentHashMap<String, CachedCatalogPage>()
    private val maniaDifficultyCache = ConcurrentHashMap<Long, List<BeatmapDifficulty>>()

    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        category: SearchCategory? = null
    ): CatalogResult {
        // Search intentionally includes every osu! ruleset and exactly two
        // public statuses. The former Settings > Content mode/status choices
        // are ignored so old saved preferences cannot silently narrow Search.
        return searchRankedAndLoved(
            query = query,
            sort = sort.apiValue,
            genre = category?.genreId,
            mode = null
        )
    }

    suspend fun searchMania(query: String, page: Int): CatalogResult {
        require(page >= 0) { "Page must not be negative" }
        return fetchHinaiSearch(
            query = query.trim(),
            status = null,
            sort = null,
            genre = null,
            mode = 3,
            page = page,
            pageSize = MANIA_PAGE_SIZE
        )
    }

    suspend fun maniaDifficulties(id: Long): List<BeatmapDifficulty> {
        require(id > 0L) { "Beatmap set ID must be positive" }
        maniaDifficultyCache[id]?.let { return it }

        val bearer = try {
            auth.bearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (!bearer.isNullOrBlank()) {
            try {
                val difficulties = api.beatmapset("******", id).beatmaps.orEmpty()
                    .mapNotNull { it.toDifficulty() }
                    .filter { it.mode == "mania" }
                if (difficulties.isNotEmpty()) {
                    maniaDifficultyCache[id] = difficulties
                    return difficulties
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                try {
                    val fresh = auth.bearer(force = true)
                    if (!fresh.isNullOrBlank()) {
                        val difficulties = api.beatmapset("******", id).beatmaps.orEmpty()
                            .mapNotNull { it.toDifficulty() }
                            .filter { it.mode == "mania" }
                        if (difficulties.isNotEmpty()) {
                            maniaDifficultyCache[id] = difficulties
                            return difficulties
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Fall back to the public beatmapset endpoint below.
                }
            }
        }

        val difficulties = withContext(Dispatchers.IO) {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host(HINAI_HOST)
                .addPathSegments("v3/osu/beatmaps/s/$id")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            publicClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Could not load map difficulties (HTTP ${response.code})")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) emptyList() else parseManiaDifficulties(JSONObject(body))
            }
        }
        if (difficulties.isEmpty()) throw IOException("No Mania difficulty details were returned for this map")
        maniaDifficultyCache[id] = difficulties
        return difficulties
    }

    private suspend fun searchRankedAndLoved(
        query: String,
        sort: String?,
        genre: Int?,
        mode: Int?
    ): CatalogResult = coroutineScope {
        val rankedRequest = async {
            fetch(
                query = query,
                status = "ranked",
                sort = sort,
                genre = genre,
                mode = mode
            )
        }
        val lovedRequest = async {
            fetch(
                query = query,
                status = "loved",
                sort = sort,
                genre = genre,
                mode = mode
            )
        }

        val ranked = rankedRequest.await()
        val loved = lovedRequest.await()
        // Interleave both result sets so a full Ranked page cannot push
        // Loved results completely out of the first visible search page.
        val rankedItems = ranked.items.filter {
            it.status.equals("ranked", ignoreCase = true)
        }
        val lovedItems = loved.items.filter {
            it.status.equals("loved", ignoreCase = true)
        }

        val merged = buildList {
            val pageCount = maxOf(rankedItems.size, lovedItems.size)
            for (index in 0 until pageCount) {
                rankedItems.getOrNull(index)?.let { add(it) }
                lovedItems.getOrNull(index)?.let { add(it) }
            }
        }.distinctBy { it.id }.take(RESULT_LIMIT)

        val error = if (merged.isNotEmpty()) {
            null
        } else {
            ranked.error ?: loved.error
        }

        CatalogResult(
            items = merged,
            error = error,
            source = when {
                ranked.source == loved.source -> ranked.source
                ranked.source == CatalogSource.NONE -> loved.source
                loved.source == CatalogSource.NONE -> ranked.source
                else -> CatalogSource.MIXED
            }
        )
    }

    suspend fun trending(): CatalogResult = fetch(
        query = "",
        status = "ranked",
        sort = "plays_desc",
        genre = null,
        mode = null
    )

    /** Newest releases are limited to the two requested catalog states. */
    suspend fun newReleases(): CatalogResult = searchRankedAndLoved(
        query = "",
        sort = "updated_desc",
        genre = null,
        mode = null
    )

    suspend fun lovedPicks(): CatalogResult = fetch(
        query = "",
        status = "loved",
        sort = "plays_desc",
        genre = null,
        mode = null
    )

    /**
     * Mix pagination sourced from osu! official first, with a public mirror
     * fallback. Only Ranked and Loved are queried, across every game mode.
     */
    suspend fun mixPage(cursor: String?, sort: String): CatalogResult {
        val cacheKey = "$sort:${cursor.orEmpty()}"
        mixPageCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.savedAtMs < MIX_CACHE_TTL_MS) {
                return cached.result
            }
        }

        // The official osu! API is the canonical catalogue. Guests (or users
        // whose token cannot be refreshed) use Hinai's public index instead.
        // Both paths request Ranked and Loved separately so no Pending, WIP,
        // Graveyard, Approved or Qualified rows can leak into the visible Mix.
        val result = when {
            cursor?.startsWith(HINAI_MIX_CURSOR_PREFIX) == true -> {
                fetchHinaiMixPage(cursor, sort)
            }
            cursor?.startsWith(OSU_MIX_CURSOR_PREFIX) == true -> {
                officialMixPage(cursor, sort) ?: CatalogResult(
                    items = emptyList(),
                    error = "The official osu! session expired. Refresh All beatmaps to reconnect.",
                    source = CatalogSource.NONE
                )
            }
            cursor == null -> {
                val official = officialMixPage(null, sort)
                when {
                    official != null && official.error == null -> official
                    // Hinai's public search index supports ranked_desc/plays_desc,
                    // but not osu!'s updated_desc/updated_asc sort fields. Do not
                    // call the same popular fallback twice and pretend it is the
                    // oldest end of history. Guests get one sequential Ranked+Loved
                    // stream, ordered by rank date rather than play count.
                    sort == "updated_asc" -> CatalogResult(
                        items = emptyList(),
                        source = CatalogSource.PUBLIC_MIRROR
                    )
                    else -> fetchHinaiMixPage(null, "ranked_desc")
                }
            }
            else -> CatalogResult(
                items = emptyList(),
                error = "This catalogue page expired. Refresh All beatmaps to start again.",
                source = CatalogSource.NONE
            )
        }

        if (result.error == null) {
            if (mixPageCache.size >= MAX_MIX_CACHE_ENTRIES) {
                val oldest = mixPageCache.entries.minByOrNull { it.value.savedAtMs }?.key
                if (oldest != null) mixPageCache.remove(oldest)
            }
            mixPageCache[cacheKey] = CachedCatalogPage(
                savedAtMs = System.currentTimeMillis(),
                result = result
            )
        }
        return result
    }

    private suspend fun officialMixPage(
        cursor: String?,
        sort: String
    ): CatalogResult? = coroutineScope {
        val pair = decodePairCursor(cursor, OSU_MIX_CURSOR_PREFIX) ?: StatusPageCursor()

        val rankedRequest = async {
            if (pair.ranked == END_CURSOR) {
                CatalogResult(emptyList(), source = CatalogSource.OFFICIAL_OSU)
            } else {
                officialPage(
                    query = "",
                    status = "ranked",
                    sort = sort,
                    genre = null,
                    mode = null,
                    nsfw = false,
                    cursor = pair.ranked
                )
            }
        }
        val lovedRequest = async {
            if (pair.loved == END_CURSOR) {
                CatalogResult(emptyList(), source = CatalogSource.OFFICIAL_OSU)
            } else {
                officialPage(
                    query = "",
                    status = "loved",
                    sort = sort,
                    genre = null,
                    mode = null,
                    nsfw = false,
                    cursor = pair.loved
                )
            }
        }

        val ranked = rankedRequest.await() ?: return@coroutineScope null
        val loved = lovedRequest.await() ?: return@coroutineScope null

        if (ranked.error != null || loved.error != null) {
            return@coroutineScope CatalogResult(
                items = interleave(ranked.items, loved.items),
                error = ranked.error ?: loved.error,
                source = CatalogSource.OFFICIAL_OSU
            )
        }

        val next = encodePairCursor(
            prefix = OSU_MIX_CURSOR_PREFIX,
            ranked = ranked.nextCursor ?: END_CURSOR,
            loved = loved.nextCursor ?: END_CURSOR
        )

        CatalogResult(
            items = interleave(
                ranked.items.filter { it.status.equals("ranked", true) },
                loved.items.filter { it.status.equals("loved", true) }
            ),
            nextCursor = next,
            source = CatalogSource.OFFICIAL_OSU
        )
    }

    private suspend fun fetchHinaiMixPage(
        cursor: String?,
        sort: String
    ): CatalogResult = coroutineScope {
        val pair = decodePairCursor(cursor, HINAI_MIX_CURSOR_PREFIX) ?: StatusPageCursor()
        val rankedPage = pair.ranked?.toIntOrNull() ?: 0
        val lovedPage = pair.loved?.toIntOrNull() ?: 0
        val hinaiSort = when (sort) {
            "updated_desc", "updated_asc" -> "ranked_desc"
            else -> sort
        }

        // Hinai v2 accepts one exact status at a time (ranked or loved), not
        // a CSV of statuses. Sending a CSV was ignored by the API and allowed
        // Pending/WIP rows to appear in All beatmaps on guest accounts.
        val rankedRequest = async {
            if (pair.ranked == END_CURSOR) {
                CatalogResult(emptyList(), source = CatalogSource.PUBLIC_MIRROR)
            } else {
                fetchHinaiSearch(
                    query = "",
                    status = "ranked",
                    sort = hinaiSort,
                    genre = null,
                    mode = null,
                    page = rankedPage,
                    pageSize = MIX_STATUS_PAGE_SIZE
                )
            }
        }
        val lovedRequest = async {
            if (pair.loved == END_CURSOR) {
                CatalogResult(emptyList(), source = CatalogSource.PUBLIC_MIRROR)
            } else {
                fetchHinaiSearch(
                    query = "",
                    status = "loved",
                    sort = hinaiSort,
                    genre = null,
                    mode = null,
                    page = lovedPage,
                    pageSize = MIX_STATUS_PAGE_SIZE
                )
            }
        }

        val ranked = rankedRequest.await()
        val loved = lovedRequest.await()

        val rankedNext = if (ranked.nextCursor == null) {
            END_CURSOR
        } else {
            ranked.nextCursor.removePrefix(HINAI_OFFSET_PREFIX)
        }
        val lovedNext = if (loved.nextCursor == null) {
            END_CURSOR
        } else {
            loved.nextCursor.removePrefix(HINAI_OFFSET_PREFIX)
        }

        CatalogResult(
            items = interleave(
                ranked.items.filter { it.status.equals("ranked", true) },
                loved.items.filter { it.status.equals("loved", true) }
            ),
            error = if (ranked.items.isEmpty() && loved.items.isEmpty()) {
                ranked.error ?: loved.error
            } else {
                null
            },
            nextCursor = encodePairCursor(
                prefix = HINAI_MIX_CURSOR_PREFIX,
                ranked = rankedNext,
                loved = lovedNext
            ),
            source = CatalogSource.PUBLIC_MIRROR
        )
    }

    private fun interleave(
        ranked: List<Beatmap>,
        loved: List<Beatmap>
    ): List<Beatmap> = buildList {
        val count = maxOf(ranked.size, loved.size)
        for (index in 0 until count) {
            ranked.getOrNull(index)?.let { add(it) }
            loved.getOrNull(index)?.let { add(it) }
        }
    }.distinctBy { it.id }

    private data class StatusPageCursor(
        val ranked: String? = null,
        val loved: String? = null
    )

    private fun encodePairCursor(
        prefix: String,
        ranked: String?,
        loved: String?
    ): String? {
        if (
            (ranked == null || ranked == END_CURSOR) &&
            (loved == null || loved == END_CURSOR)
        ) return null
        val json = JSONObject()
            .put("ranked", ranked)
            .put("loved", loved)
            .toString()
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
        return prefix + encoded
    }

    private fun decodePairCursor(
        value: String?,
        prefix: String
    ): StatusPageCursor? {
        if (value == null) return StatusPageCursor()
        if (!value.startsWith(prefix)) return null
        return try {
            val encoded = value.removePrefix(prefix)
            val json = String(
                Base64.getUrlDecoder().decode(encoded),
                Charsets.UTF_8
            )
            val objectValue = JSONObject(json)
            StatusPageCursor(
                ranked = objectValue.optString("ranked").takeIf { it.isNotBlank() && it != "null" },
                loved = objectValue.optString("loved").takeIf { it.isNotBlank() && it != "null" }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearMixCache() {
        mixPageCache.clear()
    }

    suspend fun details(id: Long): Beatmap? {
        if (id <= 0L) return null

        val bearer = try {
            auth.bearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        if (!bearer.isNullOrBlank()) {
            try {
                return api.beatmapset("Bearer $bearer", id).toBeatmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                try {
                    val fresh = auth.bearer(force = true)
                    if (!fresh.isNullOrBlank()) {
                        return api.beatmapset("Bearer $fresh", id).toBeatmap()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Continue to public metadata mirrors.
                }
            }
        }

        val hinai = try {
            withContext(Dispatchers.IO) {
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host(HINAI_HOST)
                    .addPathSegments("v3/osu/beatmaps/s/$id")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
                publicClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) null else parseDetail(JSONObject(body), id)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        return hinai ?: mirrorDetails(id)
    }

    suspend fun verifyCredentials(): Result<Unit> = try {
        val token = auth.clientBearer(force = true)
        if (token == null) {
            Result.failure(IllegalStateException("Enter both client ID and secret first"))
        } else {
            Result.success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException(describe(e), e))
    }

    private suspend fun fetch(
        query: String,
        status: String?,
        sort: String?,
        genre: Int?,
        mode: Int?
    ): CatalogResult {
        val official = officialPage(
            query = query,
            status = status ?: "any",
            sort = sort,
            genre = genre,
            mode = mode,
            nsfw = false,
            cursor = null
        )
        if (official != null && official.error == null) return official

        // Public fallback uses the same exact status as the official request.
        // In particular, it never leaves status blank for a Ranked/Loved query.
        return fetchHinaiSearch(
            query = query,
            status = status?.takeUnless { it == "any" },
            sort = sort,
            genre = genre,
            mode = mode,
            page = 0,
            pageSize = RESULT_LIMIT
        )
    }

    private suspend fun officialPage(
        query: String,
        status: String,
        sort: String?,
        genre: Int?,
        mode: Int?,
        nsfw: Boolean,
        cursor: String?
    ): CatalogResult? {
        val bearer = try {
            auth.bearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null

        return try {
            val response = try {
                api.searchBeatmapsets(
                    authorization = "Bearer $bearer",
                    query = query,
                    status = status,
                    sort = sort,
                    genre = genre,
                    mode = mode,
                    nsfw = nsfw,
                    cursorString = cursor
                )
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val fresh = auth.bearer(force = true) ?: throw e
                api.searchBeatmapsets(
                    authorization = "Bearer $fresh",
                    query = query,
                    status = status,
                    sort = sort,
                    genre = genre,
                    mode = mode,
                    nsfw = nsfw,
                    cursorString = cursor
                )
            }

            val items = response.beatmapsets.orEmpty()
                .mapNotNull { it.toBeatmap() }
                .filter { beatmap ->
                    status == "any" || beatmap.status.equals(status, ignoreCase = true)
                }

            CatalogResult(
                items = items,
                nextCursor = response.cursorString,
                source = CatalogSource.OFFICIAL_OSU
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatalogResult(
                items = emptyList(),
                error = describe(e),
                nextCursor = cursor,
                source = CatalogSource.OFFICIAL_OSU
            )
        }
    }

    private suspend fun fetchHinaiSearch(
        query: String,
        status: String?,
        sort: String?,
        genre: Int?,
        mode: Int?,
        page: Int,
        pageSize: Int
    ): CatalogResult {
        return try {
            withContext(Dispatchers.IO) {
                val builder = HttpUrl.Builder()
                    .scheme("https")
                    .host(HINAI_HOST)
                    .addPathSegments("v3/osu/beatmaps/search/v2")
                    .addQueryParameter("query", query)
                    .addQueryParameter("limit", pageSize.toString())
                    .addQueryParameter("page", page.toString())
                    .addQueryParameter("explicit", "hide")

                status?.takeUnless { it == "any" }?.let {
                    builder.addQueryParameter("status", it)
                }
                mode?.let {
                    builder.addQueryParameter("mode", it.toString())
                }
                genre?.let {
                    builder.addQueryParameter("genre", it.toString())
                }
                sort?.let { value ->
                    val supportedSort = when (value) {
                        "updated_desc", "updated_asc" -> "ranked_desc"
                        else -> value
                    }
                    builder.addQueryParameter("sort", supportedSort)
                }

                val request = Request.Builder()
                    .url(builder.build())
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                publicClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Hinai search returned HTTP ${response.code}")
                    }

                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        CatalogResult(
                            items = emptyList(),
                            error = "The public catalog returned an empty response",
                            source = CatalogSource.PUBLIC_MIRROR
                        )
                    } else {
                        val root = org.json.JSONTokener(body).nextValue()
                        val rootObject = root as? JSONObject
                        val payload = rootObject?.optJSONObject("data") ?: rootObject
                        val array = when (root) {
                            is JSONArray -> root
                            is JSONObject -> payload?.optJSONArray("beatmapsets")
                                ?: payload?.optJSONArray("results")
                                ?: payload?.optJSONArray("items")
                                ?: payload?.optJSONArray("data")
                                ?: root.optJSONArray("beatmapsets")
                                ?: root.optJSONArray("results")
                                ?: root.optJSONArray("items")
                                ?: JSONArray()
                            else -> JSONArray()
                        }
                        val items = buildList {
                            for (index in 0 until array.length()) {
                                val item = array.optJSONObject(index) ?: continue
                                parseHinaiSet(item)?.let { beatmap ->
                                    if (
                                        status == null || status == "any" ||
                                        beatmap.status.equals(status, ignoreCase = true)
                                    ) {
                                        add(beatmap)
                                    }
                                }
                            }
                        }.distinctBy { it.id }

                        val totalPages = payload?.optInt("total_pages", -1)
                            ?: rootObject?.optInt("total_pages", -1)
                            ?: -1
                        val hasNext = when {
                            totalPages >= 0 -> page + 1 < totalPages
                            else -> array.length() >= pageSize
                        }
                        val next = if (hasNext) {
                            "$HINAI_OFFSET_PREFIX${page + 1}"
                        } else {
                            null
                        }

                        CatalogResult(
                            items = items,
                            nextCursor = next,
                            source = CatalogSource.PUBLIC_MIRROR
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatalogResult(
                items = emptyList(),
                error = describe(e),
                nextCursor = if (pageSize == MIX_STATUS_PAGE_SIZE) {
                    "$HINAI_OFFSET_PREFIX$page"
                } else {
                    null
                },
                source = CatalogSource.PUBLIC_MIRROR
            )
        }
    }

    private suspend fun mirrorDetails(id: Long): Beatmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://catboy.best/api/v2/s/$id")
                .header("User-Agent", USER_AGENT)
                .build()
            publicClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                gson.fromJson(body, BeatmapsetDto::class.java)?.toBeatmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun parseHinaiSet(item: JSONObject): Beatmap? {
        val id = item.optLong("id", item.optLong("SetID", 0L))
        val title = item.optString("title", item.optString("Title"))
            .takeUnless { it.isBlank() || it == "null" } ?: return null
        if (id <= 0L) return null

        val rawStatus = item.opt("status") ?: item.opt("RankedStatus")
        val status = normalizeStatus(rawStatus)
        val updated = item.optString(
            "lastUpdated",
            item.optString("last_updated", item.optString("LastUpdate"))
        )

        return Beatmap(
            id = id,
            title = title,
            artist = item.optString("artist", item.optString("Artist"))
                .takeUnless { it == "null" }.orEmpty(),
            mapper = item.optString("creator", item.optString("Creator"))
                .takeUnless { it == "null" }.orEmpty(),
            status = status,
            updatedAt = parseInstant(updated)
        )
    }

    private fun parseDetail(root: JSONObject, fallbackId: Long): Beatmap? {
        val data = root.optJSONObject("data")
            ?: root.optJSONObject("beatmapset")
            ?: root
        val id = data.optLong("id", data.optLong("SetID", fallbackId))
        val title = data.optString("title", data.optString("Title"))
            .takeUnless { it.isBlank() || it == "null" } ?: return null
        val statusRaw = data.opt("status") ?: data.opt("RankedStatus")
        val status = normalizeStatus(statusRaw)
        return Beatmap(
            id = id,
            title = title,
            artist = data.optString("artist", data.optString("Artist")).orEmpty(),
            mapper = data.optString("creator", data.optString("Creator")).orEmpty(),
            status = status,
            updatedAt = parseInstant(
                data.optString(
                    "lastUpdated",
                    data.optString("last_updated", data.optString("LastUpdate"))
                )
            )
        )
    }


    /** Normalize osu!/Hinai status values before applying the strict Ranked+Loved filter. */
    private fun normalizeStatus(raw: Any?): String {
        val value = when (raw) {
            is Number -> raw.toInt().toString()
            null, JSONObject.NULL -> ""
            else -> raw.toString().trim()
        }
        return when (value.lowercase(Locale.US)) {
            "-2" -> "graveyard"
            "-1" -> "wip"
            "0" -> "pending"
            "1" -> "ranked"
            "2" -> "approved"
            "3" -> "qualified"
            "4" -> "loved"
            "ranked", "loved", "approved", "qualified", "pending", "wip", "graveyard" ->
                value.lowercase(Locale.US)
            else -> ""
        }
    }

    private fun BeatmapsetDto.toBeatmap(): Beatmap? {
        val safeTitle = title ?: return null
        if (id <= 0L) return null
        return Beatmap(
            id = id,
            title = safeTitle,
            artist = artist.orEmpty(),
            mapper = creator.orEmpty(),
            status = status.orEmpty(),
            updatedAt = parseInstant(lastUpdated)
        )
    }

    private fun OsuBeatmapDto.toDifficulty(): BeatmapDifficulty? {
        val ruleSet = mode?.lowercase(Locale.US)
            ?: when (modeInt) {
                0 -> "osu"
                1 -> "taiko"
                2 -> "fruits"
                3 -> "mania"
                else -> ""
            }
        if (ruleSet.isBlank() || version.isNullOrBlank()) return null
        return BeatmapDifficulty(
            id = id,
            version = version,
            mode = ruleSet,
            difficultyRating = difficultyRating ?: 0.0,
            keys = circleSize ?: 0.0,
            bpm = bpm ?: 0.0,
            lengthSeconds = totalLength ?: 0,
        )
    }

    private fun parseManiaDifficulties(root: JSONObject): List<BeatmapDifficulty> {
        val data = root.optJSONObject("data")
            ?: root.optJSONObject("beatmapset")
            ?: root
        val beatmaps = data.optJSONArray("beatmaps")
            ?: data.optJSONArray("maps")
            ?: root.optJSONArray("beatmaps")
            ?: root.optJSONArray("maps")
            ?: return emptyList()

        return buildList {
            for (index in 0 until beatmaps.length()) {
                val beatmap = beatmaps.optJSONObject(index) ?: continue
                val modeInt = beatmap.optInt("mode_int", beatmap.optInt("mode", -1))
                val modeName = beatmap.optString("mode", "").lowercase(Locale.US)
                if (modeInt != 3 && modeName != "mania") continue
                val version = beatmap.optString("version", beatmap.optString("Version"))
                    .takeUnless { it.isBlank() || it == "null" } ?: continue
                add(
                    BeatmapDifficulty(
                        id = beatmap.optLong("id", beatmap.optLong("BeatmapID")),
                        version = version,
                        mode = "mania",
                        difficultyRating = beatmap.optDouble(
                            "difficulty_rating",
                            beatmap.optDouble("star_rating", beatmap.optDouble("Stars"))
                        ),
                        keys = beatmap.optDouble("cs", beatmap.optDouble("CircleSize")),
                        bpm = beatmap.optDouble("bpm", beatmap.optDouble("BPM")),
                        lengthSeconds = beatmap.optInt("total_length", beatmap.optInt("hit_length")),
                    )
                )
            }
        }
    }

    private fun describe(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            400, 401 -> "osu! rejected the catalog request (HTTP ${e.code()})"
            429 -> "The catalog is rate-limited; try again shortly"
            else -> "Catalog API error (HTTP ${e.code()})"
        }
        is IOException -> e.message ?: "No connection to the beatmap catalog"
        else -> e.message ?: "Unknown catalog error"
    }

    companion object {
        private const val HINAI_HOST = "mirror.hinamizawa.ai"
        private const val RESULT_LIMIT = 30
        private const val MANIA_PAGE_SIZE = 20
        private const val MIX_CACHE_TTL_MS = 5L * 60L * 1000L
        private const val MAX_MIX_CACHE_ENTRIES = 20
        private const val HINAI_OFFSET_PREFIX = "hinai-page:"
        private const val OSU_MIX_CURSOR_PREFIX = "osu-ranked-loved:"
        private const val END_CURSOR = "__END__"
        private const val HINAI_MIX_CURSOR_PREFIX = "hinai-ranked-loved:"
        private const val MIX_STATUS_PAGE_SIZE = 50
        private const val USER_AGENT = "Kozu/2.5 (Android; https://github.com/kanzick/Kozu)"

        private val hinaiDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        )

        fun parseInstant(value: String?): Long {
            if (value.isNullOrBlank()) return 0L
            return try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(value, hinaiDateFormatter)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }
}
