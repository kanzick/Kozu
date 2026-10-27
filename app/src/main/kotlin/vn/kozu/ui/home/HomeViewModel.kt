package vn.kozu.ui.home

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
import vn.kozu.domain.model.ListeningStats
import vn.kozu.ui.containerFactory
import java.util.Calendar
import java.util.Random

/** UI state metadata for the Home feed screen */
data class HomeUiState(
    val displayName: String = "",
    val quickPicks: List<Beatmap> = emptyList(),
    val mix: List<Beatmap> = emptyList(),
    val recent: List<Beatmap> = emptyList(),
    val madeForYou: List<Beatmap> = emptyList(),
    val stats: ListeningStats = ListeningStats(),
    val isLoading: Boolean = true,
    val isSample: Boolean = true,
    val error: String? = null
)

/** ViewModel backing the Home screen feeds, quick picks, and daily mix */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val ui: StateFlow<HomeUiState> = combine(
        container.discover.state,
        container.history.history,
        container.favorites.favorites,
        container.settings.settings
    ) { discover, history, favorites, settings ->
        val daySeed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
        val mix = (favorites + history.mostPlayed + discover.trending + discover.loved)
            .distinctBy { it.id }
            .shuffled(Random(daySeed))

        HomeUiState(
            displayName = settings.displayName,
            quickPicks = (history.mostPlayed.take(4) + discover.trending).distinctBy { it.id }.take(10),
            mix = mix,
            recent = history.recent.take(12),
            madeForYou = discover.newReleases.take(10),
            stats = ListeningStats.from(history),
            isLoading = discover.isLoading,
            isSample = discover.isSample,
            error = discover.error
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() = container.discover.refresh()

    companion object {
        val Factory = containerFactory { HomeViewModel(it) }
    }
}
