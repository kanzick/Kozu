package vn.kozu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import vn.kozu.domain.model.AppFont
import vn.kozu.domain.model.AppearancePrefs
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.MiniPlayerColors
import vn.kozu.domain.model.MiniPlayerProgress
import vn.kozu.domain.model.MiniPlayerStyle
import vn.kozu.domain.model.PlaybackIcon
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.domain.model.SeekBarStyle
import vn.kozu.domain.model.SliderStyle
import vn.kozu.domain.model.TransportShape
import vn.kozu.ui.components.KozuSlider
import vn.kozu.ui.components.WavySlider
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.toShape
import vn.kozu.ui.player.MiniPlayer

// ---------------------------------------------------------------------------------------------
// Typography
// ---------------------------------------------------------------------------------------------

@Composable
fun TypographyPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appearance = settings.appearance
    val flex = settings.appFont.hasWidthAxis

    SettingsScaffold(title = "Typography", onBack = onBack) {
        SettingsSection("Font") {
            dropdown(
                Setting.APP_FONT,
                settings.appFont,
                AppFont.entries.toList(),
                { it.label },
            ) { font -> viewModel.save { setAppFont(font) } }
        }
        SettingsSection("Size & style") {
            slider(
                Setting.TEXT_SIZE,
                appearance.textScale.toFloat(),
                AppearancePrefs.TEXT_SCALE_MIN.toFloat()..AppearancePrefs.TEXT_SCALE_MAX.toFloat(),
                valueLabel = { "${it.toInt()}%" },
                unit = 5f,
            ) { value -> viewModel.updateAppearance { copy(textScale = value.toInt()) } }
            slider(
                Setting.FONT_WEIGHT,
                appearance.fontWeightShift.toFloat(),
                AppearancePrefs.WEIGHT_SHIFT_MIN.toFloat()..AppearancePrefs.WEIGHT_SHIFT_MAX.toFloat(),
                valueLabel = { weightLabel(it.toInt()) },
                unit = 50f,
            ) { value -> viewModel.updateAppearance { copy(fontWeightShift = value.toInt()) } }
            slider(
                Setting.FONT_WIDTH,
                appearance.fontWidth.toFloat(),
                AppearancePrefs.WIDTH_MIN.toFloat()..AppearancePrefs.WIDTH_MAX.toFloat(),
                valueLabel = { "${it.toInt()}%" },
                enabled = flex,
                unit = 5f,
            ) { value -> viewModel.updateAppearance { copy(fontWidth = value.toInt()) } }
            slider(
                Setting.FONT_ROUNDNESS,
                appearance.fontRoundness.toFloat(),
                0f..100f,
                valueLabel = { "${it.toInt()}%" },
                enabled = flex,
                unit = 10f,
            ) { value -> viewModel.updateAppearance { copy(fontRoundness = value.toInt()) } }
        }
        SettingsSection("Preview") {
            custom { shape -> TypographyPreview(shape) }
        }
        SettingsSection(null) {
            action(
                Setting.RESET_TYPOGRAPHY,
                label = "Reset",
            ) {
                viewModel.save { setAppFont(AppFont.GOOGLE_SANS_FLEX) }
                viewModel.updateAppearance { copy(textScale = 100, fontWeightShift = 0, fontWidth = 100, fontRoundness = 100) }
            }
        }
    }
}

private fun weightLabel(shift: Int): String =
    when {
        shift == 0 -> "Default"
        shift < 0 -> "Lighter ${-shift / 50}"
        else -> "Bolder +${shift / 50}"
    }

