package vn.kozu.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object KozuMotion {
    fun <T> spatial() = spring<T>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    fun <T> effects() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
