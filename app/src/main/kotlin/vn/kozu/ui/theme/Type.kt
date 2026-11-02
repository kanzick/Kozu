package vn.kozu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import vn.kozu.R
import vn.kozu.domain.model.AppFont

@OptIn(ExperimentalTextApi::class)
private fun quicksand(weight: FontWeight) =
    Font(
        resId = R.font.quicksand,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

private fun googleSans(weight: FontWeight) =
    Font(
        resId = R.font.google_sans,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

private fun googleSansFlex(weight: FontWeight) =
    Font(
        resId = R.font.google_sans_flex,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val QuicksandFontFamily =
    FontFamily(
        quicksand(FontWeight.Light),
        quicksand(FontWeight.Normal),
        quicksand(FontWeight.Medium),
        quicksand(FontWeight.SemiBold),
        quicksand(FontWeight.Bold),
    )

private val base = Typography()

private fun TextStyle.kozu(
    scale: Float,
    weight: FontWeight,
    family: FontFamily = QuicksandFontFamily,
): TextStyle =
    copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontFamily = family,
        fontWeight = weight,
    )

private fun buildTypography(family: FontFamily) =
    Typography(
        displayLarge = base.displayLarge.kozu(1.0f, FontWeight.Bold, family),
        displayMedium = base.displayMedium.kozu(1.0f, FontWeight.Bold, family),
        displaySmall = base.displaySmall.kozu(1.0f, FontWeight.Bold, family),
        headlineLarge = base.headlineLarge.kozu(1.0f, FontWeight.Bold, family),
        headlineMedium = base.headlineMedium.kozu(1.0f, FontWeight.Bold, family),
        headlineSmall = base.headlineSmall.kozu(1.0f, FontWeight.SemiBold, family),
        titleLarge = base.titleLarge.kozu(1.0f, FontWeight.SemiBold, family),
        titleMedium = base.titleMedium.kozu(1.0f, FontWeight.SemiBold, family),
        titleSmall = base.titleSmall.kozu(1.0f, FontWeight.SemiBold, family),
        bodyLarge = base.bodyLarge.kozu(1.0f, FontWeight.Medium, family),
        bodyMedium = base.bodyMedium.kozu(1.0f, FontWeight.Medium, family),
        bodySmall = base.bodySmall.kozu(1.0f, FontWeight.Medium, family),
        labelLarge = base.labelLarge.kozu(1.0f, FontWeight.SemiBold, family),
        labelMedium = base.labelMedium.kozu(1.0f, FontWeight.SemiBold, family),
        labelSmall = base.labelSmall.kozu(1.0f, FontWeight.SemiBold, family),
    )

val KozuTypography = buildTypography(QuicksandFontFamily)

val SystemKozuTypography = buildTypography(FontFamily.Default)

val RobotoKozuTypography = buildTypography(FontFamily.SansSerif)

val GoogleSansTypography =
    buildTypography(
        FontFamily(
            googleSans(FontWeight.Light),
            googleSans(FontWeight.Normal),
            googleSans(FontWeight.Medium),
            googleSans(FontWeight.SemiBold),
            googleSans(FontWeight.Bold),
        ),
    )

val GoogleSansFlexTypography =
    buildTypography(
        FontFamily(
            googleSansFlex(FontWeight.Light),
            googleSansFlex(FontWeight.Normal),
            googleSansFlex(FontWeight.Medium),
            googleSansFlex(FontWeight.SemiBold),
            googleSansFlex(FontWeight.Bold),
        ),
    )

fun typographyFor(font: AppFont) =
    when (font) {
        AppFont.KOZU -> KozuTypography
        AppFont.SYSTEM -> SystemKozuTypography
        AppFont.ROBOTO -> RobotoKozuTypography
        AppFont.GOOGLE_SANS -> GoogleSansTypography
        AppFont.GOOGLE_SANS_FLEX -> GoogleSansFlexTypography
    }
