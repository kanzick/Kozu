package vn.kozu

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okio.Path.Companion.toOkioPath
import vn.kozu.di.AppContainer
import vn.kozu.util.AppLocaleManager

class KozuApplication :
    Application(),
    SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Migrate the pre-Android-13 locale preference before the first Activity is created.
        AppLocaleManager.currentLanguageTag(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val limitMb =
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1_000L) {
                    container.settings.settings
                        .first()
                        .imageCacheMb
                }
            } ?: DEFAULT_IMAGE_CACHE_MB
        return ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.httpClient }))
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(limitMb.coerceIn(32, 1024) * 1024L * 1024L)
                    .build()
            }.build()
    }

    private companion object {
        const val DEFAULT_IMAGE_CACHE_MB = 256
    }
}
