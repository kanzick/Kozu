package vn.kozu.ui.player

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kozu.player.SleepTimer
import vn.kozu.R
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.KozuSheetHandle
import vn.kozu.ui.components.rememberKozuHaptics

private val Presets = listOf(5, 10, 15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    timer: SleepTimer?,
    onDismiss: () -> Unit,
    onSetMinutes: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit
) {
    val haptics = rememberKozuHaptics()
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var custom by remember { mutableStateOf("") }

    LaunchedEffect(timer) {
        while (timer != null && !timer.atEndOfTrack) {
            now = SystemClock.elapsedRealtime()
            delay(500L)
        }
    }

    val customMinutes = custom.toIntOrNull()?.takeIf { it in 1..MAX_CUSTOM_MINUTES }
    val status = when {
        timer == null -> "Music stops automatically when the timer ends"
        timer.atEndOfTrack -> "Stops after the current track"
        else -> "Stops in ${formatRemaining(timer.endsAtElapsedMs - now)}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (timer != null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Bedtime,
                            contentDescription = null,
                            tint = if (timer != null) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.headlineSmall)
                    Text(text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (timer != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Presets.forEach { minutes ->
                    TimerChip(
                        label = "$minutes min",
                        selected = false,
                        onClick = {
                            haptics.confirm()
                            onSetMinutes(minutes)
                            onDismiss()
                        }
                    )
                }
                TimerChip(
                    label = "End of track",
                    selected = timer?.atEndOfTrack == true,
                    onClick = {
                        haptics.confirm()
                        onEndOfTrack()
                        onDismiss()
                    }
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { value -> custom = value.filter(Char::isDigit).take(4) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.custom_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        customMinutes?.let {
                            haptics.confirm()
                            onSetMinutes(it)
                            onDismiss()
                        }
                    },
                    enabled = customMinutes != null
                ) {
                    Text(stringResource(R.string.start))
                }
            }

            if (timer != null) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        haptics.tick()
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.turn_off_timer))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TimerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.bounceClick(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = ((remainingMs.coerceAtLeast(0L) + 999L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private const val MAX_CUSTOM_MINUTES = 1440
