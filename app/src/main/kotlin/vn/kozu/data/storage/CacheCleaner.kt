package vn.kozu.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.player.PlaybackService

class CacheCleaner(
    context: Context,
    private val streams: StreamResolver,
    private val settings: SettingsStore,
) {
    private val appContext = context.applicationContext

    fun streamBytes(): Long = streams.cacheBytes() + PlaybackService.mediaCacheBytes(appContext)

    fun clearStreams() {
        streams.clearCache()
        PlaybackService.clearMediaCacheIfUnlocked(appContext)
    }

    suspend fun cleanIfNeeded(): Boolean =
        withContext(Dispatchers.IO) {
            val current = settings.settings.first()
            if (!current.autoCleanCache) return@withContext false
            val limit = current.streamCacheMb.coerceAtLeast(1) * 1024L * 1024L
            if (streamBytes() <= limit) return@withContext false
            clearStreams()
            true
        }
}
