package vn.kozu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.kozu.domain.model.SeekBarStyle
import vn.kozu.ui.theme.KozuMotion

@Composable
fun WavySlider(
    progress: Float,
    isPlaying: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    style: SeekBarStyle = LocalAppSettings.current.appearance.seekStyle,
) {
    var dragging by remember { mutableStateOf(false) }
    var lastFraction by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val glide = remember { Animatable(0f) }
    var gliding by remember { mutableStateOf(false) }

    val amplitude by animateFloatAsState(
        targetValue = if (animated && isPlaying && !dragging && style == SeekBarStyle.WAVY) 1f else 0f,
        animationSpec = KozuMotion.spatial(),
        label = "waveAmplitude",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (dragging) 0.6f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumbWidth",
    )

    LaunchedEffect(isPlaying, animated, style) {
        if (!isPlaying || !animated || style != SeekBarStyle.WAVY) return@LaunchedEffect
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
    val currentProgress by rememberUpdatedState(progress)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val from = if (gliding) glide.value else currentProgress
                        scope.launch {
                            gliding = true
                            glide.snapTo(from)
                            currentScrub(fraction)
                            currentFinished(fraction)
                            glide.animateTo(fraction, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                            delay(260)
                            gliding = false
                        }
                    }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            gliding = false
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
                        },
                    )
                },
    ) {
        val shown = if (gliding) glide.value else progress
        val centerY = size.height / 2f
        val thumbX = (size.width * shown.coerceIn(0f, 1f)).coerceIn(0f, size.width)

        when (style) {
            SeekBarStyle.WAVY -> {
                val stroke = 5.dp.toPx()
                val gap = 8.dp.toPx()
                val waveLength = 30.dp.toPx()
                val waveAmp = 4.dp.toPx() * amplitude
                val inactiveStart = (thumbX + gap).coerceAtMost(size.width)
                if (inactiveStart < size.width) {
                    drawLine(inactiveColor, Offset(inactiveStart, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
                }
                val activeEnd = (thumbX - gap).coerceAtLeast(0f)
                if (activeEnd > 0f) {
                    val path = Path()
                    var x = 0f
                    path.moveTo(0f, centerY + waveAmp * sin(phase.toDouble()).toFloat())
                    while (x < activeEnd) {
                        x = (x + 3f).coerceAtMost(activeEnd)
                        path.lineTo(x, centerY + waveAmp * sin((x / waveLength * 2f * PI.toFloat()) + phase))
                    }
                    drawPath(path, activeColor, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawHandle(activeColor, thumbX, centerY, 5.dp.toPx() * pressScale, 24.dp.toPx())
            }

            SeekBarStyle.FLAT -> {
                val stroke = 6.dp.toPx()
                val gap = 8.dp.toPx()
                val inactiveStart = (thumbX + gap).coerceAtMost(size.width)
                if (inactiveStart < size.width) {
                    drawLine(inactiveColor, Offset(inactiveStart, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
                }
                val activeEnd = (thumbX - gap).coerceAtLeast(0f)
                if (activeEnd > 0f) {
                    drawLine(activeColor, Offset(0f, centerY), Offset(activeEnd, centerY), stroke, StrokeCap.Round)
                }
                drawHandle(activeColor, thumbX, centerY, 5.dp.toPx() * pressScale, 24.dp.toPx())
            }

            SeekBarStyle.THICK -> {
                val thickness = 16.dp.toPx()
                val gap = 6.dp.toPx()
                val outer = thickness / 2f
                val inner = 3.dp.toPx()
                val top = centerY - thickness / 2f
                val activeEnd = (thumbX - gap).coerceAtLeast(0f)
                val inactiveStart = (thumbX + gap).coerceAtMost(size.width)
                if (inactiveStart < size.width) {
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(inactiveStart, top),
                        size = Size(size.width - inactiveStart, thickness),
                        cornerRadius = CornerRadius(min(inner, outer)),
                    )
                }
                if (activeEnd > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, top),
                        size = Size(activeEnd, thickness),
                        cornerRadius = CornerRadius(min(outer, activeEnd / 2f)),
                    )
                }
                drawHandle(activeColor, thumbX, centerY, 4.dp.toPx() * pressScale, 34.dp.toPx())
            }

            SeekBarStyle.THIN -> {
                val stroke = 3.dp.toPx()
                drawLine(inactiveColor, Offset(0f, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
                drawLine(activeColor, Offset(0f, centerY), Offset(thumbX, centerY), stroke, StrokeCap.Round)
                drawCircle(activeColor, 8.dp.toPx() * (0.75f + 0.25f * pressScale), Offset(thumbX, centerY))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    color: Color,
    x: Float,
    centerY: Float,
    width: Float,
    height: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x - width / 2f, centerY - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f),
    )
}
