package vn.kozu.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.kozu.R
import vn.kozu.data.lazer.ExportCandidate
import vn.kozu.data.lazer.LazerExporter
import vn.kozu.data.lazer.LazerImportState
import vn.kozu.ui.components.KozuSheetHandle

private const val EXPORT_BATCH_SIZE = 20

@Composable
fun LazerPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val container = viewModel.appContainer
    val importer = container.lazerImporter
    val exporter = container.lazerExporter
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by importer.state.collectAsStateWithLifecycle()
    var exportOpen by rememberSaveable { mutableStateOf(false) }
    var libraryOpen by rememberSaveable { mutableStateOf(false) }
    val installed = remember { exporter.isLazerInstalled() }
    val busy = state.running || state.scanning

    val filesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) importer.importUris(uris)
        }
    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
            if (tree != null) importer.importTree(tree)
        }

    val openLibrary = stringResource(R.string.lazer_library_open)
    val pickFiles = stringResource(R.string.lazer_pick_files)
    val pickFolder = stringResource(R.string.lazer_pick_folder)
    val chooseBeatmaps = stringResource(R.string.lazer_choose_beatmaps)
    val statusValue = stringResource(if (installed) R.string.lazer_installed else R.string.lazer_not_installed)
    val statusHint = if (installed) null else stringResource(R.string.lazer_not_installed_hint)
    val statusTitle = stringResource(R.string.lazer_status)

    SettingsScaffold(title = stringResource(R.string.lazer_title), onBack = onBack) {
        SettingsSection(null) {
            custom { shape ->
                NoteCard(shape, stringResource(R.string.lazer_how_title), stringResource(R.string.lazer_how_body))
            }
        }
        SettingsSection(stringResource(R.string.lazer_library_section)) {
            action(Setting.LAZER_LIBRARY, label = openLibrary, enabled = !busy) { libraryOpen = true }
        }
        SettingsSection(stringResource(R.string.lazer_import_section)) {
            action(Setting.LAZER_IMPORT_FILES, label = pickFiles, enabled = !busy) {
                filesLauncher.launch(arrayOf("*/*"))
            }
            action(Setting.LAZER_IMPORT_FOLDER, label = pickFolder, enabled = !busy) {
                folderLauncher.launch(null)
            }
            switch(Setting.LAZER_ADD_TO_GAME, settings.lazerAddToGame) { enabled ->
                viewModel.save { setLazerAddToGame(enabled) }
            }
            switch(Setting.LAZER_REPLACE, settings.lazerReplaceExisting) { enabled ->
                viewModel.save { setLazerReplaceExisting(enabled) }
            }
        }
        if (busy || state.finished) {
            SettingsSection(null) {
                custom { shape -> ImportProgressCard(shape, state, onClear = importer::clearResult) }
            }
        }
        SettingsSection(stringResource(R.string.lazer_export_section)) {
            info(Icons.Rounded.CheckCircle, statusTitle, statusValue, statusHint)
            action(Setting.LAZER_EXPORT, label = chooseBeatmaps) { exportOpen = true }
        }
    }

    if (libraryOpen) {
        LazerLibrarySheet(
            library = container.lazerLibrary,
            importer = importer,
            libraryIds = { container.downloadsStore.sets.first().map { it.beatmap.id }.toSet() },
            onDismiss = { libraryOpen = false },
        )
    }

    if (exportOpen) {
        LazerExportSheet(exporter = exporter, installed = installed, onDismiss = { exportOpen = false })
    }
}

