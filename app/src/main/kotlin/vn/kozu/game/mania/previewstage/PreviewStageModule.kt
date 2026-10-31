package vn.kozu.game.mania.previewstage

import android.content.Context
import android.util.Log
import vn.kozu.game.mania.previewstage.audio.PreviewPlayer
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.model.StartupReport
import vn.kozu.game.mania.previewstage.storage.KozuDatabase
import vn.kozu.game.mania.previewstage.storage.ModulePaths
import vn.kozu.game.mania.previewstage.storage.ScoreDatabase
import vn.kozu.game.mania.previewstage.storage.SongImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class PreviewStageConfig(
    /** Module folder. Defaults to <externalFilesDir>/addmap. */
    val rootDir: File? = null,
    /** Accepts only Mode: 3 (mania) difficulties. */
    val onlyMania: Boolean = true,
    /** true = .osz files still in downloads overwrite existing versions on each startup. */
    val reimportExisting: Boolean = false,
    val maxExtractBytes: Long = 1L shl 30,
)

/**
 * The single entry point for the PreviewStage module.
 *
 *   val module = PreviewStageModule.get(context)
 *   val report = module.start()          // suspend, runs once: create directories/db -> import .osz -> load into memory
 *   module.catalog.all()                 // song list in memory
 *   module.playPreview(song)             // play at PreviewTime
 */
