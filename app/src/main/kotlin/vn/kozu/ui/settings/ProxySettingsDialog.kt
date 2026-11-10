package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.kozu.data.remote.ProxyController
import vn.kozu.data.remote.ProxyTestResult
import vn.kozu.domain.model.ProxySettings
import vn.kozu.domain.model.ProxyType
import vn.kozu.KozuApplication
import vn.kozu.R

private val hostPattern = Regex("^[A-Za-z0-9._\\-]+$|^\\[[0-9A-Fa-f:.]+]$|^[0-9A-Fa-f:]+:[0-9A-Fa-f:]*$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySettingsDialog(
    current: ProxySettings,
    onDismiss: () -> Unit,
    onSave: (ProxySettings) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { (context.applicationContext as KozuApplication).container.httpClient }

    var type by remember { mutableStateOf(current.type) }
    var host by remember { mutableStateOf(current.host) }
    var port by remember { mutableStateOf(if (current.port > 0) current.port.toString() else "") }
    var user by remember { mutableStateOf(current.username) }
    var password by remember { mutableStateOf(current.password) }
    var showPassword by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ProxyTestResult?>(null) }

    val enabled = type != ProxyType.DISABLED
    val trimmedHost = host.trim()
    val portNumber = port.toIntOrNull()
    val hostError = enabled && trimmedHost.isNotEmpty() && !hostPattern.matches(trimmedHost)
    val portError = enabled && port.isNotEmpty() && (portNumber == null || portNumber !in 1..65535)
    val valid = !enabled || (trimmedHost.isNotEmpty() && !hostError && portNumber != null && portNumber in 1..65535)

    fun proposed() =
        ProxySettings(
            type = type,
            host = trimmedHost,
            port = portNumber ?: 0,
            username = user.trim(),
            password = password,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.proxy_server)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                        modifier =
                            Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ProxyType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    type = option
                                    testResult = null
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it.replace("\n", "").take(253)
                        testResult = null
                    },
                    enabled = enabled,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 2,
                    label = { Text(stringResource(R.string.host)) },
                    isError = hostError,
                    supportingText =
                        if (hostError) {
                            { Text(stringResource(R.string.enter_a_valid_host_name_or_ip_address)) }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it.filter(Char::isDigit).take(5)
                        testResult = null
                    },
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.port)) },
                    isError = portError,
                    supportingText =
                        if (portError) {
                            { Text(stringResource(R.string.port_must_be_between_1_and_65535)) }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = {
                        user = it.replace("\n", "").take(128)
                        testResult = null
                    },
                    enabled = enabled,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 2,
                    label = { Text(stringResource(R.string.username_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it.replace("\n", "").take(128)
                        testResult = null
                    },
                    enabled = enabled,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 2,
                    label = { Text(stringResource(R.string.password_optional)) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }, enabled = enabled) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                testResult = ProxyController.test(client, proposed())
                                testing = false
                            }
                        },
                        enabled = enabled && valid && !testing,
                    ) { Text(stringResource(R.string.test_connection)) }
                    Spacer(Modifier.width(12.dp))
                    if (testing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    when (val result = testResult) {
                        is ProxyTestResult.Success -> {
                            Text(stringResource(R.string.connected_in_ms, result.latencyMs),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        is ProxyTestResult.Failure -> {
                            Text(result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        null -> {}
                    }
                }
                Text(if (enabled) {
                        "Credentials are stored on this device and are never included in backups."
                    } else {
                        "Kozu connects directly to the internet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(proposed()) }, enabled = valid && !testing) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
