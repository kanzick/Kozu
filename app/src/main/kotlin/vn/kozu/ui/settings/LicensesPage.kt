package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext as withCoroutineContext

private data class DependencyLicense(
    val name: String,
    val version: String,
    val licenses: List<LicenseDetails>,
    val coordinate: String,
) {
    val licenseLabel: String
        get() = licenses.map { it.name }.filter(String::isNotBlank).distinct().joinToString(", ")
            .ifBlank { "License metadata not provided" }
}

private data class LicenseDetails(
    val name: String,
    val spdxId: String?,
    val url: String?,
    val content: String?,
)

private data class AttributionEntry(
    val name: String,
    val license: String,
    val description: String,
    val url: String,
)

private val attributions = listOf(
    AttributionEntry(
        name = "Kozu source code",
        license = "GNU General Public License v3.0",
        description = "The Kozu source code is licensed under GPL-3.0. The full license text is packaged with the application.",
        url = "https://www.gnu.org/licenses/gpl-3.0.html",
    ),
    AttributionEntry(
        name = "Quicksand",
        license = "SIL Open Font License 1.1",
        description = "Bundled font. Copyright 2011 The Quicksand Project Authors; Reserved Font Name: Quicksand. The original font and license notice are included in the app.",
        url = "https://github.com/andrew-paglinawan/QuicksandFamily",
    ),
    AttributionEntry(
        name = "Google Sans",
        license = "SIL Open Font License 1.1",
        description = "Bundled font. Original font software and license notice are included in the app.",
        url = "https://github.com/googlefonts/googlesans",
    ),
    AttributionEntry(
        name = "Google Sans Flex",
        license = "SIL Open Font License 1.1",
        description = "Bundled variable font. Original font software and license notice are included in the app.",
        url = "https://github.com/googlefonts/googlesans-flex",
    ),
    AttributionEntry(
        name = "osu! API and assets",
        license = "Third-party terms and creator rights",
        description = "Kozu accesses beatmap metadata, covers and previews from osu!. Beatmap audio and artwork remain owned by their respective creators; Kozu does not claim ownership.",
        url = "https://osu.ppy.sh/docs/index.html",
    ),
    AttributionEntry(
        name = "Community beatmap mirrors",
        license = "Terms set by each operator",
        description = "Optional third-party sources used for streaming and downloads. Kozu does not host these beatmaps; availability and terms are controlled by each service.",
        url = "https://osu.ppy.sh/wiki/en/Community/Beatmap_mirrors",
    ),
    AttributionEntry(
        name = "GitHub Releases API",
        license = "GitHub terms and API policies",
        description = "Used to check published Kozu releases and display recent source commits. Update checks can be disabled in Settings.",
        url = "https://docs.github.com/en/rest/releases/releases",
    ),
)

@Composable
fun LicensesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var dependencies by remember { mutableStateOf<List<DependencyLicense>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedDependency by remember { mutableStateOf<DependencyLicense?>(null) }

    LaunchedEffect(context) {
        loading = true
        val result = withCoroutineContext(Dispatchers.IO) {
            runCatching {
                Libs.Builder()
                    .withContext(context)
                    .build()
                    .libraries
                    .map { library ->
                        DependencyLicense(
                            name = library.name.ifBlank { library.uniqueId },
                            version = library.artifactVersion?.takeIf(String::isNotBlank) ?: "Version not provided",
                            licenses = library.licenses.map { license ->
                                LicenseDetails(
                                    name = license.name,
                                    spdxId = license.spdxId,
                                    url = license.url,
                                    content = license.licenseContent,
                                )
                            }.sortedBy { it.name.lowercase() },
                            coordinate = library.uniqueId,
                        )
                    }
                    .distinctBy { it.coordinate }
                    .sortedBy { it.name.lowercase() }
            }
        }
        dependencies = result.getOrDefault(emptyList())
        loadError = result.exceptionOrNull()?.let {
            "Could not load generated dependency metadata: ${it.message ?: "unknown error"}"
        }
        loading = false
    }

    val unknownCount = dependencies.count { dependency ->
        dependency.licenses.isEmpty() || dependency.licenses.any {
            it.name.contains("NOASSERTION", ignoreCase = true) ||
                it.spdxId.equals("NOASSERTION", ignoreCase = true) ||
                it.spdxId.equals("LicenseRef-scancode-unknown", ignoreCase = true)
        }
    }

    SettingsScaffold(title = "Licenses & attribution", onBack = onBack) {
        SettingsSection("Open-source dependencies · ${dependencies.size}") {
            if (loading) {
                custom { shape ->
                    SettingCard(setting = null, shape = shape, onClick = null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.width(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(14.dp))
                            Text("Reading generated dependency licenses…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (loadError != null) {
                custom { shape -> LicenseNote(shape, loadError!!) }
            } else if (dependencies.isEmpty()) {
                custom { shape ->
                    LicenseNote(shape, "The generated dependency list is empty. Run the Android build to generate AboutLibraries metadata.")
                }
            } else {
                if (unknownCount > 0) {
                    custom { shape ->
                        LicenseNote(
                            shape,
                            "$unknownCount dependency entries have missing or unrecognized license metadata. Review their upstream license files before publishing a release.",
                        )
                    }
                }
                dependencies.forEach { entry ->
                    custom { shape -> DependencyCard(shape, entry) { selectedDependency = entry } }
                }
            }
        }
        SettingsSection("Project, fonts & services") {
            attributions.forEach { entry ->
                custom { shape -> AttributionCard(shape, entry) { context.openLink(entry.url) } }
            }
            custom { shape ->
                LicenseNote(
                    shape,
                    "Full license notices are bundled under app/src/main/assets/licenses. Dependency entries are generated from the resolved Android dependency graph; select an entry to inspect its available license text and upstream URL.",
                )
            }
        }
        SettingsSection("Project notice") {
            custom { shape ->
                LicenseNote(
                    shape,
                    "Kozu is an unofficial fan project and is not affiliated with or endorsed by ppy Pty Ltd. osu! is a trademark of ppy Pty Ltd. Beatmap audio and artwork remain the property of their respective creators.",
                )
            }
        }
    }

    selectedDependency?.let { dependency ->
        AlertDialog(
            onDismissRequest = { selectedDependency = null },
            title = { Text(dependency.name) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${dependency.coordinate}:${dependency.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (dependency.licenses.isEmpty()) {
                        Text(
                            "No license information was provided by the dependency metadata. Check the upstream project before redistributing this build.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    dependency.licenses.forEach { license ->
                        Text(license.name, style = MaterialTheme.typography.titleSmall)
                        license.spdxId?.let { id ->
                            Text("SPDX: $id", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val content = license.content?.takeIf(String::isNotBlank)
                        if (content != null) {
                            Text(content, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                "Full license text was not included in the generated metadata.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        license.url?.takeIf(String::isNotBlank)?.let { url ->
                            TextButton(onClick = { context.openLink(url) }) { Text("Open license source") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDependency = null }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun DependencyCard(
    shape: androidx.compose.ui.graphics.Shape,
    entry: DependencyLicense,
    onClick: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(entry.version, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                entry.licenseLabel,
                style = MaterialTheme.typography.titleSmall,
                color = if (entry.licenseLabel == "License metadata not provided" || entry.licenseLabel.contains("NOASSERTION", true)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(
                entry.coordinate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AttributionCard(
    shape: androidx.compose.ui.graphics.Shape,
    entry: AttributionEntry,
    onClick: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.license, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(entry.url.removePrefix("https://"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open source", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LicenseNote(
    shape: androidx.compose.ui.graphics.Shape,
    text: String,
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
