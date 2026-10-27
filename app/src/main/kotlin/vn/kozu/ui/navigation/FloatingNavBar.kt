package vn.kozu.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import vn.kozu.ui.components.bounceClick

/**
 * Fixed-width bottom navigation with a compact selected state.
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(5.dp)) {
                val tabs = KozuTab.entries
                val itemWidth = (maxWidth - 2.dp * (tabs.size - 1)) / tabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = (itemWidth + 2.dp) * tabs.indexOf(selected),
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    label = "navIndicator"
                )
                Surface(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .height(54.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {}
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        NavItem(
                            tab = tab,
                            isSelected = selected == tab,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(tab) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Each item keeps a stable width so selecting a tab never expands the navigation bar.
 */
@Composable
private fun NavItem(
    tab: KozuTab,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val content by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "navContent"
    )

    Surface(
        modifier = modifier.bounceClick(pressedScale = 0.96f, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
