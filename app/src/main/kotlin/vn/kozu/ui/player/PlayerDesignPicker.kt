package vn.kozu.ui.player

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.player.RepeatState
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.i18n.KozuText as Text

/** Renders the selected layout itself, using inert sample playback data. */
@Composable
fun PlayerDesignLivePreview(
    style: PlayerLayoutStyle,
    modifier: Modifier = Modifier,
) {
    val beatmap = remember {
        Beatmap(
            id = 0L,
            title = "PASSO BEM SOLTO",
            artist = "ATLXS",
            mapper = "Kozu",
        )
    }
    val position = remember { MutableStateFlow(40_000L) }
    val duration = remember { MutableStateFlow(103_000L) }
    var playing by remember { mutableStateOf(false) }
    var favorite by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(RepeatState.OFF) }
    var sleepActive by remember { mutableStateOf(false) }

    val previewUi = remember(beatmap, position, duration, playing, favorite, shuffle, repeat, sleepActive) {
        PlayerUi(
            beatmap = beatmap,
            title = beatmap.title,
            artist = beatmap.artist,
            mapper = beatmap.mapper,
            queueLabel = null,
            isPlaying = playing,
            isFavorite = favorite,
            shuffle = shuffle,
            repeat = repeat,
            position = position,
            duration = duration,
            fileInfo = "OPUS",
            sleepActive = sleepActive,
            animated = false,
            compact = true,
            artShape = RoundedCornerShape(24.dp),
            useOriginalArtwork = false,
            coverScale = 1f,
            artworkSwipe = false,
            showToggleRow = true,
            showVolume = false,
            onClose = {},
            onQueue = {},
            onSleep = { sleepActive = !sleepActive },
            onMore = {},
            onPlayPause = { playing = !playing },
            onPrevious = {},
            onNext = {},
            onSeek = { position.value = it.coerceIn(0L, duration.value) },
            onShuffle = { shuffle = !shuffle },
            onRepeat = {
                repeat = when (repeat) {
                    RepeatState.OFF -> RepeatState.ALL
                    RepeatState.ALL -> RepeatState.ONE
                    RepeatState.ONE -> RepeatState.OFF
                }
            },
            onFavorite = { favorite = !favorite },
            onShare = {},
            onArtist = {},
            onMapper = {},
            previewMode = true,
        )
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.fillMaxSize()) {
            PlayerDesignContent(design = style, ui = previewUi)
        }
    }
}

/** Text-first design picker. The large preview updates before a layout is applied. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignSheet(
    selected: PlayerLayoutStyle,
    onSelect: (PlayerLayoutStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var previewStyle by remember(selected) { mutableStateOf(selected) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Player design", style = MaterialTheme.typography.headlineSmall)
            PlayerDesignLivePreview(
                style = previewStyle,
                modifier = Modifier.fillMaxWidth().height(340.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(previewStyle.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    previewStyle.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Text("Choose a layout", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PlayerLayoutStyle.entries.forEach { style ->
                    val isSelected = style == previewStyle
                    Surface(
                        modifier = Modifier.fillMaxWidth().bounceClick(pressedScale = 0.985f) { previewStyle = style },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colors.secondaryContainer else colors.surfaceContainer,
                        contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(style.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    style.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) colors.onSecondaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            RadioButton(selected = isSelected, onClick = { previewStyle = style })
                        }
                    }
                }
            }
            Button(
                onClick = { onSelect(previewStyle) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Use ${previewStyle.label}")
            }
        }
    }
}
