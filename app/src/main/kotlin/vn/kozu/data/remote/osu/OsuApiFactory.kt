package vn.kozu.data.remote.osu

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OsuApiFactory {

    private const val BASE_URL = "https://osu.ppy.sh/api/v2/"
    const val USER_AGENT = "Kozu/2.1 (Android; https://github.com/kanzick/Kozu)"

    fun client(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                if (request.header("User-Agent") == null) builder.header("User-Agent", USER_AGENT)
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    fun create(client: OkHttpClient): OsuApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OsuApi::class.java)
}
