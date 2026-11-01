package vn.kozu.game.mania.gamestage.engine

class AudioAnchor(
    val mediaTimeMs: Double,
    val systemNano: Long,
    val rate: Double = 1.0,

    val precise: Boolean = true,
) {

    fun mediaTimeAt(nano: Long): Double = mediaTimeMs + (nano - systemNano) / 1_000_000.0 * rate
}

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

    fun start(startTimeMs: Double, withAudio: Boolean = true) {
        baseNano = nanoSource()
        baseTimeMs = startTimeMs
        started = true
        audioMode = withAudio
        audioLocked = false
        audioStartRequested = false
    }

    private fun rawTime(nano: Long): Double = baseTimeMs + (nano - baseNano) / 1_000_000.0

    fun timeAt(nano: Long): Double {
        if (!started) return baseTimeMs
        if (paused) return pausedTimeMs
        val raw = rawTime(nano)
        return if (audioMode && !audioLocked && raw > 0.0) 0.0 else raw
    }

    fun now(): Double = timeAt(nanoSource())

    fun shouldStartAudio(leadMs: Double = 0.0): Boolean {
        if (!started || !audioMode || audioStartRequested) return false
        if (rawTime(nanoSource()) < -leadMs) return false
        audioStartRequested = true
        return true
    }

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

    fun onAudioAnchor(anchor: AudioAnchor) {
        if (!started || !audioMode) return
        val nano = nanoSource()
        val desired = anchor.mediaTimeAt(nano)
        if (!audioLocked) {
            val raw = rawTime(nano)
            audioLocked = true
            if (raw > 0.0) {

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
