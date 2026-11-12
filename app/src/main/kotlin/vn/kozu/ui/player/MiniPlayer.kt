package vn.kozu.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.AppearancePrefs
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.domain.model.IconStyle
import vn.kozu.domain.model.MiniPlayerColors
import vn.kozu.domain.model.MiniPlayerProgress
import vn.kozu.domain.model.MiniPlayerStyle
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.FullBeatmapArt
import vn.kozu.ui.components.KozuIcons
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.loopingMarquee
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.components.resolveArtworkShape
import vn.kozu.ui.components.toShape

private class MiniPalette(
    val brush: Brush?,
    val container: Color,
    val content: Color,
    val secondary: Color,
    val playContainer: Color,
    val playContent: Color,
    val sideContainer: Color,
    val sideContent: Color,
    val progress: Color,
    val track: Color,
)

@Composable
private fun miniPalette(mode: MiniPlayerColors): MiniPalette {
    val c = MaterialTheme.colorScheme
    return when (mode) {
        MiniPlayerColors.GRADIENT ->
            MiniPalette(
                brush = Brush.horizontalGradient(listOf(c.primaryContainer, c.tertiaryContainer)),
                container = c.primaryContainer,
                content = c.onPrimaryContainer,
                secondary = c.onPrimaryContainer.copy(alpha = 0.72f),
                playContainer = c.primary,
                playContent = c.onPrimary,
                sideContainer = c.surfaceContainerHighest,
                sideContent = c.onSurfaceVariant,
                progress = c.primary,
                track = c.onPrimaryContainer.copy(alpha = 0.15f),
            )

        MiniPlayerColors.TONAL ->
            MiniPalette(
                brush = null,
                container = c.secondaryContainer,
                content = c.onSecondaryContainer,
                secondary = c.onSecondaryContainer.copy(alpha = 0.72f),
                playContainer = c.secondary,
                playContent = c.onSecondary,
                sideContainer = c.onSecondaryContainer.copy(alpha = 0.12f),
                sideContent = c.onSecondaryContainer,
                progress = c.secondary,
                track = c.onSecondaryContainer.copy(alpha = 0.15f),
            )

        MiniPlayerColors.ACCENT ->
            MiniPalette(
                brush = null,
                container = c.primary,
                content = c.onPrimary,
                secondary = c.onPrimary.copy(alpha = 0.78f),
                playContainer = c.onPrimary,
                playContent = c.primary,
                sideContainer = c.onPrimary.copy(alpha = 0.16f),
                sideContent = c.onPrimary,
                progress = c.onPrimary,
                track = c.onPrimary.copy(alpha = 0.25f),
            )

        MiniPlayerColors.NEUTRAL ->
            MiniPalette(
                brush = null,
                container = c.surfaceContainerHigh,
                content = c.onSurface,
                secondary = c.onSurfaceVariant,
                playContainer = c.primary,
                playContent = c.onPrimary,
                sideContainer = c.surfaceContainerHighest,
                sideContent = c.onSurfaceVariant,
                progress = c.primary,
                track = c.onSurface.copy(alpha = 0.12f),
            )
    }
}

