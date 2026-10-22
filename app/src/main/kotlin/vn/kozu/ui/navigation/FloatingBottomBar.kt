package vn.kozu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import vn.kozu.ui.KozuTab

@Composable
internal fun FloatingBottomBar(
    selectedTab: KozuTab,
    onTabSelected: (KozuTab) -> Unit
) {
    val tabs = KozuTab.entries

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 5.dp)
            .shadow(7.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        // Fixed-width tab slots avoid the old selected-weight layout reflow.
        // Only the pill's offset and the selected label animate when a tab changes.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .height(42.dp)
        ) {
            val slotWidth = maxWidth / tabs.size.toFloat()
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedTab.ordinal.toFloat() + 3.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "kozuNavIndicator"
            )

            Surface(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth - 6.dp)
                    .height(36.dp)
                    .align(Alignment.CenterStart),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) { }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .clickable { if (!selected) onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(21.dp)
                            )
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(androidx.compose.animation.core.tween(150)) +
                                    expandHorizontally(
                                        expandFrom = Alignment.Start,
                                        animationSpec = androidx.compose.animation.core.tween(210)
                                    ),
                                exit = fadeOut(androidx.compose.animation.core.tween(90)) +
                                    shrinkHorizontally(
                                        shrinkTowards = Alignment.Start,
                                        animationSpec = androidx.compose.animation.core.tween(150)
                                    )
                            ) {
                                Row {
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
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
