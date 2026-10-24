package vn.kozu.data.repository

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import vn.kozu.data.remote.osu.BeatmapsetDto
import vn.kozu.data.remote.osu.OsuApi
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import java.io.IOException





data class CatalogResult(
    val items: List<Beatmap>,
    val isSample: Boolean,
    val error: String? = null
)

class BeatmapRepository(
    private val api: OsuApi,
    private val auth: OsuAuth
) {
    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        category: SearchCategory? = null
    ): CatalogResult = fetch(
        query = query,
        status = null,
        sort = sort.apiValue,
        genre = category?.genreId,
        fallback = { SampleBeatmaps.search(query) }
    )

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


    suspend fun verifyCredentials(): Result<Unit> = try {
        val token = auth.bearer(force = true)
        if (token == null) Result.failure(IllegalStateException("Enter both client ID and secret first"))
        else Result.success(Unit)
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
        fallback: () -> List<Beatmap>
    ): CatalogResult {
        return try {
            val bearer = auth.bearer() ?: return CatalogResult(fallback(), isSample = true)
            val response = try {
                api.searchBeatmapsets("Bearer $bearer", query, status, sort, genre)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val fresh = auth.bearer(force = true) ?: throw e
                api.searchBeatmapsets("Bearer $fresh", query, status, sort, genre)
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
            status = status.orEmpty()
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
}
