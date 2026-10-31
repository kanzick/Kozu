package vn.kozu.game.mania.gamestage.scoring

/**
 * osu!mania judgement levels (ScoreV1, stable). Values are from the osu! wiki:
 *  - "osu!mania scoring system":  HitValue / HitBonusValue / HitBonus / HitPunishment
 *  - "osu!mania judgement system": accuracy percentage for each level
 *
 * | Level   | HitValue | BonusValue | HitBonus | HitPunishment | Accuracy |
 * | PERFECT | 320      | 32         | 2        | -             | 100%     |
 * | GREAT   | 300      | 32         | 1        | -             | 100%     |
 * | GOOD    | 200      | 16         | -        | 8             | 66.67%   |
 * | OK      | 100      | 8          | -        | 24            | 33.33%   |
 * | MEH     | 50       | 4          | -        | 44            | 16.67%   |
 * | MISS    | 0        | 0          | -        | infinity      | 0%       |
 */
enum class Judgement(
    val label: String,
    val hitValue: Int,
    val bonusValue: Int,
    val hitBonus: Double,
    val hitPunishment: Double,
    /** Unit = 1/6 of 300 for integer-based accumulation (no floating-point error): 6 = 100%, 4 = 66.67%, ... */
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

/**
 * Judgement windows (ms, one-sided: |error| <= value) by OD, in stable osu!mania:
 *   PERFECT 16 · GREAT 64-3·OD · GOOD 97-3·OD · OK 127-3·OD · MEH 151-3·OD · MISS 188-3·OD
 *
 * Note (per the wiki): in stable, actual windows can be up to 0.5ms longer on each side because osu! rounds;
 * this uses the exact unrounded formula.
 */
class HitWindows(overallDifficulty: Double) {
    private val od = overallDifficulty.coerceIn(0.0, 10.0)

    val perfect: Double = 16.0
    val great: Double = 64.0 - 3.0 * od
    val good: Double = 97.0 - 3.0 * od
    val ok: Double = 127.0 - 3.0 * od
    val meh: Double = 151.0 - 3.0 * od
    val miss: Double = 188.0 - 3.0 * od

    /**
     * Judges a regular note press with [absError] = |press time - note time|.
     * [late] = true for a late press: after the OK window, it cannot receive MEH (the engine handles it as a miss).
     * Returns [Judgement.MISS] for a too-early press within the miss window (the note is consumed and the combo breaks).
     */
    fun classify(absError: Double, late: Boolean): Judgement = when {
        absError <= perfect -> Judgement.PERFECT
        absError <= great -> Judgement.GREAT
        absError <= good -> Judgement.GOOD
        absError <= ok -> Judgement.OK
        !late && absError <= meh -> Judgement.MEH
        else -> Judgement.MISS
    }
}
