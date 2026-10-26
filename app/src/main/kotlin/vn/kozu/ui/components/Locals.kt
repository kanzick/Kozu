package vn.kozu.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap

interface TrackActions {
    fun play(queue: List<Beatmap>, index: Int)
    fun playShuffled(queue: List<Beatmap>)
    fun toggleFavorite(beatmap: Beatmap)
    fun openMenu(beatmap: Beatmap)
    fun download(beatmap: Beatmap, force: Boolean = false)
    fun downloadAll(beatmaps: List<Beatmap>)
    fun removeDownload(id: Long)
}

@Immutable
data class NowPlayingInfo(val beatmapId: Long?, val isPlaying: Boolean)

@Immutable
data class DownloadInfo(
    val downloaded: Set<Long>,
    val outdated: Set<Long>,
    val active: Map<Long, DownloadStatus>
)

val LocalDownloads = compositionLocalOf { DownloadInfo(emptySet(), emptySet(), emptyMap()) }

val LocalTrackActions = staticCompositionLocalOf<TrackActions> {
    error("TrackActions not provided")
}

val LocalNowPlaying = compositionLocalOf { NowPlayingInfo(null, false) }

val LocalFavoriteIds = compositionLocalOf<Set<Long>> { emptySet() }
