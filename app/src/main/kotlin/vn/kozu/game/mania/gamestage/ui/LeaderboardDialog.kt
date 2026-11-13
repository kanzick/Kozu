package vn.kozu.game.mania.gamestage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.gamestage.GlobalLeaderboard
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.R

@Composable
fun LeaderboardDialog(
    game: GameStageModule,
    song: SongEntry,
    difficulty: DifficultyRecord,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf<List<ScoreRecord>?>(null) }
    var globalRows by remember { mutableStateOf<GlobalLeaderboard?>(null) }
    var global by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(song.songId, difficulty.version, global) {
        error = null
        try {
            if (global) {
                globalRows = null
                globalRows = game.globalLeaderboard(song, difficulty)
            } else {
                rows = null
                rows = game.leaderboard(song, difficulty)
            }
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
                Text(stringResource(R.string.leaderboard), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${difficulty.title} [${difficulty.version}]", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    TextButton(onClick = { global = false }, enabled = global) { Text(stringResource(R.string.local_top_five)) }
                    TextButton(onClick = { global = true }, enabled = !global) { Text(stringResource(R.string.global_top_fifty)) }
                }
                if (global) {
                    val remote = globalRows
                    if (remote != null && !remote.signedIn) {
                        Text(stringResource(R.string.score_sign_in_required), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (remote != null && remote.pendingCount > 0) {
                        Text(stringResource(R.string.scores_pending_sync, remote.pendingCount), color = MaterialTheme.colorScheme.error)
                    }
                    if (remote?.profileWarning == true) {
                        Text(stringResource(R.string.score_name_sync_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    val list = if (global) globalRows?.rows?.map { it.score } else rows
                    val currentError = error
                    when {
                        currentError != null -> Text(stringResource(R.string.error_prefix, currentError), color = MaterialTheme.colorScheme.error)
                        list == null -> Text(stringResource(R.string.loading_scores))
                        list.isEmpty() -> Text(stringResource(R.string.no_scores_have_been_recorded_for_this_difficulty))
                        else -> LazyColumn {
                            itemsIndexed(list) { i, r ->
                                val globalRow = if (global) globalRows?.rows?.get(i) else null
                                val highlighted = globalRow != null && globalRow.userId == globalRows?.userId
                                val rowModifier = if (highlighted) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier
                                Row(rowModifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("#${globalRow?.place ?: (i + 1)}", Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                                    Text(r.playerName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("%,d".format(r.score), Modifier.width(96.dp), fontWeight = FontWeight.SemiBold)
                                    Text("%.2f%%".format(r.accuracy), Modifier.width(72.dp))
                                    Text("${r.trueCombo?.toString() ?: "—"}/${r.maxCombo?.toString() ?: "—"}",
                                        Modifier.width(72.dp),
                                    )
                                    Text(r.rank, Modifier.width(32.dp), color = PlayColors.rank(r.rank), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text(stringResource(R.string.close)) }
            }
        }
    }
}
