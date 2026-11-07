package vn.kozu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The little pill at the top of a bottom sheet.
 *
 * Material's own `DragHandle` is interactive: holding it pops a "Drag handle" tooltip. This one
 * is purely decorative; the sheet still drags from anywhere.
 */
@Composable
fun KozuSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .padding(vertical = 14.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
    )
}
