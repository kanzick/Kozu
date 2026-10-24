package vn.kozu.ui.search

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.SearchCategory
import vn.kozu.domain.model.SearchSort
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies

@Composable
fun SearchScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = { HeaderTitle("Search") },
            actions = { SettingsAction(onOpenSettings) }
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
                contentPadding = PaddingValues(top = 20.dp, bottom = bottomPadding)
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
            when {
                ui.phase == SearchPhase.LOADING -> KozuLoading(Modifier.padding(top = 40.dp))
                ui.results.isEmpty() && ui.phase == SearchPhase.DONE -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No matching beatmaps",
                    message = ui.error ?: "Try another song, artist or mapper.",
                    shape = KozuCookies.Clover4,
                    modifier = Modifier.padding(top = 32.dp)
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (ui.isSample) {
                        item(key = "sample") {
                            SampleNote(ui.error)
                        }
                    }
                    itemsIndexed(ui.results, key = { _, b -> b.id }) { index, beatmap ->
                        TrackRow(
                            beatmap = beatmap,
                            shape = segmentedShape(index, ui.results.size),
                            onClick = {
                                viewModel.remember()
                                actions.play(ui.results, index)
                            }
                        )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.bounceClick(onClick = onClear)
            )
        }
        Spacer(Modifier.height(10.dp))
        recents.forEach { recent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(pressedScale = 0.98f) { onPick(recent) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Text(
                    recent,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { onRemove(recent) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Lưới thể loại: mỗi ô một màu và một hình khác nhau. */
@Composable
private fun BrowseAll(onPick: (SearchCategory) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer
    )
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Browse all", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        SearchCategory.entries.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { colIndex, category ->
                    val (container, content) = palette[(rowIndex + colIndex) % palette.size]
                    CategoryTile(
                        category = category,
                        container = container,
                        content = content,
                        shapeIndex = rowIndex * 2 + colIndex,
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(category) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CategoryTile(
    category: SearchCategory,
    container: Color,
    content: Color,
    shapeIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(108.dp)
            .bounceClick(pressedScale = 0.95f, onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        color = container,
        contentColor = content
    ) {
        Box(Modifier.padding(18.dp)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(52.dp),
                shape = KozuCookies.forIndex(shapeIndex),
                color = content.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(category.emoji, fontSize = 26.sp)
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
            .padding(top = 14.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null) {
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text("${category.emoji} ${category.title}") },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Clear genre", modifier = Modifier.size(18.dp)) }
            )
        }
        SearchSort.entries.forEach { option ->
            FilterChip(
                selected = option == sort,
                onClick = { onSort(option) },
                label = { Text(option.label) }
            )
        }
    }
}

@Composable
private fun SampleNote(error: String?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                text = error ?: "Searching sample songs only. Add osu! API keys in Settings for the full catalog.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
