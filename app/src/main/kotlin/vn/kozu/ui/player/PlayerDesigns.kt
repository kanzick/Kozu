package vn.kozu.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.domain.model.SeekBarStyle
import vn.kozu.player.RepeatState
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.FullBeatmapArt
import vn.kozu.ui.components.KozuIcons
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.WavySlider
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.components.toShape
import vn.kozu.ui.i18n.KozuText as Text
import vn.kozu.ui.i18n.kozuUiString
import vn.kozu.util.formatTime

/**
 * Everything a player design needs, so each design is a pure layout and the screen only has to
 * build this once.
 */
class PlayerUi(
    val beatmap: Beatmap,
    val title: String,
    val artist: String,
    val mapper: String,
    val queueLabel: String?,
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val shuffle: Boolean,
    val repeat: RepeatState,
    val position: StateFlow<Long>,
    val duration: StateFlow<Long>,
    val fileInfo: String?,
    val sleepActive: Boolean,
    val animated: Boolean,
    val compact: Boolean,
    val artShape: Shape,
    val useOriginalArtwork: Boolean,
    val coverScale: Float,
    val artworkSwipe: Boolean,
    val showToggleRow: Boolean,
    val showVolume: Boolean,
    val onClose: () -> Unit,
    val onQueue: () -> Unit,
    val onSleep: () -> Unit,
    val onMore: () -> Unit,
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onFavorite: () -> Unit,
    val onArtist: () -> Unit,
    val onMapper: () -> Unit,
)

@Composable
fun PlayerDesignContent(
    design: PlayerLayoutStyle,
    ui: PlayerUi,
) {
    when (design) {
        PlayerLayoutStyle.CLASSIC -> ClassicDesign(ui)
        PlayerLayoutStyle.MODERN -> ModernDesign(ui)
        PlayerLayoutStyle.MINIMAL -> MinimalDesign(ui)
        PlayerLayoutStyle.CINEMATIC -> CinematicDesign(ui)
        PlayerLayoutStyle.LITTLE -> LittleDesign(ui)
        PlayerLayoutStyle.EXPRESSIVE -> ExpressiveDesign(ui)
        PlayerLayoutStyle.IMMERSIVE -> ImmersiveDesign(ui, extended = false)
        PlayerLayoutStyle.IMMERSIVE_EXTENDED -> ImmersiveDesign(ui, extended = true)
        PlayerLayoutStyle.MATERIAL_EXTENDED -> MaterialExtendedDesign(ui)
        PlayerLayoutStyle.EDITORIAL -> EditorialDesign(ui)
    }
}

/** Designs that paint their own full-bleed artwork hide the shared blurred backdrop. */
fun PlayerLayoutStyle.drawsOwnBackground(): Boolean =
    this == PlayerLayoutStyle.IMMERSIVE ||
        this == PlayerLayoutStyle.IMMERSIVE_EXTENDED ||
        this == PlayerLayoutStyle.EXPRESSIVE ||
        this == PlayerLayoutStyle.EDITORIAL

// ───────────────────────────── shared building blocks ─────────────────────────────

@Composable
private fun DesignColumn(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    horizontal: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontal, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (ui.compact) 6.dp else 12.dp),
        content = content,
    )
}

