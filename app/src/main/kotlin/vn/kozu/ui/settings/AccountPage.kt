package vn.kozu.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.domain.model.OsuProfile

@Composable
fun AccountPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val loading by viewModel.profileLoading.collectAsStateWithLifecycle()
    val verify by viewModel.verify.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showCredentialsDialog by rememberSaveable { mutableStateOf(false) }

    SettingsScaffold(title = "Account", onBack = onBack) {
        val current = profile
        if (loggedIn && current != null) {
            ProfileHeader(current)
            Spacer(Modifier.height(14.dp))
            ProfileStats(current)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SecondaryPill(if (loading) "Refreshing…" else "Refresh") { viewModel.refreshProfile() }
                TextButton(onClick = { viewModel.logout() }) { Text("Sign out") }
            }
            Spacer(Modifier.height(24.dp))
        } else if (loggedIn) {
            SettingsGroup(
                title = null,
                items = listOf({
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Signed in", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your profile is loading. Pull it again if it takes too long.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SecondaryPill("Load profile") { viewModel.refreshProfile() }
                            TextButton(onClick = { viewModel.logout() }) { Text("Sign out") }
                        }
                    }
                })
            )
            Spacer(Modifier.height(24.dp))
        } else {
            SettingsGroup(
                title = null,
                items = listOf({
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sign in with osu!", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Signing in only adds your osu! profile and rank here. You can browse and play the live catalog without signing in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryPill(
                            label = "Sign in with osu!",
                            enabled = settings.hasCredentials
                        ) { viewModel.login { url -> uriHandler.openUri(url) } }
                        if (!settings.hasCredentials) {
                            Text(
                                "Add your OAuth application below first.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                })
            )
            Spacer(Modifier.height(24.dp))
        }

        SettingsGroup(
            title = "Optional osu! profile sign-in",
            items = listOf({
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (settings.hasCredentials) "osu! sign-in credentials are saved" else "Connect an osu! profile (optional)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "These credentials are only for connecting your osu! profile. Searching, browsing new releases, and listening to music work for guests without an account or OAuth setup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    when (val state = verify) {
                        VerifyState.Idle -> Unit
                        VerifyState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        VerifyState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("OAuth credentials verified — profile sign-in is ready", color = MaterialTheme.colorScheme.primary)
                        }
                        is VerifyState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PrimaryPill(if (settings.hasCredentials) "Edit sign-in credentials" else "Set up profile sign-in") {
                            showCredentialsDialog = true
                        }
                        if (settings.hasCredentials) {
                            TextButton(onClick = viewModel::clearCredentials) { Text("Remove") }
                        }
                    }
                }
            })
        )
    }

    if (showCredentialsDialog) {
        OAuthCredentialsDialog(
            initialClientId = settings.osuClientId,
            initialClientSecret = settings.osuClientSecret,
            hasCredentials = settings.hasCredentials,
            onDismiss = { showCredentialsDialog = false },
            onSave = viewModel::saveAndVerify,
            onClear = viewModel::clearCredentials,
            onOpenOsuSettings = { uriHandler.openUri("https://osu.ppy.sh/home/account/edit#oauth") }
        )
    }
}

@Composable
private fun OAuthCredentialsDialog(
    initialClientId: String,
    initialClientSecret: String,
    hasCredentials: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onOpenOsuSettings: () -> Unit
) {
    val context = LocalContext.current
    var clientId by remember(initialClientId) { mutableStateOf(initialClientId) }
    var clientSecret by remember(initialClientSecret) { mutableStateOf(initialClientSecret) }
    val canSave = clientId.isNotBlank() && clientSecret.isNotBlank()
    val save = {
        onSave(clientId, clientSecret)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("osu! profile sign-in credentials") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Create an OAuth application in your osu! account. Signing in to an osu! profile is not required to browse or play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenOsuSettings) { Text("Open osu! OAuth settings") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Callback URL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(OsuAuth.REDIRECT_URI, style = MaterialTheme.typography.bodyLarge)
                    }
                    IconButton(onClick = {
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("callback", OsuAuth.REDIRECT_URI))
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy callback URL")
                    }
                }
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it.filter { char -> char.isDigit() }.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    label = { Text("Client ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    label = { Text("Client secret") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) save() })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = canSave) { Text("Save & test") }
        },
        dismissButton = {
            Row {
                if (hasCredentials) {
                    TextButton(onClick = {
                        onClear()
                        onDismiss()
                    }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ProfileHeader(profile: OsuProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        )
                )
                if (profile.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = profile.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
                )
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 20.dp, y = 36.dp)
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                        .clip(CircleShape)
                )
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 46.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.username,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.flag.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Text(profile.flag, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileChip(profile.mode.replaceFirstChar { it.uppercase() })
                    if (profile.countryName.isNotBlank()) ProfileChip(profile.countryName)
                    if (profile.isSupporter) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Supporter", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (profile.joinDate.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Joined ${profile.joinDate} · ${formatInt(profile.followers)} followers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileChip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProfileStats(profile: OsuProfile) {
    val rank = if (profile.globalRank > 0) "#${formatInt(profile.globalRank)}" else "—"
    val countryRank = if (profile.countryRank > 0) "#${formatInt(profile.countryRank)}" else "—"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Global rank", rank, Modifier.weight(1f), highlight = true)
            StatTile("Country rank", countryRank, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Performance", "${formatInt(profile.pp.toLong())} pp", Modifier.weight(1f))
            StatTile("Accuracy", "%.2f%%".format(profile.accuracy), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Play count", formatInt(profile.playCount), Modifier.weight(1f))
            StatTile("Play time", formatDurationLong(profile.playTimeSeconds), Modifier.weight(1f))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Level ${profile.level.toInt()}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(
                        "Ranked score ${formatCompact(profile.rankedScore)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (profile.level - profile.level.toInt()).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradeTile("SS", profile.gradeSS, Color(0xFFFFD54F), Modifier.weight(1f))
            GradeTile("S", profile.gradeS, Color(0xFFB0BEC5), Modifier.weight(1f))
            GradeTile("A", profile.gradeA, Color(0xFF81C784), Modifier.weight(1f))
        }
    }
}

@Composable
private fun GradeTile(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = color.copy(alpha = 0.22f)) {
        Column(Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = color)
            Text(formatInt(count), style = MaterialTheme.typography.titleMedium)
        }
    }
}
