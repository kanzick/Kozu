package vn.kozu.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified

/**
 * Text that shrinks to fit its box instead of overflowing, down to [minScale] of the requested size.
 * Use it where a label must stay on one line whatever the font, size or density the user picked
 * (titles in headers, pills, bars). Below [minScale] it falls back to an ellipsis.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    minScale: Float = 0.62f,
    textAlign: TextAlign? = null,
) {
    var scale by remember(text, style.fontSize, style.fontFamily, style.fontWeight) { mutableFloatStateOf(1f) }
    Text(
        text = text,
        modifier = modifier,
        color = if (color == Color.Unspecified) LocalContentColor.current else color,
        style =
            if (style.fontSize.isSpecified && scale < 1f) {
                style.copy(
                    fontSize = style.fontSize * scale,
                    lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight,
                )
            } else {
                style
            },
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.hasVisualOverflow) && scale > minScale) {
                scale = (scale * 0.92f).coerceAtLeast(minScale)
            }
        },
    )
}
