package vn.kozu.game.mania.gamestage

import android.content.Context
import android.util.Log
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.kozu.game.mania.gamestage.audio.DefaultHitsound
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.chart.ManiaChartParser
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.session.GameSession
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.gamestage.settings.SettingsStore
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.game.mania.previewstage.storage.ScoreDatabase

data class SubmitOutcome(
    val isNewBest: Boolean,
    val previousBest: ScoreRecord?,

    val recorded: Boolean = true,
)

class GameStageModule private constructor(
    context: Context,
    val preview: PreviewStageModule,
) {
    val dataDir: File = File(preview.paths.root, "gamestage")
    private val store = SettingsStore(File(dataDir, "settings.properties"))
    private val defaultHitFile = File(dataDir, "default_hit.wav")
    private val scoreSync = ScoreSync(context)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startLock = Mutex()

    @Volatile var settings: GameSettings = GameSettings()
        private set

    @Volatile private var started = false

    suspend fun start() = startLock.withLock {
        if (started) return@withLock
        withContext(Dispatchers.IO) {
            dataDir.mkdirs()
            settings = store.load()
            DefaultHitsound.ensure(defaultHitFile)
        }
        started = true
        if (scoreSync.configured) {
            syncScope.launch {
                while (true) {
                    syncScores()
                    delay(60_000)
                }
            }
        }
    }

    suspend fun updateSettings(newSettings: GameSettings) {
        val s = newSettings.coerced()
        settings = s
        withContext(Dispatchers.IO) { store.save(s) }
    }

    suspend fun loadChart(song: SongEntry, difficulty: DifficultyRecord): ManiaChart =
        withContext(Dispatchers.IO) {
            ManiaChartParser.parse(File(song.folder, difficulty.osuFile).readText(Charsets.UTF_8))
        }

    fun newSession(song: SongEntry, difficulty: DifficultyRecord, chart: ManiaChart): GameSession =
        GameSession(song, difficulty, chart, settings, defaultHitFile)

    private suspend fun scoreDb(): ScoreDatabase {
        preview.start()
        return preview.scores ?: error("The score database is not ready")
    }

    suspend fun bestScore(song: SongEntry, difficulty: DifficultyRecord): ScoreRecord? =
        withContext(Dispatchers.IO) { scoreDb().bestScore(song.songId, difficulty.version) }


    suspend fun bestScores(song: SongEntry): Map<String, ScoreRecord> =
        withContext(Dispatchers.IO) {
            scoreDb().bestScores(song.songId).distinctBy { it.difficulty }.associateBy { it.difficulty }
        }

    suspend fun latestPlayedAt(song: SongEntry): Map<String, Long> =
        withContext(Dispatchers.IO) {
            scoreDb().latestPlayedAt(song.songId)
        }


    suspend fun leaderboard(song: SongEntry, difficulty: DifficultyRecord, limit: Int = 5): List<ScoreRecord> =
        withContext(Dispatchers.IO) { scoreDb().topScores(song.songId, difficulty.version, limit) }

    suspend fun globalLeaderboard(song: SongEntry, difficulty: DifficultyRecord): GlobalLeaderboard =
        withContext(Dispatchers.IO) {
            if (scoreSync.configured) syncScores()
            scoreSync.leaderboard(song.songId, difficulty.version).copy(
                pendingCount = scoreDb().pendingSubmissions().size,
                profileWarning = scoreSync.profileWarning,
            )
        }

    private suspend fun syncScores() {
        try {
            scoreSync.sync(scoreDb(), settings.playerName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("GameStageModule", "Could not synchronize scores; will retry", e)
        }
    }

    suspend fun submitResult(song: SongEntry, difficulty: DifficultyRecord, result: GameResult): SubmitOutcome =
        withContext(Dispatchers.IO) {
            val db = scoreDb()
            val name = settings.playerName
            val previous = db.bestScore(song.songId, difficulty.version)
            val score = ScoreRecord(
                    playerName = name,
                    songId = song.songId,
                    difficulty = difficulty.version,
                    score = result.score,
                    accuracy = result.accuracyPercent,
                    rank = result.rank,
                    trueCombo = result.trueCombo,
                    maxCombo = result.maxCombo,
                    playedAt = System.currentTimeMillis(),
                )
            val id = UUID.randomUUID().toString()
            val payload = try {
                ScoreSync.submission(id, score, difficulty, result, settings.speed)
            } catch (e: Exception) {
                Log.e("GameStageModule", "Could not prepare score for synchronization", e)
                null
            }
            val recorded = if (payload == null) db.insert(score) else db.insert(score, id, payload)
            if (payload != null && scoreSync.configured) syncScope.launch { syncScores() }
            val isNewBest = previous == null || result.score > previous.score ||
                (result.score == previous.score && result.accuracyPercent > previous.accuracy)
            SubmitOutcome(isNewBest, previous, recorded)
        }

    companion object {
        @Volatile private var instance: GameStageModule? = null

        fun get(context: Context, preview: PreviewStageModule): GameStageModule =
            instance ?: synchronized(this) {
                instance ?: GameStageModule(context.applicationContext, preview).also { instance = it }
            }
    }
}
