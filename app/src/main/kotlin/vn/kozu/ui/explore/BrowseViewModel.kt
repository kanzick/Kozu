package vn.kozu.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.repository.CatalogSource
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseSortField
import vn.kozu.domain.model.BrowseStatus
import vn.kozu.ui.containerFactory

data class BrowseUiState(
    val filters: BrowseFilters = BrowseFilters(),
    val items: List<Beatmap> = emptyList(),
    val total: Int? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreError: String? = null,
    val hasMore: Boolean = false,
    val source: CatalogSource = CatalogSource.NONE,
    val ignoredFilters: List<String> = emptyList(),
    val isLoggedIn: Boolean = false,
)

class BrowseViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(BrowseUiState())
    val ui: StateFlow<BrowseUiState> = _ui.asStateFlow()

    private var pageJob: Job? = null
    private var queryJob: Job? = null
    private var cursor: String? = null
    private var started = false
    private var generation = 0
    private var lastLoadedAt = 0L
    private var applied: BrowseFilters? = null

    init {
        viewModelScope.launch {
            container.accounts.loggedIn.collect { signedIn ->
                val wasSignedIn = _ui.value.isLoggedIn
                _ui.update { it.copy(isLoggedIn = signedIn) }
                if (wasSignedIn && !signedIn) {
                    val current = _ui.value.filters
                    val clean = current.withoutUserScoped()
                    if (clean != current) applyFilters(clean)
                }
            }
        }
    }

    fun onEnter() {
        if (!started) {
            started = true
            viewModelScope.launch {
                val settings = container.settings.settings.first()
                val initial =
                    BrowseFilters(
                        mode = settings.modeFilter,
                        status = BrowseStatus.from(settings.statusFilter),
                        explicit = settings.showExplicit,
                    )
                _ui.update { it.copy(filters = initial) }
                reload()
            }
            return
        }
        val state = _ui.value
        if (state.isLoading || state.isRefreshing) return
        val stale = System.currentTimeMillis() - lastLoadedAt > STALE_AFTER_MS
        if (stale || (state.error != null && state.items.isEmpty())) reload(keepItems = true)
    }

    fun onQueryChange(value: String) {
        _ui.update { it.copy(filters = it.filters.copy(query = value)) }
        queryJob?.cancel()
        queryJob =
            viewModelScope.launch {
                delay(QUERY_DEBOUNCE_MS)
                if (_ui.value.filters.normalized() != applied) reload()
            }
    }

    fun submitQuery() {
        queryJob?.cancel()
        val state = _ui.value
        if (state.filters.normalized() != applied || state.error != null) reload()
    }

    fun applyFilters(filters: BrowseFilters) {
        queryJob?.cancel()
        val previous = _ui.value.filters
        val next = filters.copy(query = previous.query).withNormalizedSort()
        val unchanged = next.normalized() == applied && _ui.value.error == null
        _ui.update { it.copy(filters = next) }
        syncSettings(previous, next)
        if (!unchanged) reload()
    }

    fun updateFilters(transform: (BrowseFilters) -> BrowseFilters) = applyFilters(transform(_ui.value.filters))

    fun resetFilters() = applyFilters(_ui.value.filters.resetFilters())

    fun retry() = reload()

    fun refresh() = reload(keepItems = true)

    fun loadMore() {
        val state = _ui.value
        val next = cursor ?: return
        val active = applied ?: return
        if (state.isLoading || state.isRefreshing || state.isLoadingMore) return
        val token = generation
        _ui.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        pageJob =
            viewModelScope.launch {
                val result = container.catalog.browse(active, next)
                if (token != generation) return@launch
                if (result.error != null) {
                    _ui.update { it.copy(isLoadingMore = false, loadMoreError = result.error) }
                    return@launch
                }
                val exhausted =
                    result.nextCursor == null ||
                        (result.items.isEmpty() && result.nextCursor == next)
                cursor = if (exhausted) null else result.nextCursor
                _ui.update {
                    it.copy(
                        items = (it.items + rankForQuery(active, result.items)).distinctBy { beatmap -> beatmap.id },
                        total = result.total ?: it.total,
                        isLoadingMore = false,
                        hasMore = !exhausted,
                    )
                }
            }
    }

    private fun reload(keepItems: Boolean = false) {
        pageJob?.cancel()
        val filters = _ui.value.filters
        val token = ++generation
        val previousCursor = cursor
        cursor = null
        applied = filters.normalized()
        val hasItems = keepItems && _ui.value.items.isNotEmpty()
        _ui.update {
            it.copy(
                items = if (hasItems) it.items else emptyList(),
                total = if (hasItems) it.total else null,
                hasMore = hasItems && it.hasMore,
                isLoading = !hasItems,
                isRefreshing = hasItems,
                isLoadingMore = false,
                error = null,
                loadMoreError = null,
            )
        }
        pageJob =
            viewModelScope.launch {
                val result = container.catalog.browse(filters, null)
                if (token != generation) return@launch
                if (result.error != null) {
                    cursor = if (hasItems) previousCursor else null
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error,
                            source = result.source,
                        )
                    }
                    return@launch
                }
                lastLoadedAt = System.currentTimeMillis()
                cursor = result.nextCursor
                _ui.update {
                    it.copy(
                        items = rankForQuery(filters, result.items).distinctBy { beatmap -> beatmap.id },
                        total = result.total,
                        isLoading = false,
                        isRefreshing = false,
                        hasMore = result.nextCursor != null,
                        error = null,
                        source = result.source,
                        ignoredFilters = result.ignoredFilters,
                    )
                }
            }
    }

    private fun syncSettings(
        previous: BrowseFilters,
        next: BrowseFilters,
    ) {
        if (previous.explicit != next.explicit) {
            viewModelScope.launch { container.settings.setShowExplicit(next.explicit) }
        }
        if (previous.mode != next.mode) {
            viewModelScope.launch { container.settings.setModeFilter(next.mode) }
        }
        if (previous.status != next.status) {
            next.status.toStatusFilter()?.let { filter ->
                viewModelScope.launch { container.settings.setStatusFilter(filter) }
            }
        }
    }

    private fun BrowseFilters.normalized(): BrowseFilters = copy(query = query.trim())

    /** Same relevance re-ranking as the Search tab so both entry points agree. */
    private fun rankForQuery(filters: BrowseFilters, items: List<Beatmap>): List<Beatmap> =
        if (filters.sort.option == BrowseSortField.RELEVANCE && filters.trimmedQuery.isNotEmpty()) {
            vn.kozu.ui.search.SearchRanker.rank(filters.trimmedQuery, items)
        } else {
            items
        }

    companion object {
        private const val QUERY_DEBOUNCE_MS = 400L
        private const val STALE_AFTER_MS = 5L * 60L * 1000L

        val Factory = containerFactory { BrowseViewModel(it) }
    }
}
