package vn.kozu.game.mania.previewstage.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.osu.FileResolver
import java.io.Closeable
import java.io.File

/** kozu.db: mỗi bài (kozu id = BeatmapSetID) có N độ khó. */
class KozuDatabase private constructor(private val db: SQLiteDatabase) : Closeable {

    fun hasSong(songId: Long): Boolean =
        db.rawQuery("SELECT 1 FROM kozu_song WHERE song_id = ?", arrayOf(songId.toString())).use { it.moveToFirst() }

    /** Existing DB rows are reusable only while their beatmap and referenced assets still exist. */
    fun hasCompleteSongFiles(songId: Long, songDir: File): Boolean {
        var found = false
        db.rawQuery(
            """SELECT osu_file, audio_filename, background_filename
               FROM kozu_difficulty WHERE song_id = ?""",
            arrayOf(songId.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                found = true
                val osuFile = File(songDir, c.getString(0))
                if (!osuFile.isFile) return false
                val baseDir = osuFile.parentFile ?: songDir
                val audioFilename = c.getString(1).orEmpty()
                if (audioFilename.isNotBlank() && FileResolver.resolve(baseDir, audioFilename) == null) return false
                val backgroundFilename = if (c.isNull(2)) null else c.getString(2)
                if (!backgroundFilename.isNullOrBlank() &&
                    FileResolver.resolve(baseDir, backgroundFilename) == null
                ) return false
            }
        }
        return found
    }

