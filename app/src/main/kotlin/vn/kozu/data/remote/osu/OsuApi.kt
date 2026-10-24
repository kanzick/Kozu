package vn.kozu.data.remote.osu

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface OsuApi {

    /** OAuth client-credentials: https://osu.ppy.sh/docs/index.html#client-credentials-grant */
    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun requestToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "public"
    ): TokenDto

    @GET("beatmapsets/search")
    suspend fun searchBeatmapsets(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("s") status: String? = null,
        @Query("sort") sort: String? = null,
        @Query("g") genre: Int? = null
    ): BeatmapsetSearchResponse
}
