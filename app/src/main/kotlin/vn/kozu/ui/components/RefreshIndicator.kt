package vn.kozu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.kozu.ui.theme.KozuCookies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val distance = state.distanceFraction.coerceIn(0f, 1.25f)
    val transition = rememberInfiniteTransition(label = "kozu-refresh")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "kozu-refresh-rotation"
    )
    val rotation = if (isRefreshing) spin else distance.coerceAtMost(1f) * 360f

    AnimatedVisibility(
        visible = isRefreshing || distance > 0.04f,
        modifier = modifier.offset(y = (-20.dp + (distance * 24f).dp).coerceIn(-20.dp, 10.dp)),
        enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.86f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.86f)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 3.dp,
            modifier = Modifier.semantics {
                contentDescription = if (isRefreshing) {
                    "Refreshing osu! catalog"
                } else {
                    "Pull down to refresh"
                }
            }
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = KozuCookies.Flower8,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(21.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {}
            }
        }
    }
}
