package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import vn.kozu.R

@Composable
fun OfflinePage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val total = downloads.sumOf { it.sizeBytes }
    val outdated = downloads.count { it.outdated }
    val lastSync =
        if (settings.lastSyncAt > 0L) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(settings.lastSyncAt))
        } else {
            "Never"
        }

    SettingsScaffold(title = stringResource(R.string.music_management), onBack = onBack) {
        SettingsGroup(
            title = stringResource(R.string.storage_sense),
            items =
                listOf(
                    {
                        Column {
                            Text(pluralStringResource(R.plurals.song_count_with_size, downloads.size, downloads.size, formatBytes(total)),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(if (outdated > 0) pluralStringResource(R.plurals.update_needed_count, outdated, outdated) else stringResource(R.string.everything_is_up_to_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    {
                        ActionRow(
                            stringResource(R.string.settings_delete_all_downloads),
                            stringResource(R.string.settings_removes_every_offline_song_from_this_device),
                            { confirmDelete = true },
                            destructive = true,
                        )
                    },
                ),
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = stringResource(R.string.downloading),
            items =
                listOf(
                    {
                        SwitchRow(
                            title = stringResource(R.string.wi_fi_only),
                            subtitle = stringResource(R.string.wait_for_wi_fi_before_downloading_songs),
                            checked = settings.wifiOnlyDownloads,
                            onChange = viewModel::setWifiOnlyDownloads,
                        )
                    },
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.public_mirror_downloads), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.downloads_use_the_same_public_mirror_sources_for_gue),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    {
                        SwitchRow(
                            title = stringResource(R.string.keep_full_beatmap),
                            subtitle = stringResource(R.string.store_charts_hitsounds_and_images_needed_for_play_as),
                            checked = settings.keepFullBeatmap,
                            onChange = viewModel::setKeepFull,
                        )
                    },
                ),
        )
        Spacer(Modifier.height(24.dp))
        SettingsGroup(
            title = stringResource(R.string.keep_songs_up_to_date),
            items =
                listOf(
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.sync_library), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.offline_sync_summary, lastSync),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PrimaryPill(if (syncing) stringResource(R.string.syncing) else stringResource(R.string.sync_now), enabled = !syncing) { viewModel.syncNow() }
                            }
                        }
                    },
                    {
                        SwitchRow(
                            title = stringResource(R.string.auto_update_downloads),
                            subtitle = stringResource(R.string.re_download_offline_songs_automatically_when_a_newer),
                            checked = settings.autoUpdateDownloads,
                            onChange = viewModel::setAutoUpdate,
                        )
                    },
                ),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_all_downloads)) },
            text = { Text(stringResource(R.string.offline_songs_will_be_removed_from_this_device_you_c)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllDownloads()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
