package vn.kozu.data.news

import com.google.gson.annotations.SerializedName

/** osu! API v2 news listing response. */
data class NewsListingDto(
    @SerializedName("news_posts") val newsPosts: List<NewsPostDto> = emptyList(),
    @SerializedName("cursor_string") val cursorString: String? = null,
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

data class NewsCommentBundleDto(
    @SerializedName("comments") val comments: List<NewsCommentDto> = emptyList(),
    @SerializedName("included_comments") val includedComments: List<NewsCommentDto> = emptyList(),
    @SerializedName("pinned_comments") val pinnedComments: List<NewsCommentDto> = emptyList(),
    @SerializedName("users") val users: List<NewsCommentUserDto> = emptyList(),
    @SerializedName("total") val total: Int? = null,
    @SerializedName("top_level_count") val topLevelCount: Int? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("has_more_id") val hasMoreId: Long? = null,
)

data class NewsCommentDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("user_id") val userId: Long = 0L,
    @SerializedName("parent_id") val parentId: Long? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("message_html") val messageHtml: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    @SerializedName("legacy_name") val legacyName: String? = null,
    @SerializedName("replies_count") val repliesCount: Int = 0,
    @SerializedName("votes_count") val votesCount: Int = 0,
)

data class NewsCommentUserDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("username") val username: String = "osu! user",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
)

/** Request body used by osu! API v2 when an authenticated user posts a comment. */
data class NewsCommentCreateRequest(
    @SerializedName("comment") val comment: NewsCommentCreateDto,
)

data class NewsCommentCreateDto(
    @SerializedName("commentable_id") val commentableId: Long? = null,
    @SerializedName("commentable_type") val commentableType: String? = null,
    @SerializedName("message") val message: String,
    @SerializedName("parent_id") val parentId: Long? = null,
)

