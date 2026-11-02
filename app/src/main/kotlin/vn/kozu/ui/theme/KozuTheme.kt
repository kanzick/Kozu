package vn.kozu.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import vn.kozu.domain.model.AppFont
import vn.kozu.domain.model.AppStyle
import vn.kozu.domain.model.PaletteStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KozuTheme(
    seed: Color,
    darkTheme: Boolean,
    amoled: Boolean,
    appStyle: AppStyle = AppStyle.EXPRESSIVE,
    appFont: AppFont = AppFont.KOZU,
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    content: @Composable () -> Unit,
) {
    val target =
        remember(seed, darkTheme, amoled, paletteStyle) {
            kozuColorScheme(seed, darkTheme, amoled, paletteStyle)
        }
    val animated = rememberAnimatedColorScheme(target)
    val typography = if (appFont == AppFont.SYSTEM) SystemKozuTypography else KozuTypography

    if (appStyle == AppStyle.EXPRESSIVE) {
        MaterialExpressiveTheme(
            colorScheme = animated,
            shapes = KozuShapes,
            typography = typography,
            content = content,
        )
    } else {
        MaterialTheme(
            colorScheme = animated,
            shapes = Shapes(),
            typography = Typography(),
            content = content,
        )
    }
}

@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(target) {
        if (to === target) return@LaunchedEffect
        from = lerpColorScheme(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }

    return if (progress.value >= 1f) to else lerpColorScheme(from, to, progress.value)
}
