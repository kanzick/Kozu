package vn.kozu.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.util.concurrent.TimeUnit
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import vn.kozu.KozuApplication
import vn.kozu.MainActivity
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.mirror.ZipAudioDataSource
import vn.kozu.data.remote.osu.OsuApiFactory
import java.io.File
import java.io.IOException

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var mediaCache: SimpleCache? = null
    private var cacheDatabaseProvider: StandaloneDatabaseProvider? = null
    private var mediaCacheDirectory: File? = null
    private var streamResolver: StreamResolver? = null
    private var taskWasRemoved = false

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val taskRemovalListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            if (taskWasRemoved && !playWhenReady) {
                stopSelf()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (taskWasRemoved && playbackState == Player.STATE_ENDED) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val container = (application as KozuApplication).container
        val streams = container.streams
        streamResolver = streams

        // This cache is intentionally session-only. Remove leftovers from a
        // previous process before opening the directory again. SimpleCache.delete
        // also clears its database index; deleting the directory alone is unsafe.
        val cacheDirectory = File(cacheDir, MEDIA_CACHE_DIRECTORY)
        val databaseProvider = StandaloneDatabaseProvider(this)
        runBlocking(Dispatchers.IO) {
            SimpleCache.delete(cacheDirectory, databaseProvider)
        }
        streams.clearCache()

        mediaCacheDirectory = cacheDirectory
        cacheDatabaseProvider = databaseProvider

        val configuredCacheMb = runBlocking(Dispatchers.IO) {
            container.settings.settings.first().streamCacheMb
        }.coerceIn(MIN_CACHE_MB, MAX_CACHE_MB)
        val cacheLimitBytes = configuredCacheMb.toLong() * BYTES_PER_MEGABYTE

        val cache = SimpleCache(
            cacheDirectory,
            LeastRecentlyUsedCacheEvictor(cacheLimitBytes),
            databaseProvider
        )
        mediaCache = cache

        // Reuse Kozu's OkHttp client so catalog, mirror and audio requests share
        // DNS, TLS sessions and the connection pool.
        // Keep first-byte connection/read waits bounded, but do not impose a
        // whole-call timeout: a song is a long-lived streaming response.
        val playbackClient = container.httpClient.newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
        val http = OkHttpDataSource.Factory(playbackClient)
            .setUserAgent(OsuApiFactory.USER_AGENT)
        val upstream = DefaultDataSource.Factory(this, http)

        // Route custom ZIP-audio URIs to a streaming/inflating DataSource.
        // Ordinary HTTP, file, content and asset URIs still use DefaultDataSource.
        val routedUpstream = DataSource.Factory {
            KozuRoutingDataSource(upstream, streams)
        }

        val cachedDataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(routedUpstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Resolve internal beatmap IDs to local downloads or a genuine full-song
        // source. Resolver errors remain explicit; FULL never falls back to preview.
        val resolving = ResolvingDataSource.Factory(
            cachedDataSource,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    if (dataSpec.uri.scheme != PlaybackController.SET_SCHEME) {
                        return dataSpec
                    }

                    val id = dataSpec.uri.host?.toLongOrNull()
                        ?: throw IOException("Invalid song reference")

                    return dataSpec.withUri(streams.resolve(id).uri)
                }
            }
        )

        // A larger buffer reduces interruptions on variable mobile networks.
        // Initial playback is still allowed after the smaller startup threshold.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,
                45_000,
                800,
                1_500
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(taskRemovalListener)

        scope.launch {
            container.settings.settings
                .map { it.skipSilence }
                .distinctUntilChanged()
                .collect { player.skipSilenceEnabled = it }
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        taskWasRemoved = true
        val player = session?.player

        // Preserve normal background playback when the task is swiped away.
        // Once the user pauses or the queue ends, destroy the service and clear
        // transient streaming caches. Offline beatmap downloads are untouched.
        if (
            player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()

        session?.run {
            player.removeListener(taskRemovalListener)
            player.release()
            release()
        }
        session = null

        mediaCache?.release()
        mediaCache = null

        val directory = mediaCacheDirectory
        val database = cacheDatabaseProvider
        val streams = streamResolver
        if (directory != null && database != null) {
            Thread({
                try {
                    SimpleCache.delete(directory, database)
                } catch (_: Exception) {
                    // A later service start also cleans any leftover cache.
                }
                streams?.clearCache()
            }, "Kozu-cache-cleanup").start()
        } else {
            streams?.clearCache()
        }

        cacheDatabaseProvider = null
        mediaCacheDirectory = null
        streamResolver = null

        super.onDestroy()
    }

    companion object {
        private const val MEDIA_CACHE_DIRECTORY = "kozu-media3-cache"
        private const val MIN_CACHE_MB = 128
        private const val MAX_CACHE_MB = 2048
        private const val BYTES_PER_MEGABYTE = 1024L * 1024L

        fun mediaCacheBytes(context: android.content.Context): Long {
            val directory = File(
                context.applicationContext.cacheDir,
                MEDIA_CACHE_DIRECTORY
            )
            return directory.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }

        /**
         * Clears Media3 cache only when no SimpleCache instance has the folder
         * locked. An active player owns that lock and clears its cache in onDestroy.
         */
        fun clearMediaCacheIfUnlocked(context: android.content.Context) {
            val appContext = context.applicationContext
            val directory = File(appContext.cacheDir, MEDIA_CACHE_DIRECTORY)
            if (SimpleCache.isCacheFolderLocked(directory)) return

            Thread({
                try {
                    SimpleCache.delete(
                        directory,
                        StandaloneDatabaseProvider(appContext)
                    )
                } catch (_: Exception) {
                    // The next PlaybackService start performs a full cleanup.
                }
            }, "Kozu-media-cache-clear").start()
        }
    }
}


/** Routes incremental ZIP audio streams while preserving the normal Media3 sources. */
@OptIn(UnstableApi::class)
private class KozuRoutingDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val streams: StreamResolver
) : DataSource {

    private val listeners = ArrayList<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = if (dataSpec.uri.scheme == StreamResolver.ZIP_AUDIO_SCHEME) {
            ZipAudioDataSource(streams)
        } else {
            upstreamFactory.createDataSource()
        }

        listeners.forEach(source::addTransferListener)
        delegate = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        delegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): android.net.Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = delegate
        delegate = null
        source?.close()
    }
}
