package vn.kozu.player

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class AudioEffectsController(
    private val onUnsupported: (String) -> Unit,
) {
    private val bassBoost =
        Slot(
            label = "Bass boost",
            create = { BassBoost(0, it) },
            configure = { effect, value ->
                if (!effect.strengthSupported) {
                    false
                } else {
                    effect.setStrength(value.coerceIn(0, MAX_STRENGTH).toShort())
                    true
                }
            },
        )

    private val loudness =
        Slot(
            label = "Loudness enhancer",
            create = { LoudnessEnhancer(it) },
            configure = { effect, value ->
                effect.setTargetGain(value.coerceIn(0, MAX_STRENGTH))
                true
            },
        )

    fun apply(
        audioSessionId: Int,
        bassBoost: Int,
        loudness: Int,
    ) {
        this.bassBoost.apply(audioSessionId, bassBoost)
        this.loudness.apply(audioSessionId, loudness)
    }

    fun release() {
        bassBoost.release()
        loudness.release()
    }

    private inner class Slot<T : AudioEffect>(
        private val label: String,
        private val create: (Int) -> T,
        private val configure: (T, Int) -> Boolean,
    ) {
        private var effect: T? = null
        private var sessionId = 0
        private var failedSessionId = 0
        private var reported = false

        fun apply(
            audioSessionId: Int,
            value: Int,
        ) {
            if (value <= 0 || audioSessionId <= 0) {
                release()
                return
            }

            if (effect == null || sessionId != audioSessionId) {
                release()
                if (failedSessionId == audioSessionId) return
                try {
                    effect = create(audioSessionId)
                    sessionId = audioSessionId
                } catch (error: RuntimeException) {
                    fail(audioSessionId, error)
                    return
                }
            }

            val current = effect ?: return
            try {
                if (configure(current, value)) {
                    current.enabled = true
                } else {
                    release()
                    fail(audioSessionId, null)
                }
            } catch (error: RuntimeException) {
                release()
                fail(audioSessionId, error)
            }
        }

        fun release() {
            val current = effect ?: return
            effect = null
            sessionId = 0
            try {
                current.release()
            } catch (error: RuntimeException) {
                Log.w(TAG, "Could not release $label", error)
            }
        }

        private fun fail(
            audioSessionId: Int,
            error: RuntimeException?,
        ) {
            failedSessionId = audioSessionId
            Log.w(TAG, "$label is not available on this device", error)
            if (!reported) {
                reported = true
                onUnsupported("$label isn't supported by this device.")
            }
        }
    }

    private companion object {
        const val TAG = "AudioEffects"
        const val MAX_STRENGTH = 1000
    }
}
