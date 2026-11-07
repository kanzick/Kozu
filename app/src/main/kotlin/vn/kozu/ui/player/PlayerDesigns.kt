package vn.kozu.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.vectorResource
import vn.kozu.R
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
    val onShare: () -> Unit,
    val onArtist: () -> Unit,
    val onMapper: () -> Unit,
    val previewMode: Boolean = false,
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
        this == PlayerLayoutStyle.LITTLE ||
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
                .then(if (ui.previewMode) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())
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
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)),
            ),
        )
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.Center).size(64.dp),
        )
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = ui.coverScale
                scaleY = ui.coverScale
            },
        )
    }
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
            icon = at(R.drawable.at_queue_music),
            label = if (withLabels) kozuUiString("Queue") else null,
            description = kozuUiString("Queue"),
            onClick = ui.onQueue,
            modifier = Modifier.weight(1f),
        )
        PillAction(
            icon = at(R.drawable.at_bedtime),
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
            icon = at(R.drawable.at_expand_more),
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

/** ArchiveTune's own vector icons, so the designs carry the same glyphs. */
@Composable
private fun at(@DrawableRes id: Int): ImageVector = ImageVector.vectorResource(id)

@Composable
private fun playIcon(ui: PlayerUi): ImageVector = at(if (ui.isPlaying) R.drawable.at_pause else R.drawable.at_play)

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
private fun shuffleIcon(): ImageVector = at(R.drawable.at_shuffle)

@Composable
private fun repeatIcon(ui: PlayerUi): ImageVector =
    at(if (ui.repeat == RepeatState.ONE) R.drawable.at_repeat_one else R.drawable.at_repeat)

@Composable
private fun favoriteIcon(ui: PlayerUi): ImageVector =
    at(if (ui.isFavorite) R.drawable.at_favorite else R.drawable.at_favorite_border)

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
            tint = if (ui.isFavorite) colors.error else colors.onSurface,
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
    val icon = at(if (next) R.drawable.at_skip_next else R.drawable.at_skip_previous)
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
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
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
        Icon(at(R.drawable.at_volume_off), contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
            },
            valueRange = 0f..max.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            colors =
                SliderDefaults.colors(
                    thumbColor = tint,
                    activeTrackColor = tint,
                    inactiveTrackColor = track,
                ),
        )
        Icon(at(R.drawable.at_volume_up), contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
    }
}

// ───────────────────────────── 1. Classic ─────────────────────────────

@Composable
private fun ClassicDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            RoundButton(
                at(R.drawable.at_share), kozuUiString("Share"), ui.onShare, size = 40.dp,
                container = colors.primary, content = colors.onPrimary, shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            RoundButton(
                at(R.drawable.at_more_horiz), kozuUiString("More options"), ui.onMore, size = 40.dp,
                container = colors.primary, content = colors.onPrimary, shape = RoundedCornerShape(24.dp),
            )
        }
        PlayerSeek(ui)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { RepeatButton(ui, plain = true) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SkipButton(ui, next = false, size = 48.dp) }
            Spacer(Modifier.width(8.dp))
            PlayButton(ui, width = 72.dp, height = 72.dp, shape = RoundedCornerShape(36.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SkipButton(ui, next = true, size = 48.dp) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { FavoriteButton(ui, plain = true) }
        }
        QueueSleepPills(ui)
    }
}

// ───────────────────────────── 2. Modern ─────────────────────────────

@Composable
private fun ModernDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.94f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            // Share and favourite are one joined capsule.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    at(R.drawable.at_share), kozuUiString("Share"), ui.onShare, size = 42.dp,
                    container = colors.primary, content = colors.onPrimary,
                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                )
                RoundButton(
                    favoriteIcon(ui), kozuUiString("Favorite"), ui.onFavorite, size = 42.dp,
                    container = colors.primary, content = colors.onPrimary,
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 50.dp, bottomEnd = 50.dp),
                )
            }
        }
        PlayerSeek(ui)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = colors.surfaceContainerHigh,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SkipButton(ui, next = false, plain = false, size = 56.dp)
                }
                PlayButton(
                    ui,
                    width = 104.dp,
                    height = 72.dp,
                    shape = RoundedCornerShape(28.dp),
                    container = colors.surface,
                    content = colors.onSurface,
                    iconSize = 40.dp,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SkipButton(ui, next = true, plain = false, size = 56.dp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            PlainIcon(at(R.drawable.at_queue_music), kozuUiString("Queue"), ui.onQueue)
            PlainIcon(
                at(R.drawable.at_bedtime), kozuUiString("Sleep timer"), ui.onSleep,
                tint = if (ui.sleepActive) colors.primary else colors.onSurface,
            )
            ShuffleButton(ui, plain = true)
            RepeatButton(ui, plain = true)
            PlainIcon(at(R.drawable.at_more_vert), kozuUiString("More options"), ui.onMore)
        }
    }
}

