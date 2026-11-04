package vn.kozu.ui.settings

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

private enum class DeveloperConfirm { RESET, CACHES }

@Composable
fun DeveloperPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = viewModel.appContainer
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<DeveloperConfirm?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    SettingsScaffold(title = "Developer Options", onBack = onBack) {
        SettingsSection("Maintenance") {
            action(
                Setting.REFRESH_CATALOG,
                label = if (catalog.isRefreshing) "Refreshing…" else "Force refresh",
                enabled = !catalog.isRefreshing,
            ) {
                container.catalog.clearMixCache()
                viewModel.refreshCatalog()
                status = "Catalog refresh started."
            }
            action(
                Setting.REBUILD_LIBRARY,
                label = if (syncing) "Rebuilding…" else "Rebuild",
                enabled = !syncing && !working,
            ) {
                scope.launch {
                    working = true
                    status =
                        try {
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
                    status =
                        try {
                            val fixed = container.history.normalize()
                            if (fixed == 0) {
                                "Stats recomputed. Everything was already consistent."
                            } else {
                                "Stats recomputed. $fixed inconsistent entries were cleaned up."
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            "Could not regenerate stats: ${e.message ?: "unknown error"}"
                        }
                    working = false
                }
            }
            action(Setting.CLEAR_ALL_CACHES, label = "Clear", enabled = !working) {
                confirm = DeveloperConfirm.CACHES
            }
            action(Setting.RESET_SETTINGS, label = "Reset", destructive = true) {
                confirm = DeveloperConfirm.RESET
            }
            status?.let { message ->
                custom { shape -> NoteCard(shape, "Result", message) }
            }
        }
    }

    when (confirm) {
        DeveloperConfirm.RESET -> {
            ConfirmDialog(
                title = "Reset all settings?",
                text = "Every preference returns to its default value. Your osu! API credentials, favourites, playlists and downloads are kept.",
                action = "Reset",
                onConfirm = {
                    confirm = null
                    viewModel.save { resetAll() }
                    status = "All settings were reset to their defaults."
                },
                onDismiss = { confirm = null },
            )
        }

        DeveloperConfirm.CACHES -> {
            ConfirmDialog(
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
                onDismiss = { confirm = null },
            )
        }

        null -> Unit
    }
}
