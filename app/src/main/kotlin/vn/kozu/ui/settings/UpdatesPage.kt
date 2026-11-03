package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import vn.kozu.data.update.RecentCommit
import vn.kozu.data.update.RecentCommitsResult
import vn.kozu.data.update.UpdateResult
import vn.kozu.domain.model.UpdateChannel

@Composable
fun UpdatesPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = viewModel.appContainer
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val version = remember { vn.kozu.data.update.UpdateChecker.installedVersion(context) }
    val channel = settings.updateChannel
    val checked = settings.lastUpdateCheckAt.takeIf { it > 0L }?.let(::formatDateTime) ?: "Not checked yet"

    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf<UpdateResult.Available?>(null) }
    var commits by remember(channel) {
        mutableStateOf(RecentCommitsResult(channel.branch, emptyList(), "Loading recent commits…"))
    }
    var loadingCommits by remember { mutableStateOf(true) }

    LaunchedEffect(channel) {
        loadingCommits = true
        commits = container.updates.recentCommits(channel)
        loadingCommits = false
    }

    fun checkForUpdates() {
        if (checking) return
        scope.launch {
            checking = true
            message = null
            try {
                when (val result = container.updates.check(channel)) {
                    is UpdateResult.Available -> {
                        available = result
                        viewModel.save { setLastUpdateCheck(System.currentTimeMillis()) }
                    }
                    is UpdateResult.UpToDate -> {
                        message = "You are up to date with v${result.version}."
                        viewModel.save { setLastUpdateCheck(System.currentTimeMillis()) }
                    }
                    is UpdateResult.Failed -> message = result.message
                }
            } finally {
                checking = false
            }
        }
    }

    SettingsScaffold(title = "Updates", onBack = onBack) {
        SettingsSection(null) {
            custom { shape ->
                UpdateSummaryCard(
                    shape = shape,
                    version = version,
                    checked = checked,
                    checking = checking,
                    autoUpdateCheck = settings.autoUpdateCheck,
                    status = message,
                    onCheck = ::checkForUpdates,
                    onAutoUpdateChange = { enabled -> viewModel.save { setAutoUpdateCheck(enabled) } },
                )
            }
        }
        SettingsSection("Update channel") {
            custom { shape ->
                ChannelSelector(
                    selected = channel,
                    onSelected = { selected ->
                        message = null
                        viewModel.save { setUpdateChannel(selected) }
                    },
                )
            }
        }
        SettingsSection("Recent commits · ${commits.branch}") {
            if (loadingCommits) {
                custom { shape ->
                    SettingsNote(shape, "Loading commit history…")
                }
            } else {
                commits.note?.let { note ->
                    custom { shape -> SettingsNote(shape, note) }
                }
                if (commits.commits.isEmpty()) {
                    custom { shape -> SettingsNote(shape, "No recent commits are available.") }
                } else {
                    commits.commits.forEach { commit ->
                        custom { shape -> CommitCard(shape, commit) { context.openLink(commit.url) } }
                    }
                }
            }
        }
        SettingsSection("Installing updates") {
            custom { shape ->
                SettingsNote(
                    shape,
                    "Install the newer APK over the existing Kozu installation. Do not uninstall first. Android can retain local settings, favorites, playlists, history and downloads when the package ID and signing certificate match. A build signed with a different key cannot update this installation.",
                )
            }
        }
    }

    available?.let { update ->
        AlertDialog(
            onDismissRequest = { available = null },
            title = { Text("Kozu v${update.version} is available") },
            text = {
                Text(
                    update.notes.ifBlank { "A newer release is available for the ${channel.label} channel." }.take(1_000),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.openLink(update.url)
                        available = null
                    },
                ) { Text("Open release") }
            },
            dismissButton = { TextButton(onClick = { available = null }) { Text("Later") } },
        )
    }
}

@Composable
private fun UpdateSummaryCard(
    shape: androidx.compose.ui.graphics.Shape,
    version: String,
    checked: String,
    checking: Boolean,
    autoUpdateCheck: Boolean,
    status: String?,
    onCheck: () -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
) {
    SettingCard(setting = Setting.CHECK_UPDATES, shape = shape, onClick = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.SystemUpdate)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Check for Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("v$version · Checked $checked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onCheck, enabled = !checking) {
                    if (checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Check")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic update check", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Check once per day when Kozu opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoUpdateCheck, onCheckedChange = onAutoUpdateChange)
            }
            if (!status.isNullOrBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ChannelSelector(
    selected: UpdateChannel,
    onSelected: (UpdateChannel) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UpdateChannel.entries.forEach { channel ->
                val active = channel == selected
                Surface(
                    onClick = { onSelected(channel) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = channel.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        Text(
            selected.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun CommitCard(
    shape: androidx.compose.ui.graphics.Shape,
    commit: RecentCommit,
    onClick: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Rounded.Code)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(commit.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(commit.sha, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    listOf(commit.author, commit.date.take(10)).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SettingsNote(
    shape: androidx.compose.ui.graphics.Shape,
    message: String,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
