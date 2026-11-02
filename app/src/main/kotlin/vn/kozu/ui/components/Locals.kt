package vn.kozu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap

interface TrackActions {
    fun play(
        queue: List<Beatmap>,
        index: Int,
    )

    fun playShuffled(queue: List<Beatmap>)

    fun toggleFavorite(beatmap: Beatmap)

    fun openMenu(beatmap: Beatmap)

    fun download(
        beatmap: Beatmap,
        force: Boolean = false,
    )

    fun downloadAll(beatmaps: List<Beatmap>)

    fun removeDownload(id: Long)
}

@Immutable
data class NowPlayingInfo(
    val beatmapId: Long?,
    val isPlaying: Boolean,
)

@Immutable
data class DownloadInfo(
    val downloaded: Set<Long>,
    val outdated: Set<Long>,
    val active: Map<Long, DownloadStatus>,
)

val LocalDownloads = compositionLocalOf { DownloadInfo(emptySet(), emptySet(), emptyMap()) }

val LocalTrackActions =
    staticCompositionLocalOf<TrackActions> {
        error("TrackActions not provided")
    }

val LocalNowPlaying = compositionLocalOf { NowPlayingInfo(null, false) }

val LocalFavoriteIds = compositionLocalOf<Set<Long>> { emptySet() }

val LocalIsSupporter = compositionLocalOf { false }

val LocalAppSettings = compositionLocalOf { AppSettings() }

@Immutable
class KozuHaptics(
    private val enabled: Boolean,
    private val feedback: HapticFeedback,
) {
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun press() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }
}

@Composable
fun rememberKozuHaptics(): KozuHaptics {
    val feedback = LocalHapticFeedback.current
    val enabled = LocalAppSettings.current.hapticFeedback
    return remember(feedback, enabled) { KozuHaptics(enabled, feedback) }
}

val LocalPerformanceMode = staticCompositionLocalOf { false }

val LocalMetered = compositionLocalOf { false }
