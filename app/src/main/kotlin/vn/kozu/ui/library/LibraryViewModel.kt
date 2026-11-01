package vn.kozu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

enum class LibraryTab(val label: String) {
    LIKED("LIKED"),
    PLAYLISTS("PLAYLISTS"),
    SONGS("SONGS"),
    DOWNLOADS("DOWNLOADS")
}

enum class LibrarySort(val label: String) {
    RECENT("Recently added"),
    TITLE("Title"),
    ARTIST("Artist")
}

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.LIKED,
    val sort: LibrarySort = LibrarySort.RECENT,
    val grid: Boolean = false,
    val liked: List<Beatmap> = emptyList(),
    val songs: List<Beatmap> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val openedPlaylist: Playlist? = null,
    val downloads: List<DownloadedSet> = emptyList(),
    val queue: Map<Long, DownloadStatus> = emptyMap(),
    val queueTitles: Map<Long, String> = emptyMap(),
    val downloadedBytes: Long = 0L
)

class LibraryViewModel(private val container: AppContainer) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.LIKED)
    private val sort = MutableStateFlow(LibrarySort.RECENT)
    private val grid = MutableStateFlow(false)
    private val openedId = MutableStateFlow<String?>(null)

    private val prefs = combine(tab, sort, grid, openedId) { t, s, g, o -> arrayOf<Any?>(t, s, g, o) }

    private val downloadState = combine(container.downloadsStore.sets, container.downloads.status) { sets, status ->
        sets to status
    }

    val ui: StateFlow<LibraryUiState> = combine(
        prefs,
        container.favorites.favorites,
        container.history.history,
        container.playlists.playlists,
        downloadState
    ) { p, favorites, history, playlists, downloadState ->
        val currentSort = p[1] as LibrarySort
        LibraryUiState(
            tab = p[0] as LibraryTab,
            sort = currentSort,
            grid = p[2] as Boolean,
            liked = favorites.applySort(currentSort),
            songs = history.recent.applySort(currentSort),
            playlists = playlists,
            openedPlaylist = playlists.firstOrNull { it.id == p[3] },
            downloads = downloadState.first,
            queue = downloadState.second,
            queueTitles = downloadState.second.keys.associateWith { container.downloads.titleOf(it) },
            downloadedBytes = downloadState.first.sumOf { it.sizeBytes }
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(value: LibraryTab) = tab.update { value }
    fun selectSort(value: LibrarySort) = sort.update { value }
    fun toggleGrid() = grid.update { !it }
    fun openPlaylist(id: String) = openedId.update { id }
    fun closePlaylist() = openedId.update { null }

    fun createPlaylist(name: String) = viewModelScope.launch { container.playlists.create(name) }
    fun renamePlaylist(id: String, name: String) = viewModelScope.launch { container.playlists.rename(id, name) }
    fun deletePlaylist(id: String) = viewModelScope.launch {
        container.playlists.delete(id)
        if (openedId.value == id) openedId.update { null }
    }
    fun deleteDownload(id: Long) = viewModelScope.launch { container.downloads.delete(id) }
    fun dismissDownload(id: Long) = container.downloads.dismiss(id)
    fun cancelDownload(id: Long) = container.downloads.cancel(id)

    fun removeFromPlaylist(playlistId: String, beatmapId: Long) =
        viewModelScope.launch { container.playlists.removeTrack(playlistId, beatmapId) }

    private fun List<Beatmap>.applySort(sort: LibrarySort): List<Beatmap> = when (sort) {
        LibrarySort.RECENT -> this
        LibrarySort.TITLE -> sortedBy { it.title.lowercase() }
        LibrarySort.ARTIST -> sortedBy { it.artist.lowercase() }
    }

    companion object {
        val Factory = containerFactory { LibraryViewModel(it) }
    }
}
