package vn.kozu.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.player.EqualizerProfile
import vn.kozu.ui.components.KozuSlider
import java.util.Locale
import kotlin.math.abs

private const val CUSTOM_PRESET = "Custom"

@Composable
fun EqualizerPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bandCount = EqualizerProfile.centerFrequenciesHz.size
    val stored = List(bandCount) { settings.equalizerGains.getOrElse(it) { 0f }.coerceIn(-1f, 1f) }
    var draft by remember(stored) { mutableStateOf(stored) }
    val enabled = settings.equalizerEnabled
    val activePreset = EqualizerProfile.presets.entries
        .firstOrNull { preset -> preset.value.zip(draft).all { (a, b) -> abs(a - b) < 0.02f } }
        ?.key ?: CUSTOM_PRESET

    SettingsScaffold(title = "Equalizer", onBack = onBack) {
        SettingsSection(null) {
            switch(Setting.EQUALIZER_BANDS, enabled) { viewModel.setEqualizerEnabled(it) }
        }
        SettingsSection("Presets") {
            custom { shape ->
                PresetCard(
                    shape = shape,
                    active = activePreset,
                    enabled = enabled,
                    onPick = { name ->
                        val gains = EqualizerProfile.presets.getValue(name)
                        draft = gains
                        viewModel.setEqualizerGains(gains)
                    }
                )
            }
        }
        SettingsSection("Frequency bands") {
            custom { shape ->
                BandsCard(
                    shape = shape,
                    gains = draft,
                    enabled = enabled,
                    onChange = { index, value -> draft = draft.toMutableList().also { it[index] = value } },
                    onCommit = { viewModel.setEqualizerGains(draft) },
                    onReset = {
                        draft = EqualizerProfile.flat
                        viewModel.setEqualizerGains(EqualizerProfile.flat)
                    }
                )
            }
        }
        SettingsSection("Audio effects") {
            slider(
                Setting.BASS_BOOST,
                settings.bassBoost.toFloat(),
                0f..1000f,
                valueLabel = { percent(it) },
                enabled = enabled
            ) { value -> viewModel.save { setBassBoost(value.toInt()) } }
            switch(
                Setting.VIRTUALIZER,
                settings.virtualizer > 0,
                enabled = enabled,
                description = Setting.VIRTUALIZER.description
            ) { checked -> viewModel.save { setVirtualizer(if (checked) 1000 else 0) } }
            slider(
                Setting.LOUDNESS,
                settings.loudness.toFloat(),
                0f..1000f,
                valueLabel = { percent(it) },
                enabled = enabled
            ) { value -> viewModel.save { setLoudness(value.toInt()) } }
        }
    }
}

private fun percent(value: Float): String = "${(value / 10f).toInt()}%"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetCard(
    shape: Shape,
    active: String,
    enabled: Boolean,
    onPick: (String) -> Unit
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (EqualizerProfile.presets.keys + CUSTOM_PRESET).forEach { name ->
                FilterChip(
                    selected = name == active,
                    onClick = { if (name != CUSTOM_PRESET) onPick(name) },
                    enabled = enabled,
                    label = { Text(name) }
                )
            }
        }
    }
}

@Composable
private fun BandsCard(
    shape: Shape,
    gains: List<Float>,
    enabled: Boolean,
    onChange: (Int, Float) -> Unit,
    onCommit: () -> Unit,
    onReset: () -> Unit
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EqualizerProfile.centerFrequenciesHz.forEachIndexed { index, frequency ->
                    Band(
                        frequency = frequency,
                        gain = gains[index],
                        enabled = enabled,
                        onChange = { onChange(index, it) },
                        onCommit = onCommit
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onReset,
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun Band(
    frequency: Float,
    gain: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    onCommit: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Text(
            text = String.format(Locale.US, "%+.1f", gain * EqualizerProfile.MAX_GAIN_DB),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(width = 48.dp, height = 200.dp)) {
            KozuSlider(
                value = gain,
                onValueChange = onChange,
                onValueChangeFinished = onCommit,
                valueRange = -1f..1f,
                origin = 0f,
                vertical = true,
                enabled = enabled,
                contentDescription = frequencyLabel(frequency),
                modifier = Modifier.height(200.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = frequencyLabel(frequency),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
    }
}

private fun frequencyLabel(hz: Float): String =
    if (hz >= 1000f) String.format(Locale.US, "%.1f kHz", hz / 1000f).replace(".0 kHz", " kHz") else "${hz.toInt()} Hz"