@Composable
private fun TypographyPreview(shape: Shape) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Headline sample", style = MaterialTheme.typography.headlineSmall)
            Text("Title: Dracula (feat. JENNIE)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Body text reads comfortably at any size. Everything in the app follows this font, so lists, " +
                    "dialogs and the player always match.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "LABEL \u00b7 0123456789 \u00b7 Aa Bb Cc",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Player & mini player
// ---------------------------------------------------------------------------------------------

@Composable
fun PlayerStylePage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appearance = settings.appearance

    SettingsScaffold(title = "Player & mini player", onBack = onBack) {
        SettingsSection("Live preview") {
            custom { shape -> PlayerPreviewCard(shape, appearance.seekStyle) }
        }
        SettingsSection("Player bar") {
            dropdown(
                Setting.MINI_STYLE,
                appearance.miniStyle,
                MiniPlayerStyle.entries.toList(),
                { it.label },
                description = appearance.miniStyle.description,
            ) { value -> viewModel.updateAppearance { copy(miniStyle = value) } }
            dropdown(
                Setting.MINI_COLORS,
                appearance.miniColors,
                MiniPlayerColors.entries.toList(),
                { it.label },
            ) { value -> viewModel.updateAppearance { copy(miniColors = value) } }
            dropdown(
                Setting.MINI_PROGRESS,
                appearance.miniProgress,
                MiniPlayerProgress.entries.toList(),
                { it.label },
            ) { value -> viewModel.updateAppearance { copy(miniProgress = value) } }
            dropdown(
                Setting.MINI_ART_SHAPE,
                appearance.miniArtShape,
                ArtworkShape.entries.toList(),
                { if (it == ArtworkShape.DEFAULT) "Follow album art shape" else it.label },
                leading = { value -> ShapeSwatch(value) },
            ) { value -> viewModel.updateAppearance { copy(miniArtShape = value) } }
            slider(
                Setting.MINI_CORNER,
                appearance.miniCorner.toFloat(),
                0f..AppearancePrefs.MINI_CORNER_MAX.toFloat(),
                valueLabel = { "${it.toInt()} dp" },
                unit = 1f,
            ) { value -> viewModel.updateAppearance { copy(miniCorner = value.toInt()) } }
            switch(Setting.MINI_CONTROLS, appearance.miniShowControls, enabled = appearance.miniStyle != MiniPlayerStyle.MINIMAL) { enabled ->
                viewModel.updateAppearance { copy(miniShowControls = enabled) }
            }
            switch(Setting.MINI_ARTIST, appearance.miniShowArtist) { enabled ->
                viewModel.updateAppearance { copy(miniShowArtist = enabled) }
            }
            switch(Setting.MINI_SPIN, appearance.miniSpinArtwork, enabled = !settings.performanceMode) { enabled ->
                viewModel.updateAppearance { copy(miniSpinArtwork = enabled) }
            }
            switch(Setting.MINI_SWIPE, appearance.miniSwipeToSkip) { enabled ->
                viewModel.updateAppearance { copy(miniSwipeToSkip = enabled) }
            }
        }
        SettingsSection("Full player") {
            dropdown(
                Setting.PLAYER_LAYOUT,
                appearance.playerLayoutStyle,
                PlayerLayoutStyle.entries.toList(),
                { it.label },
                description = appearance.playerLayoutStyle.description,
            ) { value -> viewModel.updateAppearance { copy(playerLayoutStyle = value) } }
            dropdown(
                Setting.SEEK_STYLE,
                appearance.seekStyle,
                SeekBarStyle.entries.toList(),
                { it.label },
            ) { value -> viewModel.updateAppearance { copy(seekStyle = value) } }
            dropdown(
                Setting.PLAYER_ART_SHAPE,
                appearance.playerArtShape,
                ArtworkShape.entries.toList(),
                { if (it == ArtworkShape.DEFAULT) "Follow album art shape" else it.label },
                leading = { value -> ShapeSwatch(value) },
            ) { value -> viewModel.updateAppearance { copy(playerArtShape = value) } }
            dropdown(
                Setting.TRANSPORT_SHAPE,
                appearance.transportShape,
                TransportShape.entries.toList(),
                { it.label },
            ) { value -> viewModel.updateAppearance { copy(transportShape = value) } }
            dropdown(
                Setting.ICON_STYLE,
                appearance.iconStyle,
                vn.kozu.domain.model.IconStyle.entries.toList(),
                { it.label },
            ) { value -> viewModel.updateAppearance { copy(iconStyle = value) } }
            dropdown(
                Setting.PLAYBACK_ICON,
                settings.playbackIcon,
                PlaybackIcon.entries.toList(),
                { it.label },
                leading = { icon ->
                    Icon(
                        imageVector = playbackIconVector(icon, isPlaying = false),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
            ) { icon -> viewModel.save { setPlaybackIcon(icon) } }
            switch(
                Setting.VOLUME_BAR,
                appearance.showVolumeBar,
                enabled = appearance.playerLayoutStyle.supportsVolumeBar,
            ) { enabled ->
                viewModel.updateAppearance { copy(showVolumeBar = enabled) }
            }
            switch(Setting.TOGGLE_ROW, appearance.showToggleRow) { enabled ->
                viewModel.updateAppearance { copy(showToggleRow = enabled) }
            }
            switch(Setting.SHOW_FILE_INFO, settings.showFileInfo) { enabled ->
                viewModel.save { setShowFileInfo(enabled) }
            }
        }
        SettingsSection("Background") {
            switch(Setting.BLUR_PLAYER, settings.blurPlayer, enabled = !settings.performanceMode) { enabled ->
                viewModel.save { setBlurPlayer(enabled) }
            }
            slider(
                Setting.BLUR_INTENSITY,
                settings.blurIntensity.toFloat(),
                0f..120f,
                valueLabel = { "${it.toInt()} dp" },
                enabled = settings.blurPlayer && !settings.performanceMode,
                unit = 5f,
            ) { value -> viewModel.save { setBlurIntensity(value.toInt()) } }
        }
        SettingsSection(null) {
            action(Setting.RESET_PLAYER_STYLE, label = "Reset") {
                viewModel.updateAppearance {
                    copy(
                        miniStyle = MiniPlayerStyle.FLOATING,
                        miniColors = MiniPlayerColors.GRADIENT,
                        miniProgress = MiniPlayerProgress.RING,
                        miniArtShape = ArtworkShape.CIRCLE,
                        miniCorner = 32,
                        miniShowControls = true,
                        miniShowArtist = true,
                        miniSpinArtwork = true,
                        miniSwipeToSkip = true,
                        seekStyle = SeekBarStyle.WAVY,
                        playerLayoutStyle = PlayerLayoutStyle.CLASSIC,
                        showVolumeBar = true,
                        playerArtShape = ArtworkShape.DEFAULT,
                        transportShape = TransportShape.PILL,
                        iconStyle = vn.kozu.domain.model.IconStyle.ROUNDED,
                        showToggleRow = true,
                    )
                }
            }
        }
    }
}

/** Shows the real mini player and seek bar with the saved settings, so changes can be judged at a glance. */
@Composable
private fun PlayerPreviewCard(
    shape: Shape,
    seekStyle: SeekBarStyle,
) {
    val sample = remember { Beatmap(id = 0L, title = "Dracula (feat. JENNIE)", artist = "Tame Impala") }
    val duration = remember { MutableStateFlow(214_000L) }
    val position = remember { MutableStateFlow(68_000L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            position.value = (position.value + 400L) % duration.value
        }
    }
    var seek by remember { mutableFloatStateOf(0.3f) }

    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(vertical = 14.dp)) {
            MiniPlayer(
                beatmap = sample,
                isPlaying = true,
                position = position,
                duration = duration,
                onOpen = {},
                onPlayPause = {},
                onPrevious = {},
                onNext = {},
                onDismiss = {},
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 22.dp)) {
                WavySlider(
                    progress = seek,
                    isPlaying = true,
                    onScrub = { seek = it },
                    onScrubFinished = { seek = it },
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    style = seekStyle,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------------------------

/** Small filled swatch showing an artwork shape inside a dropdown row. */
@Composable
internal fun ShapeSwatch(shape: ArtworkShape) {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = shape.toShape(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.primary,
    ) {}
}

/** Two demo sliders (smooth and stepped) so the slider style and its motion can be tried right away. */
@Composable
internal fun SliderPreviewCard(
    shape: Shape,
    style: SliderStyle,
) {
    var smooth by remember { mutableFloatStateOf(0.55f) }
    var stepped by remember { mutableFloatStateOf(4f) }
    SettingCard(setting = null, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                "Try it",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            KozuSlider(
                value = smooth,
                onValueChange = { smooth = it },
                style = style,
                bubbleLabel = { "${(it * 100).toInt()}%" },
            )
            KozuSlider(
                value = stepped,
                onValueChange = { stepped = it },
                valueRange = 0f..8f,
                steps = 7,
                style = style,
                bubbleLabel = { "${it.toInt()}" },
            )
        }
    }
}
