package vn.kozu.player

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerProfileTest {
    @Test
    fun interpolatesBetweenBandsOnLogarithmicFrequencyScale() {
        val frequency = kotlin.math.sqrt(60f * 230f)
        val gains = listOf(-1f, 1f, 0f, 0f, 0f)

        assertEquals(0f, EqualizerProfile.gainAt(frequency, gains), 0.001f)
    }

    @Test
    fun clampsFrequenciesAndGainValuesToProfileRange() {
        val gains = listOf(-2f, 2f, 0f, 0f, 0f)

        assertEquals(-1f, EqualizerProfile.gainAt(20f, gains), 0f)
        assertEquals(0f, EqualizerProfile.gainAt(30_000f, gains), 0f)
    }

    @Test
    fun convertsNormalizedGainsToMillibels() {
        assertEquals(-1_200, EqualizerProfile.gainToMilliBel(-1f))
        assertEquals(600, EqualizerProfile.gainToMilliBel(0.5f))
        assertEquals(1_200, EqualizerProfile.gainToMilliBel(2f))
    }
}