/** Swipe down closes, swipe sideways skips, like the original artwork gesture. */
@Composable
private fun Modifier.playerSwipe(ui: PlayerUi): Modifier {
    if (!ui.artworkSwipe) return this
    val haptics = rememberKozuHaptics()
    val latest by rememberUpdatedState(ui)
    return this.pointerInput(Unit) {
        var dx = 0f
        var dy = 0f
        detectDragGestures(
            onDragStart = {
                dx = 0f
                dy = 0f
            },
            onDragEnd = {
                when {
                    dy > 160f && abs(dy) > abs(dx) -> latest.onClose()
                    dx > 140f && abs(dx) > abs(dy) -> {
                        haptics.tick()
                        latest.onPrevious()
                    }
                    dx < -140f && abs(dx) > abs(dy) -> {
                        haptics.tick()
                        latest.onNext()
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
}

@Composable
private fun PlayerArt(
    ui: PlayerUi,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = ui.artShape,
    original: Boolean = ui.useOriginalArtwork,
) {
    val artModifier =
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = ui.coverScale
                scaleY = ui.coverScale
            }
    if (original) {
        FullBeatmapArt(
            beatmap = ui.beatmap,
            shape = shape,
            followGlobalShape = false,
            wrapToImage = true,
            modifier = artModifier,
        )
    } else {
        BeatmapArt(
            beatmap = ui.beatmap,
            shape = shape,
            large = false,
            followGlobalShape = false,
            modifier = artModifier,
        )
    }
}

/** Artwork cropped to fill its box edge to edge (no shape), for the immersive designs. */
@Composable
private fun FullBleedArt(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request =
        remember(ui.beatmap.id) {
            ImageRequest
                .Builder(context)
                .data(ui.beatmap.artUrl.ifBlank { ui.beatmap.thumbUrl })
                .crossfade(300)
                .build()
        }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer {
            scaleX = ui.coverScale
            scaleY = ui.coverScale
        },
    )
}

@Composable
private fun ArtworkArea(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    fraction: Float = 0.96f,
    shape: Shape = ui.artShape,
    original: Boolean = ui.useOriginalArtwork,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().playerSwipe(ui),
        contentAlignment = Alignment.Center,
    ) {
        PlayerArt(ui, minOf(maxWidth * fraction, maxHeight * 0.985f), shape = shape, original = original)
    }
}

@Composable
private fun TrackText(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    artistStyle: TextStyle = MaterialTheme.typography.titleMedium,
    titleLines: Int = 1,
    showMapper: Boolean = !ui.compact,
    titleColor: Color = Color.Unspecified,
    subColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = ui.title,
            style = titleStyle,
            color = titleColor,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
        )
        // The artist and the mapper each open a page of their own.
        Text(
            text = ui.artist,
            style = artistStyle,
            color = subColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
        )
        if (showMapper && ui.mapper.isNotBlank()) {
            Text(
                text = "Mapped by ${ui.mapper}",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = align,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onMapper),
            )
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
) {
    Surface(
        modifier = modifier.size(size).bounceClick(onClick = onClick),
        shape = shape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(iconSize))
        }
    }
}

