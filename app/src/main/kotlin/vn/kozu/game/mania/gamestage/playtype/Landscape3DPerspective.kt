package vn.kozu.game.mania.gamestage.playtype

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.ui.PlayColors
import vn.kozu.game.mania.gamestage.ui.PlayfieldLayout
import vn.kozu.game.mania.gamestage.ui.drawJudgmentXLine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// This directory documents a gameplay mode that owns its lane projection and note-rendering behavior.
// It is separate from skin selection and skin synchronization; do not couple those systems.
// Before changing this mode, understand its projection, note timing, judgement line, hold-note rendering, and touch-input assumptions, then run the game-stage tests.
object Landscape3DPerspective : PlayType(
    storageKey = "LANDSCAPE_3D_PERSPECTIVE",
    isLandscape = true,
)

internal fun DrawScope.drawPerspectivePlayfield(
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
    val projection = LandscapeProjection(layout, size.width)
    val height = size.height
    val bottomProgress = projection.progressAtY(height)

    val field = Path().apply {
        moveTo(projection.boundaryAt(0, 0f), projection.topY)
        lineTo(projection.boundaryAt(chart.keys, 0f), projection.topY)
        lineTo(projection.boundaryAt(chart.keys, bottomProgress), height)
        lineTo(projection.boundaryAt(0, bottomProgress), height)
        close()
    }
    drawPath(field, surface.copy(alpha = 0.78f))

    for (boundary in 0..chart.keys) {
        val top = Offset(projection.boundaryAt(boundary, 0f), projection.topY)
        val judgment = Offset(projection.boundaryAt(boundary, 1f), layout.judgeY)
        drawLine(onSurface.copy(alpha = 0.16f), top, judgment, 2f)
        val bottomBoundary = projection.boundaryAt(boundary, bottomProgress)
        drawLine(onSurface.copy(alpha = 0.16f), judgment, Offset(bottomBoundary, height), 2f)
    }

    for (column in 0 until chart.keys) {
        if (engine.keyDown[column]) {
            val topProgress = 0.75f
            val activeLane = Path().apply {
                moveTo(projection.boundaryAt(column, topProgress), projection.yAt(topProgress))
                lineTo(projection.boundaryAt(column + 1, topProgress), projection.yAt(topProgress))
                lineTo(projection.boundaryAt(column + 1, 1f), layout.judgeY)
                lineTo(projection.boundaryAt(column, 1f), layout.judgeY)
                close()
            }
            drawPath(activeLane, primary.copy(alpha = 0.22f))
        }
    }

    for (column in 0 until chart.keys) {
        val ids = chart.columns[column]
        val color = PlayColors.noteColor(column, chart.keys, primary, tertiary)
        var index = firstVisible[column]
        while (index < ids.size) {
            val id = ids[index]
            val note = chart.notes[id]
            val state = engine.state[id]
            if (note.timeMs - nowMs > travelMs) break

            val headProgress = projection.acceleratedProgressAt(note.timeMs.toDouble(), nowMs, travelMs)
            if (headProgress < 0f) {
                index++
                continue
            }
            val headY = projection.yAt(headProgress)
            val headHeight = projection.noteHeight(headProgress)
            val tailProgress = projection.acceleratedProgressAt(note.endTimeMs.toDouble(), nowMs, travelMs)
            val tailY = projection.yAt(tailProgress)
            val tailHeight = projection.noteHeight(tailProgress)
            if (engine.hit[id] && (!note.isHold || state == JudgementEngine.STATE_DONE)) {
                if (index == firstVisible[column]) firstVisible[column]++
                index++
                continue
            }
            if (!note.isHold) {
                if (headY - headHeight < height && headY > 0f) {
                    val noteShape = projection.notePath(column, headProgress)
                    drawPath(noteShape, color)
                    drawPath(
                        noteShape,
                        Color.White.copy(alpha = 0.2f),
                        style = Stroke(width = (projection.noteWidth(headProgress) * 0.025f).coerceAtLeast(1f)),
                    )
                }
            } else {
                val holding = state == JudgementEngine.STATE_HOLDING
                val drawHeadProgress = if (holding) 1f else headProgress
                val headDrawY = if (holding) min(headY, layout.judgeY) else headY
                val bodyColor = if (engine.broken[id]) PlayColors.broken else color
                val top = max(min(headDrawY, tailY), 0f)
                val bottom = min(max(headDrawY, tailY), height)
                if (bottom > top) {
                    val body = projection.notePathBetween(column, top, bottom, HOLD_BODY_INSET)
                    drawPath(body, bodyColor.copy(alpha = if (holding) 0.85f else 0.55f))
                }

                drawPath(projection.notePath(column, drawHeadProgress), bodyColor)
                drawPath(projection.notePath(column, tailProgress, 0.7f), bodyColor.copy(alpha = 0.92f))
            }
            if (state == JudgementEngine.STATE_DONE) {
                val isPastScreen = if (note.isHold) {
                    tailY - tailHeight > height
                } else {
                    headY - headHeight > height
                }
                if (isPastScreen && index == firstVisible[column]) firstVisible[column]++
            }
            index++
        }
    }

    drawJudgmentXLine(
        left = projection.baseLeft,
        right = projection.baseLeft + projection.baseWidth,
        y = layout.judgmentXlineY,
        color = primary,
        thickness = judgementLineHeight,
    )
}