@Composable
private fun ImportProgressCard(
    shape: Shape,
    state: LazerImportState,
    onClear: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.scanning -> {
                    Text(stringResource(R.string.lazer_progress_scanning), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                state.running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.lazer_import_files),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.lazer_progress_counts, state.processed, state.total),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    if (state.current.isNotBlank()) {
                        Text(
                            state.current,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                else -> {
                    Text(stringResource(R.string.lazer_result_title), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (state.processed > 0 || state.finished) {
                Text(
                    stringResource(R.string.lazer_result_body, state.imported, state.replaced, state.skipped, state.failed),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.problems.forEach { problem ->
                Text(
                    problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.finished && !state.running) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.lazer_clear_result)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazerExportSheet(
    exporter: LazerExporter,
    installed: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<ExportCandidate>?>(null) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var plan by remember { mutableStateOf<List<List<ExportCandidate>>>(emptyList()) }
    var nextBatch by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { candidates = exporter.candidates(includeGame = true) }

    val visible =
        remember(candidates, query) {
            val needle = query.trim().lowercase()
            candidates.orEmpty().filter {
                needle.isEmpty() ||
                    it.title.lowercase().contains(needle) ||
                    it.artist.lowercase().contains(needle) ||
                    it.mapper.lowercase().contains(needle)
            }
        }

    fun sendBatch(
        batches: List<List<ExportCandidate>>,
        index: Int,
    ) {
        val batch = batches[index]
        scope.launch {
            working = true
            try {
                if (index == 0) exporter.cleanup()
                val files = ArrayList<File>()
                batch.forEachIndexed { position, candidate ->
                    status = context.getString(R.string.lazer_preparing, position + 1, batch.size)
                    files += exporter.build(candidate)
                }
                val intent =
                    exporter.shareIntent(
                        files = files,
                        chooserTitle = context.getString(R.string.lazer_send_chooser),
                        preferLazer = installed,
                    )
                if (exporter.launch(context, intent)) {
                    nextBatch = index + 1
                    status = context.getString(R.string.lazer_batch_sent, index + 1, batches.size)
                } else {
                    status = context.getString(R.string.lazer_no_app)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = context.getString(R.string.lazer_export_failed, e.message ?: e.javaClass.simpleName)
            } finally {
                working = false
            }
        }
    }

    val chosen = candidates.orEmpty().filter { it.id in selected }

    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
            if (tree == null || chosen.isEmpty()) return@rememberLauncherForActivityResult
            scope.launch {
                working = true
                try {
                    val saved =
                        exporter.saveToFolder(tree, chosen) { done ->
                            status = context.getString(R.string.lazer_preparing, done, chosen.size)
                        }
                    status = context.getString(R.string.lazer_saved, saved)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    status = context.getString(R.string.lazer_export_failed, e.message ?: e.javaClass.simpleName)
                } finally {
                    working = false
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.lazer_export_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val loaded = candidates
            when {
                loaded == null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.lazer_loading), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                loaded.isEmpty() -> {
                    Text(
                        stringResource(R.string.lazer_export_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }

                else -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.lazer_export_search)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.lazer_selected_count, selected.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { selected = selected + visible.map { it.id } }) {
                            Text(stringResource(R.string.lazer_select_all))
                        }
                        TextButton(onClick = { selected = emptySet() }, enabled = selected.isNotEmpty()) {
                            Text(stringResource(R.string.lazer_select_none))
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(visible, key = { it.id }) { candidate ->
                            val checked = candidate.id in selected
                            Surface(
                                onClick = {
                                    selected = if (checked) selected - candidate.id else selected + candidate.id
                                },
                                shape = MaterialTheme.shapes.large,
                                color =
                                    if (checked) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selected = if (it) selected + candidate.id else selected - candidate.id
                                        },
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            candidate.title.ifBlank { "#${candidate.id}" },
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            listOf(candidate.artist, candidate.mapper)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (candidate.fromGame) {
                                        Icon(
                                            Icons.Rounded.SportsEsports,
                                            contentDescription = stringResource(R.string.lazer_game_tag),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                        )
                                    }
                                    if (candidate.sizeBytes > 0L) {
                                        Text(
                                            formatBytes(candidate.sizeBytes),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            val remaining = plan.size - nextBatch
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { folderLauncher.launch(null) },
                    enabled = chosen.isNotEmpty() && !working,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.lazer_save_to_folder)) }
                Button(
                    onClick = {
                        if (remaining > 0) {
                            sendBatch(plan, nextBatch)
                        } else {
                            val batches = chosen.chunked(EXPORT_BATCH_SIZE)
                            plan = batches
                            nextBatch = 0
                            sendBatch(batches, 0)
                        }
                    },
                    enabled = (chosen.isNotEmpty() || remaining > 0) && !working,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            remaining > 0 -> stringResource(R.string.lazer_send_next, nextBatch + 1, plan.size)
                            installed -> stringResource(R.string.lazer_send_to_lazer)
                            else -> stringResource(R.string.lazer_send_with)
                        },
                    )
                }
            }
        }
    }
}