/** An icon with no container, for the quieter designs. */
@Composable
private fun PlainIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun PillAction(
    icon: ImageVector,
    label: String?,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(48.dp).bounceClick(pressedScale = 0.96f, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (active) colors.primaryContainer else container,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
            if (label != null) {
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QueueSleepPills(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    withLabels: Boolean = true,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillAction(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = if (withLabels) kozuUiString("Queue") else null,
            description = kozuUiString("Queue"),
            onClick = ui.onQueue,
            modifier = Modifier.weight(1f),
        )
        PillAction(
            icon = Icons.Rounded.Bedtime,
            label = if (withLabels) kozuUiString("Sleep timer") else null,
            description = kozuUiString("Sleep timer"),
            onClick = ui.onSleep,
            modifier = Modifier.weight(1f),
            active = ui.sleepActive,
        )
    }
}

@Composable
private fun NowPlayingHeader(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    onSurfaceVariant: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            description = kozuUiString("Close player"),
            onClick = ui.onClose,
            size = if (ui.compact) 44.dp else 48.dp,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (ui.queueLabel != null) {
                Text(
                    text = ui.queueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(if (ui.compact) 44.dp else 48.dp))
    }
}

/** Seek bar with elapsed / total (or remaining) time. */
@Composable
private fun PlayerSeek(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    style: SeekBarStyle = LocalAppSettings.current.appearance.seekStyle,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showRemaining: Boolean = false,
    showFileInfo: Boolean = true,
) {
    val pos by ui.position.collectAsStateWithLifecycle()
    val dur by ui.duration.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<Float?>(null) }
    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val smoothed by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "designProgress")
    val shown = scrub ?: smoothed
    val shownMs = scrub?.let { (it * dur).toLong() } ?: pos

    Column(modifier = modifier.fillMaxWidth()) {
        WavySlider(
            progress = shown,
            isPlaying = ui.isPlaying,
            onScrub = { scrub = it },
            onScrubFinished = { fraction ->
                if (dur > 0L) ui.onSeek((fraction * dur).toLong())
                scrub = null
            },
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            animated = ui.animated,
            style = style,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatTime(shownMs), style = MaterialTheme.typography.labelLarge, color = textColor)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (showRemaining) "-" + formatTime((dur - shownMs).coerceAtLeast(0L)) else formatTime(dur),
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
        }
        if (showFileInfo && !ui.fileInfo.isNullOrBlank()) {
            Text(
                text = ui.fileInfo,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun playIcon(ui: PlayerUi): ImageVector =
    playbackIconVector(LocalAppSettings.current.playbackIcon, ui.isPlaying)

@Composable
private fun iconStyle() = LocalAppSettings.current.appearance.iconStyle

/** A big filled play/pause button. */
@Composable
private fun PlayButton(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
    height: Dp = 76.dp,
    shape: Shape = CircleShape,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    iconSize: Dp = 36.dp,
) {
    val haptics = rememberKozuHaptics()
    Surface(
        modifier =
            modifier
                .size(width, height)
                .bounceClick(pressedScale = 0.95f) {
                    haptics.tick()
                    ui.onPlayPause()
                },
        shape = shape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                playIcon(ui),
                contentDescription = if (ui.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun shuffleIcon(): ImageVector = KozuIcons.shuffle(iconStyle())

@Composable
private fun repeatIcon(ui: PlayerUi): ImageVector = KozuIcons.repeat(iconStyle(), ui.repeat == RepeatState.ONE)

@Composable
private fun favoriteIcon(ui: PlayerUi): ImageVector = KozuIcons.favorite(iconStyle(), ui.isFavorite)

@Composable
private fun ShuffleButton(ui: PlayerUi, modifier: Modifier = Modifier, plain: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tint = if (ui.shuffle) colors.primary else colors.onSurfaceVariant
    if (plain) {
        PlainIcon(shuffleIcon(), "Shuffle", ui.onShuffle, modifier, tint = tint)
    } else {
        RoundButton(
            shuffleIcon(), "Shuffle", ui.onShuffle, modifier,
            container = if (ui.shuffle) colors.primaryContainer else colors.surfaceContainerHigh,
            content = if (ui.shuffle) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun RepeatButton(ui: PlayerUi, modifier: Modifier = Modifier, plain: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val active = ui.repeat != RepeatState.OFF
    val tint = if (active) colors.primary else colors.onSurfaceVariant
    if (plain) {
        PlainIcon(repeatIcon(ui), "Repeat", ui.onRepeat, modifier, tint = tint)
    } else {
        RoundButton(
            repeatIcon(ui), "Repeat", ui.onRepeat, modifier,
            container = if (active) colors.primaryContainer else colors.surfaceContainerHigh,
            content = if (active) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun FavoriteButton(ui: PlayerUi, modifier: Modifier = Modifier, plain: Boolean = false, size: Dp = 48.dp) {
    val colors = MaterialTheme.colorScheme
    if (plain) {
        PlainIcon(
            favoriteIcon(ui), "Favorite", ui.onFavorite, modifier, size = size,
            tint = if (ui.isFavorite) colors.primary else colors.onSurfaceVariant,
        )
    } else {
        RoundButton(
            favoriteIcon(ui), "Favorite", ui.onFavorite, modifier, size = size,
            container = if (ui.isFavorite) colors.primaryContainer else colors.surfaceContainerHighest,
            content = if (ui.isFavorite) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun SkipButton(ui: PlayerUi, next: Boolean, modifier: Modifier = Modifier, plain: Boolean = true, size: Dp = 56.dp) {
    val haptics = rememberKozuHaptics()
    val icon = if (next) KozuIcons.skipNext(iconStyle()) else KozuIcons.skipPrevious(iconStyle())
    val action = {
        haptics.tick()
        if (next) ui.onNext() else ui.onPrevious()
    }
    if (plain) {
        PlainIcon(icon, if (next) "Next" else "Previous", action, modifier, size = size, iconSize = 30.dp)
    } else {
        RoundButton(
            icon, if (next) "Next" else "Previous", action, modifier, size = size, iconSize = 28.dp,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ───────────────────────────── volume ─────────────────────────────

/** Media volume slider that stays in step with the hardware buttons. */
@Composable
fun PlayerVolumeBar(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val sliderState = rememberSliderState(
        value = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat(),
        trackRange = 0f..max.toFloat(),
    )

    DisposableEffect(context, sliderState) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    sliderState.value = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Slider(
            state = sliderState,
            onValueChange = {
                sliderState.value = it
                runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            colors =
                SliderDefaults.colors(
                    thumbColor = tint,
                    activeTrackColor = tint,
                    inactiveTrackColor = track,
                ),
        )
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
    }
}

// ───────────────────────────── 1. Classic ─────────────────────────────

@Composable
private fun ClassicDesign(ui: PlayerUi) {
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            RoundButton(
                Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        PlayerSeek(ui)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RepeatButton(ui, plain = true)
            SkipButton(ui, next = false)
            PlayButton(ui)
            SkipButton(ui, next = true)
            FavoriteButton(ui, plain = true)
        }
        QueueSleepPills(ui)
    }
}

// ───────────────────────────── 2. Modern ─────────────────────────────

@Composable
private fun ModernDesign(ui: PlayerUi) {
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.94f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FavoriteButton(ui, size = 44.dp)
            Spacer(Modifier.width(8.dp))
            RoundButton(Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore, size = 44.dp)
        }
        PlayerSeek(ui)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShuffleButton(ui, Modifier.size(46.dp))
            SkipButton(ui, next = false, plain = false, size = 64.dp, modifier = Modifier.weight(1f).height(64.dp))
            PlayButton(ui, width = 96.dp, height = 72.dp, shape = RoundedCornerShape(36.dp))
            SkipButton(ui, next = true, plain = false, size = 64.dp, modifier = Modifier.weight(1f).height(64.dp))
            RepeatButton(ui, Modifier.size(46.dp))
        }
        QueueSleepPills(ui, withLabels = false)
    }
}

// ───────────────────────────── 3. Minimal ─────────────────────────────

@Composable
private fun MinimalDesign(ui: PlayerUi) {
    DesignColumn(ui, horizontal = 28.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PlainIcon(Icons.Rounded.KeyboardArrowDown, kozuUiString("Close player"), ui.onClose)
            Spacer(Modifier.weight(1f))
            PlainIcon(Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore)
        }
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.9f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(
                ui,
                Modifier.weight(1f),
                titleStyle = MaterialTheme.typography.titleLarge,
                artistStyle = MaterialTheme.typography.bodyLarge,
                showMapper = false,
            )
            FavoriteButton(ui, plain = true)
        }
        PlayerSeek(
            ui,
            style = SeekBarStyle.THIN,
            inactiveColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ShuffleButton(ui, plain = true)
            SkipButton(ui, next = false)
            PlayButton(
                ui, width = 64.dp, height = 64.dp,
                container = MaterialTheme.colorScheme.onSurface,
                content = MaterialTheme.colorScheme.surface,
                iconSize = 30.dp,
            )
            SkipButton(ui, next = true)
            RepeatButton(ui, plain = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction(Icons.AutoMirrored.Rounded.QueueMusic, kozuUiString("Queue"), ui.onQueue, false)
            TextAction(Icons.Rounded.Bedtime, kozuUiString("Sleep timer"), ui.onSleep, ui.sleepActive)
        }
    }
}

@Composable
private fun TextAction(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).bounceClick(pressedScale = 0.96f, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

// ───────────────────────────── 4. Cinematic ─────────────────────────────

@Composable
private fun CinematicDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().playerSwipe(ui),
            contentAlignment = Alignment.Center,
        ) {
            val size = minOf(maxWidth * 0.9f, maxHeight * 0.96f)
            // Soft glow behind the artwork, tinted by the theme (which follows the artwork colours).
            Box(
                Modifier
                    .size(size * 1.12f)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.38f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
            PlayerArt(ui, size)
        }
        TrackText(
            ui,
            Modifier.fillMaxWidth(),
            centered = true,
            titleStyle = MaterialTheme.typography.headlineMedium,
        )
        PlayerSeek(ui)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ShuffleButton(ui, plain = true)
            SkipButton(ui, next = false, plain = false, size = 60.dp)
            PlayButton(ui, width = 80.dp, height = 80.dp)
            SkipButton(ui, next = true, plain = false, size = 60.dp)
            RepeatButton(ui, plain = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueSleepPills(ui, Modifier.weight(1f))
            FavoriteButton(ui)
            RoundButton(Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore)
        }
    }
}

// ───────────────────────────── 5. Little ─────────────────────────────

@Composable
private fun LittleDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.86f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(
                ui,
                Modifier.weight(1f),
                titleStyle = MaterialTheme.typography.titleLarge,
                artistStyle = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.width(8.dp))
            FavoriteButton(ui, size = 42.dp)
            Spacer(Modifier.width(6.dp))
            RoundButton(Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore, size = 42.dp)
        }
        PlayerSeek(ui)
        // The signature Kozu transport: three chunky tonal buttons in one rounded tray.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = colors.surfaceContainer,
        ) {
            Box(Modifier.padding(8.dp)) {
                TransportControls(
                    modifier = Modifier.fillMaxWidth(),
                    isPlaying = ui.isPlaying,
                    onPrevious = ui.onPrevious,
                    onPlayPause = ui.onPlayPause,
                    onNext = ui.onNext,
                    compact = ui.compact,
                )
            }
        }
        if (ui.showToggleRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShuffleButton(ui, Modifier.size(44.dp))
                Spacer(Modifier.width(14.dp))
                RepeatButton(ui, Modifier.size(44.dp))
            }
        }
        QueueSleepPills(ui)
    }
}

// ───────────────────────────── 6. Expressive ─────────────────────────────

/** Rotates content a quarter turn anticlockwise and swaps its layout box to match. */
private fun Modifier.verticalText(): Modifier =
    this
        .layout { measurable, constraints ->
            val placeable =
                measurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxHeight.takeIf { it != Constraints.Infinity } ?: 4000,
                        minHeight = 0,
                        maxHeight = constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: 4000,
                    ),
                )
            layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -(placeable.width / 2 - placeable.height / 2),
                    y = -(placeable.height / 2 - placeable.width / 2),
                )
            }
        }.rotate(-90f)

@Composable
private fun ExpressiveDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        // Two-tone field: the upper block carries the track, the lower block the controls.
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(0.58f).fillMaxWidth().background(colors.primaryContainer))
            Box(Modifier.weight(0.42f).fillMaxWidth().background(colors.tertiaryContainer))
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    Icons.Rounded.KeyboardArrowDown, kozuUiString("Close player"), ui.onClose,
                    container = colors.surface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.weight(1f))
                RoundButton(
                    Icons.Rounded.Bedtime, kozuUiString("Sleep timer"), ui.onSleep,
                    container = if (ui.sleepActive) colors.primary else colors.surface.copy(alpha = 0.55f),
                    content = if (ui.sleepActive) colors.onPrimary else colors.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    Icons.AutoMirrored.Rounded.QueueMusic, kozuUiString("Queue"), ui.onQueue,
                    container = colors.surface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore,
                    container = colors.surface.copy(alpha = 0.55f),
                )
            }
            Row(Modifier.weight(1f).fillMaxWidth().playerSwipe(ui)) {
                // Vertical headline running up the left edge.
                Box(Modifier.width(112.dp).fillMaxHeight(), contentAlignment = Alignment.BottomStart) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = ui.title,
                            modifier = Modifier.verticalText(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ui.artist,
                            modifier =
                                Modifier
                                    .verticalText()
                                    .clip(RoundedCornerShape(8.dp))
                                    .bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onPrimaryContainer.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    PlayerArt(
                        ui,
                        minOf(maxWidth, maxHeight * 0.98f),
                        shape = if (ui.useOriginalArtwork) ui.artShape else RoundedCornerShape(36.dp),
                    )
                }
            }
            PlayerSeek(
                ui,
                activeColor = colors.onTertiaryContainer,
                inactiveColor = colors.onTertiaryContainer.copy(alpha = 0.22f),
                textColor = colors.onTertiaryContainer.copy(alpha = 0.8f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FavoriteButton(ui, Modifier.size(52.dp), size = 52.dp)
                SkipButton(ui, next = false, plain = false, size = 64.dp, modifier = Modifier.weight(1f).height(64.dp))
                PlayButton(ui, width = 104.dp, height = 72.dp, shape = RoundedCornerShape(26.dp))
                SkipButton(ui, next = true, plain = false, size = 64.dp, modifier = Modifier.weight(1f).height(64.dp))
                ShuffleButton(ui, Modifier.size(52.dp))
            }
        }
    }
}

