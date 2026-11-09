package vn.kozu.ui.settings

import vn.kozu.ui.i18n.kozuUiString
import vn.kozu.ui.components.KozuSheetHandle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.backup.BackupException
import vn.kozu.data.backup.BackupInfo
import vn.kozu.data.backup.BackupSection
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_BACKUP_BYTES = 25_000_000

private sealed interface BackupPicker {
    data object Create : BackupPicker

    data class Restore(
        val json: String,
        val info: BackupInfo,
    ) : BackupPicker
}

private data class BackupOutcome(
    val success: Boolean,
    val text: String,
)

@Composable
fun BackupPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backup = viewModel.appContainer.backup
    var infoDismissed by rememberSaveable { mutableStateOf(false) }
    var picker by remember { mutableStateOf<BackupPicker?>(null) }
    var pending by remember { mutableStateOf<Set<BackupSection>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<BackupOutcome?>(null) }

    val createLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                busy = true
                outcome =
                    try {
                        val json = backup.create(pending)
                        withContext(Dispatchers.IO) {
                            val stream =
                                context.contentResolver.openOutputStream(uri, "wt")
                                    ?: throw IOException("Could not open the selected file")
                            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        }
                        BackupOutcome(true, "Backup saved with ${pending.joinToString { it.label.lowercase() }}.")
                    } catch (e: BackupException) {
                        BackupOutcome(false, e.message ?: "Backup failed")
                    } catch (e: Exception) {
                        BackupOutcome(false, "Could not save the backup: ${e.message ?: "unknown error"}")
                    }
                busy = false
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                busy = true
                try {
                    val json =
                        withContext(Dispatchers.IO) {
                            val stream =
                                context.contentResolver.openInputStream(uri)
                                    ?: throw IOException("Could not open the selected file")
                            stream.use { readLimited(it) }
                        }
                    picker = BackupPicker.Restore(json, backup.inspect(json))
                } catch (e: BackupException) {
                    outcome = BackupOutcome(false, e.message ?: "Invalid backup")
                } catch (e: Exception) {
                    outcome = BackupOutcome(false, "Could not read the file: ${e.message ?: "unknown error"}")
                }
                busy = false
            }
        }

    SettingsScaffold(title = "Backup & Restore", onBack = onBack) {
        if (!infoDismissed) {
            SettingsSection(null) {
                custom { shape -> HowItWorksCard(shape) { infoDismissed = true } }
            }
        }
        SettingsSection("Your data") {
            action(
                Setting.EXPORT_BACKUP,
                label = if (busy) "Working…" else "Create backup",
                enabled = !busy,
            ) { picker = BackupPicker.Create }
            action(
                Setting.IMPORT_BACKUP,
                label = if (busy) "Working…" else "Restore backup",
                enabled = !busy,
            ) { restoreLauncher.launch(arrayOf("*/*")) }
        }
        outcome?.let { result ->
            SettingsSection("Last result") {
                custom { shape -> OutcomeCard(shape, result) }
            }
        }
    }

    when (val current = picker) {
        BackupPicker.Create -> {
            SectionSheet(
                title = "Create backup",
                confirm = "Choose file",
                available = BackupSection.entries,
                counts = emptyMap(),
                onConfirm = { selected ->
                    picker = null
                    pending = selected
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    createLauncher.launch("kozu-backup-$stamp.kozubackup")
                },
                onDismiss = { picker = null },
            )
        }

        is BackupPicker.Restore -> {
            SectionSheet(
                title = "Restore backup",
                confirm = "Restore",
                available = BackupSection.entries.filter { it in current.info.sections },
                counts = current.info.counts,
                subtitle = if (current.info.createdAt > 0L) "Created ${formatDateTime(current.info.createdAt)}" else null,
                onConfirm = { selected ->
                    picker = null
                    scope.launch {
                        busy = true
                        outcome =
                            try {
                                BackupOutcome(true, backup.restore(current.json, selected))
                            } catch (e: BackupException) {
                                BackupOutcome(false, e.message ?: "Restore failed")
                            } catch (e: Exception) {
                                BackupOutcome(false, "Restore failed: ${e.message ?: "unknown error"}")
                            }
                        busy = false
                    }
                },
                onDismiss = { picker = null },
            )
        }

        null -> {}
    }
}

private fun readLimited(stream: java.io.InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) break
        output.write(buffer, 0, count)
        if (output.size() > MAX_BACKUP_BYTES) throw BackupException("This file is too large to be a Kozu backup")
    }
    return output.toString(Charsets.UTF_8.name())
}

@Composable
private fun HowItWorksCard(
    shape: Shape,
    onDismiss: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text("How backup works", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "A backup is a single file you save wherever you like. It holds the sections you choose: settings, favorites, playlists and listening history. " +
                        "Restoring merges the file into what you already have and never removes anything. " +
                        "Sign-in tokens, API credentials and proxy passwords are never stored in a backup, and downloaded songs are not included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = kozuUiString("Dismiss"))
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    shape: Shape,
    outcome: BackupOutcome,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                if (outcome.success) "Done" else "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = if (outcome.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(4.dp))
            Text(outcome.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionSheet(
    title: String,
    confirm: String,
    available: List<BackupSection>,
    counts: Map<BackupSection, Int>,
    subtitle: String? = null,
    onConfirm: (Set<BackupSection>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(available.toSet()) }
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            available.forEachIndexed { index, section ->
                val checked = section in selected
                Surface(
                    onClick = { selected = if (checked) selected - section else selected + section },
                    shape = groupShape(index, available.size),
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + section else selected - section })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(section.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                section.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        counts[section]?.let {
                            Text("$it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (index < available.lastIndex) Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onConfirm(selected) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text(confirm) }
            }
        }
    }
}
