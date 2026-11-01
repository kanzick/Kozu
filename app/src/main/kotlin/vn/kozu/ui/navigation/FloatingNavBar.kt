package vn.kozu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.NavigationStyle
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuMotion

@Composable
fun FloatingNavBar(
    selected: KozuTab,
    style: NavigationStyle,
    onSelect: (KozuTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    cornerRadius: Int = 32
) {
    if (style == NavigationStyle.CLASSIC) {
        ClassicNavBar(selected = selected, onSelect = onSelect, modifier = modifier)
    } else {
        PillNavBar(
            selected = selected,
            compact = compact,
            cornerRadius = cornerRadius,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

@Composable
private fun PillNavBar(
    selected: KozuTab,
    compact: Boolean,
    cornerRadius: Int,
    onSelect: (KozuTab) -> Unit,
    modifier: Modifier
) {
    val tabs = KozuTab.entries.filter { it.inPill }
    val barHeight = if (compact) 56.dp else 64.dp
    val radius = cornerRadius.coerceIn(0, 32)
    val barShape = RoundedCornerShape((barHeight.value * radius / 64f).dp)
    val itemShape = RoundedCornerShape(((barHeight.value - 18f) * radius / 64f).dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(barHeight),
            shape = barShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    PillItem(
                        tab = tab,
                        isSelected = selected == tab,
                        compact = compact,
                        itemHeight = barHeight - 18.dp,
                        shape = itemShape,
                        onClick = { onSelect(tab) }
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        SearchButton(
            isSelected = selected == KozuTab.SEARCH,
            size = if (compact) 48.dp else 56.dp,
            shape = RoundedCornerShape(((if (compact) 48f else 56f) * radius / 64f).dp),
            onClick = { onSelect(KozuTab.SEARCH) }
        )
    }
}

@Composable
private fun RowScope.PillItem(
    tab: KozuTab,
    isSelected: Boolean,
    compact: Boolean,
    itemHeight: Dp,
    shape: Shape,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val weight by animateFloatAsState(
        targetValue = if (isSelected && !compact) 2.4f else 1f,
        animationSpec = KozuMotion.spatial(),
        label = "navItemWeight"
    )
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "navItemContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.primary,
        animationSpec = tween(200),
        label = "navItemContent"
    )

    Box(
        modifier = Modifier
            .weight(weight.coerceAtLeast(0.2f))
            .height(itemHeight)
            .bounceClick(pressedScale = 0.95f, onClick = onClick)
            .clip(shape)
            .background(container)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = tab.title
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) tab.icon else tab.outlinedIcon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(
                visible = isSelected && !compact,
                enter = fadeIn(tween(160)) + expandHorizontally(KozuMotion.spatial()),
                exit = fadeOut(tween(90)) + shrinkHorizontally(tween(160))
            ) {
                Text(
                    text = tab.title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun SearchButton(
    isSelected: Boolean,
    size: Dp,
    shape: Shape,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "searchContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.primary,
        animationSpec = tween(200),
        label = "searchContent"
    )
    Box(
        modifier = Modifier
            .size(size)
            .bounceClick(pressedScale = 0.92f, onClick = onClick)
            .clip(shape)
            .background(container)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = KozuTab.SEARCH.title
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) KozuTab.SEARCH.icon else KozuTab.SEARCH.outlinedIcon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun ClassicNavBar(
    selected: KozuTab,
    onSelect: (KozuTab) -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KozuTab.entries.forEach { tab ->
                ClassicItem(
                    tab = tab,
                    isSelected = selected == tab,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun ClassicItem(
    tab: KozuTab,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "classicNavContent"
    )
    Column(
        modifier = modifier
            .bounceClick(pressedScale = 0.96f, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
            }
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = if (isSelected) tab.icon else tab.outlinedIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}
