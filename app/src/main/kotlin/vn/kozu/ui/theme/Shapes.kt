package vn.kozu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val KozuShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

/**
 * Hình "bánh quy" (cookie) có [sides] múi – dấu ấn nhận diện của Kozu, gợi nhớ hit-circle / slider của osu!.
 * @param depth độ lõm giữa các múi (0 = tròn).
 */
class CookieShape(
    private val sides: Int,
    private val depth: Float = 0.06f,
    private val rotationDegrees: Float = 0f
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val steps = 240
        val rotation = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            // r nằm trong [1 - 2*depth, 1]; đỉnh múi chạm mép khung.
            val r = 1f - depth + depth * cos(sides * t)
            val x = cx + cx * r * cos(t + rotation)
            val y = cy + cy * r * sin(t + rotation)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Bộ hình dùng xoay vòng để các thẻ ảnh bìa trông đa dạng. */
object KozuCookies {
    val Soft9 = CookieShape(sides = 9, depth = 0.045f)
    val Bold6 = CookieShape(sides = 6, depth = 0.06f, rotationDegrees = 30f)
    val Flower8 = CookieShape(sides = 8, depth = 0.08f)
    val Clover4 = CookieShape(sides = 4, depth = 0.10f, rotationDegrees = 45f)

    private val all = listOf(Soft9, Bold6, Flower8, Clover4)
    fun forIndex(index: Int): Shape = all[index.mod(all.size)]
}
