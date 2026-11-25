package vn.kozu.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseStatus
import vn.kozu.ui.container

enum class PersonKind(val title: String) {
    ARTIST("Artist"),
    MAPPER("Mapper"),
}

data class PersonUiState(
    val items: List<Beatmap> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val exact: Boolean = true,
    val filters: BrowseFilters = BrowseFilters(),
    val isLoggedIn: Boolean = false,
)

class PersonViewModel(
    private val container: AppContainer,
    val kind: PersonKind,
    val name: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(PersonUiState(filters = defaultFilters()))
    val ui: StateFlow<PersonUiState> = _ui.asStateFlow()

    private var cursor: String? = null
    private var filters = defaultFilters()
    private val raw = mutableListOf<Beatmap>()
    private var job: Job? = null
    private var generation = 0

    init {
        viewModelScope.launch {
            filters = filters.copy(explicit = container.settings.settings.first().showExplicit)
            _ui.update { it.copy(filters = filters) }
            reload()
        }
        viewModelScope.launch {
            container.accounts.loggedIn.collect { signedIn ->
                _ui.update { it.copy(isLoggedIn = signedIn) }
                if (!signedIn) {
                    val clean = filters.withoutUserScoped().let {
                        if (filters.status.requiresLogin) it.copy(status = BrowseStatus.ANY) else it
                    }
                    if (clean != filters) applyFilters(clean)
                }
            }
        }
    }

    fun applyFilters(updated: BrowseFilters) {
        val previous = filters
        val next = updated.copy(query = name).withNormalizedSort()
        if (next == previous) return
        filters = next
        if (previous.explicit != next.explicit) {
            viewModelScope.launch { container.settings.setShowExplicit(next.explicit) }
        }
        reload()
    }

    fun resetFilters() {
        applyFilters(defaultFilters().copy(explicit = filters.explicit))
    }

    fun retry() = reload()

    fun loadMore() {
        val state = _ui.value
        if (!state.hasMore || state.isLoading || state.isLoadingMore) return
        _ui.update { it.copy(isLoadingMore = true) }
        job?.cancel()
        val token = generation
        job = viewModelScope.launch { pull(maxPages = 3, wanted = state.items.size + 24, token = token) }
    }

    private fun reload() {
        job?.cancel()
        generation++
        raw.clear()
        cursor = null
        val token = generation
        _ui.update {
            it.copy(
                items = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                hasMore = false,
                error = null,
                exact = true,
                filters = filters,
            )
        }
        job = viewModelScope.launch { pull(maxPages = 6, wanted = 30, token = token) }
    }

    private suspend fun pull(
        maxPages: Int,
        wanted: Int,
        token: Int,
    ) {
        var pages = 0
        var error: String? = null
        while (pages < maxPages) {
            if (token != generation) return
            val requestedCursor = cursor
            val result = container.catalog.browse(filters, requestedCursor)
            if (token != generation) return
            pages++
            if (result.error != null && result.items.isEmpty()) {
                error = result.error
                break
            }
            raw += result.items
            val exhausted = result.nextCursor == null || result.nextCursor == requestedCursor
            cursor = if (exhausted) null else result.nextCursor
            if (exhausted || matching().size >= wanted) break
        }
        if (token == generation) publish(error)
    }

    private fun matching(): List<Beatmap> = raw.distinctBy { it.id }.filter(::belongs)

    private fun publish(error: String?) {
        val matches = matching()
        val exact = matches.isNotEmpty() || raw.isEmpty()
        val shown = if (exact) matches else raw.distinctBy { it.id }
        _ui.update {
            it.copy(
                items = shown,
                isLoading = false,
                isLoadingMore = false,
                hasMore = cursor != null,
                error = if (shown.isEmpty()) error else null,
                exact = exact,
                filters = filters,
            )
        }
    }

    private fun defaultFilters(): BrowseFilters =
        BrowseFilters(query = name, status = BrowseStatus.ANY)

    private fun belongs(beatmap: Beatmap): Boolean =
        when (kind) {
            PersonKind.MAPPER -> beatmap.mapper.equals(name, ignoreCase = true)
            PersonKind.ARTIST ->
                artistNames(beatmap.artist).any { it.equals(name, ignoreCase = true) } ||
                    artistNames(beatmap.artistUnicode).any { it.equals(name, ignoreCase = true) }
        }

    private fun artistNames(artist: String): List<String> =
        if (artist.isBlank()) {
            emptyList()
        } else {
            listOf(artist) +
                artist
                    .split(Regex("\\s+(?:feat\\.?|ft\\.?|featuring|vs\\.?|x|&|and)\\s+|\\s*[,;/]\\s*", RegexOption.IGNORE_CASE))
                    .map(String::trim)
                    .filter(String::isNotEmpty)
        }

    companion object {
        fun factory(
            kind: PersonKind,
            name: String,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PersonViewModel(container(), kind, name) }
            }
    }
}
