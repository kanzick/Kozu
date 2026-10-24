package vn.kozu.data.remote.osu

import com.google.gson.annotations.SerializedName

data class TokenDto(
    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Long = 0L
)

data class BeatmapsetSearchResponse(
    @SerializedName("beatmapsets")
    val beatmapsets: List<BeatmapsetDto>? = null,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("cursor_string")
    val cursorString: String? = null
)

data class BeatmapsetDto(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("artist")
    val artist: String? = null,

    @SerializedName("creator")
    val creator: String? = null,

    @SerializedName("status")
    val status: String? = null
)
