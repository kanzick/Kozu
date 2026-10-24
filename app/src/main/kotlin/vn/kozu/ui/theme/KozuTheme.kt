package vn.kozu.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Theme gốc của Kozu (Material 3 Expressive).
 * Đổi [seed] -> toàn bộ màu chuyển mượt sang bảng màu mới.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KozuTheme(
    seed: Color,
    darkTheme: Boolean,
    amoled: Boolean,
    content: @Composable () -> Unit
) {
    val target = remember(seed, darkTheme, amoled) { kozuColorScheme(seed, darkTheme, amoled) }
    val animated = rememberAnimatedColorScheme(target)

    MaterialExpressiveTheme(
        colorScheme = animated,
        shapes = KozuShapes,
        typography = KozuTypography,
        content = content
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
        progress.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    return if (progress.value >= 1f) to else lerpColorScheme(from, to, progress.value)
}
