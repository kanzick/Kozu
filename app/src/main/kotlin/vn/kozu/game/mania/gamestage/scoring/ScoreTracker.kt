package vn.kozu.game.mania.gamestage.scoring

import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Final result of a play session. */
data class GameResult(
    val score: Long,
    /** 0.0 .. 1.0 */
    val accuracy: Double,
    val rank: String,
    val maxCombo: Int,
    val totalNotes: Int,
    val perfect: Int,
    val great: Int,
    val good: Int,
    val ok: Int,
    val meh: Int,
    val miss: Int,
) {
    val accuracyPercent: Double get() = accuracy * 100.0
}

/**
 * Calculates the score using osu!mania ScoreV1 (maximum score 1,000,000), following the wiki formula:
 *
 *   Score     = BaseScore + BonusScore
 *   BaseScore = (MaxScore * 0.5 / TotalNotes) * (HitValue / 320)
 *   BonusScore= (MaxScore * 0.5 / TotalNotes) * (HitBonusValue * sqrt(Bonus) / 320)
 *   Bonus     = clamp(BonusBefore + HitBonus - HitPunishment, 0, 100), initially 100; MISS sets Bonus to 0
 *
 * Each note (including holds) has only ONE judgement, so TotalNotes = number of hit objects.
 * No mods are used, so ModMultiplier = 1 and ModDivider = 1.
 *
 * Accuracy = (300·(MAX+300) + 200·c200 + 100·c100 + 50·c50) / (300·number of judged notes).
 * Rank (stable): SS = 100%, S > 95%, A > 90%, B > 80%, C > 70%, otherwise D.
 */
class ScoreTracker(val totalNotes: Int) {
    private val counts = IntArray(Judgement.entries.size)
    private var bonus = 100.0
    private var exactScore = 0.0

    var combo: Int = 0
        private set
    var maxCombo: Int = 0
        private set
    var judged: Int = 0
        private set

    init {
        require(totalNotes > 0) { "totalNotes must be > 0" }
    }

    fun count(j: Judgement): Int = counts[j.ordinal]

    fun apply(j: Judgement) {
        counts[j.ordinal]++
        judged++

        bonus = if (j == Judgement.MISS) 0.0
        else (bonus + j.hitBonus - j.hitPunishment).coerceIn(0.0, 100.0)

        if (j.breaksCombo) {
            combo = 0
        } else {
            combo++
            maxCombo = max(maxCombo, combo)
        }

        val unit = MAX_SCORE * 0.5 / totalNotes
        exactScore += unit * (j.hitValue / 320.0) + unit * (j.bonusValue * sqrt(bonus) / 320.0)
    }

    /** Current score (rounded). */
    val score: Long get() = exactScore.roundToLong()

    /** Current accuracy across judged notes (1.0 when no notes have been judged). */
    val accuracy: Double
        get() = if (judged == 0) 1.0 else weightedSixths().toDouble() / (6.0 * judged)

    val rank: String get() = rankFor(weightedSixths(), judged)

    fun result(): GameResult = GameResult(
        score = score,
        accuracy = accuracy,
        rank = rank,
        maxCombo = maxCombo,
        totalNotes = totalNotes,
        perfect = count(Judgement.PERFECT),
        great = count(Judgement.GREAT),
        good = count(Judgement.GOOD),
        ok = count(Judgement.OK),
        meh = count(Judgement.MEH),
        miss = count(Judgement.MISS),
    )

    private fun weightedSixths(): Long {
        var sum = 0L
        for (j in Judgement.entries) sum += counts[j.ordinal].toLong() * j.accuracySixths
        return sum
    }

    companion object {
        const val MAX_SCORE = 1_000_000.0

        /** Integer comparisons avoid floating-point errors at the 70/80/90/95/100% thresholds. */
        fun rankFor(weightedSixths: Long, judged: Int): String {
            if (judged <= 0) return "D"
            val full = 6L * judged // = 100%
            val v = weightedSixths * 100
            return when {
                weightedSixths == full -> "SS"
                v > 95 * full -> "S"
                v > 90 * full -> "A"
                v > 80 * full -> "B"
                v > 70 * full -> "C"
                else -> "D"
            }
        }
    }
}
