package vn.kozu.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vn.kozu.R
import vn.kozu.data.local.HistoryStore
import vn.kozu.domain.model.Beatmap
import vn.kozu.util.dayKey
import java.util.Locale

class PlaybackController(
    context: Context,
    private val history: HistoryStore
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val pendingCommands = ArrayList<(MediaController) -> Unit>()
    private var cachedQueue: List<Beatmap> = emptyList()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var pendingListenedMs = 0L

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            rebuildQueue()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val beatmap = mediaItem?.toBeatmap() ?: return
            if (controller?.playWhenReady == true) {
                scope.launch { history.recordPlay(beatmap) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _messages.tryEmit("Couldn't play that track")
            controller?.let { c ->
                if (c.hasNextMediaItem()) {
                    c.seekToNextMediaItem()
                    c.prepare()
                    c.play()
                }
            }
        }
    }

    init {
        connect()
        startTicker()
    }


    fun playQueue(queue: List<Beatmap>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        withController { c ->
            c.setMediaItems(queue.map { it.toMediaItem() }, startIndex.coerceIn(0, queue.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playShuffled(queue: List<Beatmap>) {
        if (queue.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = true
            c.setMediaItems(queue.map { it.toMediaItem() }, queue.indices.random(), 0L)
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

    fun playNext(beatmap: Beatmap) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItems(listOf(beatmap.toMediaItem()), 0, 0L)
            c.prepare()
            c.play()
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, beatmap.toMediaItem())
        }
    }

    fun addToQueue(beatmap: Beatmap) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItems(listOf(beatmap.toMediaItem()), 0, 0L)
            c.prepare()
            c.play()
        } else {
            c.addMediaItem(beatmap.toMediaItem())
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
        val parts = ArrayList<String>(3)
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

    private fun Beatmap.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong(KEY_ID, id)
            putString(KEY_MAPPER, mapper)
            putString(KEY_STATUS, status)
            putBoolean(KEY_PREVIEW, hasPreview)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(thumbUrl?.toUri())
            .setExtras(extras)
            .build()

        val uri = previewUrl ?: "android.resource://${appContext.packageName}/${R.raw.test}"
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toBeatmap(): Beatmap? {
        val extras = mediaMetadata.extras ?: return null
        return Beatmap(
            id = extras.getLong(KEY_ID),
            title = mediaMetadata.title?.toString().orEmpty(),
            artist = mediaMetadata.artist?.toString().orEmpty(),
            mapper = extras.getString(KEY_MAPPER).orEmpty(),
            status = extras.getString(KEY_STATUS).orEmpty(),
            hasPreview = extras.getBoolean(KEY_PREVIEW, true)
        )
    }

    private companion object {
        const val TAG = "PlaybackController"
        const val FLUSH_INTERVAL_MS = 10_000L
        const val KEY_ID = "kozu.id"
        const val KEY_MAPPER = "kozu.mapper"
        const val KEY_STATUS = "kozu.status"
        const val KEY_PREVIEW = "kozu.preview"
    }
}
