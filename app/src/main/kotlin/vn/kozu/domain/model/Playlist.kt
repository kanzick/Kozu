package vn.kozu.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Beatmap>,
    val createdAt: Long
)
