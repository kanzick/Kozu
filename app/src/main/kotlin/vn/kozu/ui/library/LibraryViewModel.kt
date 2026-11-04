package vn.kozu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
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
    val key: String,
    val label: String,
) {
    RECENT("recent", "Date added"),
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    MAPPER("mapper", "Mapper"),
    CUSTOM("custom", "Custom"),
    ;

    companion object {
        fun fromKey(key: String): LibrarySort = entries.firstOrNull { it.key == key } ?: RECENT
    }
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
    val customOrders: Map<String, List<Long>> = emptyMap(),
)

private fun <T> applyCustomOrder(
    items: List<T>,
    ids: List<Long>?,
    idOf: (T) -> Long,
): List<T> {
    if (items.size < 2 || ids.isNullOrEmpty()) return items
    val byId = items.associateBy(idOf)
    val orderedIds = ids.toHashSet()
    return buildList(items.size) {
        ids.forEach { id -> byId[id]?.let(::add) }
        items.filterNot { idOf(it) in orderedIds }.forEach(::add)
    }
}

class LibraryViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val tab = MutableStateFlow(LibraryTab.SONGS)
    private val sort = MutableStateFlow(LibrarySort.RECENT)
    private val grid =
        container.settings.settings
            .map { it.libraryGrid }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val ascending = MutableStateFlow(false)
    private val openedId = MutableStateFlow<String?>(null)
    private val localCustomOrders = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    private val customOrders = combine(container.settings.libraryCustomOrders, localCustomOrders) { saved, pending ->
        saved + pending
    }
    private val libraryOrderSaveJobs = mutableMapOf<String, Job>()
    private val playlistOrderSaveJobs = mutableMapOf<String, Job>()
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
        viewModelScope.launch {
            container.settings.librarySortPreferences.collect { (mode, isAscending) ->
                sort.value = LibrarySort.fromKey(mode)
                ascending.value = isAscending
            }
        }
    }

    private val prefs = combine(tab, sort, grid, openedId, ascending) { t, s, g, o, a -> arrayOf<Any?>(t, s, g, o, a) }

    private val downloadState =
        combine(container.downloadsStore.sets, container.downloads.status) { sets, status ->
            sets to status
        }

    // Playback writes a new history event as soon as a track starts. The history's natural
    // RECENT order therefore moves the playing item to index 0, which makes a visible Library
    // list jump while the user is browsing or shuffling. Keep the current session's row order
    // stable, while still replacing each row with the latest metadata and dropping removed IDs.
    // A new subscription (for example, reopening Library after leaving it) starts from the
    // newest persisted history order again.
    private val stableRecentTracks =
        container.history.history
            .map { it.recent }
            .scan(emptyList<Beatmap>()) { previous, latest ->
                if (previous.isEmpty()) {
                    latest
                } else {
                    val latestById = latest.associateBy { it.id }
                    val retainedIds = previous.mapNotNull { old ->
                        old.id.takeIf(latestById::containsKey)
                    }
                    val retainedSet = retainedIds.toHashSet()
                    val newlySeenIds = latest.map { it.id }.filterNot(retainedSet::contains)
                    (retainedIds + newlySeenIds).mapNotNull(latestById::get)
                }
            }

    private val baseUi =
        combine(
            prefs,
            container.favorites.favorites,
            stableRecentTracks,
            container.playlists.playlists,
            downloadState,
        ) { p, favorites, recentTracks, playlists, downloadState ->
            val currentSort = p[1] as LibrarySort
            val asc = p[4] as Boolean
            LibraryUiState(
                tab = p[0] as LibraryTab,
                sort = currentSort,
                ascending = asc,
                grid = p[2] as Boolean,
                liked = sortBeatmaps(favorites, currentSort, asc),
                songs = sortBeatmaps(recentTracks, currentSort, asc),
                playlists = playlists,
                openedPlaylist = playlists.firstOrNull { it.id == p[3] },
                downloads = sortDownloadedSets(downloadState.first, currentSort, asc),
                queue = downloadState.second,
                queueTitles = downloadState.second.keys.associateWith { container.downloads.titleOf(it) },
                downloadedBytes = downloadState.first.sumOf { it.sizeBytes },
            )
        }

    val ui: StateFlow<LibraryUiState> =
        combine(baseUi, tabOrder, customOrders) { state, orderedTabs, customOrders ->
            val customSongs = if (state.sort == LibrarySort.CUSTOM) applyCustomOrder(state.songs, customOrders["songs"]) { it.id } else state.songs
            val customLiked = if (state.sort == LibrarySort.CUSTOM) applyCustomOrder(state.liked, customOrders["liked"]) { it.id } else state.liked
            val customDownloads = if (state.sort == LibrarySort.CUSTOM) {
                applyCustomOrder(state.downloads, customOrders["downloads"]) { it.beatmap.id }
            } else {
                state.downloads
            }
            state.copy(
                tabOrder = orderedTabs,
                songs = customSongs,
                liked = customLiked,
                downloads = customDownloads,
                customOrders = customOrders,
            )
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

    fun selectSort(value: LibrarySort) {
        sort.value = value
        viewModelScope.launch {
            container.settings.setLibrarySortPreferences(value.key, ascending.value)
        }
    }

    /** Persist a drag result and immediately switch the active sort mode to Custom. */
    fun reorderTracks(tab: LibraryTab, orderedIds: List<Long>, playlistId: String? = null) {
        val normalized = orderedIds.filter { it > 0L }.distinct()
        if (normalized.isEmpty()) return
        if (sort.value != LibrarySort.CUSTOM) {
            sort.value = LibrarySort.CUSTOM
            val keepAscending = ascending.value
            viewModelScope.launch {
                container.settings.setLibrarySortPreferences(LibrarySort.CUSTOM.key, keepAscending)
            }
        }
        when (tab) {
            LibraryTab.SONGS, LibraryTab.LIKED, LibraryTab.DOWNLOADS -> {
                // Apply the order synchronously so Compose never snaps back while DataStore writes.
                localCustomOrders.update { it + (tab.key to normalized) }
                libraryOrderSaveJobs[tab.key]?.cancel()
                libraryOrderSaveJobs[tab.key] = viewModelScope.launch {
                    delay(160L)
                    container.settings.setLibraryCustomOrder(tab.key, normalized)
                }
            }
            LibraryTab.PLAYLISTS -> {
                val id = playlistId ?: return
                playlistOrderSaveJobs[id]?.cancel()
                playlistOrderSaveJobs[id] = viewModelScope.launch {
                    delay(160L)
                    container.playlists.reorderTracks(id, normalized)
                }
            }
        }
    }

    fun toggleGrid() = viewModelScope.launch { container.settings.setLibraryGrid(!grid.value) }

    fun toggleDirection() {
        val next = !ascending.value
        ascending.value = next
        viewModelScope.launch {
            container.settings.setLibrarySortPreferences(sort.value.key, next)
        }
    }

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
                LibrarySort.CUSTOM -> items
            }
        val reverse = when (sort) {
            LibrarySort.RECENT -> ascending
            LibrarySort.CUSTOM -> false
            else -> !ascending
        }
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
                LibrarySort.CUSTOM -> items
            }
        val reverse = when (sort) {
            LibrarySort.RECENT -> ascending
            LibrarySort.CUSTOM -> false
            else -> !ascending
        }
        return if (reverse) sorted.reversed() else sorted
    }

    companion object {
        val Factory = containerFactory { LibraryViewModel(it) }
    }
}
