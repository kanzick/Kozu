package vn.kozu.player

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.audiofx.Equalizer
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.mirror.ZipAudioDataSource
import vn.kozu.data.remote.osu.OsuApiFactory
import vn.kozu.domain.model.AppSettings
import vn.kozu.KozuApplication
import vn.kozu.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var mediaCache: SimpleCache? = null
    private var mediaEvictor: DynamicLruEvictor? = null
    private var streamResolver: StreamResolver? = null
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId = 0
    private var audioEffects: AudioEffectsController? = null
    private var crossfade: CrossfadeController? = null
    private var tickerJob: Job? = null
    private var audioManager: AudioManager? = null
    private var currentSettings = AppSettings()
    private var equalizerErrorReported = false
    private var taskWasRemoved = false
    private var pausedForNoisy = false
    private var hiFiAtBuild = false
    private var hiFiNoticeShown = false
    private val playbackSessionStore by lazy { PlaybackSessionStore(this) }

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate,
        )

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (addedDevices.any { it.isSink && isHeadsetOutput(it.type) }) {
                    resumeAfterReconnect()
                }
            }
        }

    private val playerListener =
        object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                applyAudioEffects()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                applyAudioEffects()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                crossfade?.onMediaItemTransition(reason)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (
                    reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
                ) {
                    crossfade?.onSeek()
                }
            }

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                pausedForNoisy = !playWhenReady &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY
                crossfade?.tick()
                if (taskWasRemoved && !playWhenReady) {
                    stopSelf()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                crossfade?.tick()
                if (taskWasRemoved && playbackState == Player.STATE_ENDED) {
                    stopSelf()
                }
            }

            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
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

        val initialSettings = container.settings.cachedSettings
        currentSettings = initialSettings
        hiFiAtBuild = initialSettings.hiFiMode
        val configuredCacheMb = initialSettings.streamCacheMb.coerceIn(MIN_CACHE_MB, MAX_CACHE_MB)


        val cacheLimitBytes = configuredCacheMb.toLong() * BYTES_PER_MEGABYTE * MEDIA_CACHE_SHARE_PERCENT / 100L

        val evictor = DynamicLruEvictor(cacheLimitBytes)
        mediaEvictor = evictor
        val cache =
            SimpleCache(
                cacheDirectory,
                evictor,
                databaseProvider,
            )
        mediaCache = cache
        liveCache = cache

        val playbackClient =
            container.httpClient
                .newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build()
        val http =
            OkHttpDataSource
                .Factory(playbackClient)
                .setUserAgent(OsuApiFactory.USER_AGENT)
        val upstream = DefaultDataSource.Factory(this, http)

        val routedUpstream =
            DataSource.Factory {
                KozuRoutingDataSource(upstream, streams)
            }

        val cachedDataSource =
            CacheDataSource
                .Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(routedUpstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)


        val localFiles = FileDataSource.Factory()
        val localAware =
            DataSource.Factory {
                LocalAwareDataSource(localFiles.createDataSource(), cachedDataSource.createDataSource())
            }

        val resolving =
            ResolvingDataSource.Factory(
                localAware,
                object : ResolvingDataSource.Resolver {
                    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                        if (dataSpec.uri.scheme != PlaybackController.SET_SCHEME) {
                            return dataSpec
                        }

                        val id =
                            dataSpec.uri.host?.toLongOrNull()
                                ?: throw IOException("Invalid song reference")

                        return dataSpec.withUri(streams.resolve(id).uri)
                    }
                },
            )

        val loadControl =
            DefaultLoadControl
                .Builder()
                .setBufferDurationsMs(
                    10_000,
                    45_000,
                    800,
                    1_500,
                ).build()

        val renderersFactory =
            DefaultRenderersFactory(this)
                .setEnableAudioFloatOutput(initialSettings.hiFiMode)

        val player =
            ExoPlayer
                .Builder(this, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
                .setLoadControl(loadControl)
                .setAudioAttributes(buildAudioAttributes(initialSettings), true)
                .setHandleAudioBecomingNoisy(initialSettings.pauseOnHeadsetDisconnect)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
        this.player = player

        SleepFade.gain = 1f
        val effects = AudioEffectsController { PlaybackEvents.report(it) }
        audioEffects = effects
        val fader = CrossfadeController(player) { SleepFade.gain }
        crossfade = fader

        player.addListener(playerListener)
        restorePlaybackSession(player, initialSettings.rememberShuffle)
        applySettings(player, initialSettings)

        scope.launch {
            while (true) {
                delay(5_000L)
                player.let(::savePlaybackSession)
            }
        }

        tickerJob =
            scope.launch {
                while (isActive) {
                    val ramping = fader.tick()
                    delay(
                        when {
                            ramping -> RAMP_TICK_MS
                            player.isPlaying -> PLAYING_TICK_MS
                            else -> IDLE_TICK_MS
                        },
                    )
                }
            }

        scope.launch {
            container.settings.settings
                .distinctUntilChanged()
                .collect { settings ->
                    applySettings(player, settings)
                    updateCacheLimit(settings)
                }
        }

        registerAudioDeviceCallback()

        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        session =
            MediaSession
                .Builder(this, player)
                .setSessionActivity(openApp)
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    private fun applySettings(
        player: ExoPlayer,
        settings: AppSettings,
    ) {
        currentSettings = settings
        player.skipSilenceEnabled = settings.skipSilence
        player.setHandleAudioBecomingNoisy(settings.pauseOnHeadsetDisconnect)
        player.setAudioAttributes(buildAudioAttributes(settings), true)
        applyOffload(player, settings.hardwareOffload)
        applyPreload(player, settings.preloadNext)
        crossfade?.setFadeMs(settings.crossfade.millis)
        applyAudioEffects()
        announceHiFiChange(settings.hiFiMode)
    }

    private fun buildAudioAttributes(settings: AppSettings): AudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setSpatializationBehavior(
                if (settings.virtualizer > 0) {
                    C.SPATIALIZATION_BEHAVIOR_AUTO
                } else {
                    C.SPATIALIZATION_BEHAVIOR_NEVER
                },
            ).build()

    private fun applyOffload(
        player: ExoPlayer,
        enabled: Boolean,
    ) {
        val mode =
            if (enabled) {
                TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
            } else {
                TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
            }
        val parameters = player.trackSelectionParameters
        if (parameters.audioOffloadPreferences.audioOffloadMode == mode) return

        val preferences =
            TrackSelectionParameters.AudioOffloadPreferences
                .Builder()
                .setAudioOffloadMode(mode)
                .setIsGaplessSupportRequired(true)
                .build()
        player.trackSelectionParameters =
            parameters
                .buildUpon()
                .setAudioOffloadPreferences(preferences)
                .build()
    }

    private fun applyPreload(
        player: ExoPlayer,
        enabled: Boolean,
    ) {
        val configuration =
            if (enabled) {
                ExoPlayer.PreloadConfiguration(PRELOAD_DURATION_US)
            } else {
                ExoPlayer.PreloadConfiguration.DEFAULT
            }
        if (player.preloadConfiguration.targetPreloadDurationUs == configuration.targetPreloadDurationUs) return
        player.preloadConfiguration = configuration
    }

    private fun announceHiFiChange(enabled: Boolean) {
        if (enabled == hiFiAtBuild) {
            hiFiNoticeShown = false
        } else if (!hiFiNoticeShown) {
            hiFiNoticeShown = true
            PlaybackEvents.report("Restart playback to apply Hi-Fi mode")
        }
    }

    private fun applyAudioEffects() {
        val player = player ?: return
        val settings = currentSettings
        applyEqualizer(player, settings)
        audioEffects?.apply(
            audioSessionId = player.audioSessionId,
            bassBoost = settings.bassBoost,
            loudness = settings.loudness,
        )
    }

    private fun registerAudioDeviceCallback() {
        val manager = getSystemService(AudioManager::class.java) ?: return
        audioManager = manager
        manager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
    }

    private fun resumeAfterReconnect() {
        val player = player ?: return
        if (!pausedForNoisy || !currentSettings.resumeOnHeadset) return
        pausedForNoisy = false
        if (
            player.mediaItemCount > 0 &&
            !player.playWhenReady &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        ) {
            player.play()
        }
    }

    private fun isHeadsetOutput(type: Int): Boolean =
        when (type) {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_HEARING_AID,
            -> true

            else -> isBleOutput(type)
        }

    private fun isBleOutput(type: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            type == AudioDeviceInfo.TYPE_BLE_BROADCAST
    }

    private fun applyEqualizer(
        player: ExoPlayer,
        settings: AppSettings,
    ) {
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
                val normalized =
                    EqualizerProfile.gainAt(
                        frequencyHz,
                        settings.equalizerGains,
                    )
                val level =
                    EqualizerProfile
                        .gainToMilliBel(normalized)
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

        if (!currentSettings.keepPlayingAfterClose) {
            if (player != null) {
                player.pause()
                savePlaybackSession(player)
                player.stop()
            }
            pauseAllPlayersAndStopSelf()
            return
        }

        if (
            player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0
        ) {
            stopSelf()
        }
    }


    private fun updateCacheLimit(settings: AppSettings) {
        val evictor = mediaEvictor ?: return
        val cache = mediaCache ?: return
        val limitMb = settings.streamCacheMb.coerceIn(MIN_CACHE_MB, MAX_CACHE_MB)
        val newLimit = limitMb.toLong() * BYTES_PER_MEGABYTE * MEDIA_CACHE_SHARE_PERCENT / 100L
        if (evictor.maxBytes == newLimit) return
        evictor.maxBytes = newLimit
        scope.launch(Dispatchers.IO) {
            try {
                synchronized(cache) { evictor.trim(cache) }
                streamResolver?.trimTo(limitMb)
            } catch (e: Exception) {
                Log.w(TAG, "Could not apply the new cache limit", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        tickerJob = null
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        audioManager = null
        audioEffects?.release()
        audioEffects = null
        crossfade?.release()
        crossfade = null
        SleepFade.gain = 1f
        equalizer?.release()
        equalizer = null
        equalizerSessionId = 0

        session?.run {
            savePlaybackSession(player)
            player.removeListener(playerListener)
            player.release()
            release()
        }
        session = null
        player = null

        liveCache = null
        mediaCache?.release()
        mediaCache = null
        mediaEvictor = null

        streamResolver = null

        super.onDestroy()
    }

    private fun restorePlaybackSession(
        player: ExoPlayer,
        restoreModes: Boolean,
    ) {
        val saved = playbackSessionStore.load() ?: return
        player.setMediaItems(
            saved.queue.map { it.toMediaItem() },
            saved.queueIndex,
            saved.positionMs,
        )
        player.shuffleModeEnabled = restoreModes && saved.shuffleEnabled
        player.repeatMode = if (restoreModes) saved.repeatMode else Player.REPEAT_MODE_OFF
        player.prepare()
    }

    private fun savePlaybackSession(player: Player) {
        val queue =
            List(player.mediaItemCount) { index ->
                player.getMediaItemAt(index).toBeatmap()
            }.filterNotNull()
        playbackSessionStore.save(
            PlaybackSessionSnapshot(
                queue = queue,
                queueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                positionMs = player.currentPosition,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
            ),
        )
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val MEDIA_CACHE_DIRECTORY = "kozu-media3-cache"
        private const val MIN_CACHE_MB = 128
        private const val MAX_CACHE_MB = 2048
        private const val BYTES_PER_MEGABYTE = 1024L * 1024L
        private const val MEDIA_CACHE_SHARE_PERCENT = 40L

        @Volatile
        private var liveCache: SimpleCache? = null
        private const val PRELOAD_DURATION_US = 10_000_000L
        private const val RAMP_TICK_MS = 40L
        private const val PLAYING_TICK_MS = 250L
        private const val IDLE_TICK_MS = 500L

        fun mediaCacheBytes(context: android.content.Context): Long {
            val directory =
                File(
                    context.applicationContext.cacheDir,
                    MEDIA_CACHE_DIRECTORY,
                )
            return directory
                .walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }


        fun clearMediaCache(context: android.content.Context) {
            val cache = liveCache
            if (cache == null) {
                clearMediaCacheIfUnlocked(context)
                return
            }
            try {
                for (key in HashSet(cache.keys)) cache.removeResource(key)
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear the playback cache", e)
            }
        }

        fun clearMediaCacheIfUnlocked(context: android.content.Context) {
            val appContext = context.applicationContext
            val directory = File(appContext.cacheDir, MEDIA_CACHE_DIRECTORY)
            if (SimpleCache.isCacheFolderLocked(directory)) return

            Thread({
                try {
                    SimpleCache.delete(
                        directory,
                        StandaloneDatabaseProvider(appContext),
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
    private val streams: StreamResolver,
) : DataSource {
    private val listeners = ArrayList<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source =
            if (dataSpec.uri.scheme == StreamResolver.ZIP_AUDIO_SCHEME) {
                ZipAudioDataSource(streams)
            } else {
                upstreamFactory.createDataSource()
            }

        listeners.forEach(source::addTransferListener)
        delegate = source
        return source.open(dataSpec)
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int = delegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): android.net.Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = delegate
        delegate = null
        source?.close()
    }
}


@OptIn(UnstableApi::class)
private class LocalAwareDataSource(
    private val local: DataSource,
    private val remote: DataSource,
) : DataSource {
    private val listeners = ArrayList<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = if (dataSpec.uri.scheme == "file") local else remote
        listeners.forEach(source::addTransferListener)
        delegate = source
        return source.open(dataSpec)
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int = delegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): android.net.Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = delegate
        delegate = null
        source?.close()
    }
}


@OptIn(UnstableApi::class)
private class DynamicLruEvictor(
    @Volatile var maxBytes: Long,
) : CacheEvictor {
    private val leastRecentlyUsed =
        java.util.TreeSet<CacheSpan>(
            Comparator { a, b ->
                val difference = a.lastTouchTimestamp - b.lastTouchTimestamp
                if (difference == 0L) a.compareTo(b) else if (difference < 0L) -1 else 1
            },
        )
    private var currentSize = 0L

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    override fun onStartFile(
        cache: Cache,
        key: String,
        position: Long,
        length: Long,
    ) {
        if (length != C.LENGTH_UNSET.toLong()) evict(cache, length)
    }

    override fun onSpanAdded(
        cache: Cache,
        span: CacheSpan,
    ) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evict(cache, 0L)
    }

    override fun onSpanRemoved(
        cache: Cache,
        span: CacheSpan,
    ) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(
        cache: Cache,
        oldSpan: CacheSpan,
        newSpan: CacheSpan,
    ) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    fun trim(cache: Cache) = evict(cache, 0L)

    private fun evict(
        cache: Cache,
        requiredSpace: Long,
    ) {
        while (currentSize + requiredSpace > maxBytes && leastRecentlyUsed.isNotEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }
}
