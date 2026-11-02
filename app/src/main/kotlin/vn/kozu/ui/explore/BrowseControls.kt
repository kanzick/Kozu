package vn.kozu.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kozu.data.repository.CatalogSource
import vn.kozu.domain.model.ActiveBrowseFilter
import vn.kozu.ui.components.HeaderAction
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FilterAction(
    active: Boolean,
    onClick: () -> Unit,
) {
    Box {
        HeaderAction(
            icon = Icons.Rounded.Tune,
            contentDescription = if (active) "Filters, some active" else "Filters",
            onClick = onClick,
            startPadding = 0.dp,
        )
        if (active) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

@Composable
internal fun BrowseSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 88.dp),
        singleLine = false,
        minLines = 1,
        maxLines = 2,
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = "Search title, artist or mapper",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
internal fun BrowseSummary(
    ui: BrowseUiState,
    modifier: Modifier = Modifier,
) {
    val total = ui.total
    val count =
        when {
            ui.isLoading -> "Searching..."
            total != null -> "${NumberFormat.getIntegerInstance(Locale.US).format(total)} ${if (total == 1) "result" else "results"}"
            ui.items.isNotEmpty() -> "${ui.items.size}${if (ui.hasMore) "+" else ""} results"
            else -> "No results"
        }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(
            text = "Sorted by ${ui.filters.sort.label}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun BrowseSourceNote(
    ui: BrowseUiState,
    modifier: Modifier = Modifier,
) {
    if (ui.isLoading || ui.source != CatalogSource.PUBLIC_MIRROR) return
    val text =
        if (ui.ignoredFilters.isEmpty()) {
            "Public mirror in use. Add osu! API credentials for official results."
        } else {
            "Public mirror in use. Not applied: ${ui.ignoredFilters.joinToString(", ")}."
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun ActiveFilterChips(
    filters: List<ActiveBrowseFilter>,
    onRemove: (ActiveBrowseFilter) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = true,
                onClick = { onRemove(filter) },
                label = { Text(filter.label, maxLines = 1) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove ${filter.label}",
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        TextButton(onClick = onReset) { Text("Reset") }
    }
}
