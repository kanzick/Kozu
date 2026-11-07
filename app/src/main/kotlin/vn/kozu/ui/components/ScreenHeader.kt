package vn.kozu.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.AutoAwesome
import vn.kozu.ui.i18n.kozuUiString

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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

@Composable
fun CenteredHeader(
    title: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { leading() }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = if (compactWidth) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { trailing() }
        }
    }
}

@Composable
fun HeaderTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    rotating: Boolean = false,
    startPadding: Dp = 8.dp
) {
    val rotation = if (rotating) {
        val transition = rememberInfiniteTransition(label = "header-refresh")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "header-refresh-rotation"
        )
        angle
    } else {
        0f
    }
    Surface(
        modifier = modifier
            .padding(start = startPadding)
            .size(46.dp)
            .bounceClick(onClick = onClick),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit, startPadding: Dp = 8.dp) {
    HeaderAction(
        icon = Icons.Rounded.Settings,
        contentDescription = kozuUiString("Settings"),
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
        startPadding = startPadding
    )
}

/** "New" pill beside the settings button: opens osu!'s news. */
@Composable
fun NewsAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.padding(start = 8.dp).height(46.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(kozuUiString("New"), style = MaterialTheme.typography.labelLarge)
        }
    }
}