// ───────────────────────────── 7 & 8. Immersive / Immersive Extended ─────────────────────────────

@Composable
private fun ImmersiveDesign(ui: PlayerUi, extended: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        // Artwork bleeds to the screen edges: the top ~55% (or the whole screen when extended).
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (extended) 1f else 0.64f)
                .playerSwipe(ui),
        ) {
            FullBleedArt(ui, Modifier.fillMaxSize())
        }
        // Scrim: fades the artwork into the controls.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (extended) {
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.40f),
                            0.22f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.70f to colors.surface.copy(alpha = 0.72f),
                            1f to colors.surface.copy(alpha = 0.96f),
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.34f),
                            0.20f to Color.Transparent,
                            0.40f to Color.Transparent,
                            0.64f to colors.surface,
                            1f to colors.surface,
                        )
                    },
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (ui.compact) 6.dp else 10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    Icons.Rounded.KeyboardArrowDown, kozuUiString("Close player"), ui.onClose,
                    container = Color.Black.copy(alpha = 0.35f), content = Color.White,
                )
                Text(
                    text = ui.queueLabel?.let { "Playing · $it" } ?: "Now Playing",
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RoundButton(
                    Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore,
                    container = Color.Black.copy(alpha = 0.35f), content = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                TrackText(
                    ui,
                    Modifier.weight(1f),
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    titleLines = 2,
                    showMapper = true,
                )
                Spacer(Modifier.width(8.dp))
                FavoriteButton(ui)
            }
            PlayerSeek(ui, showRemaining = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShuffleButton(ui, plain = true)
                SkipButton(ui, next = false, size = 64.dp)
                PlayButton(ui, width = 84.dp, height = 84.dp, iconSize = 40.dp)
                SkipButton(ui, next = true, size = 64.dp)
                RepeatButton(ui, plain = true)
            }
            if (ui.showVolume) PlayerVolumeBar()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextAction(Icons.AutoMirrored.Rounded.QueueMusic, kozuUiString("Queue"), ui.onQueue, false)
                TextAction(Icons.Rounded.Bedtime, kozuUiString("Sleep timer"), ui.onSleep, ui.sleepActive)
            }
        }
    }
}

