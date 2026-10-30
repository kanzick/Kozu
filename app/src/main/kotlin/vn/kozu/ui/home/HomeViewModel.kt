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

data class HomeUiState(
    val displayName: String = "",
    val quickPicks: List<Beatmap> = emptyList(),
    val mix: List<Beatmap> = emptyList(),
    val recent: List<Beatmap> = emptyList(),
    val madeForYou: List<Beatmap> = emptyList(),
    val stats: ListeningStats = ListeningStats(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    // Use nested combine calls instead of the six-flow vararg overload.
    // The vararg overload exposes Array<Any?> and cannot infer six typed
    // lambda parameters with the project's kotlinx-coroutines version.
    private val discoverAndHistory = combine(
        container.discover.state,
        container.history.history
    ) { discover, history ->
        discover to history
    }

    private val settingsAndProfile = combine(
        container.settings.settings,
        container.accounts.profile
    ) { settings, profile ->
        settings to profile
    }

    val ui: StateFlow<HomeUiState> = combine(
        discoverAndHistory,
        settingsAndProfile,
        container.accounts.loggedIn
    ) { discoverHistory, settingsProfile, loggedIn ->
        val discover = discoverHistory.first
        val history = discoverHistory.second
        val settings = settingsProfile.first
        val profile = settingsProfile.second

        val daySeed = Calendar.getInstance()
            .get(Calendar.DAY_OF_YEAR)
            .toLong()

        // Mix is sourced from the Ranked + Loved catalog, not popularity or
        // personal listening history. Keep listening history in its own rail.
        val mix = discover.mix
            .distinctBy { it.id }
            .shuffled(Random(daySeed))

        val customName = settings.displayName.trim()
        val greetingName = customName.ifBlank {
            if (loggedIn) profile?.username.orEmpty() else ""
        }

        HomeUiState(
            displayName = greetingName,
            quickPicks = (discover.mix.take(6) + history.mostPlayed.take(4))
                .distinctBy { it.id }
                .take(10),
            mix = mix,
            recent = history.recent.take(12),
            madeForYou = discover.newReleases.take(10),
            stats = ListeningStats.from(history),
            isLoading = discover.isLoading,
            isRefreshing = discover.isRefreshing,
            error = discover.error
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState()
    )

    fun refresh() = container.discover.refresh()

    fun loadMoreMix() = container.discover.loadMoreMix()

    companion object {
        val Factory = containerFactory { HomeViewModel(it) }
    }
}
