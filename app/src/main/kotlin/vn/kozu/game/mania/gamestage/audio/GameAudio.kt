package vn.kozu.game.mania.gamestage.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import vn.kozu.game.mania.gamestage.engine.AudioAnchor
import java.io.File

/**
 * Plays the song and provides accurate [AudioAnchor] values to the game clock.
 *
 * All MediaPlayer operations run on a dedicated thread, including polling MediaPlayer.getTimestamp()
 * (a binder call) every ~10ms, so the UI thread is never blocked. Results are published
 * through the volatile [anchor] field for the frame loop to read.
 *
 * getTimestamp() returns a pair (media time, System.nanoTime) from AudioTrack, reflecting audio that
 * has actually reached the output rather than the file read position. Bluetooth headphone latency is not included:
 * use the offset setting to compensate.
 */
class GameAudio {
    private val thread = HandlerThread("GameAudio").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null // Access only on the handler thread.
    private var startedNano = 0L
    private var gotTimestamp = false
    private var polling = false
    @Volatile private var pauseRequested = false
    private var resumeAfterPause = false

    @Volatile var anchor: AudioAnchor? = null
        private set
    @Volatile var failure: String? = null
        private set
    @Volatile var completed: Boolean = false
        private set

    /** Loads the song asynchronously. [onDone] runs on the main thread with an error message or null. */
    fun prepare(file: File, volume: Float, onDone: (String?) -> Unit) {
        handler.post {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                mp.setVolume(volume, volume)
                mp.setOnCompletionListener { completed = true }
                mp.setOnErrorListener { _, what, extra ->
                    failure = "MediaPlayer error (what=$what, extra=$extra)"
                    true
                }
                player = mp
                main.post { onDone(null) }
            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                main.post { onDone(msg) }
            }
        }
    }

    /** Starts playback from the beginning. The first anchor appears when audio actually reaches the output. */
    fun start() {
        handler.post {
            val mp = player ?: return@post
            try {
                mp.seekTo(0)
                startedNano = System.nanoTime()
                mp.start()
                if (!polling) { polling = true; handler.post(poll) }
            } catch (e: Exception) {
                failure = e.message ?: e.javaClass.simpleName
            }
        }
    }

    fun pause() {
        pauseRequested = true
        handler.post {
            try {
                val mp = player
                resumeAfterPause = mp?.isPlaying == true
                if (resumeAfterPause) mp?.pause()
            } catch (e: Exception) {
                failure = e.message ?: e.javaClass.simpleName
            }
        }
    }

    fun resume() {
        handler.post {
            try {
                val mp = player ?: return@post
                if (resumeAfterPause && !mp.isPlaying) mp.start()
                resumeAfterPause = false
                pauseRequested = false
            } catch (e: Exception) {
                failure = e.message ?: e.javaClass.simpleName
            }
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            val mp = player
            if (mp == null) { polling = false; return }
            if (pauseRequested) {
                handler.postDelayed(this, POLL_MS)
                return
            }
            try {
                val ts = mp.timestamp
                if (ts != null && ts.mediaClockRate > 0f) {
                    gotTimestamp = true
                    anchor = AudioAnchor(
                        mediaTimeMs = ts.anchorMediaTimeUs / 1000.0,
                        systemNano = ts.anchorSystemNanoTime,
                        rate = ts.mediaClockRate.toDouble(),
                        precise = true,
                    )
                } else if (!gotTimestamp &&
                    System.nanoTime() - startedNano > COARSE_FALLBACK_DELAY_MS * 1_000_000L &&
                    mp.isPlaying
                ) {
                    // getTimestamp is unsupported: use the coarse position, which the clock trusts less.
                    anchor = AudioAnchor(mp.currentPosition.toDouble(), System.nanoTime(), 1.0, precise = false)
                }
            } catch (e: Exception) {
                // Ignore this poll if the player is changing state and throws IllegalStateException.
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    fun release() {
        handler.post {
            polling = false
            handler.removeCallbacksAndMessages(null)
            player?.let { runCatching { it.setOnCompletionListener(null); it.setOnErrorListener(null); it.release() } }
            player = null
            thread.quitSafely()
        }
    }

    private companion object {
        const val POLL_MS = 10L
        const val COARSE_FALLBACK_DELAY_MS = 100L
    }
}