// ───────────────────────────── 3. Minimal ─────────────────────────────

@Composable
private fun MinimalDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui, horizontal = 28.dp) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.9f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(
                ui,
                Modifier.weight(1f),
                titleStyle = MaterialTheme.typography.titleLarge,
                artistStyle = MaterialTheme.typography.bodyLarge,
                showMapper = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PlainIcon(at(R.drawable.at_share), kozuUiString("Share"), ui.onShare, size = 36.dp, iconSize = 20.dp, tint = colors.onSurface.copy(alpha = 0.7f))
                PlainIcon(
                    favoriteIcon(ui), kozuUiString("Favorite"), ui.onFavorite, size = 36.dp, iconSize = 20.dp,
                    tint = if (ui.isFavorite) colors.error.copy(alpha = 0.9f) else colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        PlayerSeek(ui, style = SeekBarStyle.THIN, inactiveColor = colors.outlineVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ShuffleButton(ui, plain = true, modifier = Modifier.size(40.dp))
            RoundButton(
                at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, size = 52.dp, iconSize = 26.dp,
                container = colors.onSurface.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp),
            )
            PlayButton(ui, width = 70.dp, height = 70.dp, container = colors.onSurface, content = colors.surface, iconSize = 34.dp)
            RoundButton(
                at(R.drawable.at_skip_next), "Next", ui.onNext, size = 52.dp, iconSize = 26.dp,
                container = colors.onSurface.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp),
            )
            RepeatButton(ui, plain = true, modifier = Modifier.size(40.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextAction(at(R.drawable.at_queue_music), kozuUiString("Queue"), ui.onQueue, false)
            TextAction(at(R.drawable.at_bedtime), kozuUiString("Sleep timer"), ui.onSleep, ui.sleepActive)
            TextAction(at(R.drawable.at_more_horiz), kozuUiString("More"), ui.onMore, false)
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
    val glass = colors.onSurface.copy(alpha = 0.12f)
    val corner by animateDpAsState(
        targetValue = if (ui.isPlaying) 28.dp else 44.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cinematicPlayCorner",
    )
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.94f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(at(R.drawable.at_share), kozuUiString("Share"), ui.onShare, size = 44.dp, iconSize = 22.dp, container = glass, shape = RoundedCornerShape(14.dp))
                RoundButton(
                    favoriteIcon(ui), kozuUiString("Favorite"), ui.onFavorite, size = 44.dp, iconSize = 22.dp,
                    container = if (ui.isFavorite) colors.error.copy(alpha = 0.25f) else glass,
                    content = if (ui.isFavorite) colors.error else colors.onSurface,
                    shape = RoundedCornerShape(14.dp),
                )
                RoundButton(at(R.drawable.at_more_horiz), kozuUiString("More options"), ui.onMore, size = 44.dp, iconSize = 22.dp, container = glass, shape = RoundedCornerShape(14.dp))
            }
        }
        PlayerSeek(ui)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                ShuffleButtonSq(ui)
                Spacer(Modifier.width(12.dp))
                RoundButton(
                    at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, size = 56.dp, iconSize = 28.dp,
                    container = colors.onSurface.copy(alpha = 0.15f), shape = RoundedCornerShape(18.dp),
                )
            }
            PlayButton(
                ui, modifier = Modifier.padding(horizontal = 20.dp), width = 88.dp, height = 88.dp,
                shape = RoundedCornerShape(corner), iconSize = 44.dp,
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    at(R.drawable.at_skip_next), "Next", ui.onNext, size = 56.dp, iconSize = 28.dp,
                    container = colors.onSurface.copy(alpha = 0.15f), shape = RoundedCornerShape(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                RepeatButtonSq(ui)
            }
        }
        QueueSleepPills(ui)
    }
}