class PreviewStageModule private constructor(
    context: Context,
    config: PreviewStageConfig,
) {
    private val appContext = context.applicationContext
    private val config = config

    val paths: ModulePaths = ModulePaths(
        config.rootDir ?: File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "maniamodule")
    )
    val catalog = KozuCatalog()

    private val startLock = Mutex()
    @Volatile private var report: StartupReport? = null
    private var kozuDb: KozuDatabase? = null
    private var scoreDb: ScoreDatabase? = null
    private val player = PreviewPlayer(appContext)

    /** Player scores (null until [start] completes). */
    val scores: ScoreDatabase? get() = scoreDb

    /** Idempotent: multiple calls (rotation, recomposition) still run only once. */
    suspend fun start(): StartupReport = startLock.withLock {
        report ?: withContext(Dispatchers.IO) { doStart() }.also { report = it }
    }

    suspend fun refresh(): StartupReport = startLock.withLock {
        if (report == null) {
            withContext(Dispatchers.IO) { doStart() }.also { report = it }
        } else {
            withContext(Dispatchers.IO) { refreshCatalog() }.also { report = it }
        }
    }

    suspend fun importArchive(archive: File): StartupReport = startLock.withLock {
        if (report == null) {
            withContext(Dispatchers.IO) { doStart() }.also { report = it }
        }

        val kozu = kozuDb ?: error("The chart database is not ready")
        val t0 = System.currentTimeMillis()
        val summary = withContext(Dispatchers.IO) {
            SongImporter(
                paths = paths,
                kozu = kozu,
                onlyMania = config.onlyMania,
                reimportExisting = config.reimportExisting,
                maxExtractBytes = config.maxExtractBytes,
            ).importArchive(archive).also { result ->
                result.failed.forEach { Log.w(TAG, "Import failed for ${it.fileName}: ${it.reason}") }
            }
        }
        (summary.imported + summary.skipped).distinct().forEach { songId ->
            kozu.loadSong(songId, paths.songs)?.let(catalog::upsert)
        }
        if (summary.failed.isNotEmpty()) {
            throw IOException(summary.failed.joinToString("; ") { "${it.fileName}: ${it.reason}" })
        }
        buildReport(summary, catalog.all(), t0).also { report = it }
    }

    suspend fun deleteSong(songId: Long) = startLock.withLock {
        val kozu = kozuDb ?: throw IOException("The chart database is not ready")
        val songDir = paths.songDir(songId)
        val stagedDir = File(paths.songs, "${ModulePaths.TMP_PREFIX}delete_${UUID.randomUUID()}")
        val staged = withContext(Dispatchers.IO) {
            if (songDir.exists() && !songDir.renameTo(stagedDir)) {
                throw IOException("Could not stage the chart files for deletion")
            }
            try {
                if (!kozu.deleteSong(songId)) {
                    throw IOException("This chart is no longer in the library")
                }
            } catch (e: Exception) {
                if (stagedDir.exists() && !stagedDir.renameTo(songDir)) {
                    e.addSuppressed(IOException("Could not restore chart files after deletion failed"))
                }
                throw e
            }
            stagedDir.exists()
        }

        catalog.remove(songId)
        report = report?.copy(
            loadedSongs = catalog.all().size,
            loadedDifficulties = catalog.all().sumOf { it.difficulties.size },
        )
        if (staged && !withContext(Dispatchers.IO) { stagedDir.deleteRecursively() }) {
            Log.w(TAG, "Could not remove staged chart directory ${stagedDir.path}; it will be cleaned up on next startup")
        }
    }

    private fun doStart(): StartupReport {
        val t0 = System.currentTimeMillis()

        // 1) Directories + 2 databases
        paths.ensureLayout()
        val kozu = KozuDatabase.open(paths.kozuDb)
        val score = ScoreDatabase.open(paths.scoreDb)
        kozuDb = kozu
        scoreDb = score

        // 2) Scan downloads -> extract -> write kozu.db
        val summary = SongImporter(
            paths = paths,
            kozu = kozu,
            onlyMania = config.onlyMania,
            reimportExisting = config.reimportExisting,
            maxExtractBytes = config.maxExtractBytes,
        ).scanDownloads()
        summary.failed.forEach { Log.w(TAG, "Import failed for ${it.fileName}: ${it.reason}") }

        // 3) Load kozu.db into memory
        val songs = kozu.loadAll(paths.songs)
        catalog.replace(songs)

        return buildReport(summary, songs, t0)
    }

    private fun refreshCatalog(): StartupReport {
        paths.ensureLayout()
        val kozu = kozuDb ?: error("The chart database is not ready")
        val t0 = System.currentTimeMillis()
        val summary = SongImporter(
            paths = paths,
            kozu = kozu,
            onlyMania = config.onlyMania,
            reimportExisting = config.reimportExisting,
            maxExtractBytes = config.maxExtractBytes,
        ).scanDownloads()
        summary.failed.forEach { Log.w(TAG, "Import failed for ${it.fileName}: ${it.reason}") }
        val songs = kozu.loadAll(paths.songs)
        catalog.replace(songs)
        return buildReport(summary, songs, t0)
    }

    private fun buildReport(
        summary: vn.kozu.game.mania.previewstage.storage.ImportSummary,
        songs: List<SongEntry>,
        startedAt: Long,
    ) = StartupReport(
        rootDir = paths.root,
        importedSongIds = summary.imported,
        skippedSongIds = summary.skipped,
        failed = summary.failed,
        loadedSongs = songs.size,
        loadedDifficulties = songs.sumOf { it.difficulties.size },
        elapsedMs = System.currentTimeMillis() - startedAt,
    ).also { Log.i(TAG, "PreviewStage ready: $it") }

    /** Plays the song preview (first difficulty with audio). Call from the main thread. */
    fun playPreview(song: SongEntry, onError: (String) -> Unit = {}) {
        val diff = song.defaultPreview ?: return onError("No audio file is available for chart ${song.songId}")
        playPreview(song, diff, onError)
    }

    /** Plays the preview for a specific difficulty. Call from the main thread. */
    fun playPreview(song: SongEntry, difficulty: DifficultyRecord, onError: (String) -> Unit = {}) {
        val audio = difficulty.audioFile(song.folder)
            ?: return onError("Audio file '${difficulty.audioFilename}' could not be found")
        player.play(audio, difficulty.previewTime, onError)
    }

    fun playPreviewUrl(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onError: (String) -> Unit = {},
    ) {
        player.playUrl(url, headers, onError)
    }

    fun stopPreview() = player.stop()

    companion object {
        private const val TAG = "PreviewStage"

        @Volatile private var instance: PreviewStageModule? = null

        fun get(context: Context, config: PreviewStageConfig = PreviewStageConfig()): PreviewStageModule =
            instance ?: synchronized(this) {
                instance ?: PreviewStageModule(context, config).also { instance = it }
            }
    }
}
