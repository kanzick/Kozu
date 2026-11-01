package vn.kozu.player

import kotlin.math.ln
import kotlin.math.roundToInt

object EqualizerProfile {
    const val MAX_GAIN_DB = 12f

    val centerFrequenciesHz = listOf(60f, 230f, 910f, 3_600f, 14_000f)
    val flat = List(centerFrequenciesHz.size) { 0f }
    val presets = linkedMapOf(
        "Flat" to flat,
        "Bass" to listOf(0.75f, 0.55f, 0f, -0.15f, 0.1f),
        "Vocal" to listOf(-0.2f, 0.1f, 0.55f, 0.35f, 0f),
        "Treble" to listOf(-0.15f, -0.1f, 0.1f, 0.45f, 0.7f)
    )

    fun gainAt(frequencyHz: Float, gains: List<Float>): Float {
        require(gains.size == centerFrequenciesHz.size)
        require(frequencyHz > 0f)

        val clamped = gains.map { it.coerceIn(-1f, 1f) }
        if (frequencyHz <= centerFrequenciesHz.first()) return clamped.first()
        if (frequencyHz >= centerFrequenciesHz.last()) return clamped.last()

        val upper = centerFrequenciesHz.indexOfFirst { it >= frequencyHz }
        val lower = upper - 1
        val logFrequency = ln(frequencyHz)
        val lowerFrequency = ln(centerFrequenciesHz[lower])
        val upperFrequency = ln(centerFrequenciesHz[upper])
        val fraction = (logFrequency - lowerFrequency) / (upperFrequency - lowerFrequency)
        return clamped[lower] + (clamped[upper] - clamped[lower]) * fraction
    }

    fun gainToMilliBel(gain: Float): Int =
        (gain.coerceIn(-1f, 1f) * MAX_GAIN_DB * 100f).roundToInt()
}
