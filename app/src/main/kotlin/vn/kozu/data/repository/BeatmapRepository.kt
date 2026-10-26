package vn.kozu.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.remote.osu.BeatmapsetDto
import vn.kozu.data.remote.osu.OsuApi
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import java.io.IOException
import java.time.OffsetDateTime

data class CatalogResult(
    val items: List<Beatmap>,
    val isSample: Boolean,
    val error: String? = null
)

class BeatmapRepository(
    private val api: OsuApi,
    private val auth: OsuAuth,
    private val settings: SettingsStore,
    private val client: OkHttpClient
) {
    private val gson = Gson()

    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        category: SearchCategory? = null
    ): CatalogResult {
        val current = settings.settings.first()
        return fetch(
            query = query,
            status = current.statusFilter.apiValue,
            sort = sort.apiValue,
            genre = category?.genreId,
            fallback = { SampleBeatmaps.search(query) }
        )
    }

    suspend fun trending(): CatalogResult = fetch(
        query = "", status = "ranked", sort = "plays_desc", genre = null,
        fallback = { SampleBeatmaps.all }
    )

    suspend fun newReleases(): CatalogResult = fetch(
        query = "", status = "ranked", sort = "ranked_desc", genre = null,
        fallback = { SampleBeatmaps.all.reversed() }
    )

    suspend fun lovedPicks(): CatalogResult = fetch(
        query = "", status = "loved", sort = "plays_desc", genre = null,
        fallback = { SampleBeatmaps.all.shuffled(java.util.Random(7)) }
    )

    suspend fun details(id: Long): Beatmap? {
        val official = try {
            val bearer = auth.bearer()
            if (bearer != null) api.beatmapset("Bearer $bearer", id).toBeatmap() else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (official != null) return official
        return mirrorDetails(id)
    }

    suspend fun verifyCredentials(): Result<Unit> = try {
        val token = auth.clientBearer(force = true)
        if (token == null) Result.failure(IllegalStateException("Enter both client ID and secret first"))
        else Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException(describe(e), e))
    }

    private suspend fun mirrorDetails(id: Long): Beatmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://catboy.best/api/v2/s/$id").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body.string()
                gson.fromJson(body, BeatmapsetDto::class.java)?.toBeatmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetch(
        query: String,
        status: String?,
        sort: String?,
        genre: Int?,
        fallback: () -> List<Beatmap>
    ): CatalogResult {
        return try {
            val current = settings.settings.first()
            val mode = current.modeFilter.apiValue
            val nsfw = current.showExplicit
            val bearer = auth.bearer() ?: return CatalogResult(fallback(), isSample = true)
            val response = try {
                api.searchBeatmapsets("Bearer $bearer", query, status, sort, genre, mode, nsfw)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val fresh = auth.bearer(force = true) ?: throw e
                api.searchBeatmapsets("Bearer $fresh", query, status, sort, genre, mode, nsfw)
            }
            val items = response.beatmapsets.orEmpty().mapNotNull { it.toBeatmap() }.take(30)
            CatalogResult(items, isSample = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatalogResult(fallback(), isSample = true, error = describe(e))
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

    private fun describe(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            400, 401 -> "osu! rejected the API keys (HTTP ${e.code()})"
            429 -> "osu! rate limit reached, try again shortly"
            else -> "osu! API error (HTTP ${e.code()})"
        }
        is IOException -> "No connection to osu!"
        else -> e.message ?: "Unknown error"
    }

    companion object {
        fun parseInstant(value: String?): Long {
            if (value.isNullOrBlank()) return 0L
            return try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (e: Exception) {
                0L
            }
        }
    }
}
