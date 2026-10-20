package vn.kozu.domain.model

data class Beatmap(
    val title: String,
    val artist: String,
    val id: Long,
    val mapper: String = "",
    val beatmapsetId: Long? = null
) {
    val coverUrl: String?
        get() {
            val setId = beatmapsetId ?: return null
            return "https://assets.ppy.sh/beatmaps/$setId/covers/cover.jpg"
        }
}
