package vn.kozu.game.mania.gamestage.scoring

import kotlin.math.abs
import kotlin.math.roundToInt

enum class Judgement(
    val label: String,
    val hitValue: Int,
    val bonusValue: Int,
    val hitBonus: Double,
    val hitPunishment: Double,

    val accuracySixths: Int,
) {
    PERFECT("MAX", 320, 32, 2.0, 0.0, 6),
    GREAT("300", 300, 32, 1.0, 0.0, 6),
    GOOD("200", 200, 16, 0.0, 8.0, 4),
    OK("100", 100, 8, 0.0, 24.0, 2),
    MEH("50", 50, 4, 0.0, 44.0, 1),
    MISS("MISS", 0, 0, 0.0, Double.POSITIVE_INFINITY, 0);

    val breaksCombo: Boolean get() = this == MISS
}

class HitWindows(overallDifficulty: Double) {
    private val od = overallDifficulty.coerceIn(0.0, 10.0)

    val perfect: Double = 16.0
    val great: Double = (64.0 - 3.0 * od).toInt().toDouble()
    val good: Double = (97.0 - 3.0 * od).toInt().toDouble()
    val ok: Double = (127.0 - 3.0 * od).toInt().toDouble()
    val meh: Double = (151.0 - 3.0 * od).toInt().toDouble()
    val miss: Double = (188.0 - 3.0 * od).toInt().toDouble()

    fun roundedErrorMs(errorMs: Double): Int {
        val roundedMagnitude = abs(errorMs).roundToInt()
        return if (errorMs < 0.0) -roundedMagnitude else roundedMagnitude
    }

    fun classify(absError: Double, late: Boolean): Judgement {
        val roundedError = abs(absError).roundToInt().toDouble()
        return when {
            roundedError <= perfect -> Judgement.PERFECT
            roundedError <= great -> Judgement.GREAT
            roundedError <= good -> Judgement.GOOD
            roundedError <= ok -> Judgement.OK
            !late && roundedError <= meh -> Judgement.MEH
            else -> Judgement.MISS
        }
    }
}
