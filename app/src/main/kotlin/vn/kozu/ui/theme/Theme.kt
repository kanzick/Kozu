package vn.kozu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The same branded Material 3 palettes used by every Kozu screen. */
val KozuDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF0D400),
    onPrimary = Color(0xFF282500),
    primaryContainer = Color(0xFF514900),
    onPrimaryContainer = Color(0xFFFFF2A6),
    secondary = Color(0xFFD8D4B5),
    onSecondary = Color(0xFF343322),
    secondaryContainer = Color(0xFF29291D),
    onSecondaryContainer = Color(0xFFEDE8CB),
    tertiary = Color(0xFFFFB5C9),
    onTertiary = Color(0xFF4C1027),
    tertiaryContainer = Color(0xFF6A2940),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF11110D),
    onBackground = Color(0xFFF4F0DF),
    surface = Color(0xFF191912),
    onSurface = Color(0xFFF4F0DF),
    surfaceVariant = Color(0xFF29291E),
    onSurfaceVariant = Color(0xFFC9C4A9),
    outline = Color(0xFF85816A)
)

val KozuLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF746000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE98A),
    onPrimaryContainer = Color(0xFF352B00),
    secondary = Color(0xFF746A42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3EBD2),
    onSecondaryContainer = Color(0xFF292416),
    tertiary = Color(0xFFB65D78),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E1),
    onTertiaryContainer = Color(0xFF3E071B),
    background = Color(0xFFFFF9EC),
    onBackground = Color(0xFF242214),
    surface = Color(0xFFFFFCF4),
    onSurface = Color(0xFF242214),
    surfaceVariant = Color(0xFFF1E9D5),
    onSurfaceVariant = Color(0xFF6A6652),
    outline = Color(0xFF85806B)
)
