package vn.kozu.game.mania.gamestage

import android.content.Context
import vn.kozu.game.mania.gamestage.audio.DefaultHitsound
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.chart.ManiaChartParser
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.session.GameSession
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.gamestage.settings.SettingsStore
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.storage.ScoreDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Outcome of saving a score after a play session. */
data class SubmitOutcome(val isNewBest: Boolean, val previousBest: ScoreRecord?)

/**
 * The single entry point for the GameStage module (info popup -> gameplay -> results).
 *
 *   val preview = PreviewStageModule.get(context)
 *   val game = GameStageModule.get(context, preview)
 *   // In Compose: GameStageHost(preview, game)
 *
 * GameStage-specific data is stored in <PreviewStage root>/gamestage/ (settings file, default hitsound).
 * Player scores use PreviewStage's score.db.
 */
class GameStageModule private constructor(
    @Suppress("unused") context: Context,
    val preview: PreviewStageModule,
) {
    val dataDir: File = File(preview.paths.root, "gamestage")
    private val store = SettingsStore(File(dataDir, "settings.properties"))
    private val defaultHitFile = File(dataDir, "default_hit.wav")

    @Volatile var settings: GameSettings = GameSettings()
        private set

    @Volatile private var started = false

    /** Idempotent: loads the settings file and prepares the default hitsound. */
    suspend fun start() {
        if (started) return
        withContext(Dispatchers.IO) {
            dataDir.mkdirs()
            settings = store.load()
            DefaultHitsound.ensure(defaultHitFile)
        }
        started = true
    }

    /** Updates settings and immediately writes them to the settings file. */
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

    // ------------------------------------------------------------ scores

    private suspend fun scoreDb(): ScoreDatabase {
        preview.start() // Idempotent; ensures score.db is open.
        return preview.scores ?: error("The score database is not ready")
    }

    /** Current player's best score for this song and difficulty. */
    suspend fun bestScore(song: SongEntry, difficulty: DifficultyRecord): ScoreRecord? =
        withContext(Dispatchers.IO) { scoreDb().bestScore(song.songId, difficulty.version, settings.playerName) }

    /** Leaderboard for this song and difficulty. */
    suspend fun leaderboard(song: SongEntry, difficulty: DifficultyRecord, limit: Int = 50): List<ScoreRecord> =
        withContext(Dispatchers.IO) { scoreDb().topScores(song.songId, difficulty.version, limit) }

    /** Saves the result to score.db and indicates whether it is a new record. */
    suspend fun submitResult(song: SongEntry, difficulty: DifficultyRecord, result: GameResult): SubmitOutcome =
        withContext(Dispatchers.IO) {
            val db = scoreDb()
            val name = settings.playerName
            val previous = db.bestScore(song.songId, difficulty.version, name)
            db.insert(
                ScoreRecord(
                    playerName = name,
                    songId = song.songId,
                    difficulty = difficulty.version,
                    score = result.score,
                    accuracy = result.accuracyPercent,
                    rank = result.rank,
                )
            )
            SubmitOutcome(previous == null || result.score > previous.score, previous)
        }

    companion object {
        @Volatile private var instance: GameStageModule? = null

        fun get(context: Context, preview: PreviewStageModule): GameStageModule =
            instance ?: synchronized(this) {
                instance ?: GameStageModule(context.applicationContext, preview).also { instance = it }
            }
    }
}
