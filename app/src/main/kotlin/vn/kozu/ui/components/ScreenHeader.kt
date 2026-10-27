package vn.kozu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Modern flat screen header extending behind status bar with leading content and action buttons.
 */
@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    leading: @Composable (RowScope.() -> Unit),
    actions: @Composable (RowScope.() -> Unit)
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { leading() }
                actions()
            }
        }
    }
}

/** Standard screen title header text */
@Composable
fun HeaderTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Header action icon button component */
@Composable
fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        modifier = modifier
            .padding(start = 8.dp)
            .size(46.dp)
            .bounceClick(onClick = onClick),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Quick search action button for screen headers */
@Composable
fun SearchAction(onClick: () -> Unit) {
    HeaderAction(
        icon = Icons.Rounded.Search,
        contentDescription = "Search",
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface
    )
}

/** Quick settings action button for screen headers */
@Composable
fun SettingsAction(onClick: () -> Unit) {
    HeaderAction(
        icon = Icons.Rounded.Settings,
        contentDescription = "Settings",
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface
    )
}
