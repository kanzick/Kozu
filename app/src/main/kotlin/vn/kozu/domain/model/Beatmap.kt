package vn.kozu.domain.model

data class Beatmap(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String = "",
    val status: String = "",
    val hasPreview: Boolean = true
) {
    private val isOnline: Boolean get() = id > 0L


    val thumbUrl: String? get() = if (isOnline) "https://assets.ppy.sh/beatmaps/$id/covers/list@2x.jpg" else null


    val artUrl: String? get() = if (isOnline) "https://assets.ppy.sh/beatmaps/$id/covers/cover@2x.jpg" else null


    val previewUrl: String? get() = if (isOnline && hasPreview) "https://b.ppy.sh/preview/$id.mp3" else null

    val webUrl: String? get() = if (isOnline) "https://osu.ppy.sh/beatmapsets/$id" else null
}
