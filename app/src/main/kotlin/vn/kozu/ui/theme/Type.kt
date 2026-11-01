package vn.kozu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import vn.kozu.R

val QuicksandFontFamily = FontFamily(
    Font(R.font.quicksand),
)

private val base = Typography()

private fun TextStyle.readable(scale: Float = 1.08f): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale
)

val KozuTypography = Typography(
    displayLarge = base.displayLarge.readable(1.02f).copy(fontFamily = QuicksandFontFamily),
    displayMedium = base.displayMedium.readable(1.03f).copy(fontFamily = QuicksandFontFamily),
    displaySmall = base.displaySmall.readable(1.04f).copy(fontFamily = QuicksandFontFamily),
    headlineLarge = base.headlineLarge.readable(1.05f).copy(fontFamily = QuicksandFontFamily),
    headlineMedium = base.headlineMedium.readable(1.06f).copy(fontFamily = QuicksandFontFamily),
    headlineSmall = base.headlineSmall.readable(1.07f).copy(fontFamily = QuicksandFontFamily),
    titleLarge = base.titleLarge.readable(1.08f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.readable(1.08f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.readable(1.08f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.readable(1.08f).copy(fontFamily = QuicksandFontFamily),
    bodyMedium = base.bodyMedium.readable(1.1f).copy(fontFamily = QuicksandFontFamily),
    bodySmall = base.bodySmall.readable(1.14f).copy(fontFamily = QuicksandFontFamily),
    labelLarge = base.labelLarge.readable(1.08f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.readable(1.1f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.readable(1.12f).copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
)
