package vn.kozu.ui.settings

import vn.kozu.ui.components.KozuSheetHandle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import vn.kozu.R
import vn.kozu.data.update.RecentCommit
import vn.kozu.data.update.RecentCommitsResult
import vn.kozu.data.update.UpdateChecker
import vn.kozu.data.update.UpdateInstaller
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
    val version = remember { UpdateChecker.installedVersion(context) }
    val channel = UpdateChannel.STABLE
    val checked = settings.lastUpdateCheckAt.takeIf { it > 0L }?.let(::formatDateTime)
        ?: stringResource(R.string.not_checked)

    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf<UpdateResult.Available?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var downloadBytes by remember { mutableLongStateOf(0L) }
    var downloadTotal by remember { mutableLongStateOf(-1L) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var commitsExpanded by rememberSaveable { mutableStateOf(true) }
    var commitPage by rememberSaveable(channel) { mutableIntStateOf(1) }
    var commits by remember(channel) {
        mutableStateOf(RecentCommitsResult(channel.branch, emptyList()))
    }
    var loadingCommits by remember { mutableStateOf(true) }

    val permissionMessage = stringResource(R.string.update_install_permission)
    val readyMessage = stringResource(R.string.update_download_ready)
    val failedMessage = stringResource(R.string.update_download_failed)

    LaunchedEffect(channel, commitPage) {
        loadingCommits = true
        commits = container.updates.recentCommits(channel, commitPage)
        if (commits.page != commitPage) commitPage = commits.page
        loadingCommits = false
    }

    fun launchInstaller(apk: File) {
        try {
            when (UpdateInstaller.install(context, apk)) {
                UpdateInstaller.Result.PERMISSION_REQUIRED -> downloadStatus = permissionMessage
                UpdateInstaller.Result.INSTALLER_OPENED -> {
                    downloadStatus = readyMessage
                    available = null
                }
            }
        } catch (error: Exception) {
            downloadStatus = error.message?.takeIf(String::isNotBlank) ?: failedMessage
        }
    }

    fun downloadAndInstall(update: UpdateResult.Available) {
        val existingApk = downloadedApk?.takeIf { it.isFile }
        if (existingApk != null) {
            launchInstaller(existingApk)
            return
        }
        val downloadUrl = update.downloadUrl ?: return
        if (downloading) return

        scope.launch {
            downloading = true
            downloadStatus = null
            downloadProgress = null
            downloadBytes = 0L
            downloadTotal = -1L
            try {
                val destination = File(context.cacheDir, "updates/kozu-update.apk")
                val file = container.updates.downloadApk(downloadUrl, destination) { downloaded, total ->
                    scope.launch {
                        downloadBytes = downloaded
                        downloadTotal = total
                        downloadProgress = total.takeIf { it > 0L }
                            ?.let { (downloaded.toFloat() / it).coerceIn(0f, 1f) }
                    }
                }
                downloadedApk = file
                launchInstaller(file)
            } catch (error: Exception) {
                downloadStatus = error.message?.takeIf(String::isNotBlank) ?: failedMessage
            } finally {
                downloading = false
            }
        }
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
                        downloadStatus = null
                        downloadedApk = null
                        viewModel.save { setLastUpdateCheck(System.currentTimeMillis()) }
                    }
                    is UpdateResult.UpToDate -> {
                        message = context.getString(R.string.up_to_date_version, result.version)
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
        SettingsSection(null) {
            custom { shape ->
                RecentCommitsHeader(
                    shape = shape,
                    count = commits.commits.size,
                    expanded = commitsExpanded,
                    onClick = { commitsExpanded = !commitsExpanded },
                )
            }
            custom {
                AnimatedVisibility(
                    visible = commitsExpanded,
                    enter = expandVertically(animationSpec = tween(260)) + fadeIn(tween(180)),
                    exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(tween(140)),
                ) {
                    SettingsSection(null) {
                        if (loadingCommits) {
                            custom { shape -> SettingsNote(shape, stringResource(R.string.loading_history)) }
                        } else {
                            commits.note?.let { note -> custom { shape -> SettingsNote(shape, note) } }
                            if (commits.commits.isEmpty()) {
                                custom { shape -> SettingsNote(shape, stringResource(R.string.no_commits_found)) }
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
            }
        }
    }

    available?.let { update ->
        UpdateAvailableSheet(
            update = update,
            downloading = downloading,
            progress = downloadProgress,
            downloadedBytes = downloadBytes,
            downloadTotal = downloadTotal,
            status = downloadStatus ?: if (update.downloadUrl == null) stringResource(R.string.update_apk_unavailable) else null,
            alreadyDownloaded = downloadedApk?.isFile == true,
            onDismiss = { if (!downloading) available = null },
            onConfirm = { downloadAndInstall(update) },
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
private fun RecentCommitsHeader(
    shape: androidx.compose.ui.graphics.Shape,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "recentCommitsArrow",
    )
    SettingCard(setting = null, shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.recent_commits), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.recent_commits_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateAvailableSheet(
    update: UpdateResult.Available,
    downloading: Boolean,
    progress: Float?,
    downloadedBytes: Long,
    downloadTotal: Long,
    status: String?,
    alreadyDownloaded: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val canDownload = update.downloadUrl != null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        dragHandle = { KozuSheetHandle() },
        sheetGesturesEnabled = !downloading,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.update_available_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(update.version, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (update.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.update_release_notes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                        ) {
                            ReleaseNotesMarkdown(update.notes.take(4_000))
                        }
                    }
                }
            }

            if (downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (progress == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                    val progressLabel = progress?.let {
                        stringResource(R.string.update_download_progress, (it * 100).toInt())
                    } ?: stringResource(R.string.downloading)
                    Text(
                        progressLabel + if (downloadTotal > 0L) " · ${formatBytes(downloadedBytes)} / ${formatBytes(downloadTotal)}" else " · ${formatBytes(downloadedBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!status.isNullOrBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !downloading) {
                    Text(stringResource(R.string.later))
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onConfirm,
                    enabled = canDownload && !downloading,
                ) {
                    if (downloading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (alreadyDownloaded) Icons.Rounded.SystemUpdate else Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (alreadyDownloaded) R.string.install_update else R.string.download_and_install))
                    }
                }
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = dayLabel(day),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
            commits.forEach { commit ->
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!commit.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = commit.avatarUrl,
                    contentDescription = stringResource(R.string.avatar_for, commit.author),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(CircleShape),
                )
            } else {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Code, contentDescription = null)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    commit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        commit.author,
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
                        Text(
                            commit.sha.take(7),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun dayLabel(day: String): String = runCatching {
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
    SettingCard(setting = null, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onSelect(currentPage - 1) },
                enabled = enabled && currentPage > 1,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_page))
            }
            Text(
                stringResource(R.string.page_of, currentPage, totalPages),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { onSelect(currentPage + 1) },
                enabled = enabled && currentPage < totalPages,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.next_page))
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


