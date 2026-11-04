package vn.kozu.ui.player

import vn.kozu.ui.i18n.kozuUiString

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.heightIn
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
import vn.kozu.ui.i18n.KozuText as Text
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
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.player.PlaybackState
import vn.kozu.player.SleepTimer
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.FullBeatmapArt
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
        // Keep the artwork visually large while paused; only use a subtle scale change.
        targetValue = if (state.isPlaying) 1f else 0.98f,
        animationSpec = KozuMotion.spatial(),
        label = "coverScale",
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
                    .padding(top = 6.dp, bottom = 6.dp),
        ) {
            val layoutStyle = settings.appearance.playerLayoutStyle
            val compact = maxHeight < 660.dp || layoutStyle == PlayerLayoutStyle.COMPACT
            val artworkGutter = if (maxWidth < 380.dp) 18.dp else 22.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 9.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    PlayerTopBar(
                        onClose = onClose,
                        onQueue = { showQueue = true },
                        sleepActive = timer != null,
                        onSleep = { showSleep = true },
                        compact = compact,
                    )
                }

                // Resolve the full-player shape once and use the exact same mask for every layout.
                val effectiveShape =
                    if (settings.appearance.playerArtShape == ArtworkShape.DEFAULT) {
                        settings.appearance.artworkShape
                    } else {
                        settings.appearance.playerArtShape
                    }
                val playerArtShape = effectiveShape.toShape(RoundedCornerShape(24.dp))
                val useOriginalArtwork = effectiveShape == ArtworkShape.SQUARE
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = artworkGutter)
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
                                                    dy > 160f && abs(dy) > abs(dx) -> onClose()
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
                                } else Modifier,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    val widthFraction = when (layoutStyle) {
                        PlayerLayoutStyle.BALANCED -> 0.96f
                        PlayerLayoutStyle.EDITORIAL -> 0.94f
                        PlayerLayoutStyle.IMMERSIVE -> 0.985f
                        PlayerLayoutStyle.COMPACT -> 0.82f
                    }
                    val artworkSize = minOf(maxWidth * widthFraction, maxHeight * 0.985f)
                    val artworkModifier =
                        Modifier
                            .size(artworkSize)
                            .graphicsLayer {
                                scaleX = coverScale
                                scaleY = coverScale
                            }
                    if (useOriginalArtwork) {
                        FullBeatmapArt(
                            beatmap = beatmap,
                            shape = playerArtShape,
                            followGlobalShape = false,
                            wrapToImage = true,
                            modifier = artworkModifier,
                        )
                    } else {
                        BeatmapArt(
                            beatmap = beatmap,
                            shape = playerArtShape,
                            large = false,
                            followGlobalShape = false,
                            modifier = artworkModifier,
                        )
                    }
                }

                PlayerTrackInfo(
                    title = beatmap.displayTitle(settings.preferUnicode),
                    artist = artistName,
                    mapper = beatmap.mapper,
                    layout = layoutStyle,
                    compact = compact,
                    onArtistClick = { onArtistClick(artistName) },
                    onMore = onMore,
                )

                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    PlayerProgress(
                        isPlaying = state.isPlaying,
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        animated = !performanceMode,
                        fileInfo = if (settings.showFileInfo) fileInfo else null,
                    )
                }

                TransportControls(
                    isPlaying = state.isPlaying,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    compact = compact,
                )

                if (settings.appearance.showToggleRow) {
                    PlayerToggleRow(
                        shuffle = state.shuffle,
                        repeat = state.repeat,
                        isFavorite = isFavorite,
                        onShuffle = onShuffle,
                        onRepeat = onRepeat,
                        onFavorite = onFavorite,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        compact = compact,
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

@Composable
private fun PlayerTrackInfo(
    title: String,
    artist: String,
    mapper: String,
    layout: PlayerLayoutStyle,
    compact: Boolean,
    onArtistClick: () -> Unit,
    onMore: () -> Unit,
) {
    val titleStyle = when {
        compact -> MaterialTheme.typography.titleLarge
        layout == PlayerLayoutStyle.EDITORIAL -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.headlineSmall
    }
    val textContent: @Composable (Modifier, Boolean) -> Unit = { modifier, centered ->
        Column(
            modifier = modifier,
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = title,
                style = titleStyle,
                maxLines = if (layout == PlayerLayoutStyle.EDITORIAL && !compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Text(
                text = artist,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.bounceClick(onClick = onArtistClick),
            )
            if (mapper.isNotBlank() && !compact && layout != PlayerLayoutStyle.COMPACT) {
                Text(
                    text = "Mapped by $mapper",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }

    when (layout) {
        PlayerLayoutStyle.EDITORIAL ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(min = if (compact) 58.dp else 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                textContent(Modifier.fillMaxWidth().padding(horizontal = 52.dp), true)
                PlayerMoreButton(
                    onClick = onMore,
                    compact = compact,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

        PlayerLayoutStyle.IMMERSIVE ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    textContent(Modifier.weight(1f), false)
                    PlayerMoreButton(onClick = onMore, compact = compact, modifier = Modifier.padding(start = 10.dp))
                }
            }

        PlayerLayoutStyle.BALANCED, PlayerLayoutStyle.COMPACT ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                textContent(Modifier.weight(1f), false)
                PlayerMoreButton(onClick = onMore, compact = compact, modifier = Modifier.padding(start = 10.dp))
            }
    }
}

@Composable
private fun PlayerMoreButton(
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(if (compact) 46.dp else 52.dp).bounceClick(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = kozuUiString("More options"),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
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
    compact: Boolean,
) {
    val actionSize = if (compact) 46.dp else 52.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(actionSize)
                    .bounceClick(onClick = onClose),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = kozuUiString("Close player"))
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
                    .size(actionSize)
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
                    .size(actionSize)
                    .bounceClick(onClick = onQueue),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = kozuUiString("Queue"))
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
