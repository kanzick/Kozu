package vn.kozu.player

import vn.kozu.domain.model.Beatmap

enum class RepeatState { OFF, ALL, ONE }

data class PlaybackState(
    val current: Beatmap? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatState = RepeatState.OFF,
    val queue: List<Beatmap> = emptyList(),
    val queueIndex: Int = -1,
    /** Ví dụ "44.1 kHz • 128 kbps • MP3". */
    val audioInfo: String? = null
)
