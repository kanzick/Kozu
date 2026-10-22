
package vn.kozu.data.repository

import vn.kozu.domain.model.Beatmap

class FakeBeatmapRepository {

    private val samples = listOf(
        Beatmap(
            id = 1L,
            title = "夜に駆ける",
            artist = "YOASOBI",
            mapper = "Sample mapper"
        ),
        Beatmap(
            id = 2L,
            title = "Lemon",
            artist = "Kenshi Yonezu",
            mapper = "Tripticon",
            beatmapsetId = 1313117L
        ),
        Beatmap(
            id = 3L,
            title = "Megalovania",
            artist = "Toby Fox",
            mapper = "Kyshiro",
            beatmapsetId = 387700L
        ),
        Beatmap(
            id = 4L,
            title = "Bad Apple!!",
            artist = "Masayoshi Minoshima",
            mapper = "Pisapou",
            beatmapsetId = 1936770L
        )
    )

    suspend fun search(query: String): List<Beatmap> {
        val normalized = query.trim()

        if (normalized.isBlank()) return emptyList()

        return samples.filter { beatmap ->
            beatmap.title.contains(normalized, ignoreCase = true) ||
                    beatmap.artist.contains(normalized, ignoreCase = true) ||
                    beatmap.mapper.contains(normalized, ignoreCase = true)
        }
    }
}