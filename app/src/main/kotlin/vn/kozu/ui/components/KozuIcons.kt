package vn.kozu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.sharp.Favorite
import androidx.compose.material.icons.sharp.FavoriteBorder
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.RepeatOne
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.ui.graphics.vector.ImageVector
import vn.kozu.domain.model.IconStyle

object KozuIcons {
    fun skipPrevious(style: IconStyle): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> Icons.Rounded.SkipPrevious
            IconStyle.OUTLINED -> Icons.Outlined.SkipPrevious
            IconStyle.SHARP -> Icons.Sharp.SkipPrevious
        }

    fun skipNext(style: IconStyle): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> Icons.Rounded.SkipNext
            IconStyle.OUTLINED -> Icons.Outlined.SkipNext
            IconStyle.SHARP -> Icons.Sharp.SkipNext
        }

    fun shuffle(style: IconStyle): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> Icons.Rounded.Shuffle
            IconStyle.OUTLINED -> Icons.Outlined.Shuffle
            IconStyle.SHARP -> Icons.Sharp.Shuffle
        }

    fun repeat(
        style: IconStyle,
        one: Boolean,
    ): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> if (one) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
            IconStyle.OUTLINED -> if (one) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat
            IconStyle.SHARP -> if (one) Icons.Sharp.RepeatOne else Icons.Sharp.Repeat
        }

    fun favorite(
        style: IconStyle,
        filled: Boolean,
    ): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
            IconStyle.OUTLINED -> if (filled) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder
            IconStyle.SHARP -> if (filled) Icons.Sharp.Favorite else Icons.Sharp.FavoriteBorder
        }

    fun play(style: IconStyle): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> Icons.Rounded.PlayArrow
            IconStyle.OUTLINED -> Icons.Outlined.PlayArrow
            IconStyle.SHARP -> Icons.Sharp.PlayArrow
        }

    fun pause(style: IconStyle): ImageVector =
        when (style) {
            IconStyle.ROUNDED -> Icons.Rounded.Pause
            IconStyle.OUTLINED -> Icons.Outlined.Pause
            IconStyle.SHARP -> Icons.Sharp.Pause
        }
}
