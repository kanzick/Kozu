package vn.kozu.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    val checked = settings.lastUpdateCheckAt.takeIf { it > 0L }?.let(::formatDateTime) ?: "Not checked"

    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf<UpdateResult.Available?>(null) }
    var commitPage by rememberSaveable(channel) { mutableIntStateOf(1) }
    var commits by remember(channel) {
        mutableStateOf(RecentCommitsResult(channel.branch, emptyList(), "Loading history…"))
    }
    var loadingCommits by remember { mutableStateOf(true) }

    LaunchedEffect(channel, commitPage) {
        loadingCommits = true
        commits = container.updates.recentCommits(channel, commitPage)
        if (commits.page != commitPage) commitPage = commits.page
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
                        message = "You're up to date · v${result.version}"
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
        SettingsSection("Channel") {
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
        SettingsSection("Commit history · ${commits.branch}") {
            if (loadingCommits) {
                custom { shape -> SettingsNote(shape, "Loading history…") }
            } else {
                commits.note?.let { note -> custom { shape -> SettingsNote(shape, note) } }
                if (commits.commits.isEmpty()) {
                    custom { shape -> SettingsNote(shape, "No commits found.") }
                } else {
                    commits.commits.forEach { commit ->
                        custom { shape -> CommitCard(shape, commit) { context.openLink(commit.url) } }
                    }
                    if (commits.totalPages > 1) {
                        custom { shape ->
                            CommitPagination(
                                shape = shape,
                                currentPage = commits.page,
                                totalPages = commits.totalPages,
                                enabled = !loadingCommits,
                                onSelect = { page -> if (page != commits.page) commitPage = page },
                            )
                        }
                    }
                }
            }
        }
    }

    available?.let { update ->
        AlertDialog(
            onDismissRequest = { available = null },
            title = { Text("Kozu v${update.version} is available") },
            text = {
                Text(
                    update.notes.ifBlank { "A new ${channel.label} release is available." }.take(1_000),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.SystemUpdate)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kozu updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("v$version · $checked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onCheck, enabled = !checking) {
                    if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Check")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic checks", style = MaterialTheme.typography.titleSmall)
                    Text("Once a day on launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            if (selected == UpdateChannel.STABLE) "Public releases" else "Prerelease builds",
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
            if (!commit.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = commit.avatarUrl,
                    contentDescription = "${commit.author} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            } else {
                IconBadge(Icons.Rounded.Code)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(commit.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(commit.sha.take(8), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    listOf(commit.author, commit.date.take(10)).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommitPagination(
    shape: androidx.compose.ui.graphics.Shape,
    currentPage: Int,
    totalPages: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val pages = remember(currentPage, totalPages) { visiblePages(currentPage, totalPages) }
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Page $currentPage of $totalPages", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onSelect(currentPage - 1) }, enabled = enabled && currentPage > 1) { Text("‹") }
                pages.forEach { page ->
                    if (page == null) {
                        Text("…", modifier = Modifier.padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Surface(
                            onClick = { onSelect(page) },
                            enabled = enabled,
                            shape = CircleShape,
                            color = if (page == currentPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (page == currentPage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Text(page.toString(), modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                TextButton(onClick = { onSelect(currentPage + 1) }, enabled = enabled && currentPage < totalPages) { Text("›") }
            }
        }
    }
}

private fun visiblePages(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (1..total).map { it }
    val pages = linkedSetOf<Int?>()
    pages += 1
    val start = (current - 1).coerceAtLeast(2)
    val end = (current + 1).coerceAtMost(total - 1)
    if (start > 2) pages += null
    (start..end).forEach { pages.add(it) }
    if (end < total - 1) pages += null
    pages += total
    return pages.toList()
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
