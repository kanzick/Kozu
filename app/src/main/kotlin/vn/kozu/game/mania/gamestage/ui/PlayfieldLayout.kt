package vn.kozu.game.mania.gamestage.ui

import androidx.compose.ui.graphics.Color
import vn.kozu.game.mania.gamestage.scoring.Judgement
import kotlin.math.min

/** Playfield layout shared by rendering and touch handling so both stay aligned. */
class PlayfieldLayout(width: Float, height: Float, val keys: Int) {
    /** Column width: fits the screen width without becoming too large in landscape. */
    val columnWidth: Float = min(width / keys, height * 0.22f)
    val left: Float = (width - columnWidth * keys) / 2f
    val right: Float = left + columnWidth * keys
    val judgeY: Float = height * 0.82f
    val noteHeight: Float = columnWidth * 0.30f

    /** Column for x (touches outside the playfield snap to the nearest column). */
    fun columnAt(x: Float): Int = ((x - left) / columnWidth).toInt().coerceIn(0, keys - 1)

    fun columnX(c: Int): Float = left + c * columnWidth
}

internal object PlayColors {
    val white = Color(0xFFF2F2F2)
    val blue = Color(0xFF63B3FF)
    val yellow = Color(0xFFFFD54F)
    val broken = Color(0xFF8A8A8A)

    fun noteColor(column: Int, keys: Int, primary: Color, tertiary: Color): Color {
        if (keys % 2 == 1 && column == keys / 2) return tertiary
        return if (minOf(column, keys - 1 - column) % 2 == 0) primary else tertiary.copy(alpha = 0.9f)
    }

    fun judgement(j: Judgement): Color = when (j) {
        Judgement.PERFECT -> Color(0xFF7FF0FF)
        Judgement.GREAT -> Color(0xFFFFE066)
        Judgement.GOOD -> Color(0xFF7CE08A)
        Judgement.OK -> Color(0xFF6FA8FF)
        Judgement.MEH -> Color(0xFFB0B0B0)
        Judgement.MISS -> Color(0xFFFF5A5A)
    }

    fun rank(rank: String): Color = when (rank) {
        "SS" -> Color(0xFFFFE066)
        "S" -> Color(0xFFFFC94D)
        "A" -> Color(0xFF7CE08A)
        "B" -> Color(0xFF6FA8FF)
        "C" -> Color(0xFFC792EA)
        else -> Color(0xFFFF7A7A)
    }
}
