package vn.kozu.game.mania.gamestage.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.engine.NoteScroll
import vn.kozu.game.mania.gamestage.media.BackgroundLoader
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Popup shown when a difficulty is tapped: song info at the top, settings below, and the Play button at the bottom.
 * Includes a leaderboard for this song and difficulty; the personal best appears in the song info.
 *
 * When [refreshKey] changes, the personal best is reloaded (e.g. after returning from a completed game).
 */
@Composable
fun SongInfoDialog(
    game: GameStageModule,
    song: SongEntry,
    difficulty: DifficultyRecord,
    settings: GameSettings,
    settingsError: String?,
    refreshKey: Int,
    onSettingsChange: (GameSettings) -> Unit,
    onSettingsCommit: (GameSettings) -> Unit,
    onPlay: (ManiaChart) -> Unit,
    onDismiss: () -> Unit,
) {
    var chart by remember(song.songId, difficulty.osuFile) { mutableStateOf<ManiaChart?>(null) }
    var chartError by remember(song.songId, difficulty.osuFile) { mutableStateOf<String?>(null) }
    var best by remember(song.songId, difficulty.osuFile, refreshKey) { mutableStateOf<ScoreRecord?>(null) }
    var bestLoaded by remember(song.songId, difficulty.osuFile, refreshKey) { mutableStateOf(false) }
    var bestError by remember(song.songId, difficulty.osuFile, refreshKey) { mutableStateOf<String?>(null) }
    var header by remember(song.songId, difficulty.osuFile) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showLeaderboard by remember { mutableStateOf(false) }

    LaunchedEffect(song.songId, difficulty.osuFile) {
        try {
            chart = game.loadChart(song, difficulty)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            chartError = e.message ?: e.javaClass.simpleName
        }
    }
    LaunchedEffect(song.songId, difficulty.osuFile) {
        header = withContext(Dispatchers.IO) {
            difficulty.backgroundFile(song.folder)?.let { runCatching { BackgroundLoader.load(it, 720, 405) }.getOrNull() }
        }
    }
    LaunchedEffect(song.songId, difficulty.osuFile, refreshKey, settings.playerName) {
        try {
            best = game.bestScore(song, difficulty)
            bestError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            bestError = e.message ?: "Could not load personal best"
        }
        bestLoaded = true
    }

    Dialog(onDismissRequest = { onSettingsCommit(settings); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // ---------------- song information
                    Box(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF202020))) {
                        header?.let {
                            Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                        }
                        Box(Modifier.matchParentSize().background(Color(0x99000000)))
                        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(
                                difficulty.title, color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            Text(difficulty.artist, color = Color(0xFFE0E0E0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("[${difficulty.version}]  ·  mapped by ${difficulty.creator}", color = Color(0xFFBDBDBD), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        val c = chart
                        val keys = c?.keys ?: difficulty.circleSize.toInt()
                        val stats = buildString {
                            append("${keys}K  ·  OD ${fmt1(difficulty.overallDifficulty)}  ·  HP ${fmt1(difficulty.hpDrainRate)}")
                            if (c != null) {
                                append("  ·  ${c.bpm.roundToInt()} BPM  ·  ${c.totalNotes} notes (${c.holdCount} holds)  ·  ${formatTime(c.lastEventTimeMs)}")
                            }
                        }
                        Text(stats, style = MaterialTheme.typography.bodyMedium)
                        chartError?.let { Text("Could not read chart: $it", color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(8.dp))
                        val b = best
                        Text(
                            when {
                                !bestLoaded -> "Personal best: …"
                                bestError != null -> "Personal best unavailable: $bestError"
                                b == null -> "Personal best: not played yet"
                                else -> "Personal best: %,d  ·  %.2f%%  ·  %s".format(b.score, b.accuracy, b.rank)
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = b?.let { PlayColors.rank(it.rank) } ?: MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { showLeaderboard = true }) { Text("Leaderboard") }

                        Spacer(Modifier.height(16.dp))
                        // ---------------- settings
                        SettingsPanel(settings, onSettingsChange, onSettingsCommit)
                        settingsError?.let {
                            Text(
                                "Could not save settings: $it",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                // ---------------- Play
                Button(
                    onClick = { chart?.let { onSettingsCommit(settings); onPlay(it) } },
                    enabled = chart != null,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) { Text(if (chart == null && chartError == null) "Loading chart…" else "Play") }
            }
        }
    }

    if (showLeaderboard) {
        LeaderboardDialog(game, song, difficulty, onDismiss = { showLeaderboard = false })
    }
}

@Composable
private fun SettingsPanel(
    s: GameSettings,
    onChange: (GameSettings) -> Unit,
    onCommit: (GameSettings) -> Unit,
) {
    Text("Game settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    Text("Audio", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    SettingSlider("Music", "${s.musicVolume}", s.musicVolume.toFloat(), 0f..100f, 99,
        onChange = { onChange(s.copy(musicVolume = it.roundToInt()).coerced()) }, onFinished = { onCommit(s) })
    SettingSlider("Hitsound", "${s.hitsoundVolume}", s.hitsoundVolume.toFloat(), 0f..100f, 99,
        onChange = { onChange(s.copy(hitsoundVolume = it.roundToInt()).coerced()) }, onFinished = { onCommit(s) })

    Text("Speed", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    SettingSlider("Note speed", fmt1(s.speed), s.speed.toFloat(), NoteScroll.MIN_SPEED.toFloat()..NoteScroll.MAX_SPEED.toFloat(), 148,
        onChange = { onChange(s.copy(speed = it.toDouble()).coerced()) }, onFinished = { onCommit(s) },
        onStep = { dir -> onCommit(s.copy(speed = s.speed + dir * 0.1).coerced().also(onChange)) })

    Text("Background", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    SettingSlider("Background dim (0 = clear, 100 = dark)", "${s.backgroundDim}", s.backgroundDim.toFloat(), 0f..100f, 99,
        onChange = { onChange(s.copy(backgroundDim = it.roundToInt()).coerced()) }, onFinished = { onCommit(s) })

    Text("Offset", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    SettingSlider(
        "Timing offset (+ = notes arrive later)", "${s.offsetMs} ms", s.offsetMs.toFloat(),
        GameSettings.OFFSET_MIN.toFloat()..GameSettings.OFFSET_MAX.toFloat(), GameSettings.OFFSET_MAX - GameSettings.OFFSET_MIN - 1,
        onChange = { onChange(s.copy(offsetMs = it.roundToInt()).coerced()) }, onFinished = { onCommit(s) },
        onStep = { dir -> onCommit(s.copy(offsetMs = s.offsetMs + dir).coerced().also(onChange)) },
    )

    var name by remember(s.playerName) { mutableStateOf(s.playerName) }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(GameSettings.PLAYER_NAME_MAX); onChange(s.copy(playerName = name)) },
        label = { Text("Player name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    onStep: ((Int) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onStep != null) TextButton(onClick = { onStep(-1) }) { Text("−") }
            Text(valueText, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp), maxLines = 1)
            if (onStep != null) TextButton(onClick = { onStep(1) }) { Text("+") }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps, onValueChangeFinished = onFinished)
    }
}

private fun fmt1(v: Double) = "%.1f".format(java.util.Locale.ROOT, v)

private fun formatTime(ms: Int): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
