package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val verify by viewModel.verify.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .bounceClick(onClick = onBack),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text("Settings", style = MaterialTheme.typography.displaySmall)
            }

            Spacer(Modifier.height(24.dp))

            // ---- Appearance ----
            SettingsGroup(
                title = "Appearance",
                items = listOf(
                    {
                        Column {
                            Text("Theme", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = settings.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        label = { Text(mode.label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (mode) {
                                                    ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    },
                    {
                        SwitchRow(
                            title = "Pure black",
                            subtitle = "True black backgrounds in dark mode (AMOLED)",
                            checked = settings.amoled,
                            onChange = viewModel::setAmoled
                        )
                    },
                    {
                        SwitchRow(
                            title = "Artwork colors",
                            subtitle = "Tint the app with the cover of the playing song. Paused: back to osu! pink.",
                            checked = settings.artworkColors,
                            onChange = viewModel::setArtworkColors
                        )
                    },
                    {
                        var name by remember(settings.displayName) { mutableStateOf(settings.displayName) }
                        Column {
                            Text("Display name", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(24) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                placeholder = { Text("Shown in the Home greeting") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { viewModel.setDisplayName(name) })
                            )
                        }
                    }
                )
            )

            Spacer(Modifier.height(24.dp))

            // ---- osu! API ----
            var clientId by remember(settings.osuClientId) { mutableStateOf(settings.osuClientId) }
            var clientSecret by remember(settings.osuClientSecret) { mutableStateOf(settings.osuClientSecret) }
            SettingsGroup(
                title = "osu! API",
                items = listOf(
                    {
                        Column {
                            Text("Connect your osu! account", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Create an OAuth application in your osu! account settings, then paste its Client ID and Secret. " +
                                    "This unlocks the full catalog for Search, Explore and Home.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { uriHandler.openUri("https://osu.ppy.sh/home/account/edit#oauth") }) {
                                Text("Open osu! settings")
                            }
                        }
                    },
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = clientId,
                                onValueChange = { clientId = it.filter { c -> c.isDigit() }.take(10) },
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
                                keyboardActions = KeyboardActions(onDone = { viewModel.saveAndVerify(clientId, clientSecret) })
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.bounceClick(enabled = clientId.isNotBlank() && clientSecret.isNotBlank()) {
                                        viewModel.saveAndVerify(clientId, clientSecret)
                                    },
                                    shape = CircleShape,
                                    color = if (clientId.isNotBlank() && clientSecret.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (clientId.isNotBlank() && clientSecret.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        "Save & test",
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                if (settings.hasCredentials) {
                                    TextButton(onClick = {
                                        clientId = ""
                                        clientSecret = ""
                                        viewModel.disconnect()
                                    }) { Text("Disconnect") }
                                }
                            }
                            when (val state = verify) {
                                VerifyState.Idle -> Unit
                                VerifyState.Checking -> Text("Checking…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                VerifyState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Connected — live catalog enabled", color = MaterialTheme.colorScheme.primary)
                                }
                                is VerifyState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            )

            Spacer(Modifier.height(24.dp))

            // ---- Data ----
            SettingsGroup(
                title = "Data",
                items = listOf(
                    { ActionRow("Clear listening history", "Resets stats, recent songs and most played", viewModel::clearHistory) },
                    { ActionRow("Clear recent searches", "Removes the search suggestions", viewModel::clearSearches) }
                )
            )

            Spacer(Modifier.height(24.dp))

            SettingsGroup(
                title = "About",
                items = listOf(
                    {
                        Column {
                            Text("Kozu 1.0", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Listen to your favorite osu! songs anytime, anywhere. Songs play as the official osu! previews.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, items: List<@Composable () -> Unit>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = segmentedShape(index, items.size, large = 30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) { item() }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier
        .fillMaxWidth()
        .bounceClick(pressedScale = 0.98f, onClick = onClick)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