@Composable
fun MiniPlayer(
    beatmap: Beatmap,
    isPlaying: Boolean,
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val prefs = settings.appearance
    val palette = miniPalette(prefs.miniColors)
    val haptics = rememberKozuHaptics()
    val performanceMode = LocalPerformanceMode.current
    val scope = rememberCoroutineScope()

    val pos by position.collectAsStateWithLifecycle()
    val dur by duration.collectAsStateWithLifecycle()
    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "miniProgress")

    val cornerTarget = prefs.miniCorner.coerceIn(0, AppearancePrefs.MINI_CORNER_MAX).toFloat()
    val corner by animateFloatAsState(
        targetValue = cornerTarget,
        // Avoid spring overshoot beyond the supported corner-radius range.
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "miniCorner",
    )

    val artShape: Shape =
        if (prefs.miniArtShape == ArtworkShape.DEFAULT) {
            resolveArtworkShape(CircleShape)
        } else {
            prefs.miniArtShape.toShape(CircleShape)
        }
    val roundArt =
        prefs.miniArtShape == ArtworkShape.CIRCLE ||
            prefs.miniArtShape == ArtworkShape.COOKIE ||
            prefs.miniArtShape == ArtworkShape.FLOWER ||
            prefs.miniArtShape == ArtworkShape.CLOVER
    val canSpin = prefs.miniSpinArtwork && roundArt && !performanceMode

    val progressMode =
        if (prefs.miniProgress == MiniPlayerProgress.RING && !roundArt) MiniPlayerProgress.LINE else prefs.miniProgress

    val spin = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, canSpin) {
        if (isPlaying && canSpin) {
            while (true) {
                spin.animateTo(spin.value + 360f, tween(10_000, easing = LinearEasing))
            }
        }
    }


    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }


    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    var gestureAnimation by remember { mutableStateOf<Job?>(null) }
    val latestGestureAnimation = rememberUpdatedState(gestureAnimation)
    LaunchedEffect(beatmap.id) {
        latestGestureAnimation.value?.cancel()
        offsetX = 0f
        offsetY = 0f
    }

    val title = beatmap.displayTitle(settings.preferUnicode)
    val artist = beatmap.displayArtist(settings.preferUnicode)
    val style = prefs.miniStyle

    val shape: Shape =
        when (style) {
            MiniPlayerStyle.DOCKED -> RoundedCornerShape(topStart = corner.coerceIn(0f, 28f).dp, topEnd = corner.coerceIn(0f, 28f).dp)
            MiniPlayerStyle.CARD -> RoundedCornerShape(corner.coerceIn(0f, 36f).dp)
            else -> RoundedCornerShape(corner.coerceIn(0f, AppearancePrefs.MINI_CORNER_MAX.toFloat()).dp)
        }
    val outerPadding =
        when (style) {
            MiniPlayerStyle.DOCKED -> Modifier
            else -> Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        }

    Surface(
        onClick = onOpen,
        modifier =
            modifier
                .fillMaxWidth()
                .then(outerPadding)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    alpha = 1f - (abs(offsetX) / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.42f)
                }.pointerInput(prefs.miniSwipeToSkip) {
                    var axis = 0
                    var dx = 0f
                    var dy = 0f
                    val skipThreshold = 96.dp.toPx()


                    val dismissThreshold = 36.dp.toPx()
                    val flingThreshold = 650.dp.toPx()
                    val tracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            gestureAnimation?.cancel()
                            axis = 0
                            dx = 0f
                            dy = 0f
                            tracker.resetTracking()
                        },
                        onDrag = { change, drag ->
                            dx += drag.x
                            dy += drag.y
                            tracker.addPosition(change.uptimeMillis, Offset(dx, dy))
                            if (axis == 0 && (abs(dx) > 14f || abs(dy) > 14f)) {
                                axis = if (abs(dx) > abs(dy) && prefs.miniSwipeToSkip) 1 else 2
                            }
                            when (axis) {
                                1 -> {
                                    change.consume()
                                    offsetX = dx
                                }
                                2 -> if (dy > 0f) {
                                    change.consume()
                                    offsetY = dy
                                }
                            }
                        },
                        onDragEnd = {
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            val flingY = tracker.calculateVelocity().y
                            gestureAnimation = scope.launch {
                                val settle = spring<Float>(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                                when {
                                    axis == 1 && dx > skipThreshold -> {
                                        haptics.tick()
                                        animate(initialValue = offsetX, targetValue = width * 0.42f, animationSpec = tween(120)) { value, _ -> offsetX = value }
                                        currentOnPrevious()
                                        offsetX = -width * 0.36f
                                        animate(initialValue = offsetX, targetValue = 0f, animationSpec = settle) { value, _ -> offsetX = value }
                                    }
                                    axis == 1 && dx < -skipThreshold -> {
                                        haptics.tick()
                                        animate(initialValue = offsetX, targetValue = -width * 0.42f, animationSpec = tween(120)) { value, _ -> offsetX = value }
                                        currentOnNext()
                                        offsetX = width * 0.36f
                                        animate(initialValue = offsetX, targetValue = 0f, animationSpec = settle) { value, _ -> offsetX = value }
                                    }
                                    axis == 2 && (dy > dismissThreshold || (dy > 12.dp.toPx() && flingY > flingThreshold)) -> {
                                        animate(initialValue = offsetY, targetValue = height, animationSpec = tween(190)) { value, _ -> offsetY = value }
                                        currentOnDismiss()
                                    }
                                    else -> {
                                        launch { animate(initialValue = offsetX, targetValue = 0f, animationSpec = settle) { value, _ -> offsetX = value } }
                                        animate(initialValue = offsetY, targetValue = 0f, animationSpec = settle) { value, _ -> offsetY = value }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            gestureAnimation = scope.launch {
                                val settle = spring<Float>(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                                launch { animate(initialValue = offsetX, targetValue = 0f, animationSpec = settle) { value, _ -> offsetX = value } }
                                animate(initialValue = offsetY, targetValue = 0f, animationSpec = settle) { value, _ -> offsetY = value }
                            }
                        },
                    )
                },
        shape = shape,
        color = palette.container,
        contentColor = palette.content,
    ) {
        Box(Modifier.then(if (palette.brush != null) Modifier.background(palette.brush) else Modifier)) {
            when (style) {
                MiniPlayerStyle.FLOATING ->
                    FloatingBody(
                        beatmap, title, artist, isPlaying, progress, progressMode, palette, artShape, spin.value, canSpin,
                        prefs.miniShowControls, prefs.miniShowArtist, prefs.iconStyle, performanceMode,
                        onPlayPause, onPrevious, onNext,
                    )

                MiniPlayerStyle.DOCKED ->
                    DockedBody(
                        beatmap, title, artist, isPlaying, progress, progressMode, palette, artShape, spin.value, canSpin,
                        prefs.miniShowControls, prefs.miniShowArtist, prefs.iconStyle, performanceMode,
                        onPlayPause, onPrevious, onNext,
                    )

                MiniPlayerStyle.CARD ->
                    CardBody(
                        beatmap, title, artist, isPlaying, progress, progressMode, palette, artShape, spin.value, canSpin,
                        prefs.miniShowControls, prefs.miniShowArtist, prefs.iconStyle, performanceMode,
                        onPlayPause, onPrevious, onNext,
                    )

                MiniPlayerStyle.MINIMAL ->
                    MinimalBody(
                        beatmap, title, artist, isPlaying, progress, progressMode, palette, artShape, spin.value, canSpin,
                        prefs.miniShowArtist, performanceMode, onPlayPause,
                    )
            }
        }
    }
}


@Composable
private fun FloatingBody(
    beatmap: Beatmap,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    mode: MiniPlayerProgress,
    palette: MiniPalette,
    artShape: Shape,
    spinDegrees: Float,
    spinning: Boolean,
    showControls: Boolean,
    showArtist: Boolean,
    iconStyle: IconStyle,
    performanceMode: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Box {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = if (mode == MiniPlayerProgress.LINE || mode == MiniPlayerProgress.WAVY) 14.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniArtwork(beatmap, 50.dp, artShape, progress, mode == MiniPlayerProgress.RING, palette, spinDegrees, spinning)
            Spacer(Modifier.width(10.dp))
            MiniTexts(title, artist, showArtist, palette, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            MiniControls(isPlaying, showControls, palette, iconStyle, 36.dp, 44.dp, onPlayPause, onPrevious, onNext)
        }
        if (mode == MiniPlayerProgress.LINE || mode == MiniPlayerProgress.WAVY) {
            MiniProgressLine(
                progress = progress,
                wavy = mode == MiniPlayerProgress.WAVY,
                palette = palette,
                animated = !performanceMode,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DockedBody(
    beatmap: Beatmap,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    mode: MiniPlayerProgress,
    palette: MiniPalette,
    artShape: Shape,
    spinDegrees: Float,
    spinning: Boolean,
    showControls: Boolean,
    showArtist: Boolean,
    iconStyle: IconStyle,
    performanceMode: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column {
        if (mode == MiniPlayerProgress.LINE || mode == MiniPlayerProgress.WAVY) {
            MiniProgressLine(
                progress = progress,
                wavy = mode == MiniPlayerProgress.WAVY,
                palette = palette,
                animated = !performanceMode,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp),
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniArtwork(beatmap, 46.dp, artShape, progress, mode == MiniPlayerProgress.RING, palette, spinDegrees, spinning)
            Spacer(Modifier.width(12.dp))
            MiniTexts(title, artist, showArtist, palette, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            MiniControls(isPlaying, showControls, palette, iconStyle, 38.dp, 46.dp, onPlayPause, onPrevious, onNext)
        }
    }
}

@Composable
private fun CardBody(
    beatmap: Beatmap,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    mode: MiniPlayerProgress,
    palette: MiniPalette,
    artShape: Shape,
    spinDegrees: Float,
    spinning: Boolean,
    showControls: Boolean,
    showArtist: Boolean,
    iconStyle: IconStyle,
    performanceMode: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniArtwork(beatmap, 64.dp, artShape, progress, mode == MiniPlayerProgress.RING, palette, spinDegrees, spinning)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.content,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.loopingMarquee(),
                )
                if (showArtist) {
                    Text(text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.loopingMarquee(),
                    )
                }
            }
        }
        if (mode != MiniPlayerProgress.NONE && mode != MiniPlayerProgress.RING) {
            Spacer(Modifier.height(10.dp))
            MiniProgressLine(
                progress = progress,
                wavy = mode == MiniPlayerProgress.WAVY,
                palette = palette,
                animated = !performanceMode,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniControls(isPlaying, showControls, palette, iconStyle, 44.dp, 56.dp, onPlayPause, onPrevious, onNext)
        }
    }
}

@Composable
private fun MinimalBody(
    beatmap: Beatmap,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    mode: MiniPlayerProgress,
    palette: MiniPalette,
    artShape: Shape,
    spinDegrees: Float,
    spinning: Boolean,
    showArtist: Boolean,
    performanceMode: Boolean,
    onPlayPause: () -> Unit,
) {
    Box {
        Row(
            modifier =
                Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = if (mode == MiniPlayerProgress.LINE || mode == MiniPlayerProgress.WAVY) 12.dp else 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniArtwork(beatmap, 42.dp, artShape, progress, mode == MiniPlayerProgress.RING, palette, spinDegrees, spinning)
            Spacer(Modifier.width(10.dp))
            MiniTexts(title, artist, showArtist, palette, Modifier.weight(1f), compact = true)
            Spacer(Modifier.width(6.dp))
            MiniControls(isPlaying, false, palette, IconStyle.ROUNDED, 32.dp, 40.dp, onPlayPause, {}, {})
        }
        if (mode == MiniPlayerProgress.LINE || mode == MiniPlayerProgress.WAVY) {
            MiniProgressLine(
                progress = progress,
                wavy = mode == MiniPlayerProgress.WAVY,
                palette = palette,
                animated = !performanceMode,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 3.dp),
            )
        }
    }
}


@Composable
private fun MiniArtwork(
    beatmap: Beatmap,
    size: Dp,
    shape: Shape,
    progress: Float,
    ring: Boolean,
    palette: MiniPalette,
    spinDegrees: Float,
    spinning: Boolean,
) {
    val inner = if (ring) size - 10.dp else size
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (ring) {
            Canvas(Modifier.size(size)) {
                val strokeWidth = 3.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
                drawArc(
                    color = palette.track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth),
                )
                drawArc(
                    color = palette.progress,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }


        val appearance = LocalAppSettings.current.appearance
        val effectiveShape =
            if (appearance.miniArtShape == ArtworkShape.DEFAULT) appearance.artworkShape else appearance.miniArtShape
        if (effectiveShape == ArtworkShape.SQUARE) {
            FullBeatmapArt(
                beatmap = beatmap,
                modifier = Modifier.size(inner),
                shape = shape,
                followGlobalShape = false,
                wrapToImage = true,
            )
        } else {
            BeatmapArt(
                beatmap = beatmap,
                modifier =
                    Modifier
                        .size(inner)
                        .graphicsLayer { rotationZ = if (spinning) spinDegrees else 0f },
                shape = shape,
                followGlobalShape = false,
            )
        }
    }
}

@Composable
private fun MiniTexts(
    title: String,
    artist: String,
    showArtist: Boolean,
    palette: MiniPalette,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Column(modifier) {
        Text(text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = palette.content,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.loopingMarquee(),
        )
        if (showArtist) {
            Text(text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.loopingMarquee(),
            )
        }
    }
}

@Composable
private fun MiniControls(
    isPlaying: Boolean,
    showSkip: Boolean,
    palette: MiniPalette,
    iconStyle: IconStyle,
    sideSize: Dp,
    playSize: Dp,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val haptics = rememberKozuHaptics()
    val playIcon = playbackIconVector(LocalAppSettings.current.playbackIcon, isPlaying)
    if (showSkip) {
        MiniButton(KozuIcons.skipPrevious(iconStyle), "Previous track", sideSize, palette.sideContainer, palette.sideContent) {
            haptics.tick()
            onPrevious()
        }
        Spacer(Modifier.width(5.dp))
    }
    MiniButton(playIcon, if (isPlaying) "Pause" else "Play", playSize, palette.playContainer, palette.playContent, iconScale = 0.58f) {
        haptics.press()
        onPlayPause()
    }
    if (showSkip) {
        Spacer(Modifier.width(5.dp))
        MiniButton(KozuIcons.skipNext(iconStyle), "Next track", sideSize, palette.sideContainer, palette.sideContent) {
            haptics.tick()
            onNext()
        }
    }
}

@Composable
private fun MiniButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: Dp,
    container: Color,
    content: Color,
    iconScale: Float = 0.55f,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(size).bounceClick(onClick = onClick),
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(size * iconScale))
        }
    }
}


@Composable
private fun MiniProgressLine(
    progress: Float,
    wavy: Boolean,
    palette: MiniPalette,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "miniWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "miniWavePhase",
    )
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val stroke = 3.dp.toPx()
        val centerY = size.height / 2f
        val end = size.width * progress.coerceIn(0f, 1f)
        drawLine(palette.track, Offset(0f, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
        if (end > 0.5f) {
            if (wavy) {
                val amp = 2.2.dp.toPx()
                val waveLength = 18.dp.toPx()
                val shift = if (animated) phase else 0f
                val path = Path()
                var x = 0f
                path.moveTo(0f, centerY + amp * sin(shift))
                while (x < end) {
                    x = (x + 3f).coerceAtMost(end)
                    path.lineTo(x, centerY + amp * sin(x / waveLength * 2f * PI.toFloat() + shift))
                }
                drawPath(path, palette.progress, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawLine(palette.progress, Offset(0f, centerY), Offset(end, centerY), stroke, StrokeCap.Round)
            }
        }
    }
}
