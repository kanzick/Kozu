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

/** Kết quả của việc ghi điểm sau một lượt chơi. */
data class SubmitOutcome(val isNewBest: Boolean, val previousBest: ScoreRecord?)

/**
 * Điểm vào duy nhất của module GameStage (popup thông tin -> màn chơi -> kết quả).
 *
 *   val preview = PreviewStageModule.get(context)
 *   val game = GameStageModule.get(context, preview)
 *   // trong Compose: GameStageHost(preview, game)
 *
 * Dữ liệu riêng của GameStage nằm trong <root của PreviewStage>/gamestage/ (file cài đặt, hitsound mặc định).
 * Điểm người chơi dùng score.db của PreviewStage.
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

    /** Idempotent: nạp file cài đặt và chuẩn bị hitsound mặc định. */
    suspend fun start() {
        if (started) return
        withContext(Dispatchers.IO) {
            dataDir.mkdirs()
            settings = store.load()
            DefaultHitsound.ensure(defaultHitFile)
        }
        started = true
    }

    /** Cập nhật và ghi ngay xuống file cài đặt. */
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

    // ------------------------------------------------------------ điểm số

    private suspend fun scoreDb(): ScoreDatabase {
        preview.start() // idempotent, đảm bảo score.db đã mở
        return preview.scores ?: error("The score database is not ready")
    }

    /** Điểm cao nhất của người chơi hiện tại ở bài + độ khó này. */
    suspend fun bestScore(song: SongEntry, difficulty: DifficultyRecord): ScoreRecord? =
        withContext(Dispatchers.IO) { scoreDb().bestScore(song.songId, difficulty.version, settings.playerName) }

    /** Bảng xếp hạng của riêng bài + độ khó này. */
    suspend fun leaderboard(song: SongEntry, difficulty: DifficultyRecord, limit: Int = 50): List<ScoreRecord> =
        withContext(Dispatchers.IO) { scoreDb().topScores(song.songId, difficulty.version, limit) }

    /** Ghi kết quả vào score.db và cho biết có phải kỷ lục mới không. */
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
