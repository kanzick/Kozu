package vn.kozu.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.Modifier

/**
 * Scrolls text that is too long for its space and repeats forever. Text that fits stays still.
 * Use it on a Text with maxLines = 1 and softWrap = false.
 */
fun Modifier.loopingMarquee(): Modifier = basicMarquee(iterations = Int.MAX_VALUE)
