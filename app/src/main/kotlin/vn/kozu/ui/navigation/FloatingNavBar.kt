package vn.kozu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * Thanh điều hướng nổi: 3 tab trong một viên thuốc (tab đang chọn nở ra kèm nhãn)
 * và nút Search tròn riêng bên phải.
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(KozuTab.HOME, KozuTab.EXPLORE, KozuTab.LIBRARY).forEach { tab ->
                    NavItem(tab = tab, isSelected = selected == tab, showLabel = true, onClick = { onSelect(tab) })
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Box(Modifier.padding(6.dp)) {
                NavItem(
                    tab = KozuTab.SEARCH,
                    isSelected = selected == KozuTab.SEARCH,
                    showLabel = false,
                    onClick = { onSelect(KozuTab.SEARCH) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: KozuTab,
    isSelected: Boolean,
    showLabel: Boolean,
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
        modifier = Modifier
            .animateContentSize(animationSpec = KozuMotion.spatial())
            .bounceClick(onClick = onClick),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(24.dp)
            )
            if (showLabel) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(KozuMotion.spatial()),
                    exit = fadeOut() + shrinkHorizontally(KozuMotion.spatial())
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
