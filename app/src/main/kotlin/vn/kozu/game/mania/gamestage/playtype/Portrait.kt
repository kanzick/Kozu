package vn.kozu.game.mania.gamestage.playtype

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.engine.NoteScroll
import vn.kozu.game.mania.gamestage.ui.drawJudgmentXLine
import vn.kozu.game.mania.gamestage.ui.PlayColors
import vn.kozu.game.mania.gamestage.ui.PlayfieldLayout

object Portrait : PlayType(
    storageKey = "PORTRAIT",
    isLandscape = false,
)

internal fun DrawScope.drawPortraitPlayfield(
    chart: ManiaChart,
    engine: JudgementEngine,
    layout: PlayfieldLayout,
    nowMs: Double,
    travelMs: Double,
    firstVisible: IntArray,
    primary: Color,
    tertiary: Color,
    onSurface: Color,
    surface: Color,
    judgementLineHeight: Float,
) {
    val height = size.height
    drawRoundRect(
        color = surface.copy(alpha = 0.78f),
        topLeft = Offset(layout.left, 0f),
        size = Size(layout.right - layout.left, height),
        cornerRadius = CornerRadius(layout.columnWidth * 0.12f),
    )
    for (boundary in 0..chart.keys) {
        val x = layout.left + boundary * layout.columnWidth
        drawRect(onSurface.copy(alpha = 0.16f), Offset(x - 1f, 0f), Size(2f, height))
    }

    for (column in 0 until chart.keys) {
        if (engine.keyDown[column]) {
            drawRect(
                primary.copy(alpha = 0.22f),
                Offset(layout.columnX(column), layout.judgeY - height * 0.2f),
                Size(layout.columnWidth, height * 0.2f),
            )
        }
    }

    val pad = layout.columnWidth * NOTE_PADDING
    val noteWidth = layout.columnWidth - pad * 2
    for (column in 0 until chart.keys) {
        val ids = chart.columns[column]
        val x = layout.columnX(column) + pad
        val color = PlayColors.noteColor(column, chart.keys, primary, tertiary)
        var index = firstVisible[column]
        while (index < ids.size) {
            val id = ids[index]
            val note = chart.notes[id]
            val state = engine.state[id]
            val radius = CornerRadius(min(noteWidth * 0.28f, layout.noteHeight * 0.45f))
            if (state == JudgementEngine.STATE_DONE) {
                if (index == firstVisible[column]) firstVisible[column]++
                index++
                continue
            }
            if (note.timeMs - nowMs > travelMs + 50) break

            val headY = NoteScroll.yAt(note.timeMs.toDouble(), nowMs, travelMs, layout.judgmentXlineY)
            if (!note.isHold) {
                if (headY - layout.noteHeight < height && headY > 0f) {
                    val topLeft = Offset(x, headY - layout.noteHeight)
                    val noteSize = Size(noteWidth, layout.noteHeight)
                    drawRoundRect(color, topLeft, noteSize, radius)
                    drawRoundRect(
                        Color.White.copy(alpha = 0.32f),
                        topLeft,
                        noteSize,
                        radius,
                        style = Stroke(width = (layout.columnWidth * 0.035f).coerceAtLeast(1f)),
                    )
                }
            } else {
                val holding = state == JudgementEngine.STATE_HOLDING
                val tailY = NoteScroll.yAt(note.endTimeMs.toDouble(), nowMs, travelMs, layout.judgmentXlineY)
                val headDrawY = if (holding) min(headY, layout.judgmentXlineY) else headY
                val bodyColor = if (engine.broken[id]) PlayColors.broken else color
                val top = max(tailY - layout.noteHeight, 0f)
                val bottom = min(headDrawY, height)
                if (bottom > top) {
                    drawRoundRect(
                        color = bodyColor.copy(alpha = if (holding) 0.85f else 0.55f),
                        topLeft = Offset(x + noteWidth * 0.15f, top),
                        size = Size(noteWidth * 0.7f, bottom - top),
                        cornerRadius = CornerRadius(noteWidth * 0.18f),
                    )
                }
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(x, headDrawY - layout.noteHeight),
                    size = Size(noteWidth, layout.noteHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = bodyColor.copy(alpha = 0.92f),
                    topLeft = Offset(x, tailY - layout.noteHeight),
                    size = Size(noteWidth, layout.noteHeight * 0.7f),
                    cornerRadius = radius,
                )
            }
            index++
        }
    }

    drawJudgmentXLine(
        left = layout.left,
        right = layout.right,
        y = layout.judgmentXlineY,
        color = primary,
        thickness = judgementLineHeight,
    )
}

private const val NOTE_PADDING = 0.06f
