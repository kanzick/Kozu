package vn.kozu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height taken at the bottom of an overlay screen (Settings, All beatmaps, Stats…) by the
 * floating mini player that is drawn on top of it. Scrollable content adds it as end padding
 * so the last item can always be scrolled clear of the player.
 */
val LocalOverlayBottomInset = compositionLocalOf { 0.dp }

/** The bottom edge (window px) below which scrolling content has not "arrived" yet. */
@Stable
class ScrollViewport {
    var bottomPx by mutableFloatStateOf(Float.MAX_VALUE)
}

val LocalScrollViewport = compositionLocalOf<ScrollViewport?> { null }

/**
 * Items that sit near the bottom edge of a scrolling page start out faded and slightly lowered,
 * then settle into place as they are scrolled up into view. Items that are already comfortably
 * on screen (including everything on a page that does not scroll) are untouched.
 *
 * Both the item position and the viewport edge are read inside `graphicsLayer`, so scrolling
 * only invalidates drawing, never composition.
 */
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
