package vn.kozu.game.mania.gamestage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.game.mania.gamestage.SubmitOutcome
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.previewstage.model.DifficultyRecord

/** Dữ liệu màn kết quả; [outcome]/[saveError] điền sau khi score.db ghi xong. */
data class ResultData(
    val result: GameResult,
    val outcome: SubmitOutcome? = null,
    val saveError: String? = null,
)

@Composable
fun ResultScreen(
    difficulty: DifficultyRecord,
    data: ResultData,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val r = data.result
    Surface(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Text("${difficulty.title} [${difficulty.version}]", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(r.rank, color = PlayColors.rank(r.rank), fontSize = 96.sp, fontWeight = FontWeight.ExtraBold)
        Text("%,d".format(r.score), color = MaterialTheme.colorScheme.onSurface, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("%.2f%%".format(r.accuracyPercent), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            when {
                data.saveError != null -> Text("Could not save score: ${data.saveError}", color = Color(0xFFFFB4B4))
                data.outcome == null -> Text("Saving score…", color = Color(0xFFBBBBBB))
                data.outcome.isNewBest -> Text("New personal best!", color = Color(0xFFFFE066), fontWeight = FontWeight.Bold)
                else -> data.outcome.previousBest?.let {
                    Text("Personal best: %,d".format(it.score), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth(0.8f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Line(Judgement.PERFECT, r.perfect)
                Line(Judgement.GREAT, r.great)
                Line(Judgement.GOOD, r.good)
                Line(Judgement.OK, r.ok)
                Line(Judgement.MEH, r.meh)
                Line(Judgement.MISS, r.miss)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max combo", color = MaterialTheme.colorScheme.onSurface)
                    Text("${r.maxCombo} / ${r.totalNotes}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back to chart") }
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun Line(j: Judgement, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(j.label, color = PlayColors.judgement(j), fontWeight = FontWeight.Bold)
        Text("$count", color = MaterialTheme.colorScheme.onSurface)
    }
}
