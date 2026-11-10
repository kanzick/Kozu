package vn.kozu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalOverlayBottomInset = compositionLocalOf { 0.dp }


@Stable
class ScrollViewport {
    var bottomPx by mutableFloatStateOf(Float.MAX_VALUE)
}

val LocalScrollViewport = compositionLocalOf<ScrollViewport?> { null }


@Composable
fun Modifier.scrollReveal(zone: Dp = 80.dp, travel: Dp = 20.dp): Modifier {
    val viewport = LocalScrollViewport.current ?: return this
    if (LocalPerformanceMode.current) return this
    val density = LocalDensity.current
    val zonePx = with(density) { zone.toPx() }
    val travelPx = with(density) { travel.toPx() }
    var topPx by remember { mutableFloatStateOf(0f) }
    return this
        .onGloballyPositioned { topPx = it.positionInWindow().y }
        .graphicsLayer {
            val raw = ((viewport.bottomPx - topPx) / zonePx).coerceIn(0f, 1f)
            val eased = raw * raw * (3f - 2f * raw)
            alpha = eased
            translationY = (1f - eased) * travelPx
        }
}


fun Modifier.softTopEdge(
    height: Dp = 26.dp,
    strength: () -> Float,
): Modifier =
    this
        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val s = strength().coerceIn(0f, 1f)
            if (s > 0f) {
                val h = height.toPx()
                drawRect(
                    brush =
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 1f - s),
                            1f to androidx.compose.ui.graphics.Color.Black,
                            startY = 0f,
                            endY = h,
                        ),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
            }
        }

fun Modifier.softTopEdge(
    state: androidx.compose.foundation.lazy.LazyListState,
    height: Dp = 26.dp,
): Modifier =
    softTopEdge(height) {
        if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / 60f).coerceIn(0f, 1f)
    }

fun Modifier.softTopEdge(
    state: androidx.compose.foundation.ScrollState,
    height: Dp = 26.dp,
): Modifier = softTopEdge(height) { (state.value / 60f).coerceIn(0f, 1f) }
