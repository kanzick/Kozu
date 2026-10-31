package vn.kozu.game.mania.gamestage.scoring

/**
 * Các mức phán định của osu!mania (ScoreV1, stable). Số liệu lấy từ osu! wiki:
 *  - "osu!mania scoring system":  HitValue / HitBonusValue / HitBonus / HitPunishment
 *  - "osu!mania judgement system": tỉ lệ accuracy của từng mức
 *
 * | Mức     | HitValue | BonusValue | HitBonus | HitPunishment | Accuracy |
 * | PERFECT | 320      | 32         | 2        | -             | 100%     |
 * | GREAT   | 300      | 32         | 1        | -             | 100%     |
 * | GOOD    | 200      | 16         | -        | 8             | 66.67%   |
 * | OK      | 100      | 8          | -        | 24            | 33.33%   |
 * | MEH     | 50       | 4          | -        | 44            | 16.67%   |
 * | MISS    | 0        | 0          | -        | vô hạn        | 0%       |
 */
enum class Judgement(
    val label: String,
    val hitValue: Int,
    val bonusValue: Int,
    val hitBonus: Double,
    val hitPunishment: Double,
    /** Đơn vị = 1/6 của 300 để cộng số nguyên (không sai số): 6 = 100%, 4 = 66.67%, ... */
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
 * Cửa sổ phán định (ms, tính một phía: |sai lệch| <= giá trị) theo OD, bản stable của osu!mania:
 *   PERFECT 16 · GREAT 64-3·OD · GOOD 97-3·OD · OK 127-3·OD · MEH 151-3·OD · MISS 188-3·OD
 *
 * Lưu ý (theo wiki): ở bản stable cửa sổ thật có thể dài hơn tới 0.5ms mỗi phía vì osu! làm tròn;
 * ở đây dùng đúng công thức không làm tròn.
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
     * Phán định cho một cú nhấn nốt thường với [absError] = |thời điểm nhấn - thời điểm nốt|.
     * [late] = true nếu nhấn muộn: muộn quá cửa sổ OK thì không còn MEH (thành miss, xử lý ở engine).
     * Trả [Judgement.MISS] cho cú nhấn quá sớm nằm trong cửa sổ miss (nốt bị tiêu thụ, mất combo).
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
