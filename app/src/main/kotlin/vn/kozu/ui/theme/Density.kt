package vn.kozu.ui.theme

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.AppearancePrefs
import vn.kozu.domain.model.UiDensity

val LocalUiDensity = compositionLocalOf { UiDensity.COMFORTABLE }


val isCompactLayout: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalUiDensity.current.compactRows

private const val MAX_TOTAL_FONT_SCALE = 1.85f


@Composable
fun ProvideKozuDensity(
    prefs: AppearancePrefs,
    content: @Composable () -> Unit,
) {
    val system = LocalDensity.current
    val layout = prefs.density.layoutScale
    val text = prefs.textScale / 100f

    val scaled =
        remember(system.density, system.fontScale, layout, text) {
            val totalFont = (system.fontScale * text).coerceAtMost(MAX_TOTAL_FONT_SCALE)
            Density(
                density = system.density * layout,
                fontScale = totalFont / layout,
            )
        }

    CompositionLocalProvider(
        LocalDensity provides scaled,
        LocalUiDensity provides prefs.density,
        LocalMinimumInteractiveComponentSize provides (48f / layout).dp,
        content = content,
    )
}
