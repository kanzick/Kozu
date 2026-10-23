package vn.kozu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.model.KozuTab

@Composable
fun FloatingBottomBar(
    selectedTab: KozuTab,
    onTabSelected: (KozuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = KozuTab.entries

    val selectedWidth by animateDpAsState(
        targetValue = when (selectedTab) {
            KozuTab.HOME -> 106.dp
            KozuTab.EXPLORE -> 118.dp
            KozuTab.SEARCH -> 112.dp
            KozuTab.LIBRARY -> 116.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "floatingBottomBarSelectedWidth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .height(52.dp)
            ) {
                val availableWidth = maxWidth
                val nonSelectedCount = (tabs.size - 1).coerceAtLeast(1)
                val itemWidth = ((availableWidth - selectedWidth) / nonSelectedCount).coerceAtLeast(44.dp)

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val targetWidth = if (isSelected) selectedWidth else itemWidth

                        val animatedItemWidth by animateDpAsState(
                            targetValue = targetWidth,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "tabWidth_${tab.name}"
                        )

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "iconColor_${tab.name}"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .width(animatedItemWidth)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!isSelected) onTabSelected(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {}
                            }

                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)),
                                    exit = fadeOut(spring(stiffness = Spring.StiffnessHigh))
                                ) {
                                    Text(
                                        text = tab.title,
                                        modifier = Modifier.padding(start = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

