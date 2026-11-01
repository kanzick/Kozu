package vn.kozu.player

data class SleepTimer(
    val endsAtElapsedMs: Long = 0L,
    val atEndOfTrack: Boolean = false
)

internal object SleepFade {
    @Volatile
    var gain: Float = 1f
}
