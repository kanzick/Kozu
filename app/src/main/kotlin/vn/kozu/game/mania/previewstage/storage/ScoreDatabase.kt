package vn.kozu.game.mania.previewstage.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import java.io.Closeable
import java.io.File

/**
 * score.db: player_name, song_id, difficulty, score, accuracy (%), rank, true_combo, max_combo, played_at.
 *
 * Schema v5 stores the highest achieved combo as `true_combo` and the chart's maximum possible combo
 * as `max_combo`. Older rows can recover true combo from their recorded max combo, but not chart maximum.
 *  - v1 -> v2 adds `difficulty` and `played_at`.
 *  - v2 -> v3 adds `max_combo`.
 *  - v3 -> v4 adds `true_combo`.
 *  - v4 -> v5 migrates the old achieved-combo value to `true_combo`.
 */
class ScoreDatabase private constructor(private val db: SQLiteDatabase) : Closeable {

    /**
     * Records a play as its own row. Each difficulty keeps only its [MAX_SCORES_PER_DIFFICULTY] best scores;
     * a score that cannot beat the current last place is not added (returns false).
     */
    fun insert(s: ScoreRecord): Boolean {
        db.beginTransaction()
        try {
            val cutoff = db.rawQuery(
                """SELECT score FROM score WHERE song_id = ? AND difficulty = ?
                   ORDER BY score DESC, played_at ASC, id ASC LIMIT 1 OFFSET ${MAX_SCORES_PER_DIFFICULTY - 1}""",
                arrayOf(s.songId.toString(), s.difficulty)
            ).use { if (it.moveToFirst()) it.getLong(0) else null }
            if (cutoff != null && s.score <= cutoff) return false

            db.insertOrThrow("score", null, ContentValues().apply {
                put("player_name", s.playerName)
                put("song_id", s.songId)
                put("difficulty", s.difficulty)
                put("score", s.score)
                put("accuracy", s.accuracy)
                put("rank", s.rank)
                put("true_combo", s.trueCombo)
                put("max_combo", s.maxCombo)
                put("played_at", if (s.playedAt > 0) s.playedAt else System.currentTimeMillis())
            })
            db.execSQL(
                """DELETE FROM score WHERE song_id = ? AND difficulty = ? AND id NOT IN (
                       SELECT id FROM score WHERE song_id = ? AND difficulty = ?
                       ORDER BY score DESC, played_at ASC, id ASC LIMIT $MAX_SCORES_PER_DIFFICULTY)""",
                arrayOf<Any>(s.songId, s.difficulty, s.songId, s.difficulty)
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Leaderboard for ONE song at ONE difficulty, ordered from highest to lowest score.
     * Every recorded play is listed separately (ties are ordered by the earliest achievement).
     */
    fun topScores(songId: Long, difficulty: String, limit: Int = MAX_SCORES_PER_DIFFICULTY): List<ScoreRecord> =
        db.rawQuery(
            """SELECT s.player_name, s.song_id, s.difficulty, s.score, s.accuracy, s."rank",
                      s.true_combo, s.max_combo, s.played_at
               FROM score s
               WHERE s.song_id = ? AND s.difficulty = ?
               ORDER BY s.score DESC, s.played_at ASC, s.id ASC
               LIMIT ?""",
            arrayOf(songId.toString(), difficulty, limit.toString())
        ).use { it.toRecords() }

    fun bestScore(songId: Long, difficulty: String, playerName: String): ScoreRecord? =
        db.rawQuery(
            """SELECT player_name, song_id, difficulty, score, accuracy, "rank", true_combo, max_combo, played_at
               FROM score
               WHERE song_id = ? AND difficulty = ? AND player_name = ?
               ORDER BY score DESC, played_at ASC, id ASC LIMIT 1""",
            arrayOf(songId.toString(), difficulty, playerName)
        ).use { it.toRecords().firstOrNull() }

    /** Best score for each difficulty in one song for a player. */
    fun bestScores(songId: Long, playerName: String): List<ScoreRecord> =
        db.rawQuery(
            """SELECT player_name, song_id, difficulty, score, accuracy, "rank", true_combo, max_combo, played_at
               FROM score s
               WHERE s.song_id = ? AND s.player_name = ?
                 AND s.id = (
                     SELECT s2.id FROM score s2
                     WHERE s2.song_id = s.song_id AND s2.difficulty = s.difficulty
                       AND s2.player_name = s.player_name
                     ORDER BY s2.score DESC, s2.played_at ASC, s2.id ASC LIMIT 1)
               ORDER BY s.difficulty""",
            arrayOf(songId.toString(), playerName)
        ).use { it.toRecords() }

    private fun Cursor.toRecords(): List<ScoreRecord> {
        val out = ArrayList<ScoreRecord>()
        while (moveToNext()) {
            out += ScoreRecord(
                playerName = getString(0),
                songId = getLong(1),
                difficulty = getString(2),
                score = getLong(3),
                accuracy = getDouble(4),
                rank = getString(5),
                trueCombo = if (isNull(6)) null else getInt(6),
                maxCombo = if (isNull(7)) null else getInt(7),
                playedAt = getLong(8),
            )
        }
        return out
    }

    override fun close() = db.close()

    companion object {
        const val MAX_SCORES_PER_DIFFICULTY = 50
        private const val SCHEMA_VERSION = 5

        fun open(file: File): ScoreDatabase {
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            try {
                if (db.version < SCHEMA_VERSION) {
                    db.transaction {
                        if (db.version < 1) {
                            db.execSQL(
                                """CREATE TABLE IF NOT EXISTS score (
                                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                    player_name TEXT    NOT NULL,
                                    song_id     INTEGER NOT NULL,
                                    difficulty  TEXT    NOT NULL DEFAULT '',
                                    score       INTEGER NOT NULL,
                                    accuracy    REAL    NOT NULL,
                                    "rank"      TEXT    NOT NULL,
                                    true_combo INTEGER,
                                    max_combo   INTEGER,
                                    played_at   INTEGER NOT NULL DEFAULT 0
                                )"""
                            )
                        } else if (db.version < 2) {
                            db.execSQL("ALTER TABLE score ADD COLUMN difficulty TEXT NOT NULL DEFAULT ''")
                            db.execSQL("ALTER TABLE score ADD COLUMN played_at INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("DROP INDEX IF EXISTS idx_score_song")
                        }
                        if (db.version in 1 until 3) {
                            db.execSQL("ALTER TABLE score ADD COLUMN max_combo INTEGER")
                        }
                        if (db.version in 1 until 4) {
                            db.execSQL("ALTER TABLE score ADD COLUMN true_combo INTEGER")
                        }
                        if (db.version in 1 until 5) {
                            db.execSQL("UPDATE score SET true_combo = max_combo, max_combo = NULL")
                        }
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS idx_score_song_diff ON score(song_id, difficulty, score DESC)"
                        )
                        db.version = SCHEMA_VERSION
                    }
                }
            } catch (t: Throwable) {
                db.close(); throw t
            }
            return ScoreDatabase(db)
        }
    }
}
