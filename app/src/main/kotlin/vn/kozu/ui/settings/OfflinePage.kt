package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.text.DateFormat
import java.util.Date

@Composable
fun OfflinePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    val total = downloads.sumOf { it.sizeBytes }
    val outdated = downloads.count { it.outdated }
    val lastSync = if (settings.lastSyncAt > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(settings.lastSyncAt))
    } else {
        "Never"
    }

    SettingsScaffold(title = "Offline", onBack = onBack) {
        SettingsGroup(
            title = "Storage",
            items = listOf(
                {
                    Column {
                        Text(
                            "${downloads.size} ${if (downloads.size == 1) "song" else "songs"} · ${formatBytes(total)}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            if (outdated > 0) "$outdated need an update" else "Everything is up to date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                { ActionRow("Delete all downloads", "Removes every offline song from this device", { confirmDelete = true }, destructive = true) }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Downloading",
            items = listOf(
                {
                    SwitchRow(
                        title = "Wi-Fi only",
                        subtitle = "Wait for Wi-Fi before downloading songs",
                        checked = settings.wifiOnlyDownloads,
                        onChange = viewModel::setWifiOnlyDownloads
                    )
                },
                {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Public mirror downloads", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Downloads use the same public mirror sources for guests and signed-in users. An osu! account is not required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                {
                    SwitchRow(
                        title = "Keep full beatmap",
                        subtitle = "Store charts, hitsounds and images (needed for Play as game). Off keeps only audio and cover",
                        checked = settings.keepFullBeatmap,
                        onChange = viewModel::setKeepFull
                    )
                }
            )
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = "Keep songs up to date",
            items = listOf(
                {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sync library", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Refreshes titles and artists of your liked songs, playlists, history and downloads, and detects beatmaps that mappers have updated. Last sync: $lastSync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PrimaryPill(if (syncing) "Syncing…" else "Sync now", enabled = !syncing) { viewModel.syncNow() }
                        }
                    }
                },
                {
                    SwitchRow(
                        title = "Auto-update downloads",
                        subtitle = "Re-download offline songs automatically when a newer version exists",
                        checked = settings.autoUpdateDownloads,
                        onChange = viewModel::setAutoUpdate
                    )
                }
            )
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all downloads?") },
            text = { Text("Offline songs will be removed from this device. You can download them again anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllDownloads()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