@Composable
private fun ShuffleButtonSq(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    RoundButton(
        shuffleIcon(), "Shuffle", ui.onShuffle, size = 46.dp, iconSize = 22.dp,
        container = if (ui.shuffle) colors.primary.copy(alpha = 0.25f) else colors.onSurface.copy(alpha = 0.08f),
        content = if (ui.shuffle) colors.primary else colors.onSurface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun RepeatButtonSq(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    val active = ui.repeat != RepeatState.OFF
    RoundButton(
        repeatIcon(ui), "Repeat", ui.onRepeat, size = 46.dp, iconSize = 22.dp,
        container = if (active) colors.primary.copy(alpha = 0.25f) else colors.onSurface.copy(alpha = 0.08f),
        content = if (active) colors.primary else colors.onSurface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
    )
}

// ───────────────────────────── 5. Little ─────────────────────────────

/**
 * ArchiveTune's "Little": a landscape layout turned a quarter turn, so the title runs down the
 * screen. The tinted band is the progress: drag up and down to seek, flick sideways to skip.
 */
@Composable
private fun LittleDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    val pos by ui.position.collectAsStateWithLifecycle()
    val dur by ui.duration.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<Float?>(null) }
    val fraction = scrub ?: if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val latest by rememberUpdatedState(ui)
    val topInset = if (ui.previewMode) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = if (ui.previewMode) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(colors.primaryContainer)) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .align(Alignment.TopStart)
                .background(colors.primary.copy(alpha = 0.28f)),
        )
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(dur) {
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDragStart = {
                            dx = 0f
                            dy = 0f
                        },
                        onDragEnd = {
                            val target = scrub
                            when {
                                target != null && dur > 0L -> latest.onSeek((target * dur).toLong())
                                dx > 140f && abs(dx) > abs(dy) -> latest.onPrevious()
                                dx < -140f && abs(dx) > abs(dy) -> latest.onNext()
                            }
                            scrub = null
                        },
                        onDragCancel = { scrub = null },
                        onDrag = { change, amount ->
                            change.consume()
                            dx += amount.x
                            dy += amount.y
                            if (abs(dy) > abs(dx) && dur > 0L) {
                                scrub = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            }
                        },
                    )
                }.layout { measurable, constraints ->
                    val w = constraints.maxWidth
                    val h = constraints.maxHeight
                    val placeable = measurable.measure(Constraints.fixed(h, w))
                    layout(w, h) {
                        placeable.placeWithLayer((w - placeable.width) / 2, (h - placeable.height) / 2) {
                            rotationZ = 90f
                        }
                    }
                },
        ) {
            LittleContent(ui, shownMs = if (scrub != null) (fraction * dur).toLong() else pos, durationMs = dur, startInset = topInset, endInset = bottomInset)
        }
    }
}

@Composable
private fun LittleContent(
    ui: PlayerUi,
    shownMs: Long,
    durationMs: Long,
    startInset: Dp,
    endInset: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val text = colors.onPrimaryContainer
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / 420.dp, maxHeight / 260.dp).coerceIn(0.90f, 1.20f)
        val timeText = formatTime(shownMs) + if (durationMs > 0L) "/" + formatTime(durationMs) else ""
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = startInset + (18f * scale).dp, end = endInset + (18f * scale).dp, top = (10f * scale).dp, bottom = (10f * scale).dp),
        ) {
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ui.title,
                        color = text.copy(alpha = 0.95f),
                        fontSize = (62f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.basicMarquee(),
                    )
                    Spacer(Modifier.height((10f * scale).dp))
                    if (ui.mapper.isNotBlank()) {
                        Text(
                            text = "Mapped by ${ui.mapper}",
                            color = text.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onMapper),
                        )
                    }
                    Text(
                        text = "by - ${ui.artist}",
                        color = text.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
                    )
                }
                Spacer(Modifier.width((16f * scale).dp))
                Text(
                    text = timeText,
                    color = text.copy(alpha = 0.85f),
                    fontSize = (48f * scale).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = (140f * scale).dp),
                )
            }
            Spacer(Modifier.height((20f * scale).dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    at(R.drawable.at_expand_more), contentDescription = "Close player",
                    tint = text.copy(alpha = 0.8f),
                    modifier = Modifier.size((28f * scale).dp).bounceClick(onClick = ui.onClose),
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    favoriteIcon(ui), contentDescription = "Favorite",
                    tint = if (ui.isFavorite) colors.error.copy(alpha = 0.9f) else text.copy(alpha = 0.78f),
                    modifier = Modifier.size((26f * scale).dp).bounceClick(onClick = ui.onFavorite),
                )
                Spacer(Modifier.width((18f * scale).dp))
                Icon(
                    at(R.drawable.at_queue_music), contentDescription = "Queue",
                    tint = text.copy(alpha = 0.78f),
                    modifier = Modifier.size((26f * scale).dp).bounceClick(onClick = ui.onQueue),
                )
                Spacer(Modifier.width((18f * scale).dp))
                Icon(
                    at(R.drawable.at_more_vert), contentDescription = "More options",
                    tint = text.copy(alpha = 0.78f),
                    modifier = Modifier.size((26f * scale).dp).bounceClick(onClick = ui.onMore),
                )
            }
        }
    }
}

// ───────────────────────────── 6. Expressive ─────────────────────────────