    /** Ghi đè toàn bộ dữ liệu của 1 bài trong 1 transaction. */
    fun replaceSong(songId: Long, sourceOsz: String, difficulties: List<DifficultyRecord>) {
        db.beginTransaction()
        try {
            db.delete("kozu_song", "song_id = ?", arrayOf(songId.toString())) // cascade xoá độ khó cũ
            db.insertOrThrow("kozu_song", null, ContentValues().apply {
                put("song_id", songId)
                put("source_osz", sourceOsz)
                put("difficulty_count", difficulties.size)
                put("imported_at", System.currentTimeMillis())
            })
            for (d in difficulties) db.insertOrThrow("kozu_difficulty", null, d.toValues())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteSong(songId: Long): Boolean {
        db.beginTransaction()
        return try {
            val deleted = db.delete("kozu_song", "song_id = ?", arrayOf(songId.toString())) > 0
            db.setTransactionSuccessful()
            deleted
        } finally {
            db.endTransaction()
        }
    }

    /** Xóa các độ khó ngoài mania và bài không còn độ khó nào khỏi database. */
    fun removeNonManiaDifficulties() {
        db.beginTransaction()
        try {
            db.delete("kozu_difficulty", "mode != ?", arrayOf("3"))
            db.execSQL(
                """DELETE FROM kozu_song
                   WHERE NOT EXISTS (
                       SELECT 1 FROM kozu_difficulty
                       WHERE kozu_difficulty.song_id = kozu_song.song_id
                   )"""
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Đọc toàn bộ db vào RAM (sắp theo song_id, độ khó theo CircleSize rồi OD). */
    fun loadAll(songsDir: File): List<SongEntry> {
        val byId = LinkedHashMap<Long, MutableList<DifficultyRecord>>()
        db.rawQuery("SELECT song_id FROM kozu_song ORDER BY song_id", null).use { c ->
            while (c.moveToNext()) byId[c.getLong(0)] = ArrayList()
        }
        db.rawQuery(
            "SELECT * FROM kozu_difficulty ORDER BY song_id, overall_difficulty, circle_size, id", null
        ).use { c ->
            while (c.moveToNext()) {
                val rec = c.toRecord()
                byId.getOrPut(rec.songId) { ArrayList() }.add(rec)
            }
        }
        return byId.map { (id, diffs) -> SongEntry(id, File(songsDir, id.toString()), diffs) }
    }

    fun loadSong(songId: Long, songsDir: File): SongEntry? {
        val difficulties = ArrayList<DifficultyRecord>()
        db.rawQuery(
            """SELECT * FROM kozu_difficulty
               WHERE song_id = ?
               ORDER BY overall_difficulty, circle_size, id""",
            arrayOf(songId.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) difficulties += cursor.toRecord()
        }
        return difficulties.takeIf { it.isNotEmpty() }
            ?.let { SongEntry(songId, File(songsDir, songId.toString()), it) }
    }

    override fun close() = db.close()

    private fun DifficultyRecord.toValues() = ContentValues().apply {
        put("song_id", songId); put("osu_file", osuFile)
        put("audio_filename", audioFilename); put("audio_lead_in", audioLeadIn)
        put("preview_time", previewTime); put("countdown", countdown)
        put("sample_set", sampleSet); put("stack_leniency", stackLeniency)
        put("mode", mode); put("letterbox_in_breaks", letterboxInBreaks)
        put("widescreen_storyboard", widescreenStoryboard); put("special_style", specialStyle)
        put("title", title); put("title_unicode", titleUnicode)
        put("artist", artist); put("artist_unicode", artistUnicode)
        put("creator", creator); put("version", version)
        put("source", source); put("tags", tags)
        put("beatmap_id", beatmapId); put("beatmap_set_id", beatmapSetId)
        put("hp_drain_rate", hpDrainRate); put("circle_size", circleSize)
        put("overall_difficulty", overallDifficulty); put("approach_rate", approachRate)
        put("slider_multiplier", sliderMultiplier); put("slider_tick_rate", sliderTickRate)
        put("background_filename", backgroundFilename)
        put("audio_path", audioPath); put("audio_exists", if (audioExists) 1 else 0)
        put("background_path", backgroundPath); put("background_exists", if (backgroundExists) 1 else 0)
    }

    private fun Cursor.toRecord(): DifficultyRecord {
        fun s(n: String) = getString(getColumnIndexOrThrow(n)).orEmpty()
        fun sn(n: String): String? = getColumnIndexOrThrow(n).let { if (isNull(it)) null else getString(it) }
        fun i(n: String) = getInt(getColumnIndexOrThrow(n))
        fun l(n: String) = getLong(getColumnIndexOrThrow(n))
        fun d(n: String) = getDouble(getColumnIndexOrThrow(n))
        return DifficultyRecord(
            songId = l("song_id"), osuFile = s("osu_file"),
            audioFilename = s("audio_filename"), audioLeadIn = i("audio_lead_in"),
            previewTime = i("preview_time"), countdown = i("countdown"),
            sampleSet = s("sample_set"), stackLeniency = d("stack_leniency"),
            mode = i("mode"), letterboxInBreaks = i("letterbox_in_breaks"),
            widescreenStoryboard = i("widescreen_storyboard"), specialStyle = i("special_style"),
            title = s("title"), titleUnicode = s("title_unicode"),
            artist = s("artist"), artistUnicode = s("artist_unicode"),
            creator = s("creator"), version = s("version"),
            source = s("source"), tags = s("tags"),
            beatmapId = l("beatmap_id"), beatmapSetId = l("beatmap_set_id"),
            hpDrainRate = d("hp_drain_rate"), circleSize = d("circle_size"),
            overallDifficulty = d("overall_difficulty"), approachRate = d("approach_rate"),
            sliderMultiplier = d("slider_multiplier"), sliderTickRate = d("slider_tick_rate"),
            backgroundFilename = sn("background_filename"),
            audioPath = sn("audio_path"), audioExists = i("audio_exists") != 0,
            backgroundPath = sn("background_path"), backgroundExists = i("background_exists") != 0,
        )
    }

    companion object {
        private const val SCHEMA_VERSION = 1

        fun open(file: File): KozuDatabase {
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            try {
                db.setForeignKeyConstraintsEnabled(true)
                if (db.version < SCHEMA_VERSION) {
                    db.beginTransaction()
                    try {
                        db.execSQL(CREATE_SONG)
                        db.execSQL(CREATE_DIFFICULTY)
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_kozu_difficulty_song ON kozu_difficulty(song_id)")
                        db.version = SCHEMA_VERSION
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            } catch (t: Throwable) {
                db.close(); throw t
            }
            return KozuDatabase(db)
        }

        private const val CREATE_SONG = """
            CREATE TABLE IF NOT EXISTS kozu_song (
                song_id          INTEGER PRIMARY KEY,
                source_osz       TEXT    NOT NULL,
                difficulty_count INTEGER NOT NULL,
                imported_at      INTEGER NOT NULL
            )"""

        private const val CREATE_DIFFICULTY = """
            CREATE TABLE IF NOT EXISTS kozu_difficulty (
                id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                song_id               INTEGER NOT NULL REFERENCES kozu_song(song_id) ON DELETE CASCADE,
                osu_file              TEXT    NOT NULL,
                -- [General]
                audio_filename        TEXT    NOT NULL,
                audio_lead_in         INTEGER NOT NULL,
                preview_time          INTEGER NOT NULL,
                countdown             INTEGER NOT NULL,
                sample_set            TEXT    NOT NULL,
                stack_leniency        REAL    NOT NULL,
                mode                  INTEGER NOT NULL,
                letterbox_in_breaks   INTEGER NOT NULL,
                widescreen_storyboard INTEGER NOT NULL,
                special_style         INTEGER NOT NULL,
                -- [Metadata]
                title                 TEXT    NOT NULL,
                title_unicode         TEXT    NOT NULL,
                artist                TEXT    NOT NULL,
                artist_unicode        TEXT    NOT NULL,
                creator               TEXT    NOT NULL,
                version               TEXT    NOT NULL,
                source                TEXT    NOT NULL,
                tags                  TEXT    NOT NULL,
                beatmap_id            INTEGER NOT NULL,
                beatmap_set_id        INTEGER NOT NULL,
                -- [Difficulty]
                hp_drain_rate         REAL    NOT NULL,
                circle_size           REAL    NOT NULL,
                overall_difficulty    REAL    NOT NULL,
                approach_rate         REAL    NOT NULL,
                slider_multiplier     REAL    NOT NULL,
                slider_tick_rate      REAL    NOT NULL,
                -- [Events] + file đã resolve (đường dẫn tương đối so với songs/<song_id>/)
                background_filename   TEXT,
                audio_path            TEXT,
                audio_exists          INTEGER NOT NULL,
                background_path       TEXT,
                background_exists     INTEGER NOT NULL,
                UNIQUE (song_id, osu_file)
            )"""
    }
}
