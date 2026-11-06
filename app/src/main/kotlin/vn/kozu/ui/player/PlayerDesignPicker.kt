package vn.kozu.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.i18n.KozuText as Text

/**
 * A little schematic of a player design, drawn from simple shapes in the current theme colours,
 * so the picker can show what each design is before it is chosen.
 */
@Composable
fun PlayerDesignPreview(
    style: PlayerLayoutStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bg = colors.surface
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val soft = colors.onSurface.copy(alpha = 0.16f)
    val line = colors.onSurface.copy(alpha = 0.55f)
    val art = colors.primary.copy(alpha = 0.55f)
    val container = colors.primaryContainer
    val secondary = colors.secondaryContainer
    val tertiary = colors.tertiary.copy(alpha = 0.6f)

    Canvas(modifier.aspectRatio(9f / 16f)) {
        fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) =
            drawRoundRect(
                color = color,
                topLeft = Offset(x * size.width, y * size.height),
                size = Size(w * size.width, h * size.height),
                cornerRadius = CornerRadius(r * size.width),
            )
        // Height fraction of a square that is `w` wide.
        fun sq(w: Float) = w * size.width / size.height
        fun circle(cx: Float, cy: Float, d: Float, color: Color) = rr(cx - d / 2, cy - sq(d) / 2, d, sq(d), d / 2, color)
        fun lines(x: Float, y: Float, w: Float) {
            rr(x, y, w, 0.022f, 0.02f, line)
            rr(x, y + 0.045f, w * 0.6f, 0.016f, 0.02f, soft)
        }
        fun slider(y: Float, filled: Float = 0.45f, color: Color = primary) {
            rr(0.1f, y, 0.8f, 0.012f, 0.01f, soft)
            rr(0.1f, y, 0.8f * filled, 0.012f, 0.01f, color)
        }
        fun pills(y: Float) {
            rr(0.1f, y, 0.385f, 0.05f, 0.12f, soft)
            rr(0.515f, y, 0.385f, 0.05f, 0.12f, soft)
        }

        drawRect(bg)
        when (style) {
            PlayerLayoutStyle.CLASSIC -> {
                rr(0.35f, 0.04f, 0.3f, 0.015f, 0.02f, soft)
                rr(0.1f, 0.09f, 0.8f, sq(0.8f), 0.1f, art)
                lines(0.1f, 0.62f, 0.45f)
                circle(0.74f, 0.645f, 0.12f, primary)
                circle(0.88f, 0.645f, 0.12f, primary)
                slider(0.735f)
                circle(0.14f, 0.8f, 0.07f, soft)
                circle(0.3f, 0.8f, 0.08f, soft)
                circle(0.5f, 0.8f, 0.2f, primary)
                circle(0.7f, 0.8f, 0.08f, soft)
                circle(0.86f, 0.8f, 0.07f, soft)
                pills(0.9f)
            }
            PlayerLayoutStyle.MODERN -> {
                rr(0.35f, 0.04f, 0.3f, 0.015f, 0.02f, soft)
                rr(0.08f, 0.09f, 0.84f, sq(0.84f), 0.1f, art)
                lines(0.1f, 0.63f, 0.4f)
                rr(0.7f, 0.63f, 0.1f, sq(0.1f), 0.05f, primary)
                rr(0.82f, 0.63f, 0.1f, sq(0.1f), 0.05f, primary)
                slider(0.74f)
                rr(0.14f, 0.79f, 0.2f, 0.06f, 0.1f, primary)
                rr(0.38f, 0.775f, 0.24f, 0.09f, 0.12f, primary)
                rr(0.66f, 0.79f, 0.2f, 0.06f, 0.1f, primary)
                for (i in 0..4) circle(0.14f + i * 0.18f, 0.92f, 0.07f, soft)
            }
            PlayerLayoutStyle.MINIMAL -> {
                rr(0.15f, 0.08f, 0.7f, sq(0.7f), 0.08f, art)
                lines(0.15f, 0.6f, 0.4f)
                circle(0.78f, 0.625f, 0.05f, soft)
                circle(0.88f, 0.625f, 0.05f, soft)
                rr(0.15f, 0.73f, 0.7f, 0.006f, 0.01f, soft)
                rr(0.15f, 0.73f, 0.3f, 0.006f, 0.01f, line)
                circle(0.15f, 0.8f, 0.06f, soft)
                rr(0.26f, 0.775f, 0.12f, sq(0.12f), 0.03f, soft)
                circle(0.5f, 0.8f, 0.17f, colors.onSurface)
                rr(0.62f, 0.775f, 0.12f, sq(0.12f), 0.03f, soft)
                circle(0.85f, 0.8f, 0.06f, soft)
                rr(0.2f, 0.92f, 0.18f, 0.014f, 0.02f, soft)
                rr(0.62f, 0.92f, 0.18f, 0.014f, 0.02f, soft)
            }
            PlayerLayoutStyle.CINEMATIC -> {
                rr(0.35f, 0.04f, 0.3f, 0.015f, 0.02f, soft)
                circle(0.5f, 0.33f, 1.0f, primary.copy(alpha = 0.18f))
                rr(0.1f, 0.09f, 0.8f, sq(0.8f), 0.1f, art)
                lines(0.1f, 0.62f, 0.4f)
                for (i in 0..2) rr(0.62f + i * 0.1f, 0.625f, 0.085f, sq(0.085f), 0.025f, soft)
                slider(0.735f)
                rr(0.13f, 0.8f, 0.08f, sq(0.08f), 0.025f, soft)
                rr(0.25f, 0.79f, 0.12f, sq(0.12f), 0.04f, soft)
                rr(0.4f, 0.775f, 0.2f, sq(0.2f), 0.07f, primary)
                rr(0.63f, 0.79f, 0.12f, sq(0.12f), 0.04f, soft)
                rr(0.79f, 0.8f, 0.08f, sq(0.08f), 0.025f, soft)
                pills(0.9f)
            }
            PlayerLayoutStyle.LITTLE -> {
                drawRect(container)
                drawRect(primary.copy(alpha = 0.28f), size = Size(size.width, size.height * 0.5f))
                // Title running down the screen, like the real thing.
                rr(0.14f, 0.18f, 0.1f, 0.62f, 0.04f, colors.onPrimaryContainer.copy(alpha = 0.85f))
                rr(0.27f, 0.18f, 0.04f, 0.4f, 0.02f, colors.onPrimaryContainer.copy(alpha = 0.45f))
                rr(0.34f, 0.18f, 0.04f, 0.3f, 0.02f, colors.onPrimaryContainer.copy(alpha = 0.45f))
                rr(0.72f, 0.82f, 0.14f, 0.1f, 0.03f, colors.onPrimaryContainer.copy(alpha = 0.6f))
                circle(0.18f, 0.06f, 0.07f, colors.onPrimaryContainer.copy(alpha = 0.6f))
                circle(0.18f, 0.12f, 0.06f, colors.onPrimaryContainer.copy(alpha = 0.45f))
                circle(0.18f, 0.17f, 0.06f, colors.onPrimaryContainer.copy(alpha = 0.45f))
            }
            PlayerLayoutStyle.EXPRESSIVE -> {
                rr(0.35f, 0.04f, 0.3f, 0.015f, 0.02f, soft)
                rr(0.08f, 0.09f, 0.84f, sq(0.84f), 0.1f, art)
                lines(0.1f, 0.63f, 0.4f)
                rr(0.62f, 0.63f, 0.1f, sq(0.1f), 0.05f, soft)
                circle(0.77f, 0.645f, 0.1f, soft)
                rr(0.82f, 0.63f, 0.1f, sq(0.1f), 0.05f, soft)
                slider(0.74f)
                rr(0.08f, 0.78f, 0.84f, 0.1f, 0.1f, soft)
                rr(0.1f, 0.795f, 0.26f, 0.07f, 0.08f, secondary)
                rr(0.4f, 0.79f, 0.2f, 0.08f, 0.08f, primary)
                rr(0.64f, 0.795f, 0.26f, 0.07f, 0.08f, secondary)
                circle(0.43f, 0.915f, 0.06f, soft)
                circle(0.57f, 0.915f, 0.06f, soft)
            }
            PlayerLayoutStyle.IMMERSIVE -> {
                drawRect(art, size = Size(size.width, size.height * 0.62f))
                drawRect(
                    Brush.verticalGradient(
                        0.3f to Color.Transparent,
                        0.62f to bg,
                        startY = 0f,
                        endY = size.height,
                    ),
                )
                lines(0.08f, 0.52f, 0.55f)
                circle(0.86f, 0.55f, 0.1f, soft)
                slider(0.68f)
                circle(0.3f, 0.77f, 0.1f, soft)
                circle(0.5f, 0.77f, 0.18f, primary)
                circle(0.7f, 0.77f, 0.1f, soft)
                rr(0.1f, 0.86f, 0.8f, 0.01f, 0.01f, soft)
                for (i in 0..2) circle(0.2f + i * 0.3f, 0.94f, 0.07f, soft)
            }
            PlayerLayoutStyle.IMMERSIVE_EXTENDED -> {
                drawRect(art)
                drawRect(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        0.75f to bg.copy(alpha = 0.75f),
                        1f to bg.copy(alpha = 0.95f),
                    ),
                )
                lines(0.08f, 0.55f, 0.55f)
                circle(0.86f, 0.58f, 0.1f, soft)
                slider(0.7f)
                circle(0.3f, 0.79f, 0.1f, soft)
                circle(0.5f, 0.79f, 0.18f, primary)
                circle(0.7f, 0.79f, 0.1f, soft)
                rr(0.1f, 0.88f, 0.8f, 0.01f, 0.01f, soft)
                rr(0.7f, 0.93f, 0.2f, 0.035f, 0.1f, soft)
            }
            PlayerLayoutStyle.MATERIAL_EXTENDED -> {
                rr(0.35f, 0.04f, 0.3f, 0.015f, 0.02f, soft)
                rr(0.06f, 0.09f, 0.88f, sq(0.88f), 0.1f, art)
                circle(0.72f, 0.46f, 0.1f, bg.copy(alpha = 0.8f))
                circle(0.84f, 0.46f, 0.1f, bg.copy(alpha = 0.8f))
                lines(0.08f, 0.62f, 0.5f)
                slider(0.725f)
                rr(0.42f, 0.745f, 0.16f, 0.02f, 0.04f, soft)
                circle(0.12f, 0.8f, 0.07f, soft)
                rr(0.2f, 0.78f, 0.16f, 0.05f, 0.1f, secondary)
                rr(0.4f, 0.77f, 0.2f, 0.07f, 0.1f, primary)
                rr(0.64f, 0.78f, 0.16f, 0.05f, 0.1f, secondary)
                circle(0.88f, 0.8f, 0.07f, soft)
                rr(0.1f, 0.865f, 0.8f, 0.01f, 0.01f, soft)
                pills(0.92f)
            }
            PlayerLayoutStyle.EDITORIAL -> {
                drawRect(container)
                circle(0.2f, 0.06f, 0.08f, onPrimary.copy(alpha = 0.0f))
                circle(0.12f, 0.05f, 0.08f, colors.onPrimaryContainer.copy(alpha = 0.18f))
                for (i in 0..2) circle(0.62f + i * 0.12f, 0.05f, 0.08f, colors.onPrimaryContainer.copy(alpha = 0.18f))
                circle(0.5f, 0.3f, 0.78f, tertiary)
                rr(0.08f, 0.6f, 0.7f, 0.04f, 0.03f, colors.onPrimaryContainer.copy(alpha = 0.85f))
                rr(0.08f, 0.66f, 0.5f, 0.04f, 0.03f, colors.onPrimaryContainer.copy(alpha = 0.85f))
                rr(0.08f, 0.73f, 0.3f, 0.018f, 0.02f, colors.onPrimaryContainer.copy(alpha = 0.45f))
                rr(0.08f, 0.79f, 0.84f, 0.012f, 0.01f, colors.onPrimaryContainer.copy(alpha = 0.22f))
                rr(0.08f, 0.79f, 0.38f, 0.012f, 0.01f, colors.onPrimaryContainer)
                circle(0.14f, 0.9f, 0.07f, colors.onPrimaryContainer.copy(alpha = 0.3f))
                rr(0.26f, 0.88f, 0.12f, sq(0.12f), 0.04f, colors.onPrimaryContainer.copy(alpha = 0.18f))
                circle(0.5f, 0.9f, 0.17f, colors.onPrimaryContainer)
                rr(0.62f, 0.88f, 0.12f, sq(0.12f), 0.04f, colors.onPrimaryContainer.copy(alpha = 0.18f))
                circle(0.86f, 0.9f, 0.07f, colors.onPrimaryContainer.copy(alpha = 0.3f))
            }
        }
    }
}

/** "Player design style" sheet: every design with a preview, like ArchiveTune's picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignSheet(
    selected: PlayerLayoutStyle,
    onSelect: (PlayerLayoutStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        containerColor = colors.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Player design style",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            PlayerLayoutStyle.entries.forEach { style ->
                val isSelected = style == selected
                Surface(
                    modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f) { onSelect(style) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                    contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                        ) {
                            PlayerDesignPreview(style, Modifier.width(54.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(style.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    if (isSelected) {
                                        colors.onPrimaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        colors.onSurfaceVariant
                                    },
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
