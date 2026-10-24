package vn.kozu.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import vn.kozu.ui.containerFactory

enum class SearchPhase { IDLE, LOADING, DONE }

data class SearchUiState(
    val query: String = "",
    val sort: SearchSort = SearchSort.RELEVANCE,
    val category: SearchCategory? = null,
    val phase: SearchPhase = SearchPhase.IDLE,
    val results: List<Beatmap> = emptyList(),
    val isSample: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    private val request = MutableStateFlow(SearchUiState())

    val recentSearches: StateFlow<List<String>> = container.settings.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<SearchUiState> = _ui

    init {
        viewModelScope.launch {
            // Gõ tới đâu tìm tới đó (chờ 350ms sau lần gõ cuối); đổi từ khoá thì huỷ yêu cầu cũ.
            request
                .debounce(350)
                .distinctUntilChanged { old, new ->
                    old.query.trim() == new.query.trim() && old.sort == new.sort && old.category == new.category
                }
                .collectLatest { req ->
                    val query = req.query.trim()
                    if (query.isEmpty() && req.category == null) {
                        _ui.update { it.copy(phase = SearchPhase.IDLE, results = emptyList(), error = null) }
                    } else {
                        _ui.update { it.copy(phase = SearchPhase.LOADING, error = null) }
                        val result = container.catalog.search(query, req.sort, req.category)
                        _ui.update {
                            it.copy(
                                phase = SearchPhase.DONE,
                                results = result.items,
                                isSample = result.isSample,
                                error = result.error
                            )
                        }
                    }
                }
        }
    }

    fun onQueryChange(value: String) = push { it.copy(query = value) }
    fun onSortChange(value: SearchSort) = push { it.copy(sort = value) }
    fun onCategoryChange(value: SearchCategory?) = push { it.copy(category = value) }

    fun useRecent(query: String) = push { it.copy(query = query) }

    /** Gọi khi người dùng nhấn Search trên bàn phím hoặc chọn một kết quả. */
    fun remember() {
        val query = _ui.value.query
        viewModelScope.launch { container.settings.addRecentSearch(query) }
    }

    fun removeRecent(query: String) = viewModelScope.launch { container.settings.removeRecentSearch(query) }
    fun clearRecents() = viewModelScope.launch { container.settings.clearRecentSearches() }

    private fun push(transform: (SearchUiState) -> SearchUiState) {
        _ui.update(transform)
        request.value = _ui.value
    }

    companion object {
        val Factory = containerFactory { SearchViewModel(it) }
    }
}
