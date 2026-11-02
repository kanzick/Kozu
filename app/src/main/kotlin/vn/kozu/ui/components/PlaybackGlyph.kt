package vn.kozu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.ui.graphics.vector.ImageVector
import vn.kozu.domain.model.PlaybackIcon

fun playbackIconVector(
    style: PlaybackIcon,
    isPlaying: Boolean,
): ImageVector =
    if (isPlaying) {
        Icons.Rounded.Pause
    } else {
        when (style) {
            PlaybackIcon.ARROW -> Icons.Rounded.PlayArrow
            PlaybackIcon.CIRCLE -> Icons.Rounded.PlayCircle
            PlaybackIcon.CIRCLE_FILLED -> Icons.Rounded.PlayCircleFilled
            PlaybackIcon.CIRCLE_OUTLINE -> Icons.Rounded.PlayCircleOutline
        }
    }
