package vn.kozu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import vn.kozu.R

val QuicksandFontFamily = FontFamily(
    Font(R.font.quicksand),
)

private val base = Typography()

val KozuTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = QuicksandFontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = QuicksandFontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = QuicksandFontFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = QuicksandFontFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = QuicksandFontFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = QuicksandFontFamily),
    titleLarge = base.titleLarge.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = QuicksandFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = QuicksandFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = QuicksandFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = QuicksandFontFamily, fontWeight = FontWeight.Medium),
)
