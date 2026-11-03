package vn.kozu.player

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap
import vn.kozu.util.dayKey
import java.util.Locale

class PlaybackController(
    context: Context,
    private val history: HistoryStore,
    private val streams: StreamResolver,
    catalog: BeatmapRepository,
    private val settingsFlow: Flow<AppSettings>
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val pendingCommands = ArrayList<(MediaController) -> Unit>()
    private var cachedQueue: List<Beatmap> = emptyList()
    private var settings = AppSettings()
    private val autoplay = AutoplayQueue(catalog, history)
    private var autoplayJob: Job? = null
    private var autoplayAttempt: Pair<Long, Int>? = null
    private var sleepJob: Job? = null

    private val _audioInfo = MutableStateFlow<String?>(null)
    val audioInfo: StateFlow<String?> = _audioInfo.asStateFlow()

    private val _sleepTimer = MutableStateFlow<SleepTimer?>(null)
    val sleepTimer: StateFlow<SleepTimer?> = _sleepTimer.asStateFlow()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var pendingListenedMs = 0L
    private var retryTrackId: Long? = null
    private var currentTrackRetryCount = 0

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            rebuildQueue()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
            maybeAutoplay()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            if (id != retryTrackId) {
                retryTrackId = id
                currentTrackRetryCount = 0
            }

            val beatmap = mediaItem?.toBeatmap() ?: return
            if (controller?.playWhenReady == true) {
                scope.launch { history.recordPlay(beatmap) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller
            val id = c?.currentMediaItem?.mediaId?.toLongOrNull()

            if (
                c != null &&
                id != null &&
                currentTrackRetryCount < MAX_SOURCE_RETRIES &&
                streams.markCurrentSourceFailed(id)
            ) {
                if (retryTrackId != id) {
                    retryTrackId = id
                    currentTrackRetryCount = 0
                }
                currentTrackRetryCount++
                _messages.tryEmit("Audio source unavailable. Trying another full-song source…")
                c.seekToDefaultPosition()
                c.prepare()
                c.play()
                return
            }

            val details = generateSequence(error as Throwable?) { it.cause }
                .mapNotNull { it.message }
                .firstOrNull { it.contains("Full song", ignoreCase = true) }

            _messages.tryEmit(
                if (details != null || id != null) {
                    "Full song unavailable for this track"
                } else {
                    "Couldn't play that track"
                }
            )

            if (c != null && c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }
        }
    }

    init {
        connect()
        startTicker()
        scope.launch {
            PlaybackEvents.messages.collect { _messages.emit(it) }
        }
        scope.launch {
            try {
                settingsFlow.collect {
                    settings = it
                    maybeAutoplay()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Could not observe settings", error)
            }
        }
    }

    fun playQueue(queue: List<Beatmap>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val (items, index) = if (settings.dedupeQueue) {
            QueueDedupe.dedupe(queue, startIndex)
        } else {
            queue to startIndex.coerceIn(0, queue.lastIndex)
        }
        retryTrackId = null
        currentTrackRetryCount = 0
        withController { c ->
            c.setMediaItems(items.map { it.toMediaItem() }, index, 0L)
            c.prepare()
            c.play()
        }
    }

    fun playShuffled(queue: List<Beatmap>) {
        if (queue.isEmpty()) return
        val items = if (settings.dedupeQueue) QueueDedupe.distinct(queue) else queue
        retryTrackId = null
        currentTrackRetryCount = 0
        withController { c ->
            c.shuffleModeEnabled = true
            c.setMediaItems(items.map { it.toMediaItem() }, items.indices.random(), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.mediaItemCount == 0) return@withController
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            c.play()
        }
    }

    /** Pauses the current soundtrack without toggling a paused player back into playback. */
    fun pause() = withController { controller ->
        if (controller.mediaItemCount > 0 && controller.isPlaying) {
            controller.pause()
        }
    }

    fun next() = withController { c ->
        when {
            c.hasNextMediaItem() -> c.seekToNextMediaItem()
            c.mediaItemCount > 0 -> c.seekTo(0, 0L)
        }
    }

    fun previous() = withController { c ->
        if (c.currentPosition > 3_000L || !c.hasPreviousMediaItem()) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs.coerceAtLeast(0L))
        _position.value = positionMs.coerceAtLeast(0L)
    }

    fun toggleShuffle() = withController { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun skipToQueueItem(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun removeFromQueue(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun clearQueue() = withController { c ->
        c.pause()
        c.clearMediaItems()
        autoplayAttempt = null
        _position.value = 0L
        _duration.value = 0L
    }

    fun moveQueueItem(from: Int, to: Int) = withController { c ->
        val last = c.mediaItemCount - 1
        if (from in 0..last && to in 0..last && from != to) c.moveMediaItem(from, to)
    }

    fun playNext(beatmap: Beatmap) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItems(listOf(beatmap.toMediaItem()), 0, 0L)
            c.prepare()
            c.play()
            return@withController
        }
        val existing = if (settings.dedupeQueue) indexOfId(c, beatmap.id) else -1
        val current = c.currentMediaItemIndex
        when {
            existing == current -> Unit
            existing > current -> c.moveMediaItem(existing, current + 1)
            existing in 0 until current -> c.moveMediaItem(existing, current)
            else -> c.addMediaItem(current + 1, beatmap.toMediaItem())
        }
    }

    fun addToQueue(beatmap: Beatmap) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItems(listOf(beatmap.toMediaItem()), 0, 0L)
            c.prepare()
            c.play()
        } else if (settings.dedupeQueue && indexOfId(c, beatmap.id) >= 0) {
            _messages.tryEmit("Already in the queue")
        } else {
            c.addMediaItem(beatmap.toMediaItem())
        }
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val endsAt = SystemClock.elapsedRealtime() + minutes.coerceAtMost(MAX_SLEEP_MINUTES) * 60_000L
        startSleepTimer(SleepTimer(endsAtElapsedMs = endsAt))
    }

    fun sleepAtEndOfTrack() {
        startSleepTimer(SleepTimer(atEndOfTrack = true))
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimer.value = null
        SleepFade.gain = 1f
    }

    private fun startSleepTimer(timer: SleepTimer) {
        sleepJob?.cancel()
        _sleepTimer.value = timer
        sleepJob = scope.launch {
            try {
                runSleepTimer(timer)
            } finally {
                SleepFade.gain = 1f
            }
        }
    }

    private suspend fun runSleepTimer(timer: SleepTimer) {
        var anchorId: String? = null
        var anchorIndex = -1
        while (true) {
            val c = controller
            if (timer.atEndOfTrack) {
                if (c == null || c.mediaItemCount == 0) {
                    delay(SLEEP_IDLE_TICK_MS)
                    continue
                }
                if (anchorId == null) {
                    anchorId = c.currentMediaItem?.mediaId
                    anchorIndex = c.currentMediaItemIndex
                }
                val changed = c.currentMediaItem?.mediaId != anchorId || c.currentMediaItemIndex != anchorIndex
                val duration = c.duration
                val remaining = if (duration == C.TIME_UNSET || duration <= 0L) {
                    Long.MAX_VALUE
                } else {
                    duration - c.currentPosition
                }
                if (changed || remaining <= SLEEP_END_MARGIN_MS) {
                    finishSleep(c, skipToNext = !changed)
                    return
                }
                SleepFade.gain = fadeFor(remaining)
                delay(if (remaining <= SLEEP_FADE_MS * 2) SLEEP_FAST_TICK_MS else SLEEP_IDLE_TICK_MS)
            } else {
                val remaining = timer.endsAtElapsedMs - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    SleepFade.gain = 0f
                    if (c != null) finishSleep(c, skipToNext = false) else _sleepTimer.value = null
                    return
                }
                SleepFade.gain = fadeFor(remaining)
                val untilFade = remaining - SLEEP_FADE_MS
                delay(
                    if (untilFade <= 0L) SLEEP_FAST_TICK_MS
                    else untilFade.coerceIn(SLEEP_FAST_TICK_MS, SLEEP_IDLE_TICK_MS)
                )
            }
        }
    }

    private fun fadeFor(remainingMs: Long): Float =
        if (remainingMs >= SLEEP_FADE_MS) 1f else (remainingMs.toFloat() / SLEEP_FADE_MS).coerceIn(0f, 1f)

    private suspend fun finishSleep(c: MediaController, skipToNext: Boolean) {
        SleepFade.gain = 0f
        _sleepTimer.value = null
        c.pause()
        if (skipToNext) {
            when {
                c.repeatMode == Player.REPEAT_MODE_ONE -> c.seekTo(0L)
                c.hasNextMediaItem() -> c.seekToNextMediaItem()
            }
        }
        var waited = 0L
        while (c.playWhenReady && waited < SLEEP_PAUSE_WAIT_MS) {
            delay(SLEEP_FAST_TICK_MS)
            waited += SLEEP_FAST_TICK_MS
        }
    }

    private fun indexOfId(c: MediaController, id: Long): Int {
        val key = id.toString()
        for (index in 0 until c.mediaItemCount) {
            if (c.getMediaItemAt(index).mediaId == key) return index
        }
        return -1
    }

    private fun maybeAutoplay() {
        val c = controller ?: return
        if (!settings.autoplay || autoplayJob?.isActive == true) return
        if (c.mediaItemCount == 0 || c.repeatMode != Player.REPEAT_MODE_OFF || c.hasNextMediaItem()) return
        if (!c.playWhenReady) return
        val current = cachedQueue.getOrNull(c.currentMediaItemIndex) ?: return
        if (current.id <= 0L) return
        val attempt = current.id to c.mediaItemCount
        if (autoplayAttempt == attempt) return
        autoplayAttempt = attempt

        val queue = cachedQueue
        autoplayJob = scope.launch {
            val related = try {
                autoplay.fetch(current, queue, settings.avoidRepeats)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Autoplay lookup failed", error)
                emptyList()
            }
            val player = controller ?: return@launch
            if (related.isEmpty() || !settings.autoplay) return@launch
            val stillLast = player.currentMediaItem?.mediaId == current.id.toString() &&
                player.repeatMode == Player.REPEAT_MODE_OFF &&
                !player.hasNextMediaItem()
            if (!stillLast) return@launch
            val queued = HashSet<String>()
            for (index in 0 until player.mediaItemCount) queued.add(player.getMediaItemAt(index).mediaId)
            val fresh = related.filter { queued.add(it.id.toString()) }
            if (fresh.isEmpty()) return@launch
            val ended = player.playbackState == Player.STATE_ENDED
            player.addMediaItems(fresh.map { it.toMediaItem() })
            if (ended) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
            _messages.tryEmit("Added ${fresh.size} similar ${if (fresh.size == 1) "song" else "songs"}")
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pendingCommands.add(block)
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(listener)
                    rebuildQueue()
                    syncState()
                    pendingCommands.forEach { it(connected) }
                    pendingCommands.clear()
                } catch (e: Exception) {
                    Log.e(TAG, "Could not connect to the playback service", e)
                    _messages.tryEmit("Playback service unavailable")
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        cachedQueue = List(c.mediaItemCount) { index ->
            c.getMediaItemAt(index).toBeatmap() ?: Beatmap(id = 0L, title = "Unknown", artist = "")
        }
    }

    private fun syncState() {
        val c = controller ?: return
        val index = if (c.mediaItemCount == 0) -1 else c.currentMediaItemIndex
        _state.value = PlaybackState(
            current = cachedQueue.getOrNull(index),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING && c.playWhenReady,
            shuffle = c.shuffleModeEnabled,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatState.ALL
                Player.REPEAT_MODE_ONE -> RepeatState.ONE
                else -> RepeatState.OFF
            },
            queue = cachedQueue,
            queueIndex = index,
            audioInfo = describeAudio(c)
        )
        _audioInfo.value = describeFormat(c)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun describeFormat(c: Player): String? {
        val group = c.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?: return null
        var index = 0
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) {
                index = i
                break
            }
        }
        val format = group.getTrackFormat(index)
        val parts = ArrayList<String>(4)
        codecName(format.sampleMimeType ?: format.codecs)?.let { parts.add(it) }
        val bitrate = if (format.averageBitrate > 0) format.averageBitrate else format.bitrate
        if (bitrate > 0) parts.add("${bitrate / 1000} kbps")
        if (format.sampleRate > 0) parts.add(String.format(Locale.US, "%.1f kHz", format.sampleRate / 1000f))
        when {
            format.channelCount == 1 -> parts.add("Mono")
            format.channelCount == 2 -> parts.add("Stereo")
            format.channelCount > 2 -> parts.add("${format.channelCount} ch")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }

    private fun startTicker() {
        scope.launch {
            var last = SystemClock.elapsedRealtime()
            var lastFlush = last
            while (isActive) {
                val c = controller
                val now = SystemClock.elapsedRealtime()
                val playing = c?.isPlaying == true
                if (c != null) {
                    _position.value = c.currentPosition.coerceAtLeast(0L)
                    val d = c.duration
                    _duration.value = if (d == C.TIME_UNSET || d < 0L) 0L else d
                    if (playing) pendingListenedMs += now - last
                }
                last = now

                if (pendingListenedMs > 0L && (!playing || now - lastFlush >= FLUSH_INTERVAL_MS)) {
                    val ms = pendingListenedMs
                    pendingListenedMs = 0L
                    lastFlush = now
                    history.addListening(dayKey(System.currentTimeMillis()), ms)
                }
                delay(if (playing) 200L else 500L)
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun describeAudio(c: Player): String? {
        val group = c.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?: return null
        var format = group.getTrackFormat(0)
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) {
                format = group.getTrackFormat(i)
                break
            }
        }
        val parts = ArrayList<String>(4)
        val currentId = c.currentMediaItem?.mediaId?.toLongOrNull()
        currentId?.let { streams.labelFor(it) }?.let { parts.add(it) }
        if (format.sampleRate > 0) parts.add(String.format(Locale.US, "%.1f kHz", format.sampleRate / 1000f))
        if (format.bitrate > 0) parts.add("${format.bitrate / 1000} kbps")
        codecName(format.sampleMimeType)?.let { parts.add(it) }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }

    private fun codecName(mime: String?): String? = when (mime) {
        null -> null
        "audio/mpeg" -> "MP3"
        "audio/mp4a-latm" -> "AAC"
        "audio/opus" -> "OPUS"
        "audio/vorbis" -> "OGG"
        "audio/flac" -> "FLAC"
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }

    companion object {
        private const val MAX_SOURCE_RETRIES = 10
        private const val MAX_SLEEP_MINUTES = 24 * 60
        private const val SLEEP_FADE_MS = 3_000L
        private const val SLEEP_END_MARGIN_MS = 200L
        private const val SLEEP_FAST_TICK_MS = 40L
        private const val SLEEP_IDLE_TICK_MS = 250L
        private const val SLEEP_PAUSE_WAIT_MS = 1_500L
        const val TAG = "PlaybackController"
        const val FLUSH_INTERVAL_MS = 10_000L
        const val KEY_ID = "kozu.id"
        const val KEY_MAPPER = "kozu.mapper"
        const val KEY_STATUS = "kozu.status"
        const val KEY_UPDATED = "kozu.updated"
        const val KEY_MODES = "kozu.modes"
        const val SET_SCHEME = "kozu-set"
    }
}
