package vn.kozu.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Motion specifications for spring physics animations across Kozu.
 */
object KozuMotion {
    /** Spatial spring for layout, bounds, and translation transitions */
    fun <T> spatial() = spring<T>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    /** Bouncy spring for touch press feedback */
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    /** Smooth spring for non-bouncy color/fade transitions */
    fun <T> effects() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