// ───────────────────────────── 9. Material Extended ─────────────────────────────

@Composable
private fun MaterialExtendedDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().playerSwipe(ui), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth, maxHeight * 0.985f)
            Box(Modifier.size(size)) {
                PlayerArt(ui, size)
                // Favourite and more sit on the artwork's lower edge as tonal bubbles.
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoundButton(
                        favoriteIcon(ui), "Favorite", ui.onFavorite, size = 44.dp,
                        container = colors.surface.copy(alpha = 0.78f),
                        content = if (ui.isFavorite) colors.primary else colors.onSurface,
                    )
                    RoundButton(
                        Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore, size = 44.dp,
                        container = colors.surface.copy(alpha = 0.78f),
                    )
                }
            }
        }
        TrackText(ui, Modifier.fillMaxWidth(), titleStyle = MaterialTheme.typography.headlineSmall)
        PlayerSeek(ui, showFileInfo = false)
        if (!ui.fileInfo.isNullOrBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceContainerHigh,
            ) {
                Text(
                    text = ui.fileInfo,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ShuffleButton(ui, plain = true)
            SkipButton(ui, next = false, plain = false, size = 60.dp)
            PlayButton(ui, width = 92.dp, height = 68.dp, shape = RoundedCornerShape(34.dp))
            SkipButton(ui, next = true, plain = false, size = 60.dp)
            RepeatButton(ui, plain = true)
        }
        if (ui.showVolume) PlayerVolumeBar()
        QueueSleepPills(ui)
    }
}

