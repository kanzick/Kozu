package vn.kozu.game.mania.gamestage.scoring

import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class GameResult(
    val score: Long,

    val accuracy: Double,
    val rank: String,

    val trueCombo: Int,

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

    val score: Long get() = exactScore.roundToLong()

    val accuracy: Double
        get() = if (judged == 0) 1.0 else weightedSixths().toDouble() / (6.0 * judged)

    val rank: String get() = rankFor(weightedSixths(), judged)

    fun result(): GameResult = GameResult(
        score = score,
        accuracy = accuracy,
        rank = rank,
        trueCombo = maxCombo,
        maxCombo = totalNotes,
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

        fun rankFor(weightedSixths: Long, judged: Int): String {
            if (judged <= 0) return "D"
            val full = 6L * judged
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
