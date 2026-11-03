package vn.kozu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import vn.kozu.R
import vn.kozu.domain.model.AppFont
import vn.kozu.domain.model.AppearancePrefs

/** A resolved font family together with the weight range it can actually render. */
private class ResolvedFont(
    val family: FontFamily,
    val minWeight: Int,
    val maxWeight: Int,
    /** Thin fonts such as Quicksand need a heavier body weight to stay readable. */
    val bodyBias: Int,
)

private const val WEIGHT_STEP = 50

@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resId: Int,
    minWeight: Int,
    maxWeight: Int,
    bodyBias: Int,
    settingsFor: (Int) -> FontVariation.Settings,
): ResolvedFont {
    val first = ((minWeight + WEIGHT_STEP - 1) / WEIGHT_STEP) * WEIGHT_STEP
    val last = (maxWeight / WEIGHT_STEP) * WEIGHT_STEP
    val fonts =
        (first..last step WEIGHT_STEP).map { weight ->
            Font(
                resId = resId,
                weight = FontWeight(weight),
                variationSettings = settingsFor(weight),
            )
        }
    return ResolvedFont(FontFamily(fonts), first, last, bodyBias)
}

@OptIn(ExperimentalTextApi::class)
private fun resolveFont(
    font: AppFont,
    prefs: AppearancePrefs,
): ResolvedFont =
    when (font) {
        AppFont.GOOGLE_SANS_FLEX ->
            variableFont(R.font.google_sans_flex, 100, 1000, 0) { weight ->
                FontVariation.Settings(
                    FontVariation.weight(weight),
                    FontVariation.width(prefs.fontWidth.toFloat()),
                    FontVariation.Setting("ROND", prefs.fontRoundness.toFloat()),
                )
            }

        AppFont.GOOGLE_SANS ->
            variableFont(R.font.google_sans, 400, 700, 0) { weight ->
                FontVariation.Settings(FontVariation.weight(weight))
            }

        AppFont.KOZU ->
            variableFont(R.font.quicksand, 300, 700, 100) { weight ->
                FontVariation.Settings(FontVariation.weight(weight))
            }

        AppFont.ROBOTO -> ResolvedFont(FontFamily.SansSerif, 100, 900, 0)
        AppFont.SYSTEM -> ResolvedFont(FontFamily.Default, 100, 900, 0)
    }

private fun style(
    resolved: ResolvedFont,
    prefs: AppearancePrefs,
    baseWeight: Int,
    size: Float,
    line: Float,
    spacing: Float = 0f,
    isBody: Boolean = false,
): TextStyle {
    val wanted = baseWeight + prefs.fontWeightShift + if (isBody) resolved.bodyBias else 0
    val snapped = (wanted.toFloat() / WEIGHT_STEP).let { Math.round(it) * WEIGHT_STEP }
    val weight = snapped.coerceIn(resolved.minWeight, resolved.maxWeight)
    return TextStyle(
        fontFamily = resolved.family,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}

/**
 * Builds the Material typography for the chosen [font]. Sizes follow PixelMusic's slightly larger
 * scale, which reads better than the stock Material defaults. The user's text-size preference is
 * applied separately through the composition density so hard-coded `sp` values follow it too.
 */
fun typographyFor(
    font: AppFont,
    prefs: AppearancePrefs = AppearancePrefs(),
): Typography {
    val f = resolveFont(font, prefs)
    return Typography(
        displayLarge = style(f, prefs, 700, 52f, 60f, -0.25f),
        displayMedium = style(f, prefs, 700, 42f, 50f),
        displaySmall = style(f, prefs, 700, 34f, 42f),
        headlineLarge = style(f, prefs, 650, 32f, 40f),
        headlineMedium = style(f, prefs, 650, 28f, 36f),
        headlineSmall = style(f, prefs, 600, 24f, 32f),
        titleLarge = style(f, prefs, 600, 22f, 28f),
        titleMedium = style(f, prefs, 600, 17f, 24f, 0.1f),
        titleSmall = style(f, prefs, 600, 15f, 20f, 0.1f),
        bodyLarge = style(f, prefs, 400, 17f, 24f, 0.3f, isBody = true),
        bodyMedium = style(f, prefs, 400, 15f, 21f, 0.2f, isBody = true),
        bodySmall = style(f, prefs, 400, 13f, 18f, 0.3f, isBody = true),
        labelLarge = style(f, prefs, 600, 15f, 20f, 0.1f),
        labelMedium = style(f, prefs, 600, 13f, 18f, 0.3f),
        labelSmall = style(f, prefs, 600, 12f, 16f, 0.4f),
    )
}
