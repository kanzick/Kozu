package vn.kozu.game.mania.gamestage.playtype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.gamestage.ui.PlayfieldLayout
import vn.kozu.R

object LandscapeVerticalScrolling : PlayType(
    storageKey = "LANDSCAPE_VERTICAL_SCROLLING",
    isLandscape = true,
)

internal fun DrawScope.drawLandscapeVerticalScrolling(
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
) = drawPortraitPlayfield(
    chart = chart,
    engine = engine,
    layout = layout,
    nowMs = nowMs,
    travelMs = travelMs,
    firstVisible = firstVisible,
    primary = primary,
    tertiary = tertiary,
    onSurface = onSurface,
    surface = surface,
    judgementLineHeight = judgementLineHeight,
)

@Composable
internal fun LandscapePlayTypeOptions(
    settings: GameSettings,
    onChange: (GameSettings) -> Unit,
    onCommit: (GameSettings) -> Unit,
) {
    Column {
        Text(stringResource(R.string.scrolling_style),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        LandscapePlayTypeOption(
            selected = settings.playType == LandscapeVerticalScrolling,
            label = "Vertical Scrolling",
        ) {
            val updated = settings.copy(playType = LandscapeVerticalScrolling)
            onChange(updated)
            onCommit(updated)
        }
        LandscapePlayTypeOption(
            selected = settings.playType == Landscape3DPerspective,
            label = "3D Perspective",
        ) {
            val updated = settings.copy(playType = Landscape3DPerspective)
            onChange(updated)
            onCommit(updated)
        }
    }
}

@Composable
private fun LandscapePlayTypeOption(selected: Boolean, label: String, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth()
    if (selected) {
        Button(onClick = {}, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}
