package vn.kozu.game.mania.previewstage.model

import java.io.File

/**
 * Một độ khó (1 file .osu) đúng như được lưu trong bảng `kozu_difficulty`.
 * Các đường dẫn *Path luôn là đường dẫn TƯƠNG ĐỐI so với thư mục songs/<songId>/ (dùng dấu '/').
 */
data class DifficultyRecord(
    val songId: Long,
    val osuFile: String,

    // [General]
    val audioFilename: String,
    val audioLeadIn: Int,
    val previewTime: Int,
    val countdown: Int,
    val sampleSet: String,
    val stackLeniency: Double,
    val mode: Int,
    val letterboxInBreaks: Int,
    val widescreenStoryboard: Int,
    val specialStyle: Int,

    // [Metadata]
    val title: String,
    val titleUnicode: String,
    val artist: String,
    val artistUnicode: String,
    val creator: String,
    val version: String,
    val source: String,
    val tags: String,
    val beatmapId: Long,
    val beatmapSetId: Long,

    // [Difficulty]
    val hpDrainRate: Double,
    val circleSize: Double,
    val overallDifficulty: Double,
    val approachRate: Double,
    val sliderMultiplier: Double,
    val sliderTickRate: Double,

    // [Events] + thông tin file đã được resolve trên đĩa
    val backgroundFilename: String?,
    val audioPath: String?,
    val audioExists: Boolean,
    val backgroundPath: String?,
    val backgroundExists: Boolean,
) {
    /** File âm thanh tuyệt đối (null nếu không tồn tại). */
    fun audioFile(songDir: File): File? =
        audioPath?.takeIf { audioExists }?.let { File(songDir, it) }

    /** File ảnh nền tuyệt đối (null nếu không có / không tồn tại). */
    fun backgroundFile(songDir: File): File? =
        backgroundPath?.takeIf { backgroundExists }?.let { File(songDir, it) }
}

/** Một bài nhạc (kozu id = BeatmapSetID) trong RAM. */
data class SongEntry(
    val songId: Long,
    val folder: File,
    val difficulties: List<DifficultyRecord>,
) {
    /** Độ khó dùng để phát preview mặc định: cái đầu tiên có audio. */
    val defaultPreview: DifficultyRecord?
        get() = difficulties.firstOrNull { it.audioFile(folder) != null }

    val displayTitle: String get() = difficulties.firstOrNull()?.title.orEmpty()
    val displayArtist: String get() = difficulties.firstOrNull()?.artist.orEmpty()
}

/**
 * Một dòng trong score.db. Mỗi bài có nhiều độ khó nên điểm gắn với cả [songId] lẫn [difficulty]
 * (= DifficultyRecord.version, tên độ khó, duy nhất trong một bộ beatmap).
 *
 * [accuracy] tính theo phần trăm (0..100). [playedAt] là epoch millis; 0 = để database tự điền lúc ghi.
 */
data class ScoreRecord(
    val playerName: String,
    val songId: Long,
    val difficulty: String,
    val score: Long,
    val accuracy: Double,
    val rank: String,
    val playedAt: Long = 0L,
)

data class ImportFailure(val fileName: String, val reason: String)

data class StartupReport(
    val rootDir: File,
    val importedSongIds: List<Long>,
    val skippedSongIds: List<Long>,
    val failed: List<ImportFailure>,
    val loadedSongs: Int,
    val loadedDifficulties: Int,
    val elapsedMs: Long,
)
