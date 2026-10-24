package vn.kozu.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import vn.kozu.domain.model.Beatmap

/** Các hành động có thể làm trên bài hát, dùng chung cho mọi màn hình. */
interface TrackActions {
    fun play(queue: List<Beatmap>, index: Int)
    fun playShuffled(queue: List<Beatmap>)
    fun toggleFavorite(beatmap: Beatmap)
    fun openMenu(beatmap: Beatmap)
}

@Immutable
data class NowPlayingInfo(val beatmapId: Long?, val isPlaying: Boolean)

val LocalTrackActions = staticCompositionLocalOf<TrackActions> {
    error("TrackActions not provided")
}

val LocalNowPlaying = compositionLocalOf { NowPlayingInfo(null, false) }

val LocalFavoriteIds = compositionLocalOf<Set<Long>> { emptySet() }
