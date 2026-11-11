package vn.kozu.data.news

import com.google.gson.annotations.SerializedName

data class NewsListingDto(
    @SerializedName("news_posts") val newsPosts: List<NewsPostDto> = emptyList(),
    @SerializedName("cursor_string") val cursorString: String? = null,
    @SerializedName("years") val years: List<Int> = emptyList(),
    @SerializedName("news_sidebar") val newsSidebar: NewsSidebarDto? = null,
)

data class NewsSidebarDto(
    @SerializedName("current_year") val currentYear: Int = 0,
    @SerializedName("news_posts") val newsPosts: List<NewsPostDto> = emptyList(),
    @SerializedName("years") val years: List<Int> = emptyList(),
)

data class NewsPostDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("slug") val slug: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "osu!",
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("first_image") val firstImage: String? = null,
    @SerializedName("first_image@2x") val firstImage2x: String? = null,
    @SerializedName("preview") val preview: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("content_markdown") val contentMarkdown: String? = null,
)
