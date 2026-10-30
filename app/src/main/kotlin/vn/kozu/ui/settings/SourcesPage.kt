package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.data.mirror.MirrorTestResult
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.Mirrors

@Composable
fun SourcesPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val results by viewModel.mirrorResults.collectAsStateWithLifecycle()
    val testing by viewModel.testing.collectAsStateWithLifecycle()
    val official by viewModel.officialPreview.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    val mirrors = Mirrors.builtIn + settings.customMirrors

    SettingsScaffold(title = "Sources", onBack = onBack) {
        SettingsGroup(
            title = null,
            items = listOf({
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How Kozu finds audio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Full song mode tries the enabled public mirrors and only plays a full audio file. It never silently switches to a short preview. Preview mode is used only when you explicitly select it.\n\nGuests and signed-in users have the same catalog, streaming, and download access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimaryPill(
                            if (testing) "Testing…" else "Test sources",
                            enabled = !testing
                        ) { viewModel.testMirrors() }
                        Spacer(Modifier.width(12.dp))
                        when (official) {
                            true -> Text(
                                "osu! preview reachable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            false -> Text(
                                "osu! preview unreachable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            null -> Unit
                        }
                    }
                }
            })
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Mirrors",
            items = mirrors.map { mirror ->
                @Composable {
                    MirrorRow(
                        mirror = mirror,
                        enabled = mirror.id !in settings.disabledMirrors,
                        result = results[mirror.id],
                        onToggle = { viewModel.setMirrorEnabled(mirror.id, it) },
                        onRemove = if (mirror.builtIn) null else ({ viewModel.removeMirror(mirror.id) })
                    )
                }
            } + listOf<@Composable () -> Unit>({
                ActionRow(
                    "Add custom mirror",
                    "Use {id} where the beatmapset id goes",
                    { showAdd = true })
            })
        )
    }

    if (showAdd) AddMirrorDialog(
        onDismiss = { showAdd = false },
        onAdd = { name, download, preview ->
            showAdd = false
            viewModel.addMirror(name, download, preview)
        }
    )
}

@Composable
private fun MirrorRow(
    mirror: Mirror,
    enabled: Boolean,
    result: MirrorTestResult?,
    onToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mirror.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    mirror.downloadUrl.substringAfter("://").substringBefore('/'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (result != null) {
            Spacer(Modifier.height(6.dp))
            val color = if (result.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                "${result.note} · ${result.millis} ms" + when (result.previewOk) {
                    true -> " · preview ok"
                    false -> " · preview failed"
                    null -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        if (onRemove != null) {
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun AddMirrorDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var download by remember { mutableStateOf("https://") }
    var preview by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && download.startsWith("http") && download.contains("{id}")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add mirror") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    label = { Text("Name") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = download,
                    onValueChange = { download = it.trim() },
                    singleLine = true,
                    label = { Text("Download URL") },
                    placeholder = { Text("https://example.com/d/{id}") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = preview,
                    onValueChange = { preview = it.trim() },
                    singleLine = true,
                    label = { Text("Preview URL (optional)") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name, download, preview) }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
