package vn.kozu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val KozuLightColorScheme: ColorScheme = lightColorScheme(
    primary = OsuPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6E8),
    onPrimaryContainer = OsuOnPinkLight,
    secondary = Color(0xFF8E4C6D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9EA),
    onSecondaryContainer = Color(0xFF3A0F27),
    tertiary = Color(0xFFA64D79),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0EF),
    onTertiaryContainer = Color(0xFF3E0925),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = OsuBackground,
    onBackground = Color(0xFF201A1D),
    surface = Color(0xFFFFF8FA),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFF5E1EB),
    onSurfaceVariant = Color(0xFF514149),
    surfaceTint = OsuPink,
    outline = OsuOutline,
    outlineVariant = Color(0xFFD8C2CC),
    inverseSurface = Color(0xFF362E32),
    inverseOnSurface = Color(0xFFFBEDF3),
    inversePrimary = OsuPinkLight,
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFF8FA),
    surfaceDim = OsuSurfaceDim,
    surfaceContainer = Color(0xFFF6E8EF),
    surfaceContainerHigh = Color(0xFFF0E2EA),
    surfaceContainerHighest = Color(0xFFEADCE4),
    surfaceContainerLow = Color(0xFFFCEEF5),
    surfaceContainerLowest = Color.White
)

val KozuDarkColorScheme: ColorScheme = darkColorScheme(
    primary = OsuPinkLight,
    onPrimary = Color(0xFF510D36),
    primaryContainer = Color(0xFF72304F),
    onPrimaryContainer = Color(0xFFFFD6E8),
    secondary = Color(0xFFF0B8D5),
    onSecondary = Color(0xFF4F203A),
    secondaryContainer = Color(0xFF3A1A2E),
    onSecondaryContainer = Color(0xFFFFD9EA),
    tertiary = Color(0xFFFFB1D0),
    onTertiary = Color(0xFF5C1039),
    tertiaryContainer = Color(0xFF7A2952),
    onTertiaryContainer = Color(0xFFFFD9E6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = OsuBackgroundDark,
    onBackground = Color(0xFFEDE0E5),
    surface = Color(0xFF1A1015),
    onSurface = Color(0xFFEDE0E5),
    surfaceVariant = Color(0xFF352830),
    onSurfaceVariant = Color(0xFFD8C2CC),
    surfaceTint = OsuPinkLight,
    outline = OsuOutlineDark,
    outlineVariant = Color(0xFF514149),
    inverseSurface = Color(0xFFEDE0E5),
    inverseOnSurface = Color(0xFF362E32),
    inversePrimary = OsuPinkDark,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3F353B),
    surfaceDim = OsuSurfaceDark,
    surfaceContainer = Color(0xFF231A1F),
    surfaceContainerHigh = Color(0xFF2E242A),
    surfaceContainerHighest = Color(0xFF3A2F35),
    surfaceContainerLow = Color(0xFF1E1419),
    surfaceContainerLowest = Color(0xFF0F080C)
)

fun kozuColorScheme(
    isDarkTheme: Boolean,
    playingArtworkColor: Color?
): ColorScheme {
    val base = if (isDarkTheme) KozuDarkColorScheme else KozuLightColorScheme
    val accent = playingArtworkColor ?: return base

    val primaryTarget = lerp(accent, base.primary, 0.45f).let { raw ->
        lerp(raw, Color.White, if (isDarkTheme) 0.22f else 0.05f)
    }
    val primaryContainerTarget = lerp(base.primaryContainer, accent, 0.38f)
    val secondaryContainerTarget = lerp(base.secondaryContainer, accent, 0.14f)
    val tertiaryContainerTarget = lerp(base.tertiaryContainer, accent, 0.18f)
    val backgroundTarget = lerp(base.background, accent, 0.04f)
    val surfaceTarget = lerp(base.surface, accent, 0.07f)
    val surfaceVariantTarget = lerp(base.surfaceVariant, accent, 0.12f)
    val surfaceContainerTarget = lerp(base.surfaceContainer, accent, 0.06f)

    return base.copy(
        primary = primaryTarget,
        primaryContainer = primaryContainerTarget,
        secondaryContainer = secondaryContainerTarget,
        tertiaryContainer = tertiaryContainerTarget,
        background = backgroundTarget,
        surface = surfaceTarget,
        surfaceVariant = surfaceVariantTarget,
        surfaceContainer = surfaceContainerTarget
    )
}
