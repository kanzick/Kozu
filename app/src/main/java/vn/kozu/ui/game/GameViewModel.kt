package vn.kozu.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.download.DownloadManager
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficulty
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.previewstage.PreviewStageConfig
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.ui.containerFactory
import java.io.File

data class GameMapDownloadState(
    val progress: Float? = null,
    val error: String? = null
)

class GameViewModel(private val container: AppContainer) : ViewModel() {

    val previewModule: PreviewStageModule = PreviewStageModule.get(
        context = container.appContext,
        config = PreviewStageConfig(
            rootDir = File(
                container.appContext.getExternalFilesDir(null) ?: container.appContext.filesDir,
                "addmap"
            ),
            onlyMania = true
        )
    )
    val gameModule: GameStageModule = GameStageModule.get(container.appContext, previewModule)

    private val _downloads = MutableStateFlow<Map<Long, GameMapDownloadState>>(emptyMap())
    val downloads: StateFlow<Map<Long, GameMapDownloadState>> = _downloads.asStateFlow()

    private val _localMapIds = MutableStateFlow(previewModule.catalog.all().map { it.songId }.toSet())
    val localMapIds: StateFlow<Set<Long>> = _localMapIds.asStateFlow()
    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val jobs = mutableMapOf<Long, Job>()
    private var previewJob: Job? = null

    suspend fun searchMania(query: String, page: Int) =
        withContext(Dispatchers.IO) { container.catalog.searchMania(query, page) }

    suspend fun maniaDifficulties(beatmapSetId: Long): List<BeatmapDifficulty> =
        container.catalog.maniaDifficulties(beatmapSetId)

    fun playSearchPreview(beatmapSetId: Long) {
        previewJob?.cancel()
        previewModule.stopPreview()
        _previewError.value = null
        previewJob = viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    container.streams.resolvePreview(beatmapSetId)
                }
                previewModule.playPreviewUrl(preview.uri.toString(), preview.requestHeaders) { message ->
                    _previewError.value = message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _previewError.value = e.message ?: "Could not play this map preview"
            }
        }
    }

    fun stopSearchPreview() {
        previewJob?.cancel()
        previewJob = null
        previewModule.stopPreview()
        _previewError.value = null
    }

    fun download(beatmap: Beatmap) {
        if (beatmap.id in _localMapIds.value || jobs[beatmap.id]?.isActive == true) return

        jobs[beatmap.id] = viewModelScope.launch {
            _downloads.update { it + (beatmap.id to GameMapDownloadState(progress = 0f)) }
            try {
                val destination = File(previewModule.paths.downloads, "${beatmap.id}.osz")
                withContext(Dispatchers.IO) {
                    container.downloads.downloadArchive(beatmap, destination) { progress ->
                        _downloads.update {
                            it + (beatmap.id to GameMapDownloadState(progress = progress))
                        }
                    }
                }
                previewModule.importArchive(destination)
                _localMapIds.value = previewModule.catalog.all().map { it.songId }.toSet()
                if (beatmap.id !in _localMapIds.value) {
                    throw IllegalStateException("The downloaded archive contains no valid Mania charts")
                }
                _downloads.update { it + (beatmap.id to GameMapDownloadState(progress = 1f)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _downloads.update {
                    it + (beatmap.id to GameMapDownloadState(error = e.message ?: "Download failed"))
                }
            } finally {
                jobs.remove(beatmap.id)
            }
        }
    }

    suspend fun ensureCatalogLoaded() {
        previewModule.start()
        _localMapIds.value = previewModule.catalog.all().map { it.songId }.toSet()
    }

    companion object {
        val Factory = containerFactory { GameViewModel(it) }
    }
}
