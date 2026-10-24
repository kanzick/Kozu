package vn.kozu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 4 thanh equalizer nhảy theo nhịp khi đang phát, đứng yên khi tạm dừng. */
@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val a by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by transition.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(610, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    val d by transition.animateFloat(0.35f, 0.85f, infiniteRepeatable(tween(690, easing = LinearEasing), RepeatMode.Reverse), label = "d")

    Canvas(modifier) {
        val heights = if (isPlaying) listOf(a, b, c, d) else listOf(0.35f, 0.6f, 0.45f, 0.3f)
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * 3) / 4f
        heights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
