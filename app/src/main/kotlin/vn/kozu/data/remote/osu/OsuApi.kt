package vn.kozu.data.remote.osu

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import vn.kozu.data.news.NewsCommentBundleDto
import vn.kozu.data.news.NewsCommentCreateRequest
import vn.kozu.data.news.NewsListingDto
import vn.kozu.data.news.NewsPostDto

interface OsuApi {
    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun requestToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "public",
    ): TokenDto

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun exchangeCode(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): TokenDto

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenDto


    @Headers("Accept: application/json", "Content-Type: application/json")
    @GET("news")
    suspend fun news(
        @Query("limit") limit: Int = 21,
    ): NewsListingDto

    @Headers("Accept: application/json", "Content-Type: application/json")
    @GET("news/{news}")
    suspend fun newsPost(
        @Path("news") slug: String,
        @Query("content_formats[]") contentFormats: List<String> = listOf("html"),
    ): NewsPostDto


    @Headers(
        "Accept: application/json",
        "Content-Type: application/json",
        "X-Requested-With: XMLHttpRequest",
    )
    @GET("/comments")
    suspend fun newsComments(
        @Query("commentable_type") commentableType: String,
        @Query("commentable_id") commentableId: Long,
        @Query("sort") sort: String = "new",
        @Query("parent_id") parentId: Long? = null,
        @Query("after") after: String? = null,
    ): NewsCommentBundleDto


    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("comments")
    suspend fun postNewsComment(
        @Header("Authorization") authorization: String,
        @Body request: NewsCommentCreateRequest,
    ): NewsCommentBundleDto

    @GET("me")
    suspend fun me(
        @Header("Authorization") authorization: String,
    ): UserDto

    @GET("users/{user}/{mode}")
    suspend fun user(
        @Header("Authorization") authorization: String,
        @Path("user") user: Long,
        @Path("mode") mode: String,
    ): UserDto

    @GET("beatmapsets/{id}")
    suspend fun beatmapset(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): BeatmapsetDto

    @GET("beatmapsets/search")
    suspend fun searchBeatmapsets(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("s") status: String? = null,
        @Query("sort") sort: String? = null,
        @Query("g") genre: Int? = null,
        @Query("m") mode: Int? = null,
        @Query("nsfw") nsfw: Boolean? = null,
        @Query("cursor_string") cursorString: String? = null,
        @Query("l") language: Int? = null,
        @Query("e") extra: String? = null,
        @Query("c") general: String? = null,
        @Query("r") rank: String? = null,
        @Query("played") played: String? = null,
    ): BeatmapsetSearchResponse
}
