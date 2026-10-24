package vn.kozu.data.repository

import vn.kozu.domain.model.Beatmap

/** Dữ liệu mẫu, dùng khi chưa nhập osu! API key hoặc khi mất mạng. */
object SampleBeatmaps {

    val all: List<Beatmap> = listOf(
        Beatmap(id = -1L, title = "夜に駆ける", artist = "YOASOBI", mapper = "Demo mapper", hasPreview = false),
        Beatmap(id = 1313117L, title = "Lemon", artist = "Kenshi Yonezu", mapper = "Tripticon"),
        Beatmap(id = 387700L, title = "Megalovania", artist = "Toby Fox", mapper = "Kyshiro"),
        Beatmap(id = 1936770L, title = "Bad Apple!!", artist = "Masayoshi Minoshima", mapper = "Pisapou"),
        Beatmap(id = 39804L, title = "FREEDOM DiVE", artist = "xi", status = "ranked"),
        Beatmap(id = 292301L, title = "Blue Zenith", artist = "xi", status = "ranked"),
        Beatmap(id = 41823L, title = "The Big Black", artist = "The Quick Brown Fox", status = "ranked")
    )

    fun search(query: String): List<Beatmap> {
        val normalized = query.trim()
        if (normalized.isBlank()) return all
        return all.filter {
            it.title.contains(normalized, ignoreCase = true) ||
                it.artist.contains(normalized, ignoreCase = true) ||
                it.mapper.contains(normalized, ignoreCase = true)
        }
    }
}