internal class LandscapeProjection(
    private val layout: PlayfieldLayout,
    screenWidth: Float,
) {
    private val fieldWidth = layout.right - layout.left
    private val centerX = (layout.left + layout.right) / 2f
    private val originalBaseWidth = min(fieldWidth, 2f * layout.judgeY / TAN_SIDE_ANGLE)
    val baseWidth = screenWidth * BASE_WIDTH_FRACTION
    val baseLeft = centerX - baseWidth / 2f
    val baseColumnWidth = baseWidth / layout.keys
    private val fullTriangleHeight = originalBaseWidth * TAN_SIDE_ANGLE / 2f
    private val apexY = layout.judgeY - fullTriangleHeight
    val topY = apexY + fullTriangleHeight * TIP_CUT_FRACTION

    fun scale(progress: Float): Float {
        val depth = projectedDepth(progress)
        val widthAtProgress = originalBaseWidth * TIP_CUT_FRACTION +
            (baseWidth - originalBaseWidth * TIP_CUT_FRACTION) * depth
        return widthAtProgress / fieldWidth
    }

    fun yAt(progress: Float): Float {
        return topY + (layout.judgeY - topY) * projectedDepth(progress)
    }

    fun progressAtY(y: Float): Float {
        val depth = ((y - topY) / (layout.judgeY - topY)).coerceAtLeast(0f)
        val acceleration = PERSPECTIVE_ACCELERATION
        return ((sqrt(1.0 + 4.0 * acceleration * (1.0 + acceleration) * depth) - 1.0) /
            (2.0 * acceleration)).toFloat()
    }

    private fun projectedDepth(progress: Float): Float {
        val visibleProgress = progress.coerceAtLeast(0f)
        val acceleration = PERSPECTIVE_ACCELERATION
        return (visibleProgress + acceleration * visibleProgress * visibleProgress) /
            (1f + acceleration)
    }

    fun acceleratedProgressAt(eventTimeMs: Double, nowMs: Double, travelMs: Double): Float {
        val timeProgress = progressAt(eventTimeMs, nowMs, travelMs)
        if (timeProgress <= 0f) return timeProgress
        return timeProgress * timeProgress
    }

    fun boundaryAt(boundary: Int, progress: Float): Float {
        val x = layout.left + boundary * layout.columnWidth
        return centerX + (x - centerX) * scale(progress)
    }

    fun columnCenterAt(column: Int, progress: Float): Float {
        val x = layout.columnX(column) + layout.columnWidth / 2f
        return centerX + (x - centerX) * scale(progress)
    }

    fun noteWidth(progress: Float): Float =
        layout.columnWidth * noteScale(progress) * (1f - NOTE_PADDING * 2f)

    fun noteHeight(progress: Float): Float =
        layout.noteHeight * scale(progress) / scale(1f)

    private fun noteScale(progress: Float): Float =
        scale(progress)

    fun notePath(
        column: Int,
        bottomProgress: Float,
        heightFraction: Float = 1f,
    ): Path {
        val bottomY = yAt(bottomProgress)
        val noteTopY = max(topY, bottomY - noteHeight(bottomProgress) * heightFraction)
        return notePathBetween(column, noteTopY, bottomY)
    }

    fun notePathBetween(
        column: Int,
        topY: Float,
        bottomY: Float,
        insetFraction: Float = NOTE_PADDING,
    ): Path {
        val topProgress = progressAtY(topY)
        val bottomProgress = progressAtY(bottomY)
        val topLeft = boundaryAt(column, topProgress)
        val topRight = boundaryAt(column + 1, topProgress)
        val bottomLeft = boundaryAt(column, bottomProgress)
        val bottomRight = boundaryAt(column + 1, bottomProgress)
        val topInset = (topRight - topLeft) * insetFraction
        val bottomInset = (bottomRight - bottomLeft) * insetFraction
        return Path().apply {
            moveTo(topLeft + topInset, yAt(topProgress))
            lineTo(topRight - topInset, yAt(topProgress))
            lineTo(bottomRight - bottomInset, yAt(bottomProgress))
            lineTo(bottomLeft + bottomInset, yAt(bottomProgress))
            close()
        }
    }

    private companion object {
        const val TAN_SIDE_ANGLE = 1.7320508f
        const val TIP_CUT_FRACTION = 0.125f
        const val NOTE_PADDING = 0.08f
        const val BASE_WIDTH_FRACTION = 0.84f
        const val PERSPECTIVE_ACCELERATION = 1.3f
    }
}

private fun progressAt(eventTimeMs: Double, nowMs: Double, travelMs: Double): Float =
    (1.0 - (eventTimeMs - nowMs) / travelMs).toFloat()

private const val HOLD_BODY_INSET = 0.24f
