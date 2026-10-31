package vn.kozu.game.mania.gamestage.scoring

import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Kết quả cuối cùng của một lượt chơi. */
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
 * Tính điểm theo osu!mania ScoreV1 (điểm tối đa 1.000.000), đúng công thức trên wiki:
 *
 *   Score     = BaseScore + BonusScore
 *   BaseScore = (MaxScore * 0.5 / TotalNotes) * (HitValue / 320)
 *   BonusScore= (MaxScore * 0.5 / TotalNotes) * (HitBonusValue * sqrt(Bonus) / 320)
 *   Bonus     = clamp(BonusTrước + HitBonus - HitPunishment, 0, 100), ban đầu 100; MISS đưa Bonus về 0
 *
 * Mỗi nốt (kể cả hold) chỉ có MỘT phán định nên TotalNotes = số hit object.
 * Không dùng mod nên ModMultiplier = 1 và ModDivider = 1.
 *
 * Accuracy = (300·(MAX+300) + 200·c200 + 100·c100 + 50·c50) / (300·số nốt đã phán định).
 * Rank (bản stable): SS = 100%, S > 95%, A > 90%, B > 80%, C > 70%, còn lại D.
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
        require(totalNotes > 0) { "totalNotes phải > 0" }
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

    /** Điểm hiện tại (làm tròn). */
    val score: Long get() = exactScore.roundToLong()

    /** Accuracy hiện tại trên các nốt đã phán định (1.0 khi chưa có nốt nào). */
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

        /** So sánh bằng số nguyên nên không dính sai số số thực ở các mốc 70/80/90/95/100%. */
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
