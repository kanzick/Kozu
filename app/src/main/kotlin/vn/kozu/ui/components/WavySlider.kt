package vn.kozu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import vn.kozu.ui.theme.KozuMotion

/**
 * Animated sine-wave progress slider for audio playback control.
 */
@Composable
fun WavySlider(
    progress: Float,
    isPlaying: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var lastFraction by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableFloatStateOf(0f) }

    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying && !dragging) 1f else 0f,
        animationSpec = KozuMotion.spatial(),
        label = "waveAmplitude"
    )
    val thumbWidth by animateFloatAsState(
        targetValue = if (dragging) 3f else 5f,
        animationSpec = KozuMotion.press(),
        label = "thumbWidth"
    )

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1_000_000_000f * 2.6f
                last = now
            }
        }
    }

    val currentScrub by rememberUpdatedState(onScrub)
    val currentFinished by rememberUpdatedState(onScrubFinished)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    currentScrub(fraction)
                    currentFinished(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        lastFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        currentScrub(lastFraction)
                    },
                    onDragEnd = {
                        dragging = false
                        currentFinished(lastFraction)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        lastFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        currentScrub(lastFraction)
                    }
                )
            }
    ) {
        val stroke = 5.dp.toPx()
        val centerY = size.height / 2f
        val gap = 8.dp.toPx()
        val thumbX = (size.width * progress.coerceIn(0f, 1f)).coerceIn(0f, size.width)
        val waveLength = 30.dp.toPx()
        val waveAmp = 4.dp.toPx() * amplitude
        val inactiveStart = (thumbX + gap).coerceAtMost(size.width)
        if (inactiveStart < size.width) {
            drawLine(
                color = inactiveColor,
                start = Offset(inactiveStart, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        val activeEnd = (thumbX - gap).coerceAtLeast(0f)
        if (activeEnd > 0f) {
            val path = Path()
            var x = 0f
            path.moveTo(0f, centerY + waveAmp * sin(phase.toDouble()).toFloat())
            while (x < activeEnd) {
                x = (x + 3f).coerceAtMost(activeEnd)
                val y = centerY + waveAmp * sin((x / waveLength * 2f * PI.toFloat()) + phase)
                path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        val thumbW = thumbWidth.dp.toPx()
        val thumbH = 24.dp.toPx()
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(thumbX - thumbW / 2f, centerY - thumbH / 2f),
            size = Size(thumbW, thumbH),
            cornerRadius = CornerRadius(thumbW / 2f)
        )
    }
}
