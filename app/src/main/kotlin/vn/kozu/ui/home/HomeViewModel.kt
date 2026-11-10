package vn.kozu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Calendar
import java.util.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.ListeningStats
import vn.kozu.domain.model.OsuProfile
import vn.kozu.domain.model.QuickPicksSource
import vn.kozu.ui.containerFactory

data class HomeUiState(
    val displayName: String = "",
    val quickPicks: List<Beatmap> = emptyList(),
    val mix: List<Beatmap> = emptyList(),
    val recent: List<Beatmap> = emptyList(),
    val madeForYou: List<Beatmap> = emptyList(),
    val stats: ListeningStats = ListeningStats(),
    val profile: OsuProfile? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val discoverAndHistory =
        combine(
            container.discover.state,
            container.history.history,
        ) { discover, history ->
            discover to history
        }

    private val settingsAndProfile =
        combine(
            container.settings.settings,
            container.accounts.profile,
        ) { settings, profile ->
            settings to profile
        }

    val ui: StateFlow<HomeUiState> =
        combine(
            discoverAndHistory,
            settingsAndProfile,
            container.accounts.loggedIn,
        ) { discoverHistory, settingsProfile, loggedIn ->
            val discover = discoverHistory.first
            val history = discoverHistory.second
            val settings = settingsProfile.first
            val profile = settingsProfile.second

            val daySeed =
                Calendar
                    .getInstance()
                    .get(Calendar.DAY_OF_YEAR)
                    .toLong()

            val mix =
                discover.mix
                    .distinctBy { it.id }
                    .shuffled(Random(daySeed))
                    .take(settings.mixSize.count)

            val customName = settings.displayName.trim()
            val greetingName =
                customName.ifBlank {
                    if (loggedIn) profile?.username.orEmpty() else ""
                }

            val quickPicks =
                when (settings.quickPicksSource) {
                    QuickPicksSource.MIX -> discover.mix.take(6) + history.mostPlayed.take(4)
                    QuickPicksSource.TRENDING -> discover.trending.take(6) + history.mostPlayed.take(4)
                    QuickPicksSource.NEW_RELEASES -> discover.newReleases
                    QuickPicksSource.LOVED -> discover.loved
                }.distinctBy { it.id }.take(10).ifEmpty {
                    (discover.mix.take(6) + history.mostPlayed.take(4)).distinctBy { it.id }.take(10)
                }

            HomeUiState(
                displayName = greetingName,
                quickPicks = quickPicks,
                mix = mix,
                recent = history.recent.take(12),
                madeForYou = discover.newReleases.take(10),
                stats = ListeningStats.from(history),
                profile = profile.takeIf { loggedIn },
                isLoading = discover.isLoading,
                isRefreshing = discover.isRefreshing,
                error = discover.error,
            )
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState(),
        )

    fun refresh() = container.discover.refresh()

    companion object {
        val Factory = containerFactory { HomeViewModel(it) }
    }
}
