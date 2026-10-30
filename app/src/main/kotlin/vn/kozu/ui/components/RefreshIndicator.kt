package vn.kozu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A visible, drag-aware refresh pill for Home and Explore.
 * The arrow follows the pull distance and changes into a spinner when
 * the refresh is triggered. Its offset follows the user's finger rather
 * than staying hidden behind the screen header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val distance = state.distanceFraction.coerceIn(0f, 1.25f)
    val rotation by animateFloatAsState(
        targetValue = (distance.coerceAtMost(1f) * 180f),
        animationSpec = tween(durationMillis = 90),
        label = "kozu-refresh-arrow"
    )
    val elevation by animateDpAsState(
        targetValue = if (isRefreshing) 8.dp else (2.dp + (distance * 5f).dp).coerceAtMost(8.dp),
        label = "kozu-refresh-elevation"
    )

    AnimatedVisibility(
        visible = isRefreshing || distance > 0.04f,
        modifier = modifier.offset(y = (8.dp + (distance * 44f).dp).coerceAtMost(64.dp)),
        enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.86f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.86f)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = elevation,
            modifier = Modifier.semantics {
                contentDescription = if (isRefreshing) {
                    "Refreshing osu! catalog"
                } else {
                    "Pull down to refresh"
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }

                Spacer(Modifier.width(9.dp))

                Text(
                    text = when {
                        isRefreshing -> "Refreshing Kozu…"
                        distance >= 1f -> "Release to refresh"
                        else -> "Pull down to refresh"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
