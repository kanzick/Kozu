package vn.kozu.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.repository.CatalogSource
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseGenre
import vn.kozu.domain.model.BrowseSortField
import vn.kozu.domain.model.SearchCategory
import vn.kozu.ui.containerFactory

enum class SearchPhase { IDLE, LOADING, DONE }

data class SearchUiState(
    val filters: BrowseFilters = BrowseFilters(),
    val phase: SearchPhase = SearchPhase.IDLE,
    val results: List<Beatmap> = emptyList(),
    val total: Int? = null,
    val source: CatalogSource = CatalogSource.NONE,
    val error: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null,
    val isLoggedIn: Boolean = false,
) {
    val query: String get() = filters.query
    val isIdle: Boolean get() = filters.trimmedQuery.isEmpty() && !filters.hasActiveFilters
}

class SearchViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    val recentSearches: StateFlow<List<String>> =
        container.settings.recentSearches
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var job: Job? = null
    private var cursor: String? = null
    private var generation = 0

    init {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            _ui.update { it.copy(filters = it.filters.copy(explicit = settings.showExplicit)) }
        }
        viewModelScope.launch {
            container.accounts.loggedIn.collect { signedIn ->
                _ui.update { it.copy(isLoggedIn = signedIn) }
                if (!signedIn) {
                    val current = _ui.value.filters
                    val clean = current.withoutUserScoped()
                    if (clean != current) applyFilters(clean)
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _ui.update { it.copy(filters = it.filters.copy(query = value)) }
        schedule(350)
    }

    fun applyFilters(filters: BrowseFilters) {
        val previous = _ui.value.filters
        val next = filters.copy(query = previous.query).withNormalizedSort()
        _ui.update { it.copy(filters = next) }
        if (previous.explicit != next.explicit) {
            viewModelScope.launch { container.settings.setShowExplicit(next.explicit) }
        }
        schedule(0)
    }

    fun updateFilters(transform: (BrowseFilters) -> BrowseFilters) = applyFilters(transform(_ui.value.filters))

    fun resetFilters() {
        val explicit = _ui.value.filters.explicit
        applyFilters(
            _ui.value.filters
                .resetFilters()
                .copy(explicit = explicit),
        )
    }

    fun onCategoryChange(value: SearchCategory?) {
        val genre =
            value?.let { category -> BrowseGenre.entries.firstOrNull { it.id == category.genreId } }
                ?: BrowseGenre.ANY
        updateFilters { it.copy(genre = genre) }
    }

    fun retry() = schedule(0)

    fun useRecent(query: String) = onQueryChange(query)

    fun remember() {
        val query = _ui.value.query
        viewModelScope.launch { container.settings.addRecentSearch(query) }
    }

    fun removeRecent(query: String) = viewModelScope.launch { container.settings.removeRecentSearch(query) }

    fun clearRecents() = viewModelScope.launch { container.settings.clearRecentSearches() }

    fun loadMore() {
        val state = _ui.value
        val next = cursor ?: return
        if (state.phase != SearchPhase.DONE || state.isLoadingMore) return
        val token = generation
        val filters = state.filters
        _ui.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        viewModelScope.launch {
            val result = container.catalog.browse(filters, next)
            if (token != generation) return@launch
            if (result.error != null) {
                _ui.update { it.copy(isLoadingMore = false, loadMoreError = result.error) }
                return@launch
            }
            val exhausted = result.nextCursor == null || (result.items.isEmpty() && result.nextCursor == next)
            cursor = if (exhausted) null else result.nextCursor
            _ui.update {
                it.copy(
                    results = (it.results + rankPage(filters, result.items)).distinctBy { beatmap -> beatmap.id },
                    isLoadingMore = false,
                    hasMore = !exhausted,
                )
            }
        }
    }

    private fun schedule(delayMs: Long) {
        job?.cancel()
        val token = ++generation
        cursor = null
        if (_ui.value.isIdle) {
            _ui.update {
                it.copy(phase = SearchPhase.IDLE, results = emptyList(), error = null, hasMore = false, total = null)
            }
            return
        }
        job =
            viewModelScope.launch {
                if (delayMs > 0) delay(delayMs)
                _ui.update { it.copy(phase = SearchPhase.LOADING, error = null, hasMore = false, isLoadingMore = false) }
                val requested = _ui.value.filters
                val result = container.catalog.browse(requested, null)
                if (token != generation) return@launch
                cursor = result.nextCursor
                _ui.update {
                    it.copy(
                        phase = SearchPhase.DONE,
                        results = rankPage(requested, result.items).distinctBy { beatmap -> beatmap.id },
                        total = result.total,
                        source = result.source,
                        error = result.error,
                        hasMore = result.nextCursor != null,
                    )
                }
            }
    }

    /** Only relevance searches are re-ranked; any explicit sort order is respected as-is. */
    private fun rankPage(filters: BrowseFilters, items: List<Beatmap>): List<Beatmap> =
        if (filters.sort.option == BrowseSortField.RELEVANCE && filters.trimmedQuery.isNotEmpty()) {
            SearchRanker.rank(vn.kozu.data.repository.OsuSearchSyntax.parse(filters.trimmedQuery).text, items)
        } else {
            items
        }

    companion object {
        val Factory = containerFactory { SearchViewModel(it) }
    }
}
