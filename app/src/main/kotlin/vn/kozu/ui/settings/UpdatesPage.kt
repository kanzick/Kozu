package vn.kozu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import vn.kozu.R

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

    val channel = UpdateChannel.STABLE
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

    SettingsScaffold(title = stringResource(R.string.updates), onBack = onBack) {
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
        SettingsSection(stringResource(R.string.settings_commit_history_count, commits.branch)) {
            if (loadingCommits) {
                custom { shape -> SettingsNote(shape, "Loading history…") }
            } else {
                commits.note?.let { note -> custom { shape -> SettingsNote(shape, note) } }
                if (commits.commits.isEmpty()) {
                    custom { shape -> SettingsNote(shape, "No commits found.") }
                } else {
                    commits.commits.groupBy { it.date.take(10) }.forEach { (day, group) ->
                        custom { shape -> CommitDayCard(shape, day, group) { url -> context.openLink(url) } }
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
            title = { Text(stringResource(R.string.kozu_available, update.version)) },
            text = {
                Text(update.notes.ifBlank { "A new release is available." }.take(1_000),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.openLink(update.url)
                        available = null
                    },
                ) { Text(stringResource(R.string.open_release)) }
            },
            dismissButton = { TextButton(onClick = { available = null }) { Text(stringResource(R.string.later)) } },
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
                    Text(stringResource(R.string.kozu_updates), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.update_version_checked, version, checked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onCheck, enabled = !checking) {
                    if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.check))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.automatic_checks), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.once_a_day_on_launch), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun CommitDayCard(
    shape: androidx.compose.ui.graphics.Shape,
    day: String,
    commits: List<RecentCommit>,
    onOpen: (String) -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(text = dayLabel(day),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            commits.forEachIndexed { index, commit ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
                CommitRow(commit) { onOpen(commit.url) }
            }
        }
    }
}

@Composable
private fun CommitRow(
    commit: RecentCommit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (!commit.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = commit.avatarUrl,
                contentDescription = stringResource(R.string.avatar_for, commit.author),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(38.dp).clip(CircleShape),
            )
        } else {
            IconBadge(Icons.Rounded.Code)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(commit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(commit.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(commit.sha.take(7),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun dayLabel(day: String): String =
    runCatching {
        val date = java.time.LocalDate.parse(day)
        val today = java.time.LocalDate.now()
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault()))
        }
    }.getOrDefault(day)

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
            Text(stringResource(R.string.page_of, currentPage, totalPages), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = { onSelect(currentPage - 1) },
                    enabled = enabled && currentPage > 1,
                    modifier = Modifier.width(44.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("‹") }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                                    Text(page.toString(), modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { onSelect(currentPage + 1) },
                    enabled = enabled && currentPage < totalPages,
                    modifier = Modifier.width(44.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("›") }
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
        Text(message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
