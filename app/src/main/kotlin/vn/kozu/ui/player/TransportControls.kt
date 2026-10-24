package vn.kozu.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import vn.kozu.player.RepeatState
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuMotion





@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prevSource = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val nextSource = remember { MutableInteractionSource() }
    val prevPressed by prevSource.collectIsPressedAsState()
    val playPressed by playSource.collectIsPressedAsState()
    val nextPressed by nextSource.collectIsPressedAsState()

    val prevWeight by animateFloatAsState(if (prevPressed) 1.5f else 1f, KozuMotion.press(), label = "prevW")
    val playWeight by animateFloatAsState(if (playPressed) 2.4f else 1.8f, KozuMotion.press(), label = "playW")
    val nextWeight by animateFloatAsState(if (nextPressed) 1.5f else 1f, KozuMotion.press(), label = "nextW")
    val playRadius by animateDpAsState(if (isPlaying) 30.dp else 44.dp, KozuMotion.spatial(), label = "playRadius")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            icon = Icons.Rounded.SkipPrevious,
            description = "Previous",
            weight = prevWeight,
            shape = RoundedCornerShape(44.dp),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            source = prevSource,
            onClick = onPrevious
        )
        TransportButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            description = if (isPlaying) "Pause" else "Play",
            weight = playWeight,
            shape = RoundedCornerShape(playRadius),
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
            source = playSource,
            iconSize = 40.dp,
            onClick = onPlayPause
        )
        TransportButton(
            icon = Icons.Rounded.SkipNext,
            description = "Next",
            weight = nextWeight,
            shape = RoundedCornerShape(44.dp),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            source = nextSource,
            onClick = onNext
        )
    }
}

@Composable
private fun RowScope.TransportButton(
    icon: ImageVector,
    description: String,
    weight: Float,
    shape: androidx.compose.ui.graphics.Shape,
    container: Color,
    content: Color,
    source: MutableInteractionSource,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(88.dp)
            .clip(shape)
            .background(container)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content,
            modifier = Modifier.size(iconSize)
        )
    }
}


@Composable
fun PlayerToggleRow(
    shuffle: Boolean,
    repeat: RepeatState,
    isFavorite: Boolean,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToggleSegment(Icons.Rounded.Shuffle, "Shuffle", shuffle, onShuffle)
            ToggleSegment(
                icon = if (repeat == RepeatState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                description = "Repeat",
                active = repeat != RepeatState.OFF,
                onClick = onRepeat
            )
            ToggleSegment(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                description = "Like",
                active = isFavorite,
                onClick = onFavorite
            )
        }
    }
}

@Composable
private fun RowScope.ToggleSegment(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .bounceClick(onClick = onClick),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
