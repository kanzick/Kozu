package vn.kozu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.kozu.model.Beatmap
import vn.kozu.model.demoBeatmaps
import vn.kozu.ui.components.BeatmapCover
import vn.kozu.ui.components.StarRatingBadge

data class SearchUiState(
    val query: String = "",
    val results: List<Beatmap> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState(results = demoBeatmaps))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            executeSearch()
        }
    }

    fun search() {
        searchJob?.cancel()
        executeSearch()
    }

    private fun executeSearch() {
        val query = _uiState.value.query.trim()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                hasSearched = query.isNotBlank(),
                errorMessage = null
            )
            try {
                delay(120)
                val filtered = if (query.isBlank()) {
                    demoBeatmaps
                } else {
                    demoBeatmaps.filter { beatmap ->
                        beatmap.title.contains(query, ignoreCase = true) ||
                                beatmap.artist.contains(query, ignoreCase = true) ||
                                beatmap.mapper.contains(query, ignoreCase = true) ||
                                beatmap.genre.contains(query, ignoreCase = true)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    results = filtered,
                    isLoading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    isLoading = false,
                    errorMessage = "Không thể tìm kiếm. Hãy thử lại."
                )
            }
        }
    }
}

@Composable
fun SearchScreen(
    onBeatmapSelected: (Beatmap) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions = listOf("YOASOBI", "Lemon", "Toby Fox", "Bad Apple", "xi", "Iyowa")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = "DISCOVER BEATMAPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Search songs",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A favorite track might be one map away.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CircleShape,
                placeholder = {
                    Text(
                        "Search by title, artist, or mapper...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    val isSelected = state.query.equals(suggestion, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.onQueryChanged(suggestion)
                            viewModel.search()
                        },
                        label = { Text(suggestion) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Searching beatmaps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                state.errorMessage != null -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                state.results.isNotEmpty() -> {
                    Text(
                        text = "MATCHES (${state.results.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(items = state.results, key = { it.id }) { beatmap ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { onBeatmapSelected(beatmap) },
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BeatmapCover(
                                        beatmap = beatmap,
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = beatmap.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = beatmap.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StarRatingBadge(rating = beatmap.starRating)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "map: ${beatmap.mapper}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                state.hasSearched -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No matching beatmaps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Try searching by song name or artist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Start searching",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Type any song title, artist, or anime title.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

