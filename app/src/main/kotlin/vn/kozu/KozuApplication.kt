package vn.kozu

import android.app.Application
import coil3.annotation.DelicateCoilApi
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import vn.kozu.di.AppContainer
import vn.kozu.util.AppLocaleManager

class KozuApplication :
    Application(),
    SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        AppLocaleManager.currentLanguageTag(this)
        observeImageCacheLimit()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val limitMb = getSharedPreferences(IMAGE_CACHE_PREFS, MODE_PRIVATE)
            .getInt(IMAGE_CACHE_KEY, DEFAULT_IMAGE_CACHE_MB)
            .coerceIn(MIN_IMAGE_CACHE_MB, MAX_IMAGE_CACHE_MB)
        return buildImageLoader(context, limitMb)
    }

    private fun buildImageLoader(
        context: PlatformContext,
        limitMb: Int,
    ): ImageLoader =
        ImageLoader
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

    @OptIn(FlowPreview::class, DelicateCoilApi::class)
    private fun observeImageCacheLimit() {
        appScope.launch {
            container.settings.settings
                .map { it.imageCacheMb.coerceIn(MIN_IMAGE_CACHE_MB, MAX_IMAGE_CACHE_MB) }
                .distinctUntilChanged()
                .drop(1)
                .debounce(IMAGE_CACHE_APPLY_DELAY_MS)
                .collect { limitMb ->
                    val previous = SingletonImageLoader.get(this@KozuApplication)
                    SingletonImageLoader.setUnsafe(buildImageLoader(this@KozuApplication, limitMb))
                    previous.shutdown()
                }
        }
    }

    private companion object {
        const val DEFAULT_IMAGE_CACHE_MB = 128
        const val MIN_IMAGE_CACHE_MB = 32
        const val MAX_IMAGE_CACHE_MB = 1024
        const val IMAGE_CACHE_PREFS = "kozu_theme_sync"
        const val IMAGE_CACHE_KEY = "image_cache_mb"
        const val IMAGE_CACHE_APPLY_DELAY_MS = 700L
    }
}
