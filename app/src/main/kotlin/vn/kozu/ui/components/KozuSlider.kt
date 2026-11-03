package vn.kozu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.kozu.domain.model.SliderStyle
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val SliderCrossSize = 48.dp
private val ExpressiveTrack = 16.dp
private val ExpressiveHandleLength = 44.dp
private val ExpressiveHandleWidth = 4.dp
private val ExpressiveGap = 6.dp
private val InnerCorner = 3.dp

/** Snappy spring used when the thumb follows the finger between discrete steps. */
private val FollowSpring = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)

/** Softer spring used when the thumb travels to a tapped position or to a value set from outside. */
private val TravelSpring = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

private class SliderMath(
    val start: Float,
    val end: Float,
    val steps: Int,
) {
    private val span = (end - start).takeIf { it > 0f } ?: 1f

    fun fraction(value: Float): Float = ((value - start) / span).coerceIn(0f, 1f)

    fun value(fraction: Float): Float = start + fraction.coerceIn(0f, 1f) * span

    fun snap(fraction: Float): Float =
        if (steps > 0) {
            ((fraction.coerceIn(0f, 1f) * (steps + 1)).roundToInt() / (steps + 1f))
        } else {
            fraction.coerceIn(0f, 1f)
        }
}

/**
 * Slider used everywhere in Kozu: settings, equalizer, game options and the player seek bar.
 *
 * Unlike the stock Material slider, the thumb always *travels*: it follows the finger with a spring,
 * glides between discrete steps and animates to a tapped position instead of teleporting.
 *
 * @param origin value the filled part starts from (for example `0f` on a -1..1 equalizer band).
 * @param vertical draws the slider bottom-to-top. The caller must give it a bounded height.
 * @param bubbleLabel when set, a small bubble with the current value floats above the thumb while dragging.
 */
