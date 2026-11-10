package vn.kozu.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.TransportShape
import vn.kozu.player.RepeatState
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.KozuIcons
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.theme.KozuMotion

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = rememberKozuHaptics()
    val prefs = LocalAppSettings.current.appearance
    val iconStyle = prefs.iconStyle
    val prevSource = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val nextSource = remember { MutableInteractionSource() }
    val prevPressed by prevSource.collectIsPressedAsState()
    val playPressed by playSource.collectIsPressedAsState()
    val nextPressed by nextSource.collectIsPressedAsState()
    val prevWeight by animateFloatAsState(if (prevPressed) 1.5f else 1f, KozuMotion.press(), label = "prevW")
    val playWeight by animateFloatAsState(if (playPressed) 2.4f else 1.8f, KozuMotion.press(), label = "playW")
    val nextWeight by animateFloatAsState(if (nextPressed) 1.5f else 1f, KozuMotion.press(), label = "nextW")
    val playRadius by animateDpAsState(
        targetValue =
            when (prefs.transportShape) {
                TransportShape.PILL -> if (isPlaying) 30.dp else 44.dp
                TransportShape.ROUND -> 44.dp
                TransportShape.SQUARE -> if (isPlaying) 18.dp else 26.dp
            },
        animationSpec = KozuMotion.spatial(),
        label = "playRadius",
    )
    val controlHeight = if (compact) 72.dp else 82.dp
    val sideShape: Shape =
        when (prefs.transportShape) {
            TransportShape.PILL -> RoundedCornerShape(44.dp)
            TransportShape.ROUND -> CircleShape
            TransportShape.SQUARE -> RoundedCornerShape(22.dp)
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(controlHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            icon = KozuIcons.skipPrevious(iconStyle),
            description = "Previous",
            weight = prevWeight,
            shape = sideShape,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            source = prevSource,
            buttonHeight = controlHeight,
            onClick = {
                haptics.tick()
                onPrevious()
            },
        )
        TransportButton(
            icon = playbackIconVector(LocalAppSettings.current.playbackIcon, isPlaying),
            description = if (isPlaying) "Pause" else "Play",
            weight = playWeight,
            shape = RoundedCornerShape(playRadius),
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
            source = playSource,
            buttonHeight = controlHeight,
            iconSize = if (compact) 34.dp else 38.dp,
            onClick = {
                haptics.press()
                onPlayPause()
            },
        )
        TransportButton(
            icon = KozuIcons.skipNext(iconStyle),
            description = "Next",
            weight = nextWeight,
            shape = sideShape,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            source = nextSource,
            buttonHeight = controlHeight,
            onClick = {
                haptics.tick()
                onNext()
            },
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
    buttonHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .height(buttonHeight)
                .clip(shape)
                .background(container)
                .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content,
            modifier = Modifier.size(iconSize),
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
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = rememberKozuHaptics()
    val prefs = LocalAppSettings.current.appearance
    val iconStyle = prefs.iconStyle
    val square = prefs.transportShape == TransportShape.SQUARE
    val outer: Shape = if (square) RoundedCornerShape(26.dp) else CircleShape
    val segmentShape: Shape = if (square) RoundedCornerShape(20.dp) else CircleShape
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = outer,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToggleSegment(KozuIcons.shuffle(iconStyle), "Shuffle", shuffle, segmentShape, if (compact) 48.dp else 56.dp) {
                haptics.tick()
                onShuffle()
            }
            ToggleSegment(
                icon = KozuIcons.repeat(iconStyle, repeat == RepeatState.ONE),
                shape = segmentShape,
                description = "Repeat",
                active = repeat != RepeatState.OFF,
                height = if (compact) 48.dp else 56.dp,
                onClick = {
                    haptics.tick()
                    onRepeat()
                },
            )
            ToggleSegment(
                icon = KozuIcons.favorite(iconStyle, isFavorite),
                shape = segmentShape,
                description = "Like",
                active = isFavorite,
                height = if (compact) 48.dp else 56.dp,
                onClick = {
                    if (isFavorite) haptics.tick() else haptics.confirm()
                    onFavorite()
                },
            )
        }
    }
}

@Composable
private fun RowScope.ToggleSegment(
    icon: ImageVector,
    description: String,
    active: Boolean,
    shape: Shape,
    height: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .weight(1f)
                .height(height)
                .bounceClick(onClick = onClick),
        shape = shape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
