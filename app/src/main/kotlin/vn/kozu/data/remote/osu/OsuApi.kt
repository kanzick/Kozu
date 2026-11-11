package vn.kozu.data.remote.osu

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.ResponseBody
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


    @Headers(
        "Accept: application/json",
        "Accept-Language: en-US,en;q=0.9",
        "Referer: https://osu.ppy.sh/home/news",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 Kozu/2.1",
    )
    @GET("news")
    suspend fun news(
        @Query("limit") limit: Int = 21,
        @Query("year") year: Int? = null,
        @Query("cursor_string") cursorString: String? = null,
        @Header("Authorization") authorization: String? = null,
    ): NewsListingDto

    @Headers(
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: en-US,en;q=0.9",
        "Referer: https://osu.ppy.sh/home/news",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 Kozu/2.1",
    )
    @GET("/home/news")
    suspend fun newsWebPage(
        @Query("limit") limit: Int = 21,
        @Query("year") year: Int? = null,
        @Query("cursor_string") cursorString: String? = null,
    ): ResponseBody

    @Headers(
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: en-US,en;q=0.9",
        "Referer: https://osu.ppy.sh/home/news",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 Kozu/2.1",
    )
    @GET("/home/news/{slug}")
    suspend fun newsWebArticle(
        @Path("slug") slug: String,
    ): ResponseBody

    @Headers(
        "Accept: application/json",
        "Accept-Language: en-US,en;q=0.9",
        "Referer: https://osu.ppy.sh/home/news",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 Kozu/2.1",
    )
    @GET("news/{news}")
    suspend fun newsPost(
        @Path("news") slug: String,
        @Query("content_formats[]") contentFormats: List<String> = listOf("html"),
        @Header("Authorization") authorization: String? = null,
    ): NewsPostDto




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
