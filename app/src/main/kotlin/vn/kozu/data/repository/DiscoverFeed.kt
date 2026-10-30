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
    /** Ranked + Loved sample assembled from newest and oldest catalog pages. */
    val mix: List<Beatmap> = emptyList(),
    val mixNewestCursor: String? = null,
    val mixOldestCursor: String? = null,
    val mixSource: CatalogSource = CatalogSource.NONE,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMoreMix: Boolean = false,
    val error: String? = null
) {
    val hasMoreMix: Boolean
        get() = mixNewestCursor != null || mixOldestCursor != null
}

class DiscoverFeed(
    private val catalog: BeatmapRepository,
    settings: SettingsStore,
    auth: OsuAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val mixLoadInProgress = AtomicBoolean(false)
    private val manualRefreshPending = AtomicBoolean(false)
    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    // Alternate directions so the Mix keeps sampling both ends of
    // osu!'s update history rather than only presenting the newest uploads.
    @Volatile private var nextMixNewest = true

    init {
        // Reload the catalog when credentials or the signed-in session changes.
        // This makes osu! official search become the active source immediately
        // after login, and returns to the public mirror after logout.
        val inputsChanged = combine(settings.settings, auth.loggedIn) { s, loggedIn ->
            listOf(
                s.osuClientId,
                s.osuClientSecret,
                loggedIn
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

    /** Loads the next official catalog page, alternating newest and oldest. */
    fun loadMoreMix() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isLoadingMoreMix) return
        if (!snapshot.hasMoreMix) return
        if (!mixLoadInProgress.compareAndSet(false, true)) return

        scope.launch {
            _state.update { it.copy(isLoadingMoreMix = true, error = null) }
            try {
                val current = _state.value
                val useNewest = when {
                    nextMixNewest && current.mixNewestCursor != null -> true
                    current.mixOldestCursor != null -> false
                    current.mixNewestCursor != null -> true
                    else -> return@launch
                }

                val cursor = if (useNewest) {
                    current.mixNewestCursor
                } else {
                    current.mixOldestCursor
                } ?: return@launch

                val page = catalog.mixPage(
                    cursor = cursor,
                    sort = if (useNewest) "updated_desc" else "updated_asc"
                )
                val leaderboardItems = page.items.filter { it.isRankedOrLoved() }

                _state.update { state ->
                    if (page.error != null) {
                        state.copy(
                            isLoadingMoreMix = false,
                            error = page.error
                        )
                    } else {
                        state.copy(
                            mix = (state.mix + leaderboardItems).distinctBy { it.id },
                            mixNewestCursor = if (useNewest) page.nextCursor else state.mixNewestCursor,
                            mixOldestCursor = if (useNewest) state.mixOldestCursor else page.nextCursor,
                            mixSource = page.source,
                            isLoadingMoreMix = false,
                            error = null
                        )
                    }
                }

                nextMixNewest = !useNewest
            } finally {
                _state.update { it.copy(isLoadingMoreMix = false) }
                mixLoadInProgress.set(false)
            }
        }
    }

    private suspend fun load() = coroutineScope {
        val manualRefresh = manualRefreshPending.getAndSet(false)
        catalog.clearMixCache()
        _state.update {
            it.copy(
                isLoading = true,
                isRefreshing = manualRefresh || it.isRefreshing,
                error = null
            )
        }

        try {
            // Load the all-status-independent Ranked + Loved catalog once and
            // reuse its newest page for the New Releases rail. This avoids
            // issuing duplicate ranked/loved searches on every cold start.
            val trending = async { catalog.trending() }
            val loved = async { catalog.lovedPicks() }
            val newestMix = async { catalog.mixPage(null, "updated_desc") }
            val oldestMix = async { catalog.mixPage(null, "updated_asc") }

            val t = trending.await()
            val l = loved.await()
            val latest = newestMix.await()
            val archive = oldestMix.await()

            val mixItems = (latest.items + archive.items)
                .filter { it.isRankedOrLoved() }
                .distinctBy { it.id }

            _state.value = DiscoverState(
                trending = t.items.filter { it.status.equals("ranked", true) },
                newReleases = latest.items.filter { it.isRankedOrLoved() }.take(12),
                loved = l.items.filter { it.status.equals("loved", true) },
                mix = mixItems,
                mixNewestCursor = latest.nextCursor,
                mixOldestCursor = archive.nextCursor,
                mixSource = when {
                    latest.source == archive.source -> latest.source
                    latest.source == CatalogSource.NONE -> archive.source
                    archive.source == CatalogSource.NONE -> latest.source
                    else -> CatalogSource.MIXED
                },
                isLoading = false,
                isRefreshing = false,
                error = t.error ?: l.error ?: latest.error ?: archive.error
            )
            nextMixNewest = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Could not refresh the osu! catalog"
                )
            }
        }
    }
}


private fun Beatmap.isRankedOrLoved(): Boolean =
    status.equals("ranked", ignoreCase = true) ||
        status.equals("loved", ignoreCase = true)
