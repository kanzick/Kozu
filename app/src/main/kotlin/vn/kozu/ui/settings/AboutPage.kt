package vn.kozu.ui.settings

import vn.kozu.ui.i18n.kozuUiString

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import vn.kozu.data.update.UpdateChecker

private const val GITHUB_URL = "https://github.com/kanzick/Kozu"
private const val OSU_URL = "https://osu.ppy.sh"
private const val TAGLINE = "Listen to your favorite osu! songs online or offline, with a Material 3 Expressive design."
private const val PRIVACY_TEXT =
    "Kozu does not include ads, analytics or trackers, and it has no Kozu account or server of its own.\n\n" +
        "Your settings, favorites, playlists, listening history and downloaded songs stay on this device. Backups are files you create and store yourself, and they never contain credentials.\n\n" +
        "To work, Kozu contacts: osu! (osu.ppy.sh and assets.ppy.sh) for the catalog, covers and sign-in; the community beatmap mirrors you leave enabled to stream or download songs, which can see your IP address and the beatmap IDs you request; and GitHub to look for updates, which you can turn off.\n\n" +
        "If you sign in with osu!, the access tokens are stored only on this device. If you configure a proxy, your network traffic goes through it.\n\n" +
        "Uninstalling Kozu removes all of its local data."

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutPage(
    onOpenLicenses: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember { UpdateChecker.installedVersion(context) }
    var privacyOpen by remember { mutableStateOf(false) }

    SettingsScaffold(title = "About", onBack = onBack) {
        SettingsSection(null) {
            custom { shape -> HeroCard(shape, version) }
        }
        SettingsSection("Links") {
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Code, "GitHub", "kanzick/Kozu") { context.openLink(GITHUB_URL) }
            }
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Language, "osu!", "osu.ppy.sh") { context.openLink(OSU_URL) }
            }
        }
        SettingsSection("App information") {
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Star, "Rate Kozu", "Star us on GitHub", Setting.RATE_APP) {
                    context.openLink(GITHUB_URL)
                }
            }
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Share, "Share app", "Recommend Kozu to others", Setting.SHARE_APP) {
                    shareApp(context)
                }
            }
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Security, "Privacy policy", "How Kozu handles your data", Setting.PRIVACY_ENTRY) {
                    privacyOpen = true
                }
            }
            custom { shape ->
                LinkItem(shape, Icons.Rounded.Layers, "Licenses", "Libraries and fonts", Setting.LICENSES_ENTRY) {
                    onOpenLicenses()
                }
            }
        }
        Text(
            text = "Kozu is an unofficial fan project and is not affiliated with or endorsed by ppy Pty Ltd. osu! is a trademark of ppy Pty Ltd.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }

    if (privacyOpen) {
        AlertDialog(
            onDismissRequest = { privacyOpen = false },
            title = { Text("Privacy policy") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { privacyOpen = false }) { Text("Close") } },
        )
    }
}

private fun shareApp(context: Context) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Kozu")
            .putExtra(Intent.EXTRA_TEXT, "Kozu: $TAGLINE $GITHUB_URL")
    try {
        context.startActivity(Intent.createChooser(send, "Share Kozu").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
    shape: Shape,
    version: String,
) {
    val context = LocalContext.current
    val icon =
        remember {
            try {
                context.packageManager
                    .getApplicationIcon(context.packageName)
                    .toBitmap(256, 256)
                    .asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    val colors = MaterialTheme.colorScheme
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = kozuUiString("Kozu"), modifier = Modifier.size(96.dp))
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Kozu", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            ValueChip("v$version")
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FeatureChip(Icons.Rounded.CloudDownload, "Online & offline")
                FeatureChip(Icons.Rounded.Palette, "Material 3 Expressive")
                FeatureChip(Icons.Rounded.Code, "Open source")
            }
        }
    }
}

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun LinkItem(
    shape: Shape,
    icon: ImageVector,
    title: String,
    subtitle: String,
    setting: Setting? = null,
    onClick: () -> Unit,
) {
    SettingCard(setting = setting, shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon)
            Spacer(Modifier.width(14.dp))
            ItemText(title, subtitle, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
