package vn.kozu.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Các spring dùng chung – hơi nảy nhẹ như Material 3 Expressive. */
object KozuMotion {
    /** Chuyển động không gian (kích thước, vị trí, hình dạng) – nảy rõ. */
    fun <T> spatial() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

    /** Nảy nhanh cho phản hồi khi nhấn. */
    fun <T> press() = spring<T>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)

    /** Chuyển động hiệu ứng (độ mờ, màu) – không nảy. */
    fun <T> effects() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