@Composable
fun KozuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SliderStyle = LocalAppSettings.current.appearance.sliderStyle,
    vertical: Boolean = false,
    origin: Float? = null,
    bubbleLabel: ((Float) -> String)? = null,
    contentDescription: String? = null,
) {
    val math = remember(valueRange.start, valueRange.endInclusive, steps) {
        SliderMath(valueRange.start, valueRange.endInclusive, steps)
    }
    val scope = rememberCoroutineScope()
    val haptics = rememberKozuHaptics()
    val performance = LocalPerformanceMode.current
    val density = LocalDensity.current

    val position = remember { Animatable(math.snap(math.fraction(value))) }
    var dragging by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var lastEmitted by remember { mutableStateOf(value) }
    var trackLength by remember { mutableFloatStateOf(0f) }

    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)

    // Follow value changes that come from outside (reset buttons, presets, saved settings).
    LaunchedEffect(value, math) {
        if (!dragging) {
            lastEmitted = value
            position.animateTo(math.snap(math.fraction(value)), TravelSpring)
        }
    }

    val effectiveStyle = if (vertical && style == SliderStyle.WAVY) SliderStyle.EXPRESSIVE else style
    val insetPx =
        with(density) {
            when (effectiveStyle) {
                SliderStyle.CLASSIC -> 12.dp.toPx()
                else -> 10.dp.toPx()
            }
        }

    fun fractionAt(pointer: Offset, length: Float): Float {
        val usable = (length - 2f * insetPx).coerceAtLeast(1f)
        val along = if (vertical) length - pointer.y else pointer.x
        return ((along - insetPx) / usable).coerceIn(0f, 1f)
    }

    // Run the "finished" callback one frame later so callers see the recomposed value, which matters
    // for taps where the value change and the end of the gesture arrive together.
    fun finish() {
        scope.launch {
            withFrameNanos { }
            latestFinished?.invoke()
        }
    }

    fun emit(fraction: Float) {
        val v = math.value(fraction)
        if (v != lastEmitted) {
            lastEmitted = v
            if (math.steps > 0) haptics.tick()
            latestChange(v)
        }
    }

    val active = MaterialTheme.colorScheme.primary
    val inactive =
        if (effectiveStyle == SliderStyle.CLASSIC) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val activeTick = MaterialTheme.colorScheme.onPrimary
    val inactiveTick = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
    val alpha = if (enabled) 1f else 0.38f

    val handleWidth by animateFloatAsState(
        targetValue = if (pressed || dragging) 0.5f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "sliderHandleWidth",
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (dragging && bubbleLabel != null) 1f else 0f,
        animationSpec = tween(140),
        label = "sliderBubble",
    )
    val waveAmplitude by animateFloatAsState(
        targetValue = if (dragging || pressed) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sliderWave",
    )
    val phaseTransition = rememberInfiniteTransition(label = "sliderPhase")
    val wavePhase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec =
            infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "sliderPhaseValue",
    )

    val sizeModifier =
        if (vertical) {
            Modifier.width(SliderCrossSize)
        } else {
            Modifier.fillMaxWidth().height(SliderCrossSize)
        }

    Box(
        modifier =
            modifier
                .then(sizeModifier)
                .onSizeChanged { trackLength = if (vertical) it.height.toFloat() else it.width.toFloat() }
                .semantics {
                    if (contentDescription != null) this.contentDescription = contentDescription
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(value.coerceIn(math.start, max(math.start, math.end)), math.start..max(math.start, math.end), steps)
                    if (enabled) {
                        setProgress { target ->
                            val f = math.snap(math.fraction(target))
                            scope.launch { position.animateTo(f, TravelSpring) }
                            emit(f)
                            finish()
                            true
                        }
                    }
                }
                .pointerInput(enabled, math, vertical, insetPx) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { offset ->
                            val length = if (vertical) size.height.toFloat() else size.width.toFloat()
                            val target = math.snap(fractionAt(offset, length))
                            scope.launch { position.animateTo(target, TravelSpring) }
                            emit(target)
                            finish()
                        },
                    )
                }
                .pointerInput(enabled, math, vertical, insetPx) {
                    if (!enabled) return@pointerInput
                    val length = { if (vertical) size.height.toFloat() else size.width.toFloat() }
                    val onStart: (Offset) -> Unit = { offset ->
                        dragging = true
                        val raw = fractionAt(offset, length())
                        val snapped = math.snap(raw)
                        emit(snapped)
                        scope.launch {
                            if (math.steps > 0) position.animateTo(snapped, FollowSpring) else position.snapTo(raw)
                        }
                    }
                    val onMove: (Offset) -> Unit = { offset ->
                        val raw = fractionAt(offset, length())
                        val snapped = math.snap(raw)
                        emit(snapped)
                        scope.launch {
                            if (math.steps > 0) position.animateTo(snapped, FollowSpring) else position.snapTo(raw)
                        }
                    }
                    val onEnd: () -> Unit = {
                        dragging = false
                        val settled = math.snap(position.targetValue)
                        scope.launch { position.animateTo(settled, TravelSpring) }
                        finish()
                    }
                    if (vertical) {
                        detectVerticalDragGestures(
                            onDragStart = onStart,
                            onDragEnd = onEnd,
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                onMove(change.position)
                            },
                        )
                    } else {
                        detectHorizontalDragGestures(
                            onDragStart = onStart,
                            onDragEnd = onEnd,
                            onDragCancel = { dragging = false },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                onMove(change.position)
                            },
                        )
                    }
                },
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            val length = if (vertical) size.height else size.width
            val cross = if (vertical) size.width else size.height
            val usable = (length - 2f * insetPx).coerceAtLeast(1f)
            val handleAlong = insetPx + position.value * usable
            val originAlong = insetPx + (origin?.let { math.fraction(it) } ?: 0f) * usable

            when (effectiveStyle) {
                SliderStyle.CLASSIC -> {
                    val thickness = 4.dp.toPx()
                    val c0 = (cross - thickness) / 2f
                    val c1 = c0 + thickness
                    val lo = min(originAlong, handleAlong)
                    val hi = max(originAlong, handleAlong)
                    val r = thickness / 2f
                    segment(vertical, length, 0f, lo, c0, c1, r, r, inactive)
                    segment(vertical, length, hi, length, c0, c1, r, r, inactive)
                    segment(vertical, length, lo, hi, c0, c1, r, r, active)
                    drawTicks(vertical, length, cross, insetPx, usable, math.steps, lo, hi, 1.5.dp.toPx(), activeTick, inactiveTick, handleAlong, 0f)
                    val thumbRadius = lerpPx(10.dp.toPx(), 7.dp.toPx(), 1f - handleWidth)
                    drawCircle(active, thumbRadius, if (vertical) Offset(cross / 2f, length - handleAlong) else Offset(handleAlong, cross / 2f))
                }

                SliderStyle.WAVY -> {
                    val stroke = 5.dp.toPx()
                    val centerY = cross / 2f
                    val gap = ExpressiveGap.toPx()
                    val lo = min(originAlong, handleAlong)
                    val hi = max(originAlong, handleAlong)
                    val handleIsHi = handleAlong >= originAlong
                    val amp = 3.dp.toPx() * waveAmplitude
                    val waveLength = 26.dp.toPx()
                    val phase = if (performance) 0f else wavePhase

                    fun line(a: Float, b: Float) {
                        if (b - a > 0.5f) {
                            drawLine(inactive, Offset(a, centerY), Offset(b, centerY), stroke, StrokeCap.Round)
                        }
                    }
                    line(0f, if (handleIsHi) lo else lo - gap)
                    line(if (handleIsHi) hi + gap else hi, length)

                    val a0 = if (handleIsHi) lo else lo + gap
                    val a1 = if (handleIsHi) hi - gap else hi
                    if (a1 - a0 > 0.5f) {
                        val path = Path()
                        var x = a0
                        path.moveTo(x, centerY + amp * sin(x / waveLength * 2f * PI.toFloat() + phase))
                        while (x < a1) {
                            x = min(x + 3f, a1)
                            path.lineTo(x, centerY + amp * sin(x / waveLength * 2f * PI.toFloat() + phase))
                        }
                        drawPath(path, active, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    val handleW = ExpressiveHandleWidth.toPx() * handleWidth
                    val handleLen = 28.dp.toPx()
                    drawRoundRect(
                        color = active,
                        topLeft = Offset(handleAlong - handleW / 2f, centerY - handleLen / 2f),
                        size = androidx.compose.ui.geometry.Size(handleW, handleLen),
                        cornerRadius = CornerRadius(handleW / 2f),
                    )
                }

                SliderStyle.EXPRESSIVE -> {
                    val thickness = ExpressiveTrack.toPx()
                    val c0 = (cross - thickness) / 2f
                    val c1 = c0 + thickness
                    val gap = ExpressiveGap.toPx()
                    val outer = thickness / 2f
                    val inner = InnerCorner.toPx()
                    val handleIsHi = handleAlong >= originAlong
                    val lo = min(originAlong, handleAlong)
                    val hi = max(originAlong, handleAlong)

                    val activeStart = if (handleIsHi) lo else lo + gap
                    val activeEnd = if (handleIsHi) hi - gap else hi
                    val lowInactiveEnd = if (handleIsHi) lo else lo - gap
                    val highInactiveStart = if (handleIsHi) hi + gap else hi

                    // Inactive parts on both sides of the filled part.
                    segment(vertical, length, 0f, lowInactiveEnd, c0, c1, outer, inner, inactive)
                    segment(vertical, length, highInactiveStart, length, c0, c1, inner, outer, inactive)
                    // Filled part: round at the track end when it starts there, square-ish next to the handle.
                    val startsAtEdge = origin == null || math.fraction(origin) <= 0.001f
                    segment(
                        vertical, length, activeStart, activeEnd, c0, c1,
                        if (handleIsHi && startsAtEdge) outer else inner,
                        inner, active,
                    )
                    drawTicks(vertical, length, cross, insetPx, usable, math.steps, activeStart, activeEnd, 2.dp.toPx(), activeTick, inactiveTick, handleAlong, gap + 4.dp.toPx())

                    val handleW = ExpressiveHandleWidth.toPx() * handleWidth
                    val handleLen = ExpressiveHandleLength.toPx().coerceAtMost(cross)
                    val handleRect =
                        if (vertical) {
                            Rect(
                                cross / 2f - handleLen / 2f, length - handleAlong - handleW / 2f,
                                cross / 2f + handleLen / 2f, length - handleAlong + handleW / 2f,
                            )
                        } else {
                            Rect(
                                handleAlong - handleW / 2f, cross / 2f - handleLen / 2f,
                                handleAlong + handleW / 2f, cross / 2f + handleLen / 2f,
                            )
                        }
                    drawRoundRect(
                        color = active,
                        topLeft = handleRect.topLeft,
                        size = handleRect.size,
                        cornerRadius = CornerRadius(min(handleRect.width, handleRect.height) / 2f),
                    )
                }
            }
        }

        if (bubbleLabel != null && !vertical && bubbleAlpha > 0.01f) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .width(88.dp)
                        .offset {
                            val usable = (trackLength - 2f * insetPx).coerceAtLeast(1f)
                            val x = insetPx + position.value * usable
                            IntOffset((x - 44.dp.toPx()).roundToInt(), (-30).dp.roundToPx())
                        }
                        .graphicsLayer {
                            this.alpha = bubbleAlpha
                            scaleX = 0.8f + 0.2f * bubbleAlpha
                            scaleY = 0.8f + 0.2f * bubbleAlpha
                        },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    text = bubbleLabel(math.value(math.snap(position.value))),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun lerpPx(
    a: Float,
    b: Float,
    t: Float,
): Float = a + (b - a) * t

/** Draws one rounded segment of a track between two positions along the slider axis. */
private fun DrawScope.segment(
    vertical: Boolean,
    length: Float,
    a0: Float,
    a1: Float,
    c0: Float,
    c1: Float,
    startRadius: Float,
    endRadius: Float,
    color: Color,
) {
    if (a1 - a0 <= 0.5f) return
    val sr = CornerRadius(startRadius)
    val er = CornerRadius(endRadius)
    val round =
        if (vertical) {
            RoundRect(
                rect = Rect(c0, length - a1, c1, length - a0),
                topLeft = er,
                topRight = er,
                bottomRight = sr,
                bottomLeft = sr,
            )
        } else {
            RoundRect(
                rect = Rect(a0, c0, a1, c1),
                topLeft = sr,
                topRight = er,
                bottomRight = er,
                bottomLeft = sr,
            )
        }
    drawPath(Path().apply { addRoundRect(round) }, color)
}

/** Draws the step dots of a discrete slider, skipping the ones hidden behind the handle. */
private fun DrawScope.drawTicks(
    vertical: Boolean,
    length: Float,
    cross: Float,
    inset: Float,
    usable: Float,
    steps: Int,
    activeStart: Float,
    activeEnd: Float,
    radius: Float,
    activeColor: Color,
    inactiveColor: Color,
    handleAlong: Float,
    keepClear: Float,
) {
    if (steps <= 0 || steps > 40) return
    for (i in 0..steps + 1) {
        val along = inset + usable * i / (steps + 1f)
        if (keepClear > 0f && kotlin.math.abs(along - handleAlong) < keepClear) continue
        val inActive = along >= activeStart - 0.5f && along <= activeEnd + 0.5f
        val center = if (vertical) Offset(cross / 2f, length - along) else Offset(along, cross / 2f)
        drawCircle(if (inActive) activeColor else inactiveColor, radius, center)
    }
}
