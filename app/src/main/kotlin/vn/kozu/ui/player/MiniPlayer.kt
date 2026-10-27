package vn.kozu.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.bounceClick

/**
 * Compact persistent mini player bar floating above bottom navigation bar.
 * Displays artwork with progress ring, song title, artist, and playback controls (previous, play/pause, next).
 * Tapping opens the full player; swiping down closes the current track and queue.
 */
@Composable
fun MiniPlayer(
    beatmap: Beatmap,
    isPlaying: Boolean,
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pos by position.collectAsStateWithLifecycle()
    val dur by duration.collectAsStateWithLifecycle()
    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "miniProgress")

    val spin = remember { Animatable(0f) }
    val settleOffset = remember { Animatable(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val dragThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(beatmap.id) {
        dragOffset = 0f
        isDragging = false
        settleOffset.snapTo(0f)
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                spin.animateTo(spin.value + 360f, tween(10_000, easing = LinearEasing))
            }
        }
    }

    val ringTrack = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
    val ringColor = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .graphicsLayer { translationY = if (isDragging) dragOffset else settleOffset.value }
            .pointerInput(onDismiss, dragThreshold) {
                var verticalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        verticalDrag = 0f
                        dragOffset = 0f
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        val dragged = dragOffset
                        if (verticalDrag >= dragThreshold) {
                            scope.launch {
                                settleOffset.snapTo(dragged)
                                settleOffset.animateTo(size.height.toFloat(), tween(180))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                settleOffset.snapTo(dragged)
                                settleOffset.animateTo(0f, tween(180))
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        val dragged = dragOffset
                        scope.launch {
                            settleOffset.snapTo(dragged)
                            settleOffset.animateTo(0f, tween(180))
                        }
                    },
                    onVerticalDrag = { change, amount ->
                        verticalDrag += amount
                        if (verticalDrag > 0f) {
                            change.consume()
                            dragOffset = verticalDrag
                        }
                    }
                )
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(50.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = ringTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(strokeWidth)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                BeatmapArt(
                    beatmap = beatmap,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationZ = spin.value },
                    shape = CircleShape
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = beatmap.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = beatmap.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick(onClick = onPrevious),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(5.dp))
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClick(onClick = onPlayPause),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(5.dp))
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick(onClick = onNext),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
