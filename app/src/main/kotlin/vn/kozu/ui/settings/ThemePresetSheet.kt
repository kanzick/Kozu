package vn.kozu.ui.settings

import vn.kozu.ui.components.KozuSheetHandle
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.ThemePreset
import vn.kozu.R
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.kozuColorScheme

@Composable
fun rememberPresetSwatches(
    preset: ThemePreset,
    accent: AccentColor,
): List<Color> {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(preset, accent, dark) {
        val scheme =
            when {
                preset == ThemePreset.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

                preset == ThemePreset.CUSTOM -> kozuColorScheme(accent.color, dark)

                else ->
                    kozuColorScheme(
                        seed = preset.seed,
                        dark = dark,
                        style = preset.style,
                        secondarySeed = preset.secondary,
                        tertiarySeed = preset.tertiary,
                    )
            }
        listOf(scheme.primary, scheme.primaryContainer, scheme.tertiary, scheme.surface)
    }
}


@Composable
fun SwatchDots(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 26.dp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-size.value * 0.3f).dp)) {
        colors.forEach { color ->
            Surface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                color = color,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainer),
            ) {}
        }
    }
}


@Composable
fun ThemePresetRow(
    preset: ThemePreset,
    accent: AccentColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val unavailable = preset == ThemePreset.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    Surface(
        modifier = modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f, enabled = !unavailable, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.label, style = MaterialTheme.typography.titleMedium)
                Text(text = if (unavailable) "Requires Android 12 or newer" else preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            SwatchDots(rememberPresetSwatches(preset, accent))
            Box(Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePresetSheet(
    selected: ThemePreset,
    accent: AccentColor,
    artworkColors: Boolean,
    onSelect: (ThemePreset) -> Unit,
    onArtworkColors: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.theme_preset),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ThemePreset.entries.forEach { preset ->
                ThemePresetRow(
                    preset = preset,
                    accent = accent,
                    selected = preset == selected,
                    onClick = { onSelect(preset) },
                )
            }
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .bounceClick(pressedScale = 0.99f) { onArtworkColors(!artworkColors) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.adapt_to_artwork), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.artwork_theme_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = artworkColors, onCheckedChange = onArtworkColors)
                }
            }
        }
    }
}
