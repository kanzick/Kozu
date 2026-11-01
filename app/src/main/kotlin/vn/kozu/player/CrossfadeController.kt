package vn.kozu.player

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

object CrossfadeCurve {
    const val MIN_TRACK_FACTOR = 3

    fun isEligible(durationMs: Long, fadeMs: Int): Boolean =
        fadeMs > 0 && durationMs != C.TIME_UNSET && durationMs >= MIN_TRACK_FACTOR.toLong() * fadeMs

    fun fadeOutGain(remainingMs: Long, fadeMs: Int): Float {
        if (fadeMs <= 0 || remainingMs >= fadeMs) return 1f
        return ease(remainingMs.toFloat() / fadeMs)
    }

    fun fadeInGain(positionMs: Long, fadeMs: Int): Float {
        if (fadeMs <= 0 || positionMs >= fadeMs) return 1f
        return ease(positionMs.toFloat() / fadeMs)
    }

    private fun ease(fraction: Float): Float =
        sin(fraction.coerceIn(0f, 1f) * (PI.toFloat() / 2f))
}

class CrossfadeController(
    private val player: Player,
    private val externalGain: () -> Float = { 1f }
) {
    private var fadeMs = 0
    private var fadeInPending = false
    private var fadeOutSuppressed = false
    private var applied = 1f

    fun setFadeMs(value: Int) {
        fadeMs = value.coerceAtLeast(0)
        if (fadeMs == 0) {
            fadeInPending = false
            fadeOutSuppressed = false
        }
        tick()
    }

    fun onMediaItemTransition(reason: Int) {
        fadeOutSuppressed = false
        fadeInPending = fadeMs > 0 && (
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            )
        tick()
    }

    fun onSeek() {
        fadeInPending = false
        val duration = player.duration
        fadeOutSuppressed = CrossfadeCurve.isEligible(duration, fadeMs) &&
            duration - player.currentPosition < fadeMs
        tick()
    }

    fun tick(): Boolean {
        val crossfade = crossfadeGain()
        val target = (crossfade * externalGain()).coerceIn(0f, 1f)
        if (abs(applied - target) > VOLUME_EPSILON || (target == 1f && applied != 1f)) {
            player.volume = target
            applied = target
        }
        return target < 1f
    }

    fun release() {
        fadeInPending = false
        fadeOutSuppressed = false
        if (applied != 1f) {
            player.volume = 1f
            applied = 1f
        }
    }

    private fun crossfadeGain(): Float {
        if (fadeMs <= 0 || player.mediaItemCount == 0) return 1f

        val duration = player.duration
        if (!CrossfadeCurve.isEligible(duration, fadeMs) || player.repeatMode == Player.REPEAT_MODE_ONE) {
            return 1f
        }

        val position = player.currentPosition.coerceAtLeast(0L)
        if (!player.playWhenReady) {
            fadeOutSuppressed = fadeOutSuppressed || duration - position < fadeMs
            return 1f
        }

        var gain = 1f
        if (fadeInPending) {
            val fadeInMs = fadeMs / 2
            if (position >= fadeInMs) {
                fadeInPending = false
            } else {
                gain = min(gain, CrossfadeCurve.fadeInGain(position, fadeInMs))
            }
        }
        if (!fadeOutSuppressed && player.hasNextMediaItem()) {
            gain = min(gain, CrossfadeCurve.fadeOutGain(duration - position, fadeMs))
        }
        return gain
    }

    private companion object {
        const val VOLUME_EPSILON = 0.002f
    }
}
