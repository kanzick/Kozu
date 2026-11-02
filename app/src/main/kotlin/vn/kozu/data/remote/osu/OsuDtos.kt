package vn.kozu.data.remote.osu

import com.google.gson.annotations.SerializedName

data class TokenDto(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long = 0L,
)

data class BeatmapsetSearchResponse(
    @SerializedName("beatmapsets") val beatmapsets: List<BeatmapsetDto>? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("cursor_string") val cursorString: String? = null,
)

data class BeatmapsetDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("creator") val creator: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("last_updated") val lastUpdated: String? = null,
    @SerializedName("beatmaps") val beatmaps: List<OsuBeatmapDto>? = null,
    @SerializedName("title_unicode") val titleUnicode: String? = null,
    @SerializedName("artist_unicode") val artistUnicode: String? = null,
    @SerializedName("play_count") val playCount: Long? = null,
    @SerializedName("favourite_count") val favouriteCount: Int? = null,
    @SerializedName("bpm") val bpm: Double? = null,
    @SerializedName("genre_id") val genreId: Int? = null,
    @SerializedName("language_id") val languageId: Int? = null,
    @SerializedName("video") val video: Boolean? = null,
    @SerializedName("storyboard") val storyboard: Boolean? = null,
    @SerializedName("nsfw") val nsfw: Boolean? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("ranked_date") val rankedDate: String? = null,
    @SerializedName("submitted_date") val submittedDate: String? = null,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("ratings") val ratings: List<Int>? = null,
)

data class OsuBeatmapDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("mode_int") val modeInt: Int? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("difficulty_rating") val difficultyRating: Double? = null,
    @SerializedName("cs") val circleSize: Double? = null,
    @SerializedName("bpm") val bpm: Double? = null,
    @SerializedName("total_length") val totalLength: Int? = null,
    @SerializedName("hit_length") val hitLength: Int? = null,
    @SerializedName("ar") val approachRate: Double? = null,
    @SerializedName("accuracy") val accuracy: Double? = null,
    @SerializedName("drain") val drain: Double? = null,
    @SerializedName("count_circles") val countCircles: Int? = null,
    @SerializedName("count_sliders") val countSliders: Int? = null,
    @SerializedName("count_spinners") val countSpinners: Int? = null,
    @SerializedName("max_combo") val maxCombo: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("convert") val convert: Boolean? = null,
)

data class UserDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("username") val username: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("country") val country: CountryDto? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("is_supporter") val isSupporter: Boolean? = null,
    @SerializedName("join_date") val joinDate: String? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("playmode") val playmode: String? = null,
    @SerializedName("rank_history") val rankHistory: RankHistoryDto? = null,
    @SerializedName("statistics") val statistics: StatisticsDto? = null,
)

data class RankHistoryDto(
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("data") val data: List<Int> = emptyList(),
)

data class CountryDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
)

data class CoverDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("custom_url") val customUrl: String? = null,
)

data class StatisticsDto(
    @SerializedName("global_rank") val globalRank: Int? = null,
    @SerializedName("country_rank") val countryRank: Int? = null,
    @SerializedName("pp") val pp: Double? = null,
    @SerializedName("hit_accuracy") val hitAccuracy: Double? = null,
    @SerializedName("play_count") val playCount: Int? = null,
    @SerializedName("play_time") val playTime: Long? = null,
    @SerializedName("level") val level: LevelDto? = null,
    @SerializedName("ranked_score") val rankedScore: Long? = null,
    @SerializedName("grade_counts") val gradeCounts: GradeCountsDto? = null,
)

data class LevelDto(
    @SerializedName("current") val current: Int? = null,
    @SerializedName("progress") val progress: Int? = null,
)

data class GradeCountsDto(
    @SerializedName("ss") val ss: Int? = null,
    @SerializedName("ssh") val ssh: Int? = null,
    @SerializedName("s") val s: Int? = null,
    @SerializedName("sh") val sh: Int? = null,
    @SerializedName("a") val a: Int? = null,
)
