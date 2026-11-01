package vn.kozu.game.mania.previewstage.model

import java.io.File

data class DifficultyRecord(
    val songId: Long,
    val osuFile: String,

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

    val hpDrainRate: Double,
    val circleSize: Double,
    val overallDifficulty: Double,
    val approachRate: Double,
    val sliderMultiplier: Double,
    val sliderTickRate: Double,
    val starRating: Double = 0.0,

    val backgroundFilename: String?,
    val audioPath: String?,
    val audioExists: Boolean,
    val backgroundPath: String?,
    val backgroundExists: Boolean,
) {

    fun audioFile(songDir: File): File? =
        audioPath?.takeIf { audioExists }?.let { File(songDir, it) }

    fun backgroundFile(songDir: File): File? =
        backgroundPath?.takeIf { backgroundExists }?.let { File(songDir, it) }
}

data class SongEntry(
    val songId: Long,
    val folder: File,
    val difficulties: List<DifficultyRecord>,
) {

    val defaultPreview: DifficultyRecord?
        get() = difficulties.firstOrNull { it.audioFile(folder) != null }

    val displayTitle: String get() = difficulties.firstOrNull()?.title.orEmpty()
    val displayArtist: String get() = difficulties.firstOrNull()?.artist.orEmpty()
}

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
