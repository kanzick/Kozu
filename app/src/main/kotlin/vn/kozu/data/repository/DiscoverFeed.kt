package vn.kozu.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.Beatmap

data class DiscoverState(
    val trending: List<Beatmap> = emptyList(),
    val newReleases: List<Beatmap> = emptyList(),
    val loved: List<Beatmap> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DiscoverFeed(
    private val catalog: BeatmapRepository,
    settings: SettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    init {
        val inputsChanged = settings.settings
            .map { s -> listOf(s.osuClientId, s.osuClientSecret, s.modeFilter, s.showExplicit) }
            .distinctUntilChanged()
            .map { }
        scope.launch {
            merge(inputsChanged, refreshTrigger).collectLatest { load() }
        }
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    private suspend fun load() = coroutineScope {
        _state.update { it.copy(isLoading = true) }
        val trending = async { catalog.trending() }
        val fresh = async { catalog.newReleases() }
        val loved = async { catalog.lovedPicks() }
        val t = trending.await()
        val n = fresh.await()
        val l = loved.await()
        _state.value = DiscoverState(
            trending = t.items,
            newReleases = n.items,
            loved = l.items,
            isLoading = false,
            error = t.error ?: n.error ?: l.error
        )
    }
}
