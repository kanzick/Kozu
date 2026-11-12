package vn.kozu

import android.app.Application
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.PlatformContext
import coil3.SingletonImageLoader
import okio.Path.Companion.toOkioPath
import vn.kozu.di.AppContainer
import vn.kozu.util.AppLocaleManager

class KozuApplication :
    Application(),
    SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()

        AppLocaleManager.currentLanguageTag(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val limitMb = getSharedPreferences(IMAGE_CACHE_PREFS, MODE_PRIVATE)
            .getInt(IMAGE_CACHE_KEY, DEFAULT_IMAGE_CACHE_MB)
            .coerceIn(MIN_IMAGE_CACHE_MB, MAX_IMAGE_CACHE_MB)
        return ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.httpClient }))
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(limitMb.toLong() * 1024L * 1024L)
                    .build()
            }.build()
    }

    private companion object {
        const val DEFAULT_IMAGE_CACHE_MB = 128
        const val MIN_IMAGE_CACHE_MB = 32
        const val MAX_IMAGE_CACHE_MB = 1024
        const val IMAGE_CACHE_PREFS = "kozu_theme_sync"
        const val IMAGE_CACHE_KEY = "image_cache_mb"
    }
}
