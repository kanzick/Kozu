package vn.kozu.game.mania.previewstage

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.kozu.game.mania.previewstage.audio.PreviewPlayer
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.model.StartupReport
import vn.kozu.game.mania.previewstage.storage.KozuDatabase
import vn.kozu.game.mania.previewstage.storage.ModulePaths
import vn.kozu.game.mania.previewstage.storage.ScoreDatabase
import vn.kozu.game.mania.previewstage.storage.SongImporter

data class PreviewStageConfig(

    val rootDir: File? = null,

    val onlyMania: Boolean = true,

    val reimportExisting: Boolean = false,
    val maxExtractBytes: Long = 1L shl 30,
)

class PreviewStageModule private constructor(
    context: Context,
    config: PreviewStageConfig,
) {
    private val appContext = context.applicationContext
    private val config = config

    val paths: ModulePaths = ModulePaths(config.rootDir ?: defaultRootDir(appContext))
    val catalog = KozuCatalog()

    private val startLock = Mutex()
    @Volatile private var report: StartupReport? = null
    private var kozuDb: KozuDatabase? = null
    private var scoreDb: ScoreDatabase? = null
    private val player = PreviewPlayer(appContext)

    val scores: ScoreDatabase? get() = scoreDb

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

    suspend fun exportDataFiles(consume: (File) -> Unit) = startLock.withLock {
        val wasStarted = kozuDb != null || scoreDb != null
        closeDatabases()
        try {
            consume(paths.root)
        } finally {
            if (wasStarted) openDatabases()
        }
    }

    suspend fun restoreDataFiles(source: File) = startLock.withLock {
        if (!source.isDirectory) throw IOException("The game backup is missing its data folder")
        val wasStarted = kozuDb != null || scoreDb != null
        stopPreview()
        closeDatabases()

        val parent = paths.root.parentFile ?: throw IOException("The game data folder is unavailable")
        if (!parent.exists() && !parent.mkdirs()) {
            if (wasStarted) openDatabases()
            throw IOException("Could not prepare the game data folder")
        }
        val stamp = System.currentTimeMillis()
        val staging = File(parent, ".${paths.root.name}.restore-$stamp")
        val previous = File(parent, ".${paths.root.name}.previous-$stamp")
        var previousMoved = false
        var restoredInstalled = false

        try {
            staging.deleteRecursively()
            source.copyRecursively(staging, overwrite = true)

            if (paths.root.exists()) {
                if (!paths.root.renameTo(previous)) throw IOException("Could not stage the current game data")
                previousMoved = true
            }
            if (!staging.renameTo(paths.root)) throw IOException("Could not install the restored game data")
            restoredInstalled = true

            if (wasStarted) report = doStart()
            previous.deleteRecursively()
        } catch (error: Exception) {
            closeDatabases()
            if (restoredInstalled) paths.root.deleteRecursively()
            if (previousMoved && previous.exists() && !previous.renameTo(paths.root)) {
                error.addSuppressed(IOException("Could not roll back the previous game data"))
            }
            if (wasStarted) {
                try {
                    report = doStart()
                } catch (restoreError: Exception) {
                    error.addSuppressed(restoreError)
                }
            }
            throw error
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun closeDatabases() {
        scoreDb?.close()
        scoreDb = null
        kozuDb?.close()
        kozuDb = null
    }

    private fun openDatabases() {
        paths.ensureLayout()
        kozuDb = KozuDatabase.open(paths.kozuDb)
        scoreDb = ScoreDatabase.open(paths.scoreDb)
    }

    suspend fun collectionNames(): List<String> = startLock.withLock {
        val kozu = kozuDb ?: throw IOException("The chart database is not ready")
        withContext(Dispatchers.IO) { kozu.collectionNames() }
    }

    suspend fun setSongCollections(songId: Long, names: List<String>) = startLock.withLock {
        val kozu = kozuDb ?: throw IOException("The chart database is not ready")
        withContext(Dispatchers.IO) {
            kozu.setSongCollections(songId, names)
            kozu.loadSong(songId, paths.songs)?.let(catalog::upsert)
        }
    }

    suspend fun updateStarRatings(songId: Long, ratings: Map<Long, Double>): Int = startLock.withLock {
        val kozu = kozuDb ?: throw IOException("The chart database is not ready")
        val updated = withContext(Dispatchers.IO) { kozu.updateStarRatings(songId, ratings) }
        if (updated > 0) kozu.loadSong(songId, paths.songs)?.let(catalog::upsert)
        updated
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

        paths.ensureLayout()
        val kozu = KozuDatabase.open(paths.kozuDb)
        val score = ScoreDatabase.open(paths.scoreDb)
        kozuDb = kozu
        scoreDb = score

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

    fun playPreview(song: SongEntry, onError: (String) -> Unit = {}) {
        val diff = song.defaultPreview ?: return onError("No audio file is available for chart ${song.songId}")
        playPreview(song, diff, onError)
    }

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

        fun defaultRootDir(context: Context): File =
            File(context.applicationContext.getExternalFilesDir(null) ?: context.applicationContext.filesDir, "addmap")

        fun peek(): PreviewStageModule? = instance

        fun get(context: Context, config: PreviewStageConfig = PreviewStageConfig()): PreviewStageModule =
            instance ?: synchronized(this) {
                instance ?: PreviewStageModule(context, config).also { instance = it }
            }
    }
}
