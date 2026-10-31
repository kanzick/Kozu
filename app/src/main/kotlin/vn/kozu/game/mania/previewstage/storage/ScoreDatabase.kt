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
 * Schema v2 (v1 chưa có độ khó):
 *  - v1 -> v2 thêm cột `difficulty` và `played_at`. Các dòng cũ giữ nguyên với difficulty = '' nên
 *    không còn khớp với bảng xếp hạng của độ khó nào (v1 không ghi lại được điểm đó thuộc độ khó nào).
 */
class ScoreDatabase private constructor(private val db: SQLiteDatabase) : Closeable {

    /** Ghi một lượt chơi. Mọi lượt đều được giữ; bảng xếp hạng chỉ hiển thị điểm cao nhất của từng người. */
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
     * Bảng xếp hạng của MỘT bài ở MỘT độ khó, điểm cao xuống thấp.
     * Mỗi người chơi chỉ xuất hiện một lần với điểm cao nhất của họ (hoà điểm: ai đạt trước xếp trên).
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

    /** Điểm cao nhất của một người chơi ở bài + độ khó này (null nếu chưa chơi). */
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
