package vn.kozu.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * OKLCH color space transformations for perceptually uniform color generation.
 */
internal object Oklch {

    data class Lch(val l: Double, val c: Double, val h: Double)

    /** Converts a Compose [Color] into OKLCH representation */
    fun fromColor(color: Color): Lch {
        val r = srgbToLinear(color.red.toDouble())
        val g = srgbToLinear(color.green.toDouble())
        val b = srgbToLinear(color.blue.toDouble())

        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

        val lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s

        var hue = Math.toDegrees(atan2(bb, a))
        if (hue < 0) hue += 360.0
        return Lch(lightness, hypot(a, bb), hue)
    }

    /** Converts OKLCH parameters to a gamut-clipped sRGB Compose [Color] */
    fun toColor(lightness: Double, chroma: Double, hue: Double): Color {
        var c = chroma
        if (!inGamut(lightness, c, hue)) {
            var lo = 0.0
            var hi = c
            repeat(24) {
                val mid = (lo + hi) / 2.0
                if (inGamut(lightness, mid, hue)) lo = mid else hi = mid
            }
            c = lo
        }
        val rgb = linearRgb(lightness, c, hue)
        return Color(
            red = linearToSrgb(rgb[0]).toFloat(),
            green = linearToSrgb(rgb[1]).toFloat(),
            blue = linearToSrgb(rgb[2]).toFloat()
        )
    }

    private fun inGamut(lightness: Double, chroma: Double, hue: Double): Boolean {
        val rgb = linearRgb(lightness, chroma, hue)
        return rgb.all { it >= -EPSILON && it <= 1.0 + EPSILON }
    }

    private fun linearRgb(lightness: Double, chroma: Double, hue: Double): DoubleArray {
        val radians = Math.toRadians(hue)
        val a = chroma * cos(radians)
        val b = chroma * sin(radians)

        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)

        return doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        )
    }

    private fun srgbToLinear(x: Double): Double =
        if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)

    private fun linearToSrgb(x: Double): Double {
        val v = x.coerceIn(0.0, 1.0)
        return if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
    }

    private const val EPSILON = 1e-4
}

/**
 * Tonal palette generator producing colors for given lightness tones (0..100).
 */
internal class TonalPalette(private val hue: Double, private val chroma: Double) {

    private val cache = HashMap<Int, Color>()

    fun tone(tone: Int): Color = cache.getOrPut(tone) {
        when {
            tone <= 0 -> Color.Black
            tone >= 100 -> Color.White
            else -> Oklch.toColor(toneToLightness(tone.toDouble()), chroma, hue)
        }
    }

    private fun toneToLightness(tone: Double): Double {
        val y = if (tone > 8.0) {
            val f = (tone + 16.0) / 116.0
            f * f * f
        } else {
            tone / 903.2962962962963
        }
        return Math.cbrt(y)
    }
}
