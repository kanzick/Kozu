package vn.kozu.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import vn.kozu.domain.model.AppearancePrefs
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
    appFont: AppFont = AppFont.GOOGLE_SANS_FLEX,
    appearance: AppearancePrefs = AppearancePrefs(),
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    secondarySeed: Color? = null,
    tertiarySeed: Color? = null,
    useSystemDynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamicAvailable = useSystemDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val target =
        remember(seed, darkTheme, amoled, paletteStyle, secondarySeed, tertiarySeed, dynamicAvailable) {
            if (dynamicAvailable) {
                systemDynamicScheme(context, darkTheme, amoled)
            } else {
                kozuColorScheme(seed, darkTheme, amoled, paletteStyle, secondarySeed, tertiarySeed)
            }
        }
    val animated = rememberAnimatedColorScheme(target)
    val typography =
        remember(appFont, appearance.fontWeightShift, appearance.fontWidth, appearance.fontRoundness) {
            typographyFor(appFont, appearance)
        }

    if (appStyle == AppStyle.EXPRESSIVE) {
        MaterialExpressiveTheme(
            colorScheme = animated,
            shapes = KozuShapes,
            typography = typography,
        ) {
            ProvideKozuDensity(appearance, content)
        }
    } else {
        MaterialTheme(
            colorScheme = animated,
            shapes = Shapes(),
            typography = typography,
        ) {
            ProvideKozuDensity(appearance, content)
        }
    }
}


@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun systemDynamicScheme(
    context: Context,
    dark: Boolean,
    amoled: Boolean,
): ColorScheme {
    if (!dark) return dynamicLightColorScheme(context)
    val scheme = dynamicDarkColorScheme(context)
    if (!amoled) return scheme
    return scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1B1B1B),
        surfaceContainerHighest = Color(0xFF242424),
    )
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
