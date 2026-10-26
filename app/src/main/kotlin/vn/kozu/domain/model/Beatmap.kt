package vn.kozu.domain.model

data class Beatmap(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String = "",
    val status: String = "",
    val updatedAt: Long = 0L
) {
    val thumbUrl: String get() = "https://assets.ppy.sh/beatmaps/$id/covers/list@2x.jpg"
    val artUrl: String get() = "https://assets.ppy.sh/beatmaps/$id/covers/cover@2x.jpg"
    val webUrl: String get() = "https://osu.ppy.sh/beatmapsets/$id"
}
