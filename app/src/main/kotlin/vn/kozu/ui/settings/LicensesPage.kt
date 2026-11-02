package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class LicenseEntry(
    val name: String,
    val license: String,
    val url: String,
    val description: String,
)

private val libraries =
    listOf(
        LicenseEntry(
            "Kotlin & Coroutines",
            "Apache License 2.0",
            "https://github.com/JetBrains/kotlin",
            "Programming language and asynchronous runtime.",
        ),
        LicenseEntry(
            "AndroidX Compose & Material 3",
            "Apache License 2.0",
            "https://developer.android.com/jetpack/compose",
            "UI toolkit and Material components.",
        ),
        LicenseEntry(
            "AndroidX Media3",
            "Apache License 2.0",
            "https://github.com/androidx/media",
            "ExoPlayer playback engine and media session.",
        ),
        LicenseEntry("Retrofit", "Apache License 2.0", "https://github.com/square/retrofit", "Type-safe HTTP client for the osu! API."),
        LicenseEntry("OkHttp", "Apache License 2.0", "https://github.com/square/okhttp", "HTTP engine behind every network request."),
        LicenseEntry("Coil", "Apache License 2.0", "https://github.com/coil-kt/coil", "Image loading and caching."),
        LicenseEntry(
            "AndroidX Room",
            "Apache License 2.0",
            "https://developer.android.com/jetpack/androidx/releases/room",
            "Local database for playlists.",
        ),
        LicenseEntry(
            "AndroidX DataStore",
            "Apache License 2.0",
            "https://developer.android.com/jetpack/androidx/releases/datastore",
            "Settings and library storage.",
        ),
        LicenseEntry(
            "AndroidX Palette",
            "Apache License 2.0",
            "https://developer.android.com/jetpack/androidx/releases/palette",
            "Colors extracted from cover art.",
        ),
        LicenseEntry("Gson", "Apache License 2.0", "https://github.com/google/gson", "JSON conversion for API responses."),
    )

private val assets =
    listOf(
        LicenseEntry("Quicksand", "SIL Open Font License 1.1", "https://fonts.google.com/specimen/Quicksand", "The Kozu typeface."),
        LicenseEntry(
            "osu! API & assets",
            "© ppy Pty Ltd",
            "https://osu.ppy.sh/docs/index.html",
            "Beatmap data, covers and previews are provided by osu! and remain the property of their owners.",
        ),
        LicenseEntry(
            "Community beatmap mirrors",
            "Terms set by each operator",
            "https://osu.ppy.sh/wiki/en/Community/Beatmap_mirrors",
            "Third-party services used to stream and download beatmaps. Kozu does not host any beatmaps.",
        ),
    )

@Composable
fun LicensesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsScaffold(title = "Licenses & attribution", onBack = onBack) {
        SettingsSection("Open source libraries") {
            libraries.forEach { entry -> custom { shape -> LicenseCard(shape, entry) { context.openLink(entry.url) } } }
        }
        SettingsSection("Fonts, services & content") {
            assets.forEach { entry -> custom { shape -> LicenseCard(shape, entry) { context.openLink(entry.url) } } }
        }
        SettingsSection("Inspiration") {
            custom { shape ->
                LicenseCard(
                    shape,
                    LicenseEntry(
                        "PixelMusic",
                        "GPL-3.0",
                        "https://github.com/ianshulyadav/PixelMusic",
                        "The structure of the settings screens is inspired by the open-source PixelMusic project. This is design inspiration only; no PixelMusic code is included in Kozu.",
                    ),
                ) { context.openLink("https://github.com/ianshulyadav/PixelMusic") }
            }
        }
    }
}

@Composable
private fun LicenseCard(
    shape: Shape,
    entry: LicenseEntry,
    onClick: () -> Unit,
) {
    SettingCard(setting = null, shape = shape, onClick = onClick) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.license, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(2.dp))
                Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
