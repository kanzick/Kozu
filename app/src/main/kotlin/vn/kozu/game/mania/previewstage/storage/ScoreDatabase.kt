package vn.kozu.game.mania.previewstage.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import java.io.Closeable
import java.io.File
import vn.kozu.game.mania.previewstage.model.ScoreRecord

class ScoreDatabase private constructor(private val db: SQLiteDatabase) : Closeable {


    fun insert(s: ScoreRecord): Boolean {
        db.beginTransaction()
        try {
            val playedAt = if (s.playedAt > 0) s.playedAt else System.currentTimeMillis()
            val existingPlayedAt = db.rawQuery(
                """SELECT played_at FROM score_play_history
                   WHERE player_name = ? AND song_id = ? AND difficulty = ?""",
                arrayOf(s.playerName, s.songId.toString(), s.difficulty),
            ).use { if (it.moveToFirst()) it.getLong(0) else null }
            if (existingPlayedAt == null) {
                db.insertOrThrow("score_play_history", null, ContentValues().apply {
                    put("player_name", s.playerName)
                    put("song_id", s.songId)
                    put("difficulty", s.difficulty)
                    put("played_at", playedAt)
                })
            } else if (playedAt > existingPlayedAt) {
                db.update(
                    "score_play_history",
                    ContentValues().apply { put("played_at", playedAt) },
                    "player_name = ? AND song_id = ? AND difficulty = ?",
                    arrayOf(s.playerName, s.songId.toString(), s.difficulty),
                )
            }
            val personalBest = db.rawQuery(
                """SELECT score, accuracy FROM score_personal_best
                   WHERE player_name = ? AND song_id = ? AND difficulty = ?""",
                arrayOf(s.playerName, s.songId.toString(), s.difficulty),
            ).use { if (it.moveToFirst()) it.getLong(0) to it.getDouble(1) else null }
            if (personalBest == null || s.score > personalBest.first ||
                (s.score == personalBest.first && s.accuracy > personalBest.second)
            ) {
                db.insertWithOnConflict(
                    "score_personal_best",
                    null,
                    ContentValues().apply {
                        put("player_name", s.playerName)
                        put("song_id", s.songId)
                        put("difficulty", s.difficulty)
                        put("score", s.score)
                        put("accuracy", s.accuracy)
                        put("rank", s.rank)
                        put("true_combo", s.trueCombo)
                        put("max_combo", s.maxCombo)
                        put("played_at", playedAt)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            val cutoff = db.rawQuery(
                """SELECT score FROM score WHERE song_id = ? AND difficulty = ?
                   ORDER BY score DESC, played_at ASC, id ASC LIMIT 1 OFFSET ${MAX_SCORES_PER_DIFFICULTY - 1}""",
                arrayOf(s.songId.toString(), s.difficulty)
            ).use { if (it.moveToFirst()) it.getLong(0) else null }
            if (cutoff != null && s.score <= cutoff) {
                db.setTransactionSuccessful()
                return false
            }

            db.insertOrThrow("score", null, ContentValues().apply {
                put("player_name", s.playerName)
                put("song_id", s.songId)
                put("difficulty", s.difficulty)
                put("score", s.score)
                put("accuracy", s.accuracy)
                put("rank", s.rank)
                put("true_combo", s.trueCombo)
                put("max_combo", s.maxCombo)
                put("played_at", playedAt)
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
               FROM score_personal_best
               WHERE song_id = ? AND difficulty = ? AND player_name = ?
               LIMIT 1""",
            arrayOf(songId.toString(), difficulty, playerName)
        ).use { it.toRecords().firstOrNull() }


    fun bestScores(songId: Long, playerName: String): List<ScoreRecord> =
        db.rawQuery(
            """SELECT player_name, song_id, difficulty, score, accuracy, "rank", true_combo, max_combo, played_at
               FROM score_personal_best
               WHERE song_id = ? AND player_name = ?
               ORDER BY difficulty""",
            arrayOf(songId.toString(), playerName)
        ).use { it.toRecords() }

    fun latestPlayedAt(songId: Long, playerName: String): Map<String, Long> =
        db.rawQuery(
            """SELECT difficulty, played_at FROM score_play_history
               WHERE song_id = ? AND player_name = ?""",
            arrayOf(songId.toString(), playerName),
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.getLong(1))
            }
        }

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
        private const val SCHEMA_VERSION = 7

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
                            """CREATE TABLE IF NOT EXISTS score_play_history (
                                player_name TEXT NOT NULL,
                                song_id INTEGER NOT NULL,
                                difficulty TEXT NOT NULL,
                                played_at INTEGER NOT NULL,
                                PRIMARY KEY (player_name, song_id, difficulty)
                            )"""
                        )
                        if (db.version < 6) {
                            db.execSQL(
                                """INSERT OR REPLACE INTO score_play_history (player_name, song_id, difficulty, played_at)
                                   SELECT player_name, song_id, difficulty, MAX(played_at)
                                   FROM score GROUP BY player_name, song_id, difficulty"""
                            )
                        }
                        db.execSQL(
                            """CREATE TABLE IF NOT EXISTS score_personal_best (
                                player_name TEXT NOT NULL,
                                song_id INTEGER NOT NULL,
                                difficulty TEXT NOT NULL,
                                score INTEGER NOT NULL,
                                accuracy REAL NOT NULL,
                                "rank" TEXT NOT NULL,
                                true_combo INTEGER,
                                max_combo INTEGER,
                                played_at INTEGER NOT NULL,
                                PRIMARY KEY (player_name, song_id, difficulty)
                            )"""
                        )
                        if (db.version < 7) {
                            db.execSQL(
                                """INSERT OR REPLACE INTO score_personal_best
                                   (player_name, song_id, difficulty, score, accuracy, "rank", true_combo, max_combo, played_at)
                                   SELECT s.player_name, s.song_id, s.difficulty, s.score, s.accuracy, s."rank",
                                          s.true_combo, s.max_combo, s.played_at
                                   FROM score s
                                   WHERE s.id = (
                                       SELECT s2.id FROM score s2
                                       WHERE s2.player_name = s.player_name AND s2.song_id = s.song_id
                                         AND s2.difficulty = s.difficulty
                                       ORDER BY s2.score DESC, s2.accuracy DESC, s2.played_at ASC, s2.id ASC LIMIT 1
                                   )"""
                            )
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
