package vn.kozu.data.remote.osu

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OsuApi {

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun requestToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "public"
    ): TokenDto

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun exchangeCode(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): TokenDto

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): TokenDto

    @GET("me")
    suspend fun me(@Header("Authorization") authorization: String): UserDto

    @GET("beatmapsets/{id}")
    suspend fun beatmapset(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): BeatmapsetDto

    @GET("beatmapsets/search")
    suspend fun searchBeatmapsets(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("s") status: String? = null,
        @Query("sort") sort: String? = null,
        @Query("g") genre: Int? = null,
        @Query("m") mode: Int? = null,
        @Query("nsfw") nsfw: Boolean? = null
    ): BeatmapsetSearchResponse
}
