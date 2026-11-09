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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.Beatmap
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoverState(
    val trending: List<Beatmap> = emptyList(),
    val newReleases: List<Beatmap> = emptyList(),
    val loved: List<Beatmap> = emptyList(),
    val mix: List<Beatmap> = emptyList(),
    val mixSource: CatalogSource = CatalogSource.NONE,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class DiscoverFeed(
    private val catalog: BeatmapRepository,
    settings: SettingsStore,
    auth: OsuAuth,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val manualRefreshPending = AtomicBoolean(false)
    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()


    init {

        val inputsChanged =
            combine(settings.settings, auth.loggedIn) { s, loggedIn ->
                listOf(
                    s.osuClientId,
                    s.osuClientSecret,
                    loggedIn,
                )
            }.distinctUntilChanged().map { }

        scope.launch {
            merge(inputsChanged, refreshTrigger).collectLatest {
                load()
            }
        }
    }

    fun refresh() {
        manualRefreshPending.set(true)
        _state.update { it.copy(isRefreshing = true) }
        refreshTrigger.tryEmit(Unit)
    }

    private suspend fun load() =
        coroutineScope {
            val manualRefresh = manualRefreshPending.getAndSet(false)
            catalog.clearMixCache()
            _state.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = manualRefresh || it.isRefreshing,
                    error = null,
                )
            }

            try {
                val trending = async { catalog.trending() }
                val loved = async { catalog.lovedPicks() }
                val newestMix = async { catalog.mixPage(null, "updated_desc") }
                val oldestMix = async { catalog.mixPage(null, "updated_asc") }

                val t = trending.await()
                val l = loved.await()
                val latest = newestMix.await()
                val archive = oldestMix.await()

                val mixItems =
                    (latest.items + archive.items)
                        .filter { it.isRankedOrLoved() }
                        .distinctBy { it.id }

                _state.value =
                    DiscoverState(
                        trending = t.items.filter { it.status.equals("ranked", true) },
                        newReleases = latest.items.filter { it.isRankedOrLoved() }.take(12),
                        loved = l.items.filter { it.status.equals("loved", true) },
                        mix = mixItems,
                        mixSource =
                            when {
                                latest.source == archive.source -> latest.source
                                latest.source == CatalogSource.NONE -> archive.source
                                archive.source == CatalogSource.NONE -> latest.source
                                else -> CatalogSource.MIXED
                            },
                        isLoading = false,
                        isRefreshing = false,
                        error = t.error ?: l.error ?: latest.error ?: archive.error,
                    )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Could not refresh the osu! catalog",
                    )
                }
            }
        }
}

private fun Beatmap.isRankedOrLoved(): Boolean =
    status.equals("ranked", ignoreCase = true) ||
        status.equals("loved", ignoreCase = true)
