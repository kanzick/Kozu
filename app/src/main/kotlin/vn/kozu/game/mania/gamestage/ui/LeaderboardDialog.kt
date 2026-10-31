package vn.kozu.game.mania.gamestage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry

/** Bảng xếp hạng của riêng một bài ở một độ khó (mỗi người chơi hiện điểm cao nhất của họ). */
@Composable
fun LeaderboardDialog(
    game: GameStageModule,
    song: SongEntry,
    difficulty: DifficultyRecord,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf<List<ScoreRecord>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(song.songId, difficulty.version) {
        try {
            rows = game.leaderboard(song, difficulty)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: e.javaClass.simpleName
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Leaderboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${difficulty.title} [${difficulty.version}]", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    val list = rows
                    when {
                        error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        list == null -> Text("Loading scores…")
                        list.isEmpty() -> Text("No scores have been recorded for this difficulty.")
                        else -> LazyColumn {
                            itemsIndexed(list) { i, r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("#${i + 1}", Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                                    Text(r.playerName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("%,d".format(r.score), Modifier.width(96.dp), fontWeight = FontWeight.SemiBold)
                                    Text("%.2f%%".format(r.accuracy), Modifier.width(72.dp))
                                    Text(r.rank, Modifier.width(32.dp), color = PlayColors.rank(r.rank), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text("Close") }
            }
        }
    }
}
