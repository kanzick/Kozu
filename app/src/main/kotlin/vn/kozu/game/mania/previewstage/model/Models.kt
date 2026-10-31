package vn.kozu.game.mania.previewstage.model

import java.io.File

/**
 * A difficulty (one .osu file), as stored in the `kozu_difficulty` table.
 * All *Path values are RELATIVE to songs/<songId>/ (using '/').
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

    // [Events] + file information resolved on disk
    val backgroundFilename: String?,
    val audioPath: String?,
    val audioExists: Boolean,
    val backgroundPath: String?,
    val backgroundExists: Boolean,
) {
    /** Absolute audio file (null if it does not exist). */
    fun audioFile(songDir: File): File? =
        audioPath?.takeIf { audioExists }?.let { File(songDir, it) }

    /** Absolute background image file (null if absent or nonexistent). */
    fun backgroundFile(songDir: File): File? =
        backgroundPath?.takeIf { backgroundExists }?.let { File(songDir, it) }
}

/** A song (kozu ID = BeatmapSetID) held in memory. */
data class SongEntry(
    val songId: Long,
    val folder: File,
    val difficulties: List<DifficultyRecord>,
) {
    /** Difficulty used for the default preview: the first one with audio. */
    val defaultPreview: DifficultyRecord?
        get() = difficulties.firstOrNull { it.audioFile(folder) != null }

    val displayTitle: String get() = difficulties.firstOrNull()?.title.orEmpty()
    val displayArtist: String get() = difficulties.firstOrNull()?.artist.orEmpty()
}

/**
 * A row in score.db. Each song has multiple difficulties, so scores are associated with both [songId] and [difficulty]
 * (= DifficultyRecord.version, the difficulty name, unique within a beatmap set).
 *
 * [accuracy] is a percentage (0..100). [playedAt] is epoch millis; 0 lets the database fill it in when writing.
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
