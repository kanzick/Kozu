package vn.kozu.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.kozu.R
import vn.kozu.data.lazer.LazerAccessException
import vn.kozu.data.lazer.LazerAccessProblem
import vn.kozu.data.lazer.LazerDataSource
import vn.kozu.data.lazer.LazerImporter
import vn.kozu.data.lazer.LazerCollection
import vn.kozu.data.lazer.LazerLibrary
import vn.kozu.data.lazer.LazerLibraryData
import vn.kozu.data.lazer.LazerLibraryFilter
import vn.kozu.data.lazer.LazerLibraryScope
import vn.kozu.data.lazer.LazerOrigin
import vn.kozu.data.lazer.LazerSort
import vn.kozu.data.lazer.LazerStatusGroup
import vn.kozu.data.lazer.LazerLibrarySet
import vn.kozu.data.lazer.shizuku.ShizukuAccess
import vn.kozu.data.lazer.shizuku.ShizukuState
import vn.kozu.ui.components.KozuSheetHandle

private sealed interface LibraryStage {
    data object Sources : LibraryStage

    class Reading(
        val copiedBytes: Long,
    ) : LibraryStage

    class Choose(
        val source: LazerDataSource,
        val data: LazerLibraryData,
    ) : LibraryStage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LazerLibrarySheet(
    library: LazerLibrary,
    importer: LazerImporter,
    libraryIds: suspend () -> Set<Long>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<LibraryStage>(LibraryStage.Sources) }
    var error by remember { mutableStateOf<String?>(null) }
    var shizuku by remember { mutableStateOf(ShizukuAccess.state(context)) }
    var direct by remember { mutableStateOf(library.directSource() != null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var existing by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var handedOver by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        while (stage is LibraryStage.Sources) {
            shizuku = ShizukuAccess.state(context)
            delay(1_200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val current = stage
            if (current is LibraryStage.Choose && !handedOver) {
                try {
                    current.source.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            direct = library.directSource() != null
        }
    LaunchedEffect(Unit) {
        if (library.needsLegacyStoragePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun scan(open: suspend () -> LazerDataSource) {
        scope.launch {
            error = null
            stage = LibraryStage.Reading(0L)
            var source: LazerDataSource? = null
            try {
                source = open()
                val data = library.scan(source) { copied -> stage = LibraryStage.Reading(copied) }
                existing = libraryIds()
                selected = data.sets.filter { it.expectedId !in existing }.map { it.key }.toSet()
                stage = LibraryStage.Choose(source, data)
            } catch (e: CancellationException) {
                source?.close()
                throw e
            } catch (e: Throwable) {
                source?.close()
                error = accessMessage(context, e)
                stage = LibraryStage.Sources
            }
        }
    }

    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
            if (tree != null) scan { library.treeSource(tree) }
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.lazer_library_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when (val current = stage) {
                LibraryStage.Sources -> {
                    Text(
                        stringResource(R.string.lazer_library_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    if (direct) {
                        SourceCard(
                            icon = Icons.Rounded.PhoneAndroid,
                            title = stringResource(R.string.lazer_source_direct),
                            subtitle = stringResource(R.string.lazer_source_direct_desc),
                        ) {
                            scan { library.directSource() ?: throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND) }
                        }
                    }
                    SourceCard(
                        icon = Icons.Rounded.Usb,
                        title = stringResource(R.string.lazer_source_shizuku),
                        subtitle = shizukuSubtitle(shizuku),
                    ) {
                        when (shizuku) {
                            ShizukuState.MISSING, ShizukuState.UNSUPPORTED ->
                                openLink(context, ShizukuAccess.DOWNLOAD_URL.toUri())

                            ShizukuState.STOPPED ->
                                context.packageManager
                                    .getLaunchIntentForPackage(ShizukuAccess.MANAGER_PACKAGE)
                                    ?.let { runCatching { context.startActivity(it) } }

                            ShizukuState.NEEDS_PERMISSION, ShizukuState.READY -> scan { library.shizukuSource() }
                        }
                    }
                    SourceCard(
                        icon = Icons.Rounded.FolderOpen,
                        title = stringResource(R.string.lazer_source_folder),
                        subtitle = stringResource(R.string.lazer_source_folder_desc),
                    ) { folderLauncher.launch(null) }
                }

                is LibraryStage.Reading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.lazer_reading), style = MaterialTheme.typography.bodyLarge)
                            if (current.copiedBytes > 0L) {
                                Text(
                                    stringResource(R.string.lazer_copied, formatBytes(current.copiedBytes)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                is LibraryStage.Choose ->
                    LibraryChooser(
                        stage = current,
                        existing = existing,
                        selected = selected,
                        onSelectedChange = { selected = it },
                        onBack = {
                            current.source.close()
                            stage = LibraryStage.Sources
                        },
                        onImport = { chosen, collections ->
                            handedOver = true
                            importer.importLibrary(current.source, library, chosen, collections)
                            onDismiss()
                        },
                    )
            }
        }
    }
}

@Composable
private fun LibraryChooser(
    stage: LibraryStage.Choose,
    existing: Set<Long>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onImport: (List<LazerLibrarySet>, List<LazerCollection>) -> Unit,
) {
    val data = stage.data
    var filter by remember { mutableStateOf(LazerLibraryFilter()) }
    var filtersOpen by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf(true) }
    val visible = remember(data, filter, existing) { filter.apply(data.sets, data.collections, existing) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (data.sets.isEmpty()) {
            Text(
                stringResource(R.string.lazer_library_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            Text(
                stringResource(R.string.lazer_found_count, data.sets.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { filter = filter.copy(query = it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.lazer_export_search)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = filtersOpen || filter.activeCount > 0,
                    onClick = { filtersOpen = !filtersOpen },
                    label = {
                        Text(
                            if (filter.activeCount > 0) {
                                stringResource(R.string.lazer_filters) + " (${filter.activeCount})"
                            } else {
                                stringResource(R.string.lazer_filters)
                            },
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                )
            }
            if (filtersOpen) {
                FilterPanel(
                    filter = filter,
                    collections = data.collections,
                    onChange = { filter = it },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.lazer_selected_count, selected.size),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.lazer_showing, visible.size, data.sets.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onSelectedChange(selected + visible.map { it.key }) }, enabled = visible.isNotEmpty()) {
                    Text(stringResource(R.string.lazer_select_shown))
                }
                TextButton(onClick = { onSelectedChange(emptySet()) }, enabled = selected.isNotEmpty()) {
                    Text(stringResource(R.string.lazer_select_none))
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(visible, key = { it.key }) { set ->
                    val checked = set.key in selected
                    val owned = set.expectedId in existing
                    val details =
                        listOfNotNull(
                            if (set.stars > 0.0) "★ " + String.format(java.util.Locale.US, "%.2f", set.stars) else null,
                            stringResource(R.string.lazer_diffs, set.difficulties),
                            statusLabel(LazerStatusGroup.of(set.status)),
                        ).joinToString(" · ")
                    Surface(
                        onClick = {
                            onSelectedChange(if (checked) selected - set.key else selected + set.key)
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
                                    onSelectedChange(if (it) selected + set.key else selected - set.key)
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    set.title.ifBlank { set.key },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOf(set.artist, set.author).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (set.hasMania) {
                                Icon(
                                    Icons.Rounded.SportsEsports,
                                    contentDescription = stringResource(R.string.lazer_game_tag),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (owned) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.lazer_scope_owned),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (data.collections.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.lazer_create_playlists), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.lazer_create_playlists_desc, data.collections.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = playlists, onCheckedChange = { playlists = it })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.lazer_back_to_sources))
            }
            Button(
                onClick = {
                    val chosen = data.sets.filter { it.key in selected }
                    val collections =
                        if (playlists) {
                            data.collections.filter { collection -> collection.sets.any { it.key in selected } }
                        } else {
                            emptyList()
                        }
                    onImport(chosen, collections)
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.lazer_import_selected, selected.size)) }
        }
    }
}

@Composable
private fun FilterPanel(
    filter: LazerLibraryFilter,
    collections: List<LazerCollection>,
    onChange: (LazerLibraryFilter) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChipRow(stringResource(R.string.lazer_filter_mode)) {
                listOf(0 to "osu!", 1 to "taiko", 2 to "catch", 3 to "mania").forEach { (mode, name) ->
                    FilterChip(
                        selected = mode in filter.modes,
                        onClick = {
                            onChange(filter.copy(modes = if (mode in filter.modes) filter.modes - mode else filter.modes + mode))
                        },
                        label = { Text(name) },
                    )
                }
            }
            ChipRow(stringResource(R.string.lazer_filter_status)) {
                LazerStatusGroup.entries.forEach { group ->
                    FilterChip(
                        selected = group in filter.statuses,
                        onClick = {
                            onChange(
                                filter.copy(
                                    statuses = if (group in filter.statuses) filter.statuses - group else filter.statuses + group,
                                ),
                            )
                        },
                        label = { Text(statusLabel(group)) },
                    )
                }
            }
            ChipRow(stringResource(R.string.lazer_filter_origin)) {
                val labels =
                    mapOf(
                        LazerOrigin.ALL to stringResource(R.string.lazer_all),
                        LazerOrigin.ONLINE to stringResource(R.string.lazer_origin_online),
                        LazerOrigin.LOCAL to stringResource(R.string.lazer_origin_local),
                    )
                LazerOrigin.entries.forEach { origin ->
                    FilterChip(
                        selected = filter.origin == origin,
                        onClick = { onChange(filter.copy(origin = origin)) },
                        label = { Text(labels.getValue(origin)) },
                    )
                }
            }
            ChipRow(stringResource(R.string.lazer_filter_library)) {
                val labels =
                    mapOf(
                        LazerLibraryScope.ALL to stringResource(R.string.lazer_all),
                        LazerLibraryScope.NEW to stringResource(R.string.lazer_scope_new),
                        LazerLibraryScope.OWNED to stringResource(R.string.lazer_scope_owned),
                    )
                LazerLibraryScope.entries.forEach { scope ->
                    FilterChip(
                        selected = filter.scope == scope,
                        onClick = { onChange(filter.copy(scope = scope)) },
                        label = { Text(labels.getValue(scope)) },
                    )
                }
            }
            ChipRow(stringResource(R.string.lazer_filter_stars)) {
                listOf(0, 3, 4, 5, 6).forEach { stars ->
                    FilterChip(
                        selected = filter.minStars == stars,
                        onClick = { onChange(filter.copy(minStars = stars)) },
                        label = {
                            Text(
                                if (stars == 0) stringResource(R.string.lazer_stars_any) else stringResource(R.string.lazer_stars_min, stars),
                            )
                        },
                    )
                }
            }
            if (collections.isNotEmpty()) {
                ChipRow(stringResource(R.string.lazer_filter_collection)) {
                    FilterChip(
                        selected = filter.collection < 0,
                        onClick = { onChange(filter.copy(collection = -1)) },
                        label = { Text(stringResource(R.string.lazer_all)) },
                    )
                    collections.forEachIndexed { index, collection ->
                        FilterChip(
                            selected = filter.collection == index,
                            onClick = { onChange(filter.copy(collection = index)) },
                            label = { Text("${collection.name} (${collection.sets.size})") },
                        )
                    }
                }
            }
            ChipRow(stringResource(R.string.lazer_filter_sort)) {
                val labels =
                    mapOf(
                        LazerSort.ARTIST to stringResource(R.string.lazer_sort_artist),
                        LazerSort.TITLE to stringResource(R.string.lazer_sort_title),
                        LazerSort.ADDED to stringResource(R.string.lazer_sort_added),
                        LazerSort.STARS to stringResource(R.string.lazer_sort_stars),
                    )
                LazerSort.entries.forEach { sort ->
                    FilterChip(
                        selected = filter.sort == sort,
                        onClick = { onChange(filter.copy(sort = sort)) },
                        label = { Text(labels.getValue(sort)) },
                    )
                }
                FilterChip(
                    selected = filter.descending,
                    onClick = { onChange(filter.copy(descending = !filter.descending)) },
                    label = { Text(stringResource(R.string.lazer_sort_descending)) },
                )
            }
            TextButton(
                onClick = { onChange(LazerLibraryFilter(query = filter.query)) },
                enabled = filter.activeCount > 0 || filter.sort != LazerSort.ARTIST || filter.descending,
            ) { Text(stringResource(R.string.lazer_filters_reset)) }
        }
    }
}

@Composable
private fun ChipRow(
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun statusLabel(group: LazerStatusGroup): String =
    stringResource(
        when (group) {
            LazerStatusGroup.RANKED -> R.string.lazer_status_ranked
            LazerStatusGroup.LOVED -> R.string.lazer_status_loved
            LazerStatusGroup.PENDING -> R.string.lazer_status_pending
            LazerStatusGroup.GRAVEYARD -> R.string.lazer_status_graveyard
            LazerStatusGroup.LOCAL -> R.string.lazer_status_local
        },
    )

@Composable
private fun SourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun shizukuSubtitle(state: ShizukuState): String =
    when (state) {
        ShizukuState.MISSING -> stringResource(R.string.lazer_shizuku_missing) + " · " + stringResource(R.string.lazer_shizuku_get)
        ShizukuState.STOPPED -> stringResource(R.string.lazer_shizuku_stopped)
        ShizukuState.UNSUPPORTED -> stringResource(R.string.lazer_shizuku_unsupported)
        ShizukuState.NEEDS_PERMISSION -> stringResource(R.string.lazer_shizuku_permission)
        ShizukuState.READY -> stringResource(R.string.lazer_source_shizuku_desc)
    }

private fun openLink(
    context: Context,
    uri: Uri,
) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun accessMessage(
    context: Context,
    error: Throwable,
): String =
    if (error is LazerAccessException) {
        when (error.problem) {
            LazerAccessProblem.SHIZUKU_MISSING -> context.getString(R.string.lazer_err_shizuku_missing)
            LazerAccessProblem.SHIZUKU_STOPPED -> context.getString(R.string.lazer_err_shizuku_stopped)
            LazerAccessProblem.SHIZUKU_SERVICE ->
                context.getString(R.string.lazer_err_shizuku_service, error.message ?: error.javaClass.simpleName)
            LazerAccessProblem.SHIZUKU_UNSUPPORTED -> context.getString(R.string.lazer_err_shizuku_unsupported)
            LazerAccessProblem.SHIZUKU_DENIED -> context.getString(R.string.lazer_err_shizuku_denied)
            LazerAccessProblem.DATA_NOT_FOUND -> context.getString(R.string.lazer_err_not_found)
            LazerAccessProblem.NOT_LAZER -> context.getString(R.string.lazer_err_not_lazer)
            LazerAccessProblem.STORAGE_FULL -> context.getString(R.string.lazer_err_storage_full)
            LazerAccessProblem.DATABASE ->
                context.getString(R.string.lazer_err_database, error.message ?: error.javaClass.simpleName)
        }
    } else {
        context.getString(R.string.lazer_err_database, error.message ?: error.javaClass.simpleName)
    }
