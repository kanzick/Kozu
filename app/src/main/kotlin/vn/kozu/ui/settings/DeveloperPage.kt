package vn.kozu.ui.settings

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.update.UpdateChecker
import vn.kozu.domain.model.AppSettings
import java.util.Locale

private enum class DeveloperConfirm { RESET, CACHES }

@Composable
fun DeveloperPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = viewModel.appContainer
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<DeveloperConfirm?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    val version = remember { UpdateChecker.installedVersion(context) }
    val build = remember { buildNumber(context) }

    SettingsScaffold(title = "Developer Options", onBack = onBack) {
        SettingsSection("Maintenance") {
            action(
                Setting.REFRESH_CATALOG,
                label = if (catalog.isRefreshing) "Refreshingâ€¦" else "Force refresh",
                enabled = !catalog.isRefreshing
            ) {
                container.catalog.clearMixCache()
                viewModel.refreshCatalog()
                status = "Catalog refresh started."
            }
            action(
                Setting.REBUILD_LIBRARY,
                label = if (syncing) "Rebuildingâ€¦" else "Rebuild",
                enabled = !syncing && !working
            ) {
                scope.launch {
                    working = true
                    status = try {
                        val report = container.sync.run()
                        "Library rebuilt: ${report.checked} songs checked, ${report.refreshed} refreshed, ${report.outdated} outdated downloads."
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Library rebuild failed: ${e.message ?: "unknown error"}"
                    }
                    working = false
                }
            }
            action(Setting.REGENERATE_STATS, label = "Regenerate", enabled = !working) {
                scope.launch {
                    working = true
                    status = try {
                        val fixed = container.history.normalize()
                        if (fixed == 0) "Stats recomputed. Everything was already consistent." else "Stats recomputed. $fixed inconsistent entries were cleaned up."
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Could not regenerate stats: ${e.message ?: "unknown error"}"
                    }
                    working = false
                }
            }
            action(Setting.CLEAR_ALL_CACHES, label = "Clear", enabled = !working) { confirm = DeveloperConfirm.CACHES }
            action(Setting.RESET_SETTINGS, label = "Reset", destructive = true) { confirm = DeveloperConfirm.RESET }
            status?.let { message ->
                custom { shape -> NoteCard(shape, "Result", message) }
            }
        }
        SettingsSection("Diagnostics") {
            info(Icons.Rounded.Info, "Kozu", version, "Build $build Â· ${context.packageName}")
            info(Icons.Rounded.DeveloperBoard, "Device", Build.MODEL, Build.MANUFACTURER)
            info(Icons.Rounded.Android, "Android", Build.VERSION.RELEASE, "API ${Build.VERSION.SDK_INT}")
            info(
                Icons.Rounded.Memory,
                "CPU architecture",
                Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                Build.SUPPORTED_ABIS.joinToString(", ")
            )
            action(Setting.COPY_DEBUG, label = "Copy") {
                val copied = context.copyToClipboard("Kozu debug info", debugReport(context, version, build, settings))
                status = if (copied) "Debug info copied to the clipboard." else "Could not access the clipboard."
            }
        }
    }

    when (confirm) {
        DeveloperConfirm.RESET -> ConfirmDialog(
            title = "Reset all settings?",
            text = "Every preference returns to its default value. Your osu! API credentials, favourites, playlists and downloads are kept.",
            action = "Reset",
            onConfirm = {
                confirm = null
                viewModel.save { resetAll() }
                status = "All settings were reset to their defaults."
            },
            onDismiss = { confirm = null }
        )
        DeveloperConfirm.CACHES -> ConfirmDialog(
            title = "Clear all caches?",
            text = "Cached streams, artwork and catalog pages will be deleted. Downloads are not affected.",
            action = "Clear",
            onConfirm = {
                confirm = null
                scope.launch {
                    working = true
                    viewModel.clearStreamCache()
                    container.catalog.clearMixCache()
                    withContext(Dispatchers.IO) {
                        val loader = SingletonImageLoader.get(context)
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                    }
                    status = "Stream, artwork and catalog caches were cleared."
                    working = false
                }
            },
            onDismiss = { confirm = null }
        )
        null -> Unit
    }
}

private fun buildNumber(context: android.content.Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toString() else "${info.versionCode}"
} catch (_: Exception) {
    "?"
}

private fun debugReport(
    context: android.content.Context,
    version: String,
    build: String,
    settings: AppSettings
): String = buildString {
    appendLine("Kozu $version (build $build)")
    appendLine("Package: ${context.packageName}")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
    appendLine("Locale: ${Locale.getDefault().toLanguageTag()}")
    appendLine("Stream mode: ${settings.streamMode.label}")
    appendLine("Stream cache limit: ${settings.streamCacheMb} MB")
    appendLine("Download limit: ${if (settings.downloadLimitMb == 0) "unlimited" else "${settings.downloadLimitMb} MB"}")
    appendLine("Proxy: ${if (settings.proxy.isActive) settings.proxy.type.label else "disabled"}")
    appendLine("Active mirrors: ${settings.activeMirrors.size}")
    appendLine("osu! API credentials configured: ${if (settings.hasCredentials) "yes" else "no"}")
    appendLine("Equalizer: ${if (settings.equalizerEnabled) "on" else "off"}")
    append("Last library sync: ${if (settings.lastSyncAt > 0L) formatDateTime(settings.lastSyncAt) else "never"}")
}
