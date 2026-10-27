package vn.kozu.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape

/**
 * Full screen Search overlay allowing users to search songs, artists, and mappers.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        ScreenHeader(
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(12.dp))
                HeaderTitle("Search")
            },
            actions = {
                SettingsAction(onOpenSettings)
            }
        )

        Spacer(Modifier.height(16.dp))
        SearchField(
            query = ui.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                viewModel.remember()
                keyboard?.hide()
            }
        )

        val isIdle = ui.query.isBlank() && ui.category == null
        if (isIdle) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
            ) {
                if (recents.isNotEmpty()) {
                    item(key = "recents") {
                        RecentSearches(
                            recents = recents,
                            onPick = viewModel::useRecent,
                            onRemove = { viewModel.removeRecent(it) },
                            onClear = { viewModel.clearRecents() }
                        )
                    }
                }
                item(key = "browse") {
                    BrowseAll(onPick = viewModel::onCategoryChange)
                }
            }
        } else {
            FilterRow(
                sort = ui.sort,
                category = ui.category,
                onSort = viewModel::onSortChange,
                onClearCategory = { viewModel.onCategoryChange(null) }
            )
            when (ui.phase) {
                SearchPhase.IDLE -> Unit
                SearchPhase.LOADING -> KozuLoading()
                SearchPhase.DONE -> {
                    if (ui.results.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            title = if (ui.error == null) "No results found" else "Catalog unavailable",
                            message = ui.error ?: "Try searching for a different song, artist or mapper.",
                            action = if (ui.error == null) null else ({
                                TextButton(onClick = viewModel::retry) { Text("Retry") }
                            })
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ui.results, key = { it.id }) { beatmap ->
                                TrackRow(
                                    beatmap = beatmap,
                                    onClick = {
                                        viewModel.remember()
                                        keyboard?.hide()
                                        actions.play(ui.results, ui.results.indexOf(beatmap))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.titleMedium,
        placeholder = { Text("Songs, artists or mappers") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun RecentSearches(
    recents: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent searches", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            recents.forEachIndexed { index, query ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(pressedScale = 0.98f) { onPick(query) },
                    shape = segmentedShape(index, recents.size),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onRemove(query) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove recent search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrowseAll(onPick: (SearchCategory) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Browse all", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        val categories = SearchCategory.entries.chunked(2)
        val colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEachIndexed { colIndex, cat ->
                        val cardColor = colors[(rowIndex * 2 + colIndex) % colors.size]
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .bounceClick(pressedScale = 0.96f) { onPick(cat) },
                            shape = RoundedCornerShape(24.dp),
                            color = cardColor
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cat.emoji,
                                    style = MaterialTheme.typography.headlineLarge,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    sort: SearchSort,
    category: SearchCategory?,
    onSort: (SearchSort) -> Unit,
    onClearCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null) {
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(category.title) },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, Modifier.size(16.dp)) }
            )
        }
        SearchSort.entries.forEach { s ->
            FilterChip(
                selected = sort == s,
                onClick = { onSort(s) },
                label = { Text(s.label) }
            )
        }
    }
}
