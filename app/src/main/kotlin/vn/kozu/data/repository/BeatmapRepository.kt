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
import vn.kozu.domain.model.BeatmapDifficultyInfo
import vn.kozu.domain.model.BeatmapSetInfo
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseLanguage
import vn.kozu.domain.model.BrowsePlayed
import vn.kozu.domain.model.BrowseSortField
import vn.kozu.domain.model.BrowseStatus
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

data class CatalogResult(
    val items: List<Beatmap>,
    val error: String? = null,
    val nextCursor: String? = null,
    val source: CatalogSource = CatalogSource.NONE,
    val total: Int? = null,
    val ignoredFilters: List<String> = emptyList()
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

private data class CachedSetInfo(
    val savedAtMs: Long,
    val info: BeatmapSetInfo
)

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
    private val setInfoCache = ConcurrentHashMap<Long, CachedSetInfo>()

    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        category: SearchCategory? = null
    ): CatalogResult {

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
                val body = response.body.string()
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

    suspend fun mixPage(cursor: String?, sort: String): CatalogResult {
        val cacheKey = "$sort:${cursor.orEmpty()}"
        mixPageCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.savedAtMs < MIX_CACHE_TTL_MS) {
                return cached.result
            }
        }

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
                    val body = response.body.string()
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

    suspend fun browse(filters: BrowseFilters, cursor: String? = null): CatalogResult {
        val signedIn = try {
            auth.loggedIn.first()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        if (!signedIn && filters.status.requiresLogin) {
            return CatalogResult(
                items = emptyList(),
                error = "Sign in with your osu! account to browse ${filters.status.label.lowercase(Locale.US)}"
            )
        }
        val effective = if (signedIn) filters else filters.withoutUserScoped()
        val stripped = if (effective == filters) emptyList() else userScopedLabels(filters)

        if (cursor != null && (cursor.startsWith(HINAI_OFFSET_PREFIX) || cursor.startsWith(HINAI_LEADERBOARD_PREFIX))) {
            return hinaiBrowse(effective, cursor, stripped)
        }

        val official = officialBrowse(effective, cursor)
        if (official != null && official.error == null) {
            return official.copy(ignoredFilters = stripped)
        }
        if (cursor != null || effective.status.requiresLogin) {
            return official ?: CatalogResult(
                items = emptyList(),
                error = "The official osu! session expired. Refresh to reconnect.",
                nextCursor = cursor
            )
        }
        return hinaiBrowse(effective, null, stripped)
    }

    suspend fun setInfo(id: Long): BeatmapSetInfo? {
        if (id <= 0L) return null
        setInfoCache[id]?.let { cached ->
            if (System.currentTimeMillis() - cached.savedAtMs < SET_INFO_TTL_MS) return cached.info
        }
        val info = officialSetInfo(id) ?: mirrorSetInfo(id)
        if (info != null) {
            if (setInfoCache.size >= MAX_SET_INFO_ENTRIES) {
                setInfoCache.entries.minByOrNull { it.value.savedAtMs }?.key?.let { setInfoCache.remove(it) }
            }
            setInfoCache[id] = CachedSetInfo(System.currentTimeMillis(), info)
        }
        return info
    }

    private fun userScopedLabels(filters: BrowseFilters): List<String> = buildList {
        filters.general.filter { it.requiresLogin }.forEach { add(it.label) }
        if (filters.played != BrowsePlayed.ANY) add("Played")
        if (filters.ranks.isNotEmpty()) add("Rank achieved")
    }

    private suspend fun officialBrowse(filters: BrowseFilters, cursor: String?): CatalogResult? {
        val bearer = try {
            auth.bearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null

        return try {
            val response = try {
                searchOfficial(bearer, filters, cursor)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val fresh = auth.bearer(force = true) ?: throw e
                searchOfficial(fresh, filters, cursor)
            }
            CatalogResult(
                items = response.beatmapsets.orEmpty().mapNotNull { it.toBeatmap() }.distinctBy { it.id },
                nextCursor = response.cursorString?.takeIf { it.isNotBlank() },
                source = CatalogSource.OFFICIAL_OSU,
                total = response.total
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

    private suspend fun searchOfficial(bearer: String, filters: BrowseFilters, cursor: String?) =
        api.searchBeatmapsets(
            authorization = "Bearer $bearer",
            query = filters.trimmedQuery,
            status = filters.statusParam,
            sort = filters.sortParam,
            genre = filters.genreParam,
            mode = filters.modeParam,
            nsfw = filters.explicit,
            cursorString = cursor,
            language = filters.languageParam,
            extra = filters.extraParam,
            general = filters.generalParam,
            rank = filters.rankParam,
            played = filters.playedParam
        )

    private suspend fun hinaiBrowse(
        filters: BrowseFilters,
        cursor: String?,
        ignored: List<String>
    ): CatalogResult {
        val field = filters.sort.option
        val mirrorSort = when (field) {
            BrowseSortField.RELEVANCE, BrowseSortField.RATING -> null
            else -> filters.sortParam
        }
        val ignoredAll = buildList {
            addAll(ignored)
            if (filters.language != BrowseLanguage.ANY) add("Language")
            if (filters.extras.isNotEmpty()) add("Extra")
            if (filters.general.isNotEmpty()) add("General")
            if (filters.played != BrowsePlayed.ANY) add("Played")
            if (filters.ranks.isNotEmpty()) add("Rank achieved")
            if (field == BrowseSortField.RATING) add("Sort: ${field.label}")
        }.distinct()

        val result = if (filters.status == BrowseStatus.HAS_LEADERBOARD) {
            hinaiLeaderboardPage(filters, cursor, mirrorSort)
        } else {
            fetchHinaiSearch(
                query = filters.trimmedQuery,
                status = filters.statusParam?.takeUnless { it == "any" },
                sort = mirrorSort,
                genre = filters.genreParam,
                mode = filters.modeParam,
                page = cursor?.removePrefix(HINAI_OFFSET_PREFIX)?.toIntOrNull() ?: 0,
                pageSize = BROWSE_PAGE_SIZE,
                rawSort = true,
                explicit = filters.explicit
            )
        }
        return result.copy(ignoredFilters = ignoredAll)
    }

    private suspend fun hinaiLeaderboardPage(
        filters: BrowseFilters,
        cursor: String?,
        sort: String?
    ): CatalogResult = coroutineScope {
        val pages = cursor?.removePrefix(HINAI_LEADERBOARD_PREFIX)?.split(":")
        val rankedPage = pages?.getOrNull(0)?.toIntOrNull() ?: 0
        val lovedPage = pages?.getOrNull(1)?.toIntOrNull() ?: 0

        suspend fun request(status: String, page: Int): CatalogResult =
            if (page < 0) {
                CatalogResult(emptyList(), source = CatalogSource.PUBLIC_MIRROR)
            } else {
                fetchHinaiSearch(
                    query = filters.trimmedQuery,
                    status = status,
                    sort = sort,
                    genre = filters.genreParam,
                    mode = filters.modeParam,
                    page = page,
                    pageSize = BROWSE_PAGE_SIZE,
                    rawSort = true,
                    explicit = filters.explicit
                )
            }

        val rankedRequest = async { request("ranked", rankedPage) }
        val lovedRequest = async { request("loved", lovedPage) }
        val ranked = rankedRequest.await()
        val loved = lovedRequest.await()

        val error = ranked.error ?: loved.error
        if (error != null) {
            return@coroutineScope CatalogResult(
                items = emptyList(),
                error = error,
                nextCursor = cursor,
                source = CatalogSource.PUBLIC_MIRROR
            )
        }

        val rankedNext = if (rankedPage < 0 || ranked.nextCursor == null) -1 else rankedPage + 1
        val lovedNext = if (lovedPage < 0 || loved.nextCursor == null) -1 else lovedPage + 1
        val rankedTotal = ranked.total
        val lovedTotal = loved.total

        CatalogResult(
            items = interleave(ranked.items, loved.items),
            nextCursor = if (rankedNext < 0 && lovedNext < 0) {
                null
            } else {
                "$HINAI_LEADERBOARD_PREFIX$rankedNext:$lovedNext"
            },
            source = CatalogSource.PUBLIC_MIRROR,
            total = if (rankedTotal != null && lovedTotal != null) rankedTotal + lovedTotal else null
        )
    }

    private suspend fun officialSetInfo(id: Long): BeatmapSetInfo? {
        val bearer = try {
            auth.bearer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null

        return try {
            val dto = try {
                api.beatmapset("Bearer $bearer", id)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val fresh = auth.bearer(force = true) ?: throw e
                api.beatmapset("Bearer $fresh", id)
            }
            dto.toSetInfo(reduced = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun mirrorSetInfo(id: Long): BeatmapSetInfo? {
        publicGet("https://$HINAI_HOST/v3/osu/beatmaps/s/$id")?.let { body ->
            parseSetInfo(body, id)?.let { return it }
        }
        return publicGet("https://catboy.best/api/v2/s/$id")?.let { body ->
            try {
                gson.fromJson(body, BeatmapsetDto::class.java)?.toSetInfo(reduced = true)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseSetInfo(body: String, id: Long): BeatmapSetInfo? = try {
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: root.optJSONObject("beatmapset") ?: root
        val parsed = try {
            gson.fromJson(data.toString(), BeatmapsetDto::class.java)?.toSetInfo(reduced = true)
        } catch (_: Exception) {
            null
        }
        parsed ?: parseDetail(root, id)?.let { BeatmapSetInfo(beatmap = it, isReduced = true) }
    } catch (_: Exception) {
        null
    }

    private suspend fun publicGet(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            publicClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.string().takeIf { it.isNotBlank() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
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
        pageSize: Int,
        language: Int? = null,
        rawSort: Boolean = false,
        explicit: Boolean = false
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
                if (!explicit) builder.addQueryParameter("explicit", "hide")

                status?.takeUnless { it == "any" }?.let {
                    builder.addQueryParameter("status", it)
                }
                mode?.let {
                    builder.addQueryParameter("mode", it.toString())
                }
                genre?.let {
                    builder.addQueryParameter("genre", it.toString())
                }
                language?.let {
                    builder.addQueryParameter("language", it.toString())
                }
                sort?.let { value ->
                    val supportedSort = when {
                        rawSort -> value
                        value == "updated_desc" || value == "updated_asc" -> "ranked_desc"
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

                    val body = response.body.string()
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
                            else -> array.length() >= pageSize ||
                                (pageSize == MANIA_PAGE_SIZE && array.length() > 0)
                        }
                        val next = if (hasNext) {
                            "$HINAI_OFFSET_PREFIX${page + 1}"
                        } else {
                            null
                        }
                        val total = rootObject?.optInt("total_count", -1)?.takeIf { it >= 0 }
                            ?: payload?.optInt("total_count", -1)?.takeIf { it >= 0 }

                        CatalogResult(
                            items = items,
                            nextCursor = next,
                            source = CatalogSource.PUBLIC_MIRROR,
                            total = total
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
                val body = response.body.string()
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
            updatedAt = parseInstant(updated),
            modes = parseModes(item.optJSONArray("beatmaps"))
        ).withExtras(item)
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
            ),
            modes = parseModes(data.optJSONArray("beatmaps"))
        ).withExtras(data)
    }

    private fun Beatmap.withExtras(item: JSONObject): Beatmap {
        val stars = buildList {
            val maps = item.optJSONArray("beatmaps") ?: return@buildList
            for (index in 0 until maps.length()) {
                val rating = maps.optJSONObject(index)?.optDouble("difficulty_rating", 0.0) ?: 0.0
                if (rating > 0.0) add(rating)
            }
        }
        val setBpm = item.optDouble("bpm", 0.0)
        return copy(
            titleUnicode = item.optText("title_unicode"),
            artistUnicode = item.optText("artist_unicode"),
            playCount = item.optLong("play_count", 0L),
            favouriteCount = item.optInt("favourite_count", 0),
            bpm = if (setBpm.isNaN()) 0.0 else setBpm,
            genreId = item.optInt("genre_id", 0),
            languageId = item.optInt("language_id", 0),
            hasVideo = item.optBoolean("video", false),
            hasStoryboard = item.optBoolean("storyboard", false),
            nsfw = item.optBoolean("nsfw", false),
            source = item.optText("source"),
            tags = item.optText("tags"),
            rankedAt = parseInstant(item.optText("ranked_date")),
            submittedAt = parseInstant(item.optText("submitted_date")),
            minStars = stars.minOrNull() ?: 0.0,
            maxStars = stars.maxOrNull() ?: 0.0
        )
    }

    private fun JSONObject.optText(key: String): String =
        if (isNull(key)) "" else optString(key)

    private fun parseModes(beatmaps: JSONArray?): Set<Int> =
        beatmaps?.let { items ->
            List(items.length()) { index ->
                val item = items.optJSONObject(index) ?: return@List -1
                val mode = item.optInt("mode_int", -1)
                if (mode in 0..3) mode else when (item.optString("mode").lowercase(Locale.US)) {
                    "osu" -> 0
                    "taiko" -> 1
                    "fruits", "catch" -> 2
                    "mania" -> 3
                    else -> -1
                }
            }.filter { it in 0..3 }.toSet()
        }.orEmpty()

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
        val maps = beatmaps.orEmpty()
        val stars = maps.mapNotNull { it.difficultyRating }.filter { it > 0.0 }
        return Beatmap(
            id = id,
            title = safeTitle,
            artist = artist.orEmpty(),
            mapper = creator.orEmpty(),
            status = status.orEmpty(),
            updatedAt = parseInstant(lastUpdated),
            modes = maps.mapNotNull { beatmap ->
                beatmap.modeInt ?: when (beatmap.mode?.lowercase(Locale.US)) {
                    "osu" -> 0
                    "taiko" -> 1
                    "fruits", "catch" -> 2
                    "mania" -> 3
                    else -> null
                }
            }.filter { it in 0..3 }.toSet(),
            titleUnicode = titleUnicode.orEmpty(),
            artistUnicode = artistUnicode.orEmpty(),
            playCount = playCount ?: 0L,
            favouriteCount = favouriteCount ?: 0,
            bpm = bpm ?: maps.firstNotNullOfOrNull { it.bpm } ?: 0.0,
            genreId = genreId ?: 0,
            languageId = languageId ?: 0,
            hasVideo = video == true,
            hasStoryboard = storyboard == true,
            nsfw = nsfw == true,
            source = source.orEmpty(),
            tags = tags.orEmpty(),
            rankedAt = parseInstant(rankedDate),
            submittedAt = parseInstant(submittedDate),
            minStars = stars.minOrNull() ?: 0.0,
            maxStars = stars.maxOrNull() ?: 0.0
        )
    }

    private fun BeatmapsetDto.toSetInfo(reduced: Boolean): BeatmapSetInfo? {
        val beatmap = toBeatmap() ?: return null
        var votes = 0
        var weighted = 0L
        ratings.orEmpty().forEachIndexed { score, count ->
            if (score in 1..10 && count > 0) {
                votes += count
                weighted += score.toLong() * count
            }
        }
        return BeatmapSetInfo(
            beatmap = beatmap.copy(status = normalizeStatus(beatmap.status).ifEmpty { beatmap.status }),
            ratingAverage = if (votes > 0) weighted.toDouble() / votes else 0.0,
            ratingCount = votes,
            difficulties = beatmaps.orEmpty()
                .mapNotNull { it.toDifficultyInfo() }
                .sortedWith(compareBy({ it.modeInt }, { it.stars })),
            isReduced = reduced
        )
    }

    private fun OsuBeatmapDto.toDifficultyInfo(): BeatmapDifficultyInfo? {
        val ruleSet = modeInt ?: when (mode?.lowercase(Locale.US)) {
            "osu" -> 0
            "taiko" -> 1
            "fruits", "catch" -> 2
            "mania" -> 3
            else -> null
        }
        if (ruleSet == null || ruleSet !in 0..3 || version.isNullOrBlank()) return null
        return BeatmapDifficultyInfo(
            id = id,
            version = version,
            modeInt = ruleSet,
            stars = difficultyRating ?: 0.0,
            lengthSeconds = totalLength ?: 0,
            drainSeconds = hitLength ?: 0,
            bpm = bpm ?: 0.0,
            circleSize = circleSize ?: 0.0,
            approachRate = approachRate ?: 0.0,
            overallDifficulty = accuracy ?: 0.0,
            hpDrain = drain ?: 0.0,
            circles = countCircles ?: 0,
            sliders = countSliders ?: 0,
            spinners = countSpinners ?: 0,
            maxCombo = maxCombo ?: 0,
            status = status.orEmpty(),
            isConvert = convert == true
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
        private const val BROWSE_PAGE_SIZE = 50
        private const val HINAI_LEADERBOARD_PREFIX = "hinai-leaderboard:"
        private const val SET_INFO_TTL_MS = 3L * 60L * 1000L
        private const val MAX_SET_INFO_ENTRIES = 24
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
