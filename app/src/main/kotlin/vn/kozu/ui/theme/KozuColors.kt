package vn.kozu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import vn.kozu.domain.model.PaletteStyle

val OsuPink = Color(0xFFFF66AA)

fun kozuColorScheme(
    seed: Color,
    dark: Boolean,
    amoled: Boolean = false,
    style: PaletteStyle = PaletteStyle.TONAL_SPOT
): ColorScheme {
    val lch = Oklch.fromColor(seed)
    val isGray = lch.c < 0.035 || style == PaletteStyle.MONOCHROME
    val hue = lch.h

    val baseChroma = if (isGray) 0.02 else (lch.c * 1.15).coerceIn(0.12, 0.24)
    val primaryChroma = when (style) {
        PaletteStyle.TONAL_SPOT -> baseChroma
        PaletteStyle.VIBRANT -> (baseChroma * 1.35).coerceIn(0.16, 0.3)
        PaletteStyle.EXPRESSIVE -> (baseChroma * 1.2).coerceIn(0.14, 0.28)
        PaletteStyle.NEUTRAL -> if (isGray) 0.02 else (baseChroma * 0.38).coerceIn(0.03, 0.08)
        PaletteStyle.MONOCHROME -> 0.0
    }
    val secondaryHue = when (style) {
        PaletteStyle.EXPRESSIVE -> (hue + 65.0) % 360.0
        PaletteStyle.VIBRANT -> (hue + 25.0) % 360.0
        else -> (hue + 12.0) % 360.0
    }
    val tertiaryHue = when (style) {
        PaletteStyle.EXPRESSIVE -> (hue + 135.0) % 360.0
        PaletteStyle.VIBRANT -> (hue + 60.0) % 360.0
        else -> (hue + 305.0) % 360.0
    }
    val secondaryFactor = when (style) {
        PaletteStyle.VIBRANT -> 0.6
        PaletteStyle.EXPRESSIVE -> 0.55
        PaletteStyle.NEUTRAL -> 0.5
        else -> 0.32
    }
    val tertiaryFactor = when (style) {
        PaletteStyle.VIBRANT -> 0.85
        PaletteStyle.EXPRESSIVE -> 0.8
        PaletteStyle.NEUTRAL -> 0.7
        else -> 0.62
    }
    val neutralChroma = when {
        isGray -> 0.004
        style == PaletteStyle.VIBRANT || style == PaletteStyle.EXPRESSIVE -> 0.02
        style == PaletteStyle.NEUTRAL -> 0.006
        else -> 0.012
    }
    val primary = TonalPalette(hue, primaryChroma)
    val secondary = TonalPalette(secondaryHue, if (isGray) 0.0 else primaryChroma * secondaryFactor)
    val tertiary = TonalPalette(tertiaryHue, if (isGray) 0.0 else primaryChroma * tertiaryFactor)
    val neutral = TonalPalette(hue, neutralChroma)
    val neutralVariant = TonalPalette(hue, neutralChroma * 1.8)
    val error = TonalPalette(29.0, 0.2)

    return if (dark) {
        val base = if (amoled) 0 else 6
        darkColorScheme(
            primary = primary.tone(80),
            onPrimary = primary.tone(20),
            primaryContainer = primary.tone(30),
            onPrimaryContainer = primary.tone(90),
            inversePrimary = primary.tone(40),
            secondary = secondary.tone(80),
            onSecondary = secondary.tone(20),
            secondaryContainer = secondary.tone(30),
            onSecondaryContainer = secondary.tone(90),
            tertiary = tertiary.tone(80),
            onTertiary = tertiary.tone(20),
            tertiaryContainer = tertiary.tone(30),
            onTertiaryContainer = tertiary.tone(90),
            background = neutral.tone(base),
            onBackground = neutral.tone(90),
            surface = neutral.tone(base),
            onSurface = neutral.tone(90),
            surfaceVariant = neutralVariant.tone(30),
            onSurfaceVariant = neutralVariant.tone(80),
            surfaceTint = primary.tone(80),
            inverseSurface = neutral.tone(90),
            inverseOnSurface = neutral.tone(20),
            error = error.tone(80),
            onError = error.tone(20),
            errorContainer = error.tone(30),
            onErrorContainer = error.tone(90),
            outline = neutralVariant.tone(60),
            outlineVariant = neutralVariant.tone(30),
            scrim = Color.Black,
            surfaceBright = neutral.tone(if (amoled) 24 else 28),
            surfaceDim = neutral.tone(base),
            surfaceContainerLowest = neutral.tone(if (amoled) 0 else 4),
            surfaceContainerLow = neutral.tone(if (amoled) 14 else 12),
            surfaceContainer = neutral.tone(if (amoled) 20 else 16),
            surfaceContainerHigh = neutral.tone(if (amoled) 26 else 22),
            surfaceContainerHighest = neutral.tone(if (amoled) 32 else 28)
        )
    } else {
        lightColorScheme(
            primary = primary.tone(40),
            onPrimary = primary.tone(100),
            primaryContainer = primary.tone(90),
            onPrimaryContainer = primary.tone(10),
            inversePrimary = primary.tone(80),
            secondary = secondary.tone(40),
            onSecondary = secondary.tone(100),
            secondaryContainer = secondary.tone(90),
            onSecondaryContainer = secondary.tone(10),
            tertiary = tertiary.tone(40),
            onTertiary = tertiary.tone(100),
            tertiaryContainer = tertiary.tone(90),
            onTertiaryContainer = tertiary.tone(10),
            background = neutral.tone(98),
            onBackground = neutral.tone(10),
            surface = neutral.tone(98),
            onSurface = neutral.tone(10),
            surfaceVariant = neutralVariant.tone(90),
            onSurfaceVariant = neutralVariant.tone(30),
            surfaceTint = primary.tone(40),
            inverseSurface = neutral.tone(20),
            inverseOnSurface = neutral.tone(95),
            error = error.tone(40),
            onError = error.tone(100),
            errorContainer = error.tone(90),
            onErrorContainer = error.tone(10),
            outline = neutralVariant.tone(50),
            outlineVariant = neutralVariant.tone(80),
            scrim = Color.Black,
            surfaceBright = neutral.tone(98),
            surfaceDim = neutral.tone(87),
            surfaceContainerLowest = neutral.tone(100),
            surfaceContainerLow = neutral.tone(96),
            surfaceContainer = neutral.tone(94),
            surfaceContainerHigh = neutral.tone(92),
            surfaceContainerHighest = neutral.tone(90)
        )
    }
}

