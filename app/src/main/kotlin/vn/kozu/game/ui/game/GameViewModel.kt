package vn.kozu.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficulty
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.previewstage.PreviewStageConfig
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.ui.containerFactory

data class GameMapDownloadState(
    val progress: Float? = null,
    val error: String? = null,
    val ratingWarning: String? = null
)

class GameViewModel(private val container: AppContainer) : ViewModel() {

    val previewModule: PreviewStageModule = PreviewStageModule.get(
        context = container.appContext,
        config = PreviewStageConfig(
            rootDir = PreviewStageModule.defaultRootDir(container.appContext),
            onlyMania = true
        )
    )
    val gameModule: GameStageModule = GameStageModule.get(container.appContext, previewModule)

    val playerName: StateFlow<String> = combine(
        container.settings.settings,
        container.accounts.profile,
        container.accounts.loggedIn,
    ) { settings, profile, loggedIn ->
        settings.displayName.trim().ifBlank { if (loggedIn) profile?.username.orEmpty() else "" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val osuUsername: StateFlow<String> = combine(
        container.accounts.profile,
        container.accounts.loggedIn,
    ) { profile, loggedIn ->
        if (loggedIn) profile?.username.orEmpty() else ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val customOrderIds: StateFlow<List<Long>> = container.settings.libraryCustomOrders
        .map { it[GAME_ORDER_KEY].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCustomOrder(ids: List<Long>) {
        val normalized = ids.asSequence().filter { it > 0L }.distinct().take(MAX_CUSTOM_ORDER_SIZE).toList()
        if (normalized == lastRequestedCustomOrder) return
        lastRequestedCustomOrder = normalized
        customOrderSaveJob?.cancel()
        customOrderSaveJob = viewModelScope.launch {
            delay(CUSTOM_ORDER_SAVE_DEBOUNCE_MS)
            container.settings.setLibraryCustomOrder(GAME_ORDER_KEY, normalized)
        }
    }

    suspend fun rankedAt(song: vn.kozu.game.mania.previewstage.model.SongEntry): Long =
        container.catalog.setInfo(song.songId)?.beatmap?.rankedAt ?: 0L

    private val _downloads = MutableStateFlow<Map<Long, GameMapDownloadState>>(emptyMap())
    val downloads: StateFlow<Map<Long, GameMapDownloadState>> = _downloads.asStateFlow()

    private val _localMapIds = MutableStateFlow(previewModule.catalog.all().map { it.songId }.toSet())
    val localMapIds: StateFlow<Set<Long>> = _localMapIds.asStateFlow()
    private val _requestedMapId = MutableStateFlow<Long?>(null)
    val requestedMapId: StateFlow<Long?> = _requestedMapId.asStateFlow()
    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val jobs = mutableMapOf<Long, Job>()
    private var previewJob: Job? = null
    private var customOrderSaveJob: Job? = null
    private var lastRequestedCustomOrder: List<Long> = emptyList()

    init {
        viewModelScope.launch {
            previewModule.catalog.entries.collect { entries ->
                val localIds = entries.mapTo(mutableSetOf()) { it.songId }
                _localMapIds.value = localIds
                _downloads.update { states ->
                    states.filter { (songId, state) ->
                        songId !in localIds || state.error != null || state.ratingWarning != null ||
                            (state.progress != null && state.progress < 1f)
                    }
                }
            }
        }
    }

    suspend fun searchMania(query: String, page: Int, status: String? = null, explicit: Boolean = false) =
        withContext(Dispatchers.IO) { container.catalog.searchMania(query, page, status, explicit) }

    suspend fun maniaDifficulties(beatmapSetId: Long): List<BeatmapDifficulty> =
        container.catalog.maniaDifficulties(beatmapSetId)

    fun requestOpenMap(beatmapSetId: Long) {
        _requestedMapId.value = beatmapSetId
    }

    fun consumeOpenMapRequest(beatmapSetId: Long) {
        _requestedMapId.update { if (it == beatmapSetId) null else it }
    }

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
            setDownloadState(beatmap.id, GameMapDownloadState(progress = 0f))
            try {
                val destination = File(previewModule.paths.downloads, "${beatmap.id}.osz")
                withContext(Dispatchers.IO) {
                    container.downloads.downloadArchive(beatmap, destination) { progress ->
                        setDownloadState(beatmap.id, GameMapDownloadState(progress = progress))
                    }
                }
                previewModule.importArchive(destination)
                _localMapIds.value = previewModule.catalog.all().map { it.songId }.toSet()
                if (beatmap.id !in _localMapIds.value) {
                    throw IllegalStateException("The downloaded archive contains no valid Mania charts")
                }
                var ratingWarning: String? = null
                try {
                    val ratings = container.catalog.maniaDifficulties(beatmap.id)
                        .associate { it.id to it.difficultyRating }
                    if (previewModule.updateStarRatings(beatmap.id, ratings) == 0) {
                        ratingWarning = "Star ratings were not available for this map."
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ratingWarning = "Map added, but star ratings could not be loaded: ${e.message ?: "request failed"}"
                }
                if (ratingWarning == null) {
                    _downloads.update { it - beatmap.id }
                } else {
                    setDownloadState(beatmap.id, GameMapDownloadState(progress = 1f, ratingWarning = ratingWarning))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setDownloadState(beatmap.id, GameMapDownloadState(error = e.message ?: "Download failed"))
            } finally {
                jobs.remove(beatmap.id)
            }
        }
    }

    private fun setDownloadState(songId: Long, state: GameMapDownloadState) {
        _downloads.update { current ->
            val updated = current.toMutableMap().apply { put(songId, state) }
            while (updated.size > MAX_DOWNLOAD_HISTORY) {
                val oldestFinished = updated.entries.firstOrNull { (id, value) ->
                    id != songId && (value.error != null || value.ratingWarning != null)
                }?.key ?: break
                updated.remove(oldestFinished)
            }
            updated.toMap()
        }
    }

    suspend fun ensureCatalogLoaded() {
        previewModule.start()
        _localMapIds.value = previewModule.catalog.all().map { it.songId }.toSet()
    }

    companion object {
        private const val GAME_ORDER_KEY = "game"
        private const val MAX_CUSTOM_ORDER_SIZE = 20_000
        private const val MAX_DOWNLOAD_HISTORY = 40
        private const val CUSTOM_ORDER_SAVE_DEBOUNCE_MS = 160L
        val Factory = containerFactory { GameViewModel(it) }
    }
}
