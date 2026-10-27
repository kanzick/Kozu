package vn.kozu.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.containerFactory

/** Library collection item in Explore tab */
data class LibraryCollection(
    val id: String,
    val title: String,
    val label: String,
    val tracks: List<Beatmap>
)

/** UI state metadata for Explore screen */
data class ExploreUiState(
    val collections: List<LibraryCollection> = emptyList(),
    val newReleases: List<Beatmap> = emptyList(),
    val listenAgain: List<Beatmap> = emptyList(),
    val trending: List<Beatmap> = emptyList(),
    val loved: List<Beatmap> = emptyList(),
    val isLoading: Boolean = true
) {
    val surprisePool: List<Beatmap> get() = (trending + loved + newReleases).distinctBy { it.id }
}

/** ViewModel providing collections, trending songs, and recommendations for Explore screen */
class ExploreViewModel(private val container: AppContainer) : ViewModel() {

    val ui: StateFlow<ExploreUiState> = combine(
        container.discover.state,
        container.favorites.favorites,
        container.history.history,
        container.playlists.playlists
    ) { discover, favorites, history, playlists ->
        val collections = buildList {
            if (favorites.isNotEmpty()) add(LibraryCollection("liked", "Liked songs", "Liked", favorites))
            val most = history.mostPlayed.take(30)
            if (most.isNotEmpty()) add(LibraryCollection("most-played", "Most played", "Auto", most))
            playlists.forEach { add(LibraryCollection(it.id, it.name, "Playlist", it.tracks)) }
        }
        ExploreUiState(
            collections = collections,
            newReleases = discover.newReleases,
            listenAgain = history.recent.take(12),
            trending = discover.trending,
            loved = discover.loved,
            isLoading = discover.isLoading
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    companion object {
        val Factory = containerFactory { ExploreViewModel(it) }
    }
}
