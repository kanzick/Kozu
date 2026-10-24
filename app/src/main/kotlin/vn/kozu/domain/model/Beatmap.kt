package vn.kozu.domain.model

/**
 * Một bài hát (beatmapset) trên osu!.
 *
 * @param id id của beatmapset; giá trị <= 0 nghĩa là bài demo cục bộ (không có ảnh bìa / bản nghe thử online).
 */
data class Beatmap(
    val id: Long,
    val title: String,
    val artist: String,
    val mapper: String = "",
    val status: String = "",
    val hasPreview: Boolean = true
) {
    private val isOnline: Boolean get() = id > 0L

    /** Ảnh vuông nhỏ, dùng cho danh sách / thẻ. */
    val thumbUrl: String? get() = if (isOnline) "https://assets.ppy.sh/beatmaps/$id/covers/list@2x.jpg" else null

    /** Ảnh lớn (banner, được crop vuông), dùng cho player / thẻ hero. */
    val artUrl: String? get() = if (isOnline) "https://assets.ppy.sh/beatmaps/$id/covers/cover@2x.jpg" else null

    /** Bản nghe thử chính thức của osu!. Null -> phát file demo cục bộ. */
    val previewUrl: String? get() = if (isOnline && hasPreview) "https://b.ppy.sh/preview/$id.mp3" else null

    val webUrl: String? get() = if (isOnline) "https://osu.ppy.sh/beatmapsets/$id" else null
}
