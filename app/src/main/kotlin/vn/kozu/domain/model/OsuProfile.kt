package vn.kozu.domain.model

data class OsuProfile(
    val id: Long,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String,
    val countryCode: String,
    val countryName: String,
    val isSupporter: Boolean,
    val joinDate: String,
    val followers: Int,
    val mode: String,
    val globalRank: Int,
    val countryRank: Int,
    val pp: Double,
    val accuracy: Double,
    val playCount: Int,
    val playTimeSeconds: Long,
    val level: Double,
    val rankedScore: Long,
    val gradeSS: Int,
    val gradeS: Int,
    val gradeA: Int
) {
    val flag: String
        get() {
            val code = countryCode.uppercase()
            if (code.length != 2 || code.any { it !in 'A'..'Z' }) return ""
            return code.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
        }
}
