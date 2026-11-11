package vn.kozu.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.R
import vn.kozu.data.news.NewsReadState
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.ThemePreset

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
            Text(text = title,
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
    Text(text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun headerIndicatorColor(): Color {
    val appSettings = LocalAppSettings.current
    val isPlaying = LocalNowPlaying.current.isPlaying
    val context = LocalContext.current
    val dynamicPresetColor = if (!isPlaying && appSettings.themePreset == ThemePreset.DYNAMIC) {
        val dark = when (appSettings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        remember(context, dark) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
            } else {
                ThemePreset.DYNAMIC.seed
            }
        }
    } else {
        ThemePreset.DYNAMIC.seed
    }
    return when {
        isPlaying -> MaterialTheme.colorScheme.primary
        appSettings.themePreset == ThemePreset.CUSTOM -> appSettings.accent.color
        appSettings.themePreset == ThemePreset.DYNAMIC -> dynamicPresetColor
        else -> appSettings.themePreset.seed
    }
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
    startPadding: Dp = 8.dp,
    enabled: Boolean = true,
    showIndicator: Boolean = false,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
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
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier),
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
                    .graphicsLayer { rotationZ = rotation },
                tint = if (enabled) content else content.copy(alpha = 0.38f),
            )
            if (showIndicator) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(10.dp)
                        .background(indicatorColor, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
        }
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit, startPadding: Dp = 8.dp) {
    HeaderAction(
        icon = Icons.Rounded.Settings,
        contentDescription = stringResource(R.string.settings),
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
        startPadding = startPadding
    )
}


@Composable
fun NewsAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startPadding: Dp = 8.dp,
) {
    val hasUnread = NewsReadState.hasUnread
    HeaderAction(
        icon = ImageVector.vectorResource(R.drawable.at_newspaper),
        contentDescription = stringResource(R.string.osu_updates),
        onClick = onClick,
        modifier = modifier,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
        startPadding = startPadding,
        showIndicator = hasUnread,
        indicatorColor = if (hasUnread) headerIndicatorColor() else MaterialTheme.colorScheme.primary,
    )
}
