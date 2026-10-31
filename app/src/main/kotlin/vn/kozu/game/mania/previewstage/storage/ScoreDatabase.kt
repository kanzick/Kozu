package vn.kozu.game.mania.previewstage.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import java.io.Closeable
import java.io.File

/**
 * score.db: player_name, song_id, difficulty, score, accuracy (%), rank, played_at.
 *
 * Schema v2 (v1 did not include difficulty):
 *  - v1 -> v2 adds the `difficulty` and `played_at` columns. Existing rows retain difficulty = '',
 *    so they do not match any difficulty leaderboard (v1 did not record which difficulty a score belonged to).
 */
class ScoreDatabase private constructor(private val db: SQLiteDatabase) : Closeable {

    /** Records a play session. All sessions are retained; the leaderboard shows each player's best score. */
    fun insert(s: ScoreRecord): Long = db.insertOrThrow("score", null, ContentValues().apply {
        put("player_name", s.playerName)
        put("song_id", s.songId)
        put("difficulty", s.difficulty)
        put("score", s.score)
        put("accuracy", s.accuracy)
        put("rank", s.rank)
        put("played_at", if (s.playedAt > 0) s.playedAt else System.currentTimeMillis())
    })

    /**
     * Leaderboard for ONE song at ONE difficulty, ordered from highest to lowest score.
     * Each player appears once with their best score (ties are ordered by the earliest achievement).
     */
    fun topScores(songId: Long, difficulty: String, limit: Int = 10): List<ScoreRecord> =
        db.rawQuery(
            """SELECT s.player_name, s.song_id, s.difficulty, s.score, s.accuracy, s."rank", s.played_at
               FROM score s
               WHERE s.song_id = ? AND s.difficulty = ?
                 AND s.id = (
                     SELECT s2.id FROM score s2
                     WHERE s2.song_id = s.song_id AND s2.difficulty = s.difficulty
                       AND s2.player_name = s.player_name
                     ORDER BY s2.score DESC, s2.played_at ASC, s2.id ASC LIMIT 1)
               ORDER BY s.score DESC, s.played_at ASC, s.id ASC
               LIMIT ?""",
            arrayOf(songId.toString(), difficulty, limit.toString())
        ).use { it.toRecords() }

    /** A player's best score for this song and difficulty (null if they have not played). */
    fun bestScore(songId: Long, difficulty: String, playerName: String): ScoreRecord? =
        db.rawQuery(
            """SELECT player_name, song_id, difficulty, score, accuracy, "rank", played_at
               FROM score
               WHERE song_id = ? AND difficulty = ? AND player_name = ?
               ORDER BY score DESC, played_at ASC, id ASC LIMIT 1""",
            arrayOf(songId.toString(), difficulty, playerName)
        ).use { it.toRecords().firstOrNull() }

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
                playedAt = getLong(6),
            )
        }
        return out
    }

    override fun close() = db.close()

    companion object {
        private const val SCHEMA_VERSION = 2

        fun open(file: File): ScoreDatabase {
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            try {
                if (db.version < SCHEMA_VERSION) {
                    db.beginTransaction()
                    try {
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
                                    played_at   INTEGER NOT NULL DEFAULT 0
                                )"""
                            )
                        } else {
                            // v1 -> v2
                            db.execSQL("ALTER TABLE score ADD COLUMN difficulty TEXT NOT NULL DEFAULT ''")
                            db.execSQL("ALTER TABLE score ADD COLUMN played_at INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("DROP INDEX IF EXISTS idx_score_song")
                        }
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS idx_score_song_diff ON score(song_id, difficulty, score DESC)"
                        )
                        db.version = SCHEMA_VERSION
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            } catch (t: Throwable) {
                db.close(); throw t
            }
            return ScoreDatabase(db)
        }
    }
}
