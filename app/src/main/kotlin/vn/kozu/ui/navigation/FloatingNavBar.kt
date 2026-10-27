package vn.kozu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuMotion

/**
 * Floating bottom navigation bar displaying app tabs with clean fade animations.
 */
@Composable
fun FloatingNavBar(
    selected: KozuTab,
    onSelect: (KozuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KozuTab.entries.forEach { tab ->
                    NavItem(tab = tab, isSelected = selected == tab, onClick = { onSelect(tab) })
                }
            }
        }
    }
}

/**
 * Individual navigation bar item with smooth fade label animation and bounce click response.
 */
@Composable
private fun NavItem(
    tab: KozuTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = KozuMotion.effects(),
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = KozuMotion.effects(),
        label = "navContent"
    )

    Surface(
        modifier = Modifier.bounceClick(pressedScale = 0.94f, onClick = onClick),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180))
            ) {
                Text(
                    text = tab.title,
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}
