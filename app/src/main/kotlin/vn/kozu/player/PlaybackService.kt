package vn.kozu.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import vn.kozu.KozuApplication
import vn.kozu.MainActivity
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.mirror.ZipAudioDataSource
import vn.kozu.data.remote.osu.OsuApiFactory
import vn.kozu.domain.model.AppSettings
import java.io.File
import java.io.IOException

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var mediaCache: SimpleCache? = null
    private var streamResolver: StreamResolver? = null
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId = 0
    private var currentSettings = AppSettings()
    private var equalizerErrorReported = false
    private var taskWasRemoved = false
    private val playbackSessionStore by lazy { PlaybackSessionStore(this) }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val taskRemovalListener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) {
            player?.let { applyEqualizer(it, currentSettings) }
        }

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

        override fun onEvents(player: Player, events: Player.Events) {
            savePlaybackSession(player)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val container = (application as KozuApplication).container
        val streams = container.streams
        streamResolver = streams

        val cacheDirectory = File(cacheDir, MEDIA_CACHE_DIRECTORY)
        val databaseProvider = StandaloneDatabaseProvider(this)

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

        val playbackClient = container.httpClient.newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
        val http = OkHttpDataSource.Factory(playbackClient)
            .setUserAgent(OsuApiFactory.USER_AGENT)
        val upstream = DefaultDataSource.Factory(this, http)

        val routedUpstream = DataSource.Factory {
            KozuRoutingDataSource(upstream, streams)
        }

        val cachedDataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(routedUpstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

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
        this.player = player

        player.addListener(taskRemovalListener)
        restorePlaybackSession(player)
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5_000L)
                player.let(::savePlaybackSession)
            }
        }

        scope.launch {
            container.settings.settings
                .distinctUntilChanged()
                .collect { settings ->
                    currentSettings = settings
                    player.skipSilenceEnabled = settings.skipSilence
                    applyEqualizer(player, settings)
                }
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

    private fun applyEqualizer(player: ExoPlayer, settings: AppSettings) {
        if (!settings.equalizerEnabled) {
            equalizer?.enabled = false
            return
        }

        val audioSessionId = player.audioSessionId
        if (audioSessionId <= 0) return

        if (audioSessionId != equalizerSessionId) {
            equalizer?.release()
            equalizer = null
            equalizerSessionId = 0
            try {
                equalizer = Equalizer(0, audioSessionId)
                equalizerSessionId = audioSessionId
            } catch (error: RuntimeException) {
                reportEqualizerFailure(error)
                return
            }
        }

        val effect = equalizer ?: return
        try {
            val range = effect.bandLevelRange
            val minimum = range[0].toInt()
            val maximum = range[1].toInt()
            for (index in 0 until effect.numberOfBands.toInt()) {
                val frequencyHz = effect.getCenterFreq(index.toShort()) / 1_000f
                val normalized = EqualizerProfile.gainAt(
                    frequencyHz,
                    settings.equalizerGains
                )
                val level = EqualizerProfile.gainToMilliBel(normalized)
                    .coerceIn(minimum, maximum)
                effect.setBandLevel(index.toShort(), level.toShort())
            }
            effect.enabled = true
            equalizerErrorReported = false
        } catch (error: RuntimeException) {
            equalizer?.release()
            equalizer = null
            equalizerSessionId = 0
            reportEqualizerFailure(error)
        }
    }

    private fun reportEqualizerFailure(error: RuntimeException) {
        Log.e(TAG, "Could not apply the device equalizer", error)
        if (!equalizerErrorReported) {
            PlaybackEvents.report("The equalizer is not supported by this device.")
            equalizerErrorReported = true
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        taskWasRemoved = true
        val player = session?.player

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
        equalizer?.release()
        equalizer = null
        equalizerSessionId = 0

        session?.run {
            savePlaybackSession(player)
            player.removeListener(taskRemovalListener)
            player.release()
            release()
        }
        session = null
        player = null

        mediaCache?.release()
        mediaCache = null

        streamResolver = null

        super.onDestroy()
    }

    private fun restorePlaybackSession(player: ExoPlayer) {
        val saved = playbackSessionStore.load() ?: return
        player.setMediaItems(
            saved.queue.map { it.toMediaItem() },
            saved.queueIndex,
            saved.positionMs
        )
        player.shuffleModeEnabled = saved.shuffleEnabled
        player.repeatMode = saved.repeatMode
        player.prepare()
    }

    private fun savePlaybackSession(player: Player) {
        val queue = List(player.mediaItemCount) { index ->
            player.getMediaItemAt(index).toBeatmap()
        }.filterNotNull()
        playbackSessionStore.save(
            PlaybackSessionSnapshot(
                queue = queue,
                queueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                positionMs = player.currentPosition,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode
            )
        )
    }

    companion object {
        private const val TAG = "PlaybackService"
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

                }
            }, "Kozu-media-cache-clear").start()
        }
    }
}

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
