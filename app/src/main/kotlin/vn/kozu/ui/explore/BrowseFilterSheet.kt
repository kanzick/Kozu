package vn.kozu.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.BrowseExtra
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseGeneral
import vn.kozu.domain.model.BrowseGenre
import vn.kozu.domain.model.BrowseLanguage
import vn.kozu.domain.model.BrowsePlayed
import vn.kozu.domain.model.BrowseRank
import vn.kozu.domain.model.BrowseSortField
import vn.kozu.domain.model.BrowseStatus
import vn.kozu.domain.model.ModeFilter
import vn.kozu.ui.components.LocalIsSupporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseFilterSheet(
    initial: BrowseFilters,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onApply: (BrowseFilters) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val isSupporter = LocalIsSupporter.current
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilterSection("Mode") {
                    SingleChoiceChips(ModeFilter.entries, draft.mode, { it.label }) {
                        draft = draft.copy(mode = it)
                    }
                }
                FilterSection("Status") {
                    SingleChoiceChips(
                        options = BrowseStatus.entries.filter { isLoggedIn || !it.requiresLogin },
                        selected = draft.status,
                        label = { it.label },
                    ) { draft = draft.copy(status = it) }
                }
                FilterSection("Genre") {
                    SingleChoiceChips(BrowseGenre.entries, draft.genre, { it.label }) {
                        draft = draft.copy(genre = it)
                    }
                }
                FilterSection("Language") {
                    SingleChoiceChips(BrowseLanguage.entries, draft.language, { it.label }) {
                        draft = draft.copy(language = it)
                    }
                }
                FilterSection("Extra") {
                    MultiChoiceChips(BrowseExtra.entries, draft.extras, { it.label }) {
                        draft = draft.copy(extras = draft.extras.toggle(it))
                    }
                }
                FilterSection("General") {
                    MultiChoiceChips(
                        options = BrowseGeneral.entries.filter { isLoggedIn || !it.requiresLogin },
                        selected = draft.general,
                        label = { it.label },
                    ) { draft = draft.copy(general = draft.general.toggle(it)) }
                }
                if (isLoggedIn) {
                    FilterSection(
                        title = "Played",
                        hint = if (isSupporter) null else "osu!supporter feature",
                    ) {
                        SingleChoiceChips(BrowsePlayed.entries, draft.played, { it.label }) {
                            draft = draft.copy(played = it)
                        }
                    }
                    FilterSection(
                        title = "Rank achieved",
                        hint = if (isSupporter) null else "osu!supporter feature",
                    ) {
                        MultiChoiceChips(BrowseRank.entries, draft.ranks, { it.label }) {
                            draft = draft.copy(ranks = draft.ranks.toggle(it))
                        }
                    }
                }
                FilterSection("Content") {
                    MultiChoiceChips(listOf(true), if (draft.explicit) setOf(true) else emptySet(), { "Show explicit content" }) {
                        draft = draft.copy(explicit = !draft.explicit)
                    }
                }
                FilterSection("Sort") {
                    SingleChoiceChips(BrowseSortField.entries, draft.sort.option, { it.label }) {
                        draft = draft.copy(sortField = it)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DirectionChip(
                            label = "Descending",
                            selected = !draft.ascending,
                            ascending = false,
                        ) { draft = draft.copy(ascending = false) }
                        DirectionChip(
                            label = "Ascending",
                            selected = draft.ascending,
                            ascending = true,
                        ) { draft = draft.copy(ascending = true) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { draft = draft.resetFilters() },
                        enabled = draft.hasActiveFilters,
                    ) { Text("Reset") }
                    Button(
                        onClick = { onApply(draft) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SingleChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            ChoiceChip(label(option), option == selected) { onSelect(option) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChoiceChips(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            ChoiceChip(label(option), option in selected) { onToggle(option) }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun DirectionChip(
    label: String,
    selected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
