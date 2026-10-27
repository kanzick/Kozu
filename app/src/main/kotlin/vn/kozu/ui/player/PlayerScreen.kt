package vn.kozu.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.Beatmap
import vn.kozu.player.PlaybackState
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.WavySlider
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.util.formatTime
import kotlin.math.abs


@Composable
fun PlayerScreen(
    beatmap: Beatmap,
    state: PlaybackState,
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQueue by remember { mutableStateOf(false) }

    val coverScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.9f,
        animationSpec = KozuMotion.spatial(),
        label = "coverScale"
    )
    val coverRadius by animateDpAsState(
        targetValue = if (state.isPlaying) 40.dp else 72.dp,
        animationSpec = KozuMotion.spatial(),
        label = "coverRadius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            PlayerTopBar(onClose = onClose, onQueue = { showQueue = true })

            Spacer(Modifier.height(20.dp))


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDragEnd = {
                                when {
                                    dy > 160f && abs(dy) > abs(dx) -> onClose()
                                    dx > 140f && abs(dx) > abs(dy) -> onPrevious()
                                    dx < -140f && abs(dx) > abs(dy) -> onNext()
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dx += amount.x
                                dy += amount.y
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                BeatmapArt(
                    beatmap = beatmap,
                    large = true,
                    shape = RoundedCornerShape(coverRadius),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                        }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = beatmap.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = beatmap.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (beatmap.mapper.isNotBlank()) {
                        Text(
                            text = "Mapped by ${beatmap.mapper}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(width = 56.dp, height = 56.dp)
                        .bounceClick(onClick = onMore),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PlayerProgress(
                isPlaying = state.isPlaying,
                audioInfo = state.audioInfo ?: if (state.isBuffering) "Loading audio…" else "Ready",
                position = position,
                duration = duration,
                onSeek = onSeek
            )

            Spacer(Modifier.height(20.dp))

            TransportControls(
                isPlaying = state.isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext
            )

            Spacer(Modifier.height(20.dp))

            PlayerToggleRow(
                shuffle = state.shuffle,
                repeat = state.repeat,
                isFavorite = isFavorite,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
                onFavorite = onFavorite
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.queueIndex,
            isPlaying = state.isPlaying,
            onDismiss = { showQueue = false },
            onSkipTo = onSkipToQueueItem,
            onRemove = onRemoveFromQueue
        )
    }
}

@Composable
private fun PlayerTopBar(onClose: () -> Unit, onQueue: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .bounceClick(onClick = onClose),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player")
            }
        }
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Surface(
            modifier = Modifier
                .size(52.dp)
                .bounceClick(onClick = onQueue),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue")
            }
        }
    }
}

@Composable
private fun PlayerProgress(
    isPlaying: Boolean,
    audioInfo: String,
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    onSeek: (Long) -> Unit
) {
    val pos by position.collectAsStateWithLifecycle()
    val dur by duration.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<Float?>(null) }

    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "progress")
    val shown = scrub ?: animated
    val shownMs = scrub?.let { (it * dur).toLong() } ?: pos

    Column {
        WavySlider(
            progress = shown,
            isPlaying = isPlaying,
            onScrub = { scrub = it },
            onScrubFinished = { fraction ->
                if (dur > 0L) onSeek((fraction * dur).toLong())
                scrub = null
            },
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(shownMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = audioInfo,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatTime(dur),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