// ───────────────────────────── 10. Editorial ─────────────────────────────

@Composable
private fun EditorialDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(colors.primaryContainer)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (ui.compact) 6.dp else 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    Icons.Rounded.KeyboardArrowDown, kozuUiString("Close player"), ui.onClose,
                    container = colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = colors.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
                RoundButton(
                    Icons.Rounded.Bedtime, kozuUiString("Sleep timer"), ui.onSleep,
                    container = if (ui.sleepActive) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = if (ui.sleepActive) colors.onPrimary else colors.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    Icons.AutoMirrored.Rounded.QueueMusic, kozuUiString("Queue"), ui.onQueue,
                    container = colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = colors.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    Icons.Rounded.MoreVert, kozuUiString("More options"), ui.onMore,
                    container = colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = colors.onPrimaryContainer,
                )
            }
            // Artwork in a scalloped cookie, unless the user picked a specific shape.
            ArtworkArea(
                ui,
                Modifier.weight(1f),
                fraction = 0.92f,
                shape = if (ui.useOriginalArtwork) ui.artShape else ArtworkShape.COOKIE.toShape(ui.artShape),
                original = false,
            )
            TrackText(
                ui,
                Modifier.fillMaxWidth(),
                titleStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                artistStyle = MaterialTheme.typography.titleLarge,
                titleLines = if (ui.compact) 1 else 3,
                titleColor = colors.onPrimaryContainer,
                subColor = colors.onPrimaryContainer.copy(alpha = 0.78f),
                accent = colors.onPrimaryContainer,
            )
            PlayerSeek(
                ui,
                activeColor = colors.onPrimaryContainer,
                inactiveColor = colors.onPrimaryContainer.copy(alpha = 0.22f),
                textColor = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShuffleButton(ui, plain = true)
                SkipButton(ui, next = false, plain = false, size = 60.dp)
                PlayButton(
                    ui, width = 84.dp, height = 84.dp,
                    container = colors.onPrimaryContainer, content = colors.primaryContainer,
                )
                SkipButton(ui, next = true, plain = false, size = 60.dp)
                FavoriteButton(ui, plain = true)
            }
        }
    }
}
