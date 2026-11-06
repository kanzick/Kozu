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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BrowseFilters
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
    /** False when nothing matched the name exactly and the raw search results are shown instead. */
    val exact: Boolean = true,
)

/**
 * Loads everything a single artist or mapper has in the catalog.
 *
 * The catalog only offers free-text search, so the name is searched and the hits are then
 * narrowed to the ones that really belong to that person. Several pages are pulled so a prolific
 * artist is not cut off after the first handful of results.
 */
class PersonViewModel(
    private val container: AppContainer,
    val kind: PersonKind,
    val name: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(PersonUiState())
    val ui: StateFlow<PersonUiState> = _ui.asStateFlow()

    private var cursor: String? = null
    private var filters = BrowseFilters(query = name)
    private val raw = mutableListOf<Beatmap>()
    private var job: Job? = null

    init {
        load()
    }

    fun retry() {
        raw.clear()
        cursor = null
        _ui.value = PersonUiState()
        load()
    }

    fun loadMore() {
        val state = _ui.value
        if (!state.hasMore || state.isLoading || state.isLoadingMore) return
        _ui.update { it.copy(isLoadingMore = true) }
        job?.cancel()
        job = viewModelScope.launch { pull(maxPages = 3, wanted = state.items.size + 24) }
    }

    private fun load() {
        job?.cancel()
        job =
            viewModelScope.launch {
                val showExplicit = container.settings.settings.first().showExplicit
                filters = filters.copy(explicit = showExplicit)
                pull(maxPages = 6, wanted = 30)
            }
    }

    private suspend fun pull(
        maxPages: Int,
        wanted: Int,
    ) {
        var pages = 0
        var error: String? = null
        var first = cursor == null && raw.isEmpty()
        while (pages < maxPages) {
            val result = container.catalog.browse(filters, cursor.takeUnless { first })
            first = false
            pages++
            if (result.error != null && result.items.isEmpty()) {
                error = result.error
                break
            }
            raw += result.items
            val exhausted = result.nextCursor == null || result.nextCursor == cursor
            cursor = if (exhausted) null else result.nextCursor
            if (cursor == null || matching().size >= wanted) break
        }
        publish(error)
    }

    private fun matching(): List<Beatmap> = raw.distinctBy { it.id }.filter(::belongs)

    private fun publish(error: String?) {
        val matches = matching()
        val exact = matches.isNotEmpty() || raw.isEmpty()
        val shown = if (exact) matches else raw.distinctBy { it.id }
        _ui.value =
            PersonUiState(
                items = shown,
                isLoading = false,
                isLoadingMore = false,
                hasMore = cursor != null,
                error = if (shown.isEmpty()) error else null,
                exact = exact,
            )
    }

    private fun belongs(beatmap: Beatmap): Boolean =
        when (kind) {
            PersonKind.MAPPER -> beatmap.mapper.equals(name, ignoreCase = true)
            PersonKind.ARTIST ->
                artistNames(beatmap.artist).any { it.equals(name, ignoreCase = true) } ||
                    artistNames(beatmap.artistUnicode).any { it.equals(name, ignoreCase = true) }
        }

    /** "A feat. B", "A & B", "A, B", "A x B" all credit both people. */
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
