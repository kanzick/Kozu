package vn.kozu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.download.DownloadStatus
import vn.kozu.data.local.DownloadedSet
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import vn.kozu.ui.containerFactory

enum class LibraryTab(
    val key: String,
    val label: String,
) {
    SONGS("songs", "Songs"),
    LIKED("liked", "Liked"),
    PLAYLISTS("playlists", "Playlists"),
    DOWNLOADS("downloads", "Downloads"),
}

enum class LibrarySort(
    val label: String,
) {
    RECENT("Date added"),
    TITLE("Title"),
    ARTIST("Artist"),
    MAPPER("Mapper"),
}

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.SONGS,
    val tabOrder: List<LibraryTab> = LibraryTab.entries.toList(),
    val sort: LibrarySort = LibrarySort.RECENT,
    val ascending: Boolean = false,
    val grid: Boolean = false,
    val liked: List<Beatmap> = emptyList(),
    val songs: List<Beatmap> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val openedPlaylist: Playlist? = null,
    val downloads: List<DownloadedSet> = emptyList(),
    val queue: Map<Long, DownloadStatus> = emptyMap(),
    val queueTitles: Map<Long, String> = emptyMap(),
    val downloadedBytes: Long = 0L,
)

class LibraryViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val tab = MutableStateFlow(LibraryTab.SONGS)
    private val sort = MutableStateFlow(LibrarySort.RECENT)
    private val grid = MutableStateFlow(false)
    private val ascending = MutableStateFlow(false)
    private val openedId = MutableStateFlow<String?>(null)
    private val tabOrder = MutableStateFlow<List<LibraryTab>>(LibraryTab.entries.toList())
    val syncing: StateFlow<Boolean> = container.sync.running
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.libraryTabOrder.collect { keys ->
                val ordered = keys.mapNotNull { key -> LibraryTab.entries.firstOrNull { it.key == key } }
                tabOrder.value = (ordered + LibraryTab.entries.toList()).distinct()
            }
        }
    }

    private val prefs = combine(tab, sort, grid, openedId, ascending) { t, s, g, o, a -> arrayOf<Any?>(t, s, g, o, a) }

    private val downloadState =
        combine(container.downloadsStore.sets, container.downloads.status) { sets, status ->
            sets to status
        }

    private val baseUi =
        combine(
            prefs,
            container.favorites.favorites,
            container.history.history,
            container.playlists.playlists,
            downloadState,
        ) { p, favorites, history, playlists, downloadState ->
            val currentSort = p[1] as LibrarySort
            val asc = p[4] as Boolean
            LibraryUiState(
                tab = p[0] as LibraryTab,
                sort = currentSort,
                ascending = asc,
                grid = p[2] as Boolean,
                liked = sortBeatmaps(favorites, currentSort, asc),
                songs = sortBeatmaps(history.recent, currentSort, asc),
                playlists = playlists,
                openedPlaylist = playlists.firstOrNull { it.id == p[3] },
                downloads = sortDownloadedSets(downloadState.first, currentSort, asc),
                queue = downloadState.second,
                queueTitles = downloadState.second.keys.associateWith { container.downloads.titleOf(it) },
                downloadedBytes = downloadState.first.sumOf { it.sizeBytes },
            )
        }

    val ui: StateFlow<LibraryUiState> =
        baseUi
            .combine(tabOrder) { state, orderedTabs ->
                state.copy(tabOrder = orderedTabs)
            }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(value: LibraryTab) = tab.update { value }

    fun saveTabOrder(order: List<LibraryTab>) {
        val normalized = (order.distinct() + LibraryTab.entries.toList()).distinct()
        tabOrder.value = normalized
        viewModelScope.launch {
            container.settings.setLibraryTabOrder(normalized.map { it.key })
        }
    }

    fun syncLibrary() =
        viewModelScope.launch {
            if (container.sync.running.value) return@launch
            _syncError.value = null
            try {
                container.sync.run()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _syncError.value = "Could not refresh your library. Check your connection and try again."
            }
        }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun selectSort(value: LibrarySort) = sort.update { value }

    fun toggleGrid() = grid.update { !it }

    fun toggleDirection() = ascending.update { !it }

    fun openPlaylist(id: String) = openedId.update { id }

    fun closePlaylist() = openedId.update { null }

    fun createPlaylist(name: String) = viewModelScope.launch { container.playlists.create(name) }

    fun renamePlaylist(
        id: String,
        name: String,
    ) = viewModelScope.launch { container.playlists.rename(id, name) }

    fun deletePlaylist(id: String) =
        viewModelScope.launch {
            container.playlists.delete(id)
            if (openedId.value == id) openedId.update { null }
        }

    fun deleteDownload(id: Long) = viewModelScope.launch { container.downloads.delete(id) }

    fun dismissDownload(id: Long) = container.downloads.dismiss(id)

    fun cancelDownload(id: Long) = container.downloads.cancel(id)

    fun removeFromPlaylist(
        playlistId: String,
        beatmapId: Long,
    ) = viewModelScope.launch { container.playlists.removeTrack(playlistId, beatmapId) }

    private fun sortBeatmaps(
        items: List<Beatmap>,
        sort: LibrarySort,
        ascending: Boolean,
    ): List<Beatmap> {
        val sorted =
            when (sort) {
                LibrarySort.RECENT -> items
                LibrarySort.TITLE -> items.sortedBy { it.title.lowercase() }
                LibrarySort.ARTIST -> items.sortedBy { it.artist.lowercase() }
                LibrarySort.MAPPER -> items.sortedBy { it.mapper.lowercase() }
            }
        val reverse = if (sort == LibrarySort.RECENT) ascending else !ascending
        return if (reverse) sorted.reversed() else sorted
    }

    private fun sortDownloadedSets(
        items: List<DownloadedSet>,
        sort: LibrarySort,
        ascending: Boolean,
    ): List<DownloadedSet> {
        val sorted =
            when (sort) {
                LibrarySort.RECENT -> items
                LibrarySort.TITLE -> items.sortedBy { it.beatmap.title.lowercase() }
                LibrarySort.ARTIST -> items.sortedBy { it.beatmap.artist.lowercase() }
                LibrarySort.MAPPER -> items.sortedBy { it.beatmap.mapper.lowercase() }
            }
        val reverse = if (sort == LibrarySort.RECENT) ascending else !ascending
        return if (reverse) sorted.reversed() else sorted
    }

    companion object {
        val Factory = containerFactory { LibraryViewModel(it) }
    }
}