internal fun lerpColorScheme(a: ColorScheme, b: ColorScheme, fraction: Float): ColorScheme = a.copy(
    primary = lerp(a.primary, b.primary, fraction),
    onPrimary = lerp(a.onPrimary, b.onPrimary, fraction),
    primaryContainer = lerp(a.primaryContainer, b.primaryContainer, fraction),
    onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, fraction),
    inversePrimary = lerp(a.inversePrimary, b.inversePrimary, fraction),
    secondary = lerp(a.secondary, b.secondary, fraction),
    onSecondary = lerp(a.onSecondary, b.onSecondary, fraction),
    secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, fraction),
    onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, fraction),
    tertiary = lerp(a.tertiary, b.tertiary, fraction),
    onTertiary = lerp(a.onTertiary, b.onTertiary, fraction),
    tertiaryContainer = lerp(a.tertiaryContainer, b.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(a.onTertiaryContainer, b.onTertiaryContainer, fraction),
    background = lerp(a.background, b.background, fraction),
    onBackground = lerp(a.onBackground, b.onBackground, fraction),
    surface = lerp(a.surface, b.surface, fraction),
    onSurface = lerp(a.onSurface, b.onSurface, fraction),
    surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, fraction),
    onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, fraction),
    surfaceTint = lerp(a.surfaceTint, b.surfaceTint, fraction),
    inverseSurface = lerp(a.inverseSurface, b.inverseSurface, fraction),
    inverseOnSurface = lerp(a.inverseOnSurface, b.inverseOnSurface, fraction),
    error = lerp(a.error, b.error, fraction),
    onError = lerp(a.onError, b.onError, fraction),
    errorContainer = lerp(a.errorContainer, b.errorContainer, fraction),
    onErrorContainer = lerp(a.onErrorContainer, b.onErrorContainer, fraction),
    outline = lerp(a.outline, b.outline, fraction),
    outlineVariant = lerp(a.outlineVariant, b.outlineVariant, fraction),
    scrim = lerp(a.scrim, b.scrim, fraction),
    surfaceBright = lerp(a.surfaceBright, b.surfaceBright, fraction),
    surfaceDim = lerp(a.surfaceDim, b.surfaceDim, fraction),
    surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, fraction),
    surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, fraction)
)
