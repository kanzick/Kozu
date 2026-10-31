package vn.kozu.game.mania.gamestage.engine

/**
 * A synchronization point between the song file's time and the system clock (System.nanoTime).
 * For MediaPlayer.getTimestamp(), this is (anchorMediaTime, anchorSystemNanoTime, mediaClockRate).
 */
class AudioAnchor(
    val mediaTimeMs: Double,
    val systemNano: Long,
    val rate: Double = 1.0,
    /** false = only the coarse getCurrentPosition() value is available, so it is less reliable. */
    val precise: Boolean = true,
) {
    /** Position in the song at system time [nano]. */
    fun mediaTimeAt(nano: Long): Double = mediaTimeMs + (nano - systemNano) / 1_000_000.0 * rate
}

/**
 * Game clock. Goal: smooth motion without stuttering, synchronized with the actual audio.
 *
 *  - Time advances using System.nanoTime (smooth, nanosecond resolution) rather than reading the MediaPlayer position directly,
 *    because getCurrentPosition() only updates in chunks of several dozen milliseconds.
 *  - Audio is the "slave": whenever a new [AudioAnchor] arrives, the clock gradually corrects toward it
 *    with a limited adjustment step so abnormal timestamps cannot move chart time backward.
 *  - Before the music actually starts (negative time = lead-in), the clock runs freely from [start].
 *    If it reaches t = 0 before the music starts, it stays at 0 until the first anchor arrives (or [giveUpOnAudio]),
 *    preventing any offset caused by MediaPlayer startup latency.
 *
 * All functions are called on a single thread (the main thread).
 */
class GameClock(private val nanoSource: () -> Long = System::nanoTime) {
    private var baseNano = 0L
    private var baseTimeMs = 0.0
    private var started = false
    private var paused = false
    private var pausedTimeMs = 0.0
    private var audioLocked = false
    private var audioMode = true
    private var audioStartRequested = false

    val isAudioLocked: Boolean get() = audioLocked
    val isWaitingForAudio: Boolean get() = started && audioMode && !audioLocked && rawTime(nanoSource()) > 0.0

    /** Starts at [startTimeMs] (usually negative for the lead-in). */
    fun start(startTimeMs: Double, withAudio: Boolean = true) {
        baseNano = nanoSource()
        baseTimeMs = startTimeMs
        started = true
        audioMode = withAudio
        audioLocked = false
        audioStartRequested = false
    }

    private fun rawTime(nano: Long): Double = baseTimeMs + (nano - baseNano) / 1_000_000.0

    /** Song time (ms) at system time [nano]; used to convert touch event timestamps. */
    fun timeAt(nano: Long): Double {
        if (!started) return baseTimeMs
        if (paused) return pausedTimeMs
        val raw = rawTime(nano)
        return if (audioMode && !audioLocked && raw > 0.0) 0.0 else raw
    }

    fun now(): Double = timeAt(nanoSource())

    /** True exactly once, when it is time to call start() on the music player. */
    fun shouldStartAudio(leadMs: Double = 0.0): Boolean {
        if (!started || !audioMode || audioStartRequested) return false
        if (rawTime(nanoSource()) < -leadMs) return false
        audioStartRequested = true
        return true
    }

    /** If the music cannot play, stop waiting and let the clock run freely from the current time. */
    fun giveUpOnAudio() {
        if (!started || audioLocked) return
        val nano = nanoSource()
        val current = timeAt(nano)
        baseNano = nano
        baseTimeMs = current
        audioMode = false
    }

    fun pause() {
        if (!started || paused) return
        pausedTimeMs = timeAt(nanoSource())
        paused = true
    }

    fun resume() {
        if (!started || !paused) return
        baseTimeMs = pausedTimeMs
        baseNano = nanoSource()
        paused = false
    }

    /** Loads a new synchronization point from the music player. */
    fun onAudioAnchor(anchor: AudioAnchor) {
        if (!started || !audioMode) return
        val nano = nanoSource()
        val desired = anchor.mediaTimeAt(nano)
        if (!audioLocked) {
            val raw = rawTime(nano)
            audioLocked = true
            if (raw > 0.0) {
                // Held at 0: jump directly to the actual song time (continuous with the displayed value of 0).
                baseNano = nano
                baseTimeMs = desired
                return
            }
        }
        val error = desired - rawTime(nano)
        val gain = if (anchor.precise) GAIN_PRECISE else GAIN_COARSE
        val limit = if (anchor.precise) MAX_STEP_PRECISE_MS else MAX_STEP_COARSE_MS
        val shift = (error * gain).coerceIn(-limit, limit)
        baseTimeMs += shift
    }

    companion object {
        private const val GAIN_PRECISE = 0.15
        private const val GAIN_COARSE = 0.03
        private const val MAX_STEP_PRECISE_MS = 1.5
        private const val MAX_STEP_COARSE_MS = 0.7
    }
}
