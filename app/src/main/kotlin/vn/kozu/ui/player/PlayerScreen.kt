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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.player.PlaybackState
import vn.kozu.player.SleepTimer
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.WavySlider
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.components.toShape
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
    onArtistClick: (String) -> Unit = {},
    onSkipToQueueItem: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    audioInfo: StateFlow<String?>? = null,
    sleepTimer: StateFlow<SleepTimer?>? = null,
    onSetSleepTimer: (Int) -> Unit = {},
    onSleepAtEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onMoveQueueItem: (from: Int, to: Int) -> Unit = { _, _ -> },
    onSaveAsPlaylist: (List<Beatmap>) -> Unit = {},
) {
    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val performanceMode = LocalPerformanceMode.current
    val haptics = rememberKozuHaptics()
    val timer by (sleepTimer ?: NoSleepTimer).collectAsStateWithLifecycle()
    val fileInfo by (audioInfo ?: NoAudioInfo).collectAsStateWithLifecycle()
    val blurRadius = if (settings.blurPlayer && !performanceMode) settings.blurIntensity else 0
    val artistName = beatmap.displayArtist(settings.preferUnicode)

    val view = LocalView.current
    DisposableEffect(view, settings.keepScreenOn) {
        val previous = view.keepScreenOn
        if (settings.keepScreenOn) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }

    val coverScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.9f,
        animationSpec = KozuMotion.spatial(),
        label = "coverScale",
    )
    val coverRadius by animateDpAsState(
        targetValue = if (state.isPlaying) 40.dp else 72.dp,
        animationSpec = KozuMotion.spatial(),
        label = "coverRadius",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        if (blurRadius > 0) {
            val backdropRequest =
                remember(beatmap.id) {
                    ImageRequest
                        .Builder(context)
                        .data(beatmap.thumbUrl)
                        .crossfade(400)
                        .build()
                }
            AsyncImage(
                model = backdropRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.55f }
                        .blur(blurRadius.dp, BlurredEdgeTreatment.Unbounded),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            val artworkSize = minOf(maxWidth, maxHeight * if (maxHeight < 700.dp) 0.3f else 0.4f)
            val compact = maxHeight < 580.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerTopBar(
                    onClose = onClose,
                    onQueue = { showQueue = true },
                    sleepActive = timer != null,
                    onSleep = { showSleep = true },
                )

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(artworkSize)
                            .then(
                                if (settings.artworkSwipe) {
                                    Modifier.pointerInput(Unit) {
                                        var dx = 0f
                                        var dy = 0f
                                        detectDragGestures(
                                            onDragStart = {
                                                dx = 0f
                                                dy = 0f
                                            },
                                            onDragEnd = {
                                                when {
                                                    dy > 160f && abs(dy) > abs(dx) -> {
                                                        onClose()
                                                    }

                                                    dx > 140f && abs(dx) > abs(dy) -> {
                                                        haptics.tick()
                                                        onPrevious()
                                                    }

                                                    dx < -140f && abs(dx) > abs(dy) -> {
                                                        haptics.tick()
                                                        onNext()
                                                    }
                                                }
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dx += amount.x
                                                dy += amount.y
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    BeatmapArt(
                        beatmap = beatmap,
                        large = true,
                        shape = settings.appearance.playerArtShape.toShape(RoundedCornerShape(coverRadius)),
                        followGlobalShape = settings.appearance.playerArtShape == ArtworkShape.DEFAULT,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = coverScale
                                    scaleY = coverScale
                                },
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = beatmap.displayTitle(settings.preferUnicode),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.bounceClick {
                                onArtistClick(artistName)
                            },
                        )
                        if (beatmap.mapper.isNotBlank() && !compact) {
                            Text(
                                text = "Mapped by ${beatmap.mapper}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Surface(
                        modifier =
                            Modifier
                                .padding(start = 12.dp)
                                .size(width = 56.dp, height = 56.dp)
                                .bounceClick(onClick = onMore),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                PlayerProgress(
                    isPlaying = state.isPlaying,
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    animated = !performanceMode,
                    fileInfo = if (settings.showFileInfo) fileInfo else null,
                )

                TransportControls(
                    isPlaying = state.isPlaying,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                )

                if (settings.appearance.showToggleRow) {
                    PlayerToggleRow(
                        shuffle = state.shuffle,
                        repeat = state.repeat,
                        isFavorite = isFavorite,
                        onShuffle = onShuffle,
                        onRepeat = onRepeat,
                        onFavorite = onFavorite,
                    )
                }
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.queueIndex,
            isPlaying = state.isPlaying,
            onDismiss = { showQueue = false },
            onSkipTo = onSkipToQueueItem,
            onRemove = onRemoveFromQueue,
            shuffle = state.shuffle,
            onClear = onClearQueue,
            onMove = onMoveQueueItem,
            onSaveAsPlaylist = onSaveAsPlaylist,
        )
    }

    if (showSleep) {
        SleepTimerSheet(
            timer = timer,
            onDismiss = { showSleep = false },
            onSetMinutes = onSetSleepTimer,
            onEndOfTrack = onSleepAtEndOfTrack,
            onCancel = onCancelSleepTimer,
        )
    }
}

private val NoSleepTimer = MutableStateFlow<SleepTimer?>(null)
private val NoAudioInfo = MutableStateFlow<String?>(null)

@Composable
private fun PlayerTopBar(
    onClose: () -> Unit,
    onQueue: () -> Unit,
    sleepActive: Boolean,
    onSleep: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(52.dp)
                    .bounceClick(onClick = onClose),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player")
            }
        }
        Text(
            text = "Now Playing",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier =
                Modifier
                    .size(52.dp)
                    .bounceClick(onClick = onSleep),
            shape = CircleShape,
            color =
                if (sleepActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = if (sleepActive) "Sleep timer active" else "Sleep timer",
                    tint =
                        if (sleepActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (sleepActive) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 11.dp, end = 11.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier =
                Modifier
                    .size(52.dp)
                    .bounceClick(onClick = onQueue),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
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
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    onSeek: (Long) -> Unit,
    animated: Boolean,
    fileInfo: String?,
) {
    val pos by position.collectAsStateWithLifecycle()
    val dur by duration.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<Float?>(null) }

    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val smoothed by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "progress")
    val shown = scrub ?: smoothed
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
            inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            animated = animated,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(shownMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatTime(dur),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!fileInfo.isNullOrBlank()) {
            Text(
                text = fileInfo,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