@Composable
private fun ExpressiveDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    val glass = colors.onSurface.copy(alpha = 0.12f)
    DesignColumn(ui) {
        NowPlayingHeader(ui)
        ArtworkArea(ui, Modifier.weight(1f), fraction = 0.94f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackText(ui, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    at(R.drawable.at_share), kozuUiString("Share"), ui.onShare, size = 42.dp, iconSize = 20.dp, container = glass,
                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                )
                RoundButton(
                    favoriteIcon(ui), kozuUiString("Favorite"), ui.onFavorite, size = 42.dp, iconSize = 20.dp,
                    container = if (ui.isFavorite) colors.error.copy(alpha = 0.18f) else glass,
                    content = if (ui.isFavorite) colors.error else colors.onSurface,
                    shape = RoundedCornerShape(50),
                )
                RoundButton(
                    at(R.drawable.at_more_horiz), kozuUiString("More options"), ui.onMore, size = 42.dp, iconSize = 20.dp, container = glass,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 50.dp, bottomEnd = 50.dp),
                )
            }
        }
        PlayerSeek(ui)
        // The chunky tray: previous and next hug a big play button.
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = colors.onSurface.copy(alpha = 0.08f)) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrayKey(
                    at(R.drawable.at_skip_previous), "Previous", ui.onPrevious,
                    RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                PlayButton(ui, width = 88.dp, height = 80.dp, shape = RoundedCornerShape(28.dp), iconSize = 44.dp)
                Spacer(Modifier.width(6.dp))
                TrayKey(
                    at(R.drawable.at_skip_next), "Next", ui.onNext,
                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp),
                    Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ShuffleSmall(ui)
            Spacer(Modifier.width(16.dp))
            RepeatSmall(ui)
        }
        QueueSleepPills(ui)
    }
}

@Composable
private fun TrayKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberKozuHaptics()
    RoundButton(
        icon, description,
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier.height(56.dp),
        size = 56.dp, iconSize = 28.dp,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = shape,
    )
}

@Composable
private fun ShuffleSmall(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    RoundButton(
        shuffleIcon(), "Shuffle", ui.onShuffle, size = 40.dp, iconSize = 20.dp,
        container = if (ui.shuffle) colors.primary.copy(alpha = 0.25f) else colors.onSurface.copy(alpha = 0.08f),
        content = if (ui.shuffle) colors.primary else colors.onSurface.copy(alpha = 0.5f),
    )
}

@Composable
private fun RepeatSmall(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    val active = ui.repeat != RepeatState.OFF
    RoundButton(
        repeatIcon(ui), "Repeat", ui.onRepeat, size = 40.dp, iconSize = 20.dp,
        container = if (active) colors.primary.copy(alpha = 0.25f) else colors.onSurface.copy(alpha = 0.08f),
        content = if (active) colors.primary else colors.onSurface.copy(alpha = 0.5f),
    )
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
                .then(if (ui.previewMode) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (ui.compact) 6.dp else 10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    at(R.drawable.at_expand_more), kozuUiString("Close player"), ui.onClose,
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
                    at(R.drawable.at_more_vert), kozuUiString("More options"), ui.onMore,
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
                TextAction(at(R.drawable.at_queue_music), kozuUiString("Queue"), ui.onQueue, false)
                TextAction(at(R.drawable.at_bedtime), kozuUiString("Sleep timer"), ui.onSleep, ui.sleepActive)
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
                        at(R.drawable.at_more_vert), kozuUiString("More options"), ui.onMore, size = 44.dp,
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
                .then(if (ui.previewMode) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (ui.compact) 6.dp else 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    at(R.drawable.at_expand_more), kozuUiString("Close player"), ui.onClose,
                    container = colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = colors.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
                RoundButton(
                    at(R.drawable.at_bedtime), kozuUiString("Sleep timer"), ui.onSleep,
                    container = if (ui.sleepActive) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = if (ui.sleepActive) colors.onPrimary else colors.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    at(R.drawable.at_queue_music), kozuUiString("Queue"), ui.onQueue,
                    container = colors.onPrimaryContainer.copy(alpha = 0.14f),
                    content = colors.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(
                    at(R.drawable.at_more_vert), kozuUiString("More options"), ui.onMore,
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
                titleStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                artistStyle = MaterialTheme.typography.titleMedium,
                titleLines = if (ui.compact) 1 else 2,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(76.dp).bounceClick(pressedScale = 0.96f, onClick = ui.onPlayPause),
                    shape = CircleShape,
                    color = colors.onPrimaryContainer,
                    contentColor = colors.primaryContainer,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (ui.isPlaying) "PAUSE" else "PLAY",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                            ),
                        )
                    }
                }
                RoundButton(
                    at(R.drawable.at_skip_next), "Next", ui.onNext,
                    size = 76.dp, iconSize = 32.dp,
                    container = colors.onPrimaryContainer,
                    content = colors.primaryContainer,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShuffleButton(ui, plain = true)
                SkipButton(ui, next = false, plain = false, size = 56.dp)
                RepeatButton(ui, plain = true)
                FavoriteButton(ui, plain = true)
            }
        }
    }
}
