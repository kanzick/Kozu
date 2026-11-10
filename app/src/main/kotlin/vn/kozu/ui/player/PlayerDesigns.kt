package vn.kozu.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.request.ImageRequest
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.domain.model.SeekBarStyle
import vn.kozu.player.RepeatState
import vn.kozu.R
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.FullBeatmapArt
import vn.kozu.ui.components.KozuIcons
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.playbackIconVector
import vn.kozu.ui.components.rememberKozuHaptics
import vn.kozu.ui.components.toShape
import vn.kozu.ui.components.WavySlider
import vn.kozu.ui.theme.KozuCookies
import vn.kozu.util.formatTime

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


fun PlayerLayoutStyle.drawsOwnBackground(): Boolean =
    this == PlayerLayoutStyle.IMMERSIVE ||
        this == PlayerLayoutStyle.IMMERSIVE_EXTENDED ||
        this == PlayerLayoutStyle.LITTLE ||
        this == PlayerLayoutStyle.EDITORIAL


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
        verticalArrangement = Arrangement.Top,
        content = content,
    )
}


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
private fun at(@DrawableRes id: Int): ImageVector = ImageVector.vectorResource(id)

private class Pal(
    val cream: Color,
    val onCream: Color,
    val muted: Color,
    val soft: Color,
    val faint: Color,
)

@Composable
private fun pal(): Pal {
    val c = MaterialTheme.colorScheme
    return Pal(
        cream = c.onSurface,
        onCream = c.surface,
        muted = c.onSurface.copy(alpha = 0.62f),
        soft = c.onSurface.copy(alpha = 0.14f),
        faint = c.onSurface.copy(alpha = 0.08f),
    )
}


@Composable
private fun ArtBox(
    ui: PlayerUi,
    modifier: Modifier = Modifier,
    hPad: Dp = 32.dp,
    shape: Shape = ui.artShape,
    original: Boolean = ui.useOriginalArtwork,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().playerSwipe(ui),
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth - hPad * 2, maxHeight)
        PlayerArt(ui, side.coerceAtLeast(80.dp), shape = shape, original = original)
    }
}

@Composable
private fun PlayerHeader(
    ui: PlayerUi,
    p: Pal,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val main = if (onDark) Color.White else p.cream
    val sub = if (onDark) Color.White.copy(alpha = 0.7f) else p.muted
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = if (ui.compact) 8.dp else 20.dp, bottom = if (ui.compact) 6.dp else 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .bounceClick(pressedScale = 0.98f, onClick = ui.onClose),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.headlineSmall, color = main)
        Text(text = ui.title,
            style = MaterialTheme.typography.titleMedium,
            color = sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun TitleText(
    ui: PlayerUi,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
) {
    Text(text = ui.title,
        fontSize = size,
        fontWeight = weight,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = if (ui.previewMode) modifier else modifier.basicMarquee(iterations = Int.MAX_VALUE),
    )
}


@Composable
private fun SubLine(
    ui: PlayerUi,
    color: Color,
    size: TextUnit = 17.sp,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = ui.artist,
            fontSize = size,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
        )
        if (ui.mapper.isNotBlank()) {
            Text("  •  ", fontSize = size, color = color.copy(alpha = 0.6f), maxLines = 1)
            Text(text = ui.mapper,
                fontSize = (size.value - 3).sp,
                color = color.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(pressedScale = 0.97f, onClick = ui.onMapper),
            )
        }
    }
}

@Composable
private fun TitleRow(
    ui: PlayerUi,
    p: Pal,
    hPad: Dp,
    titleSize: TextUnit = 26.sp,
    titleColor: Color = p.cream,
    subColor: Color = p.muted,
    weight: FontWeight = FontWeight.Medium,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = hPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            TitleText(ui, titleSize, titleColor, weight)
            Spacer(Modifier.height(2.dp))
            SubLine(ui, subColor)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun Btn(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    width: Dp = Dp.Unspecified,
    iconSize: Dp = 22.dp,
    container: Color = MaterialTheme.colorScheme.onSurface,
    content: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = CircleShape,
) {
    val sized = if (width == Dp.Unspecified) modifier.size(size) else modifier.size(width, size)
    Surface(
        modifier = sized.bounceClick(onClick = onClick),
        shape = shape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun PlainBtn(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 26.dp,
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
private fun playIcon(ui: PlayerUi): ImageVector = at(if (ui.isPlaying) R.drawable.at_pause else R.drawable.at_play)

@Composable
private fun shuffleIcon(): ImageVector = at(R.drawable.at_shuffle)

@Composable
private fun repeatIcon(ui: PlayerUi): ImageVector =
    at(if (ui.repeat == RepeatState.ONE) R.drawable.at_repeat_one else R.drawable.at_repeat)

@Composable
private fun favoriteIcon(ui: PlayerUi): ImageVector =
    at(if (ui.isFavorite) R.drawable.at_favorite else R.drawable.at_favorite_border)

@Composable
private fun PlayKey(
    ui: PlayerUi,
    width: Dp,
    height: Dp,
    shape: Shape,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 38.dp,
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
            Icon(playIcon(ui), contentDescription = if (ui.isPlaying) "Pause" else "Play", modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun SkipKey(
    ui: PlayerUi,
    next: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 36.dp,
) {
    val haptics = rememberKozuHaptics()
    PlainBtn(
        icon = at(if (next) R.drawable.at_skip_next else R.drawable.at_skip_previous),
        description = if (next) "Next" else "Previous",
        onClick = {
            haptics.tick()
            if (next) ui.onNext() else ui.onPrevious()
        },
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        tint = tint,
    )
}


private class SeekState(
    val fraction: Float,
    val shownMs: Long,
    val durationMs: Long,
    val onScrub: (Float) -> Unit,
    val onFinish: () -> Unit,
)

@Composable
private fun rememberSeek(ui: PlayerUi): SeekState {
    val pos by ui.position.collectAsStateWithLifecycle()
    val dur by ui.duration.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<Float?>(null) }
    val latest by rememberUpdatedState(ui)
    val target = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val smooth by animateFloatAsState(target, tween(220, easing = LinearEasing), label = "playerSeek")
    val shown = scrub ?: smooth
    return SeekState(
        fraction = shown,
        shownMs = scrub?.let { (it * dur).toLong() } ?: pos,
        durationMs = dur,
        onScrub = { scrub = it.coerceIn(0f, 1f) },
        onFinish = {
            val s = scrub
            if (s != null && dur > 0L) latest.onSeek((s * dur).toLong())
            scrub = null
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekM3(
    seek: SeekState,
    p: Pal,
    hPad: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    val sliderState = rememberSliderState(value = seek.fraction)


    LaunchedEffect(seek.fraction) {
        sliderState.value = seek.fraction
    }

    Slider(
        state = sliderState,
        onValueChange = { value ->
            sliderState.value = value
            seek.onScrub(value)
        },
        onValueChangeFinished = seek.onFinish,
        modifier = modifier.fillMaxWidth().padding(horizontal = hPad),
        colors =
            SliderDefaults.colors(
                thumbColor = p.cream,
                activeTrackColor = p.cream,
                inactiveTrackColor = p.cream.copy(alpha = 0.2f),
            ),
    )
}


@Composable
private fun LineSeek(
    fraction: Float,
    onScrub: (Float) -> Unit,
    onFinish: () -> Unit,
    height: Dp,
    active: Color,
    inactive: Color,
    modifier: Modifier = Modifier,
    thumb: Dp = 0.dp,
) {
    val scrubNow by rememberUpdatedState(onScrub)
    val finishNow by rememberUpdatedState(onFinish)
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(maxOf(height, 32.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        scrubNow((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                        finishNow()
                    }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> scrubNow((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)) },
                        onDragEnd = { finishNow() },
                        onDragCancel = { finishNow() },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scrubNow((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                        },
                    )
                },
    ) {
        val h = height.toPx()
        val top = (size.height - h) / 2f
        val radius = CornerRadius(h / 2f, h / 2f)
        val f = fraction.coerceIn(0f, 1f)
        val inset = thumb.toPx()
        drawRoundRect(inactive, Offset(0f, top), Size(size.width, h), radius)
        drawRoundRect(active, Offset(0f, top), Size(maxOf(h, (size.width - inset) * f + inset / 2f), h), radius)
        if (thumb > 0.dp) {
            drawCircle(active, radius = inset / 2f + 1.dp.toPx(), center = Offset(inset / 2f + (size.width - inset) * f, size.height / 2f))
        }
    }
}

@Composable
private fun SeekTimes(
    seek: SeekState,
    color: Color,
    hPad: Dp,
    remaining: Boolean = false,
    center: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = hPad + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatTime(seek.shownMs), fontSize = 14.sp, color = color)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { center?.invoke() }
        Text(text = if (remaining) "-" + formatTime((seek.durationMs - seek.shownMs).coerceAtLeast(0L)) else formatTime(seek.durationMs),
            fontSize = 14.sp,
            color = color,
        )
    }
}


@Composable
fun PlayerVolumeBar(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    track: Color = Color.White.copy(alpha = 0.3f),
    iconTint: Color = Color.White,
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
        Icon(at(R.drawable.at_volume_off), contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        LineSeek(
            fraction = volume / max,
            onScrub = {
                volume = it * max
                runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume.roundToInt(), 0) }
            },
            onFinish = {},
            height = 5.dp,
            active = tint,
            inactive = track,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        Icon(at(R.drawable.at_volume_up), contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
    }
}


@Composable
private fun ClassicDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    DesignColumn(ui, horizontal = 0.dp) {
        PlayerHeader(ui, p)
        ArtBox(ui, Modifier.weight(1f), hPad = 32.dp)
        Spacer(Modifier.height(14.dp))
        TitleRow(ui, p, hPad = 32.dp) {
            Btn(at(R.drawable.at_share), "Share", ui.onShare, size = 40.dp, container = p.cream, content = p.onCream)
            Spacer(Modifier.width(12.dp))
            Btn(at(R.drawable.at_more_horiz), stringResource(R.string.more_options), ui.onMore, size = 40.dp, container = p.cream, content = p.onCream)
        }
        Spacer(Modifier.height(4.dp))
        SeekM3(seek, p, hPad = 28.dp)
        SeekTimes(seek, p.muted, hPad = 28.dp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlainBtn(
                    repeatIcon(ui), "Repeat", ui.onRepeat, size = 48.dp, iconSize = 30.dp,
                    tint = if (ui.repeat != RepeatState.OFF) MaterialTheme.colorScheme.primary else p.muted,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SkipKey(ui, false, p.cream, size = 52.dp, iconSize = 38.dp) }
            PlayKey(ui, 72.dp, 72.dp, CircleShape, p.cream, p.onCream, iconSize = 38.dp)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SkipKey(ui, true, p.cream, size = 52.dp, iconSize = 38.dp) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlainBtn(
                    favoriteIcon(ui), "Favorite", ui.onFavorite, size = 48.dp, iconSize = 30.dp,
                    tint = if (ui.isFavorite) MaterialTheme.colorScheme.error else p.cream,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextBtn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, p.cream, Modifier.weight(1f))
            TextBtn(
                at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep,
                if (ui.sleepActive) MaterialTheme.colorScheme.primary else p.cream, Modifier.weight(1f),
            )
            TextBtn(at(R.drawable.at_more_vert), "More", ui.onMore, p.cream, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TextBtn(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).bounceClick(pressedScale = 0.96f, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            if (label != null) {
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 16.sp, color = tint, maxLines = 1)
            }
        }
    }
}


@Composable
private fun ModernDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    DesignColumn(ui, horizontal = 0.dp) {
        PlayerHeader(ui, p)
        ArtBox(ui, Modifier.weight(1f), hPad = 32.dp)
        Spacer(Modifier.height(14.dp))
        TitleRow(ui, p, hPad = 32.dp, titleSize = 28.sp) {

            Btn(
                at(R.drawable.at_share), "Share", ui.onShare, size = 42.dp, container = p.cream, content = p.onCream,
                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 10.dp, bottomEnd = 10.dp),
            )
            Spacer(Modifier.width(12.dp))
            Btn(
                favoriteIcon(ui), "Favorite", ui.onFavorite, size = 42.dp, container = p.cream,
                content = if (ui.isFavorite) MaterialTheme.colorScheme.error else p.onCream,
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 50.dp, bottomEnd = 50.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        SeekM3(seek, p, hPad = 28.dp)
        SeekTimes(seek, p.muted, hPad = 28.dp)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val playH = maxWidth / 6f
            val sideH = playH * 0.8f
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                PillKey(at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, sideH * 1.3f, sideH, p)
                Spacer(Modifier.width(16.dp))
                PlayKey(ui, playH * 1.6f, playH, RoundedCornerShape(playH / 2f), p.cream, p.onCream, iconSize = 38.dp)
                Spacer(Modifier.width(16.dp))
                PillKey(at(R.drawable.at_skip_next), "Next", ui.onNext, sideH * 1.3f, sideH, p)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedKey(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, p, false)
                OutlinedKey(at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, p, ui.sleepActive)
                OutlinedKey(shuffleIcon(), "Shuffle", ui.onShuffle, p, ui.shuffle)
                OutlinedKey(repeatIcon(ui), "Repeat", ui.onRepeat, p, ui.repeat != RepeatState.OFF)
            }
            Btn(at(R.drawable.at_more_vert), stringResource(R.string.more_options), ui.onMore, size = 42.dp, container = p.cream, content = p.onCream)
        }
    }
}

@Composable
private fun PillKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    p: Pal,
) {
    val haptics = rememberKozuHaptics()
    Btn(
        icon, description,
        onClick = {
            haptics.tick()
            onClick()
        },
        size = height, width = width, iconSize = 30.dp,
        container = p.cream, content = p.onCream, shape = RoundedCornerShape(height / 2f),
    )
}

@Composable
private fun OutlinedKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    p: Pal,
    active: Boolean,
) {
    Surface(
        modifier = Modifier.size(42.dp).bounceClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else p.cream.copy(alpha = 0.32f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, contentDescription = description, modifier = Modifier.size(22.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else p.cream,
            )
        }
    }
}


@Composable
private fun MinimalDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui, horizontal = 0.dp) {
        PlayerHeader(ui, p)
        ArtBox(ui, Modifier.weight(1f), hPad = 32.dp)
        Spacer(Modifier.height(14.dp))
        TitleRow(ui, p, hPad = 32.dp, titleSize = 28.sp) {
            PlainBtn(at(R.drawable.at_share), "Share", ui.onShare, size = 44.dp, iconSize = 26.dp, tint = p.cream.copy(alpha = 0.8f))
            PlainBtn(
                favoriteIcon(ui), "Favorite", ui.onFavorite, size = 44.dp, iconSize = 26.dp,
                tint = if (ui.isFavorite) colors.error else p.cream.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(4.dp))
        SeekM3(seek, p, hPad = 28.dp)
        SeekTimes(seek, p.muted, hPad = 28.dp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlainBtn(shuffleIcon(), "Shuffle", ui.onShuffle, size = 48.dp, iconSize = 28.dp, tint = if (ui.shuffle) colors.primary else p.muted)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Btn(
                    at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, size = 52.dp, iconSize = 30.dp,
                    container = p.faint, content = p.cream, shape = RoundedCornerShape(16.dp),
                )
            }
            PlayKey(ui, 70.dp, 70.dp, CircleShape, p.cream, p.onCream, iconSize = 36.dp)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Btn(
                    at(R.drawable.at_skip_next), "Next", ui.onNext, size = 52.dp, iconSize = 30.dp,
                    container = p.faint, content = p.cream, shape = RoundedCornerShape(16.dp),
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlainBtn(repeatIcon(ui), "Repeat", ui.onRepeat, size = 48.dp, iconSize = 28.dp, tint = if (ui.repeat != RepeatState.OFF) colors.primary else p.muted)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextBtn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, p.cream, Modifier.weight(1f))
            TextBtn(
                at(R.drawable.at_bedtime), null, ui.onSleep,
                if (ui.sleepActive) colors.primary else p.cream, Modifier.weight(1f),
            )
            TextBtn(at(R.drawable.at_more_vert), null, ui.onMore, p.cream, Modifier.weight(1f))
        }
    }
}


@Composable
private fun CinematicDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui, horizontal = 0.dp) {
        PlayerHeader(ui, p)
        ArtBox(ui, Modifier.weight(1f), hPad = 32.dp)
        Spacer(Modifier.height(14.dp))
        TitleRow(ui, p, hPad = 32.dp, titleSize = 28.sp) {
            val r = RoundedCornerShape(16.dp)
            Btn(at(R.drawable.at_share), "Share", ui.onShare, size = 44.dp, iconSize = 24.dp, container = p.faint, content = p.cream, shape = r)
            Spacer(Modifier.width(10.dp))
            Btn(
                favoriteIcon(ui), "Favorite", ui.onFavorite, size = 44.dp, iconSize = 24.dp, container = p.faint,
                content = if (ui.isFavorite) colors.error else p.cream, shape = r,
            )
            Spacer(Modifier.width(10.dp))
            Btn(at(R.drawable.at_more_horiz), stringResource(R.string.more_options), ui.onMore, size = 44.dp, iconSize = 24.dp, container = p.faint, content = p.cream, shape = r)
        }
        Spacer(Modifier.height(4.dp))
        SeekM3(seek, p, hPad = 28.dp)
        SeekTimes(seek, p.muted, hPad = 28.dp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Btn(
                shuffleIcon(), "Shuffle", ui.onShuffle, size = 32.dp, iconSize = 18.dp,
                container = if (ui.shuffle) colors.primary.copy(alpha = 0.28f) else p.faint,
                content = if (ui.shuffle) colors.primary else p.muted, shape = RoundedCornerShape(10.dp),
            )
            Spacer(Modifier.width(9.dp))
            Btn(
                at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, size = 40.dp, iconSize = 22.dp,
                container = p.soft, content = p.cream, shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(20.dp))
            PlayKey(ui, 88.dp, 88.dp, CircleShape, p.cream, p.onCream, iconSize = 44.dp)
            Spacer(Modifier.width(20.dp))
            Btn(
                at(R.drawable.at_skip_next), "Next", ui.onNext, size = 40.dp, iconSize = 22.dp,
                container = p.soft, content = p.cream, shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(9.dp))
            Btn(
                repeatIcon(ui), "Repeat", ui.onRepeat, size = 32.dp, iconSize = 18.dp,
                container = if (ui.repeat != RepeatState.OFF) colors.primary.copy(alpha = 0.28f) else p.faint,
                content = if (ui.repeat != RepeatState.OFF) colors.primary else p.muted, shape = RoundedCornerShape(10.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        BottomPills(ui, p)
    }
}


@Composable
private fun BottomPills(
    ui: PlayerUi,
    p: Pal,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, p, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Btn(
            at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, size = 47.dp, iconSize = 24.dp,
            container = if (ui.sleepActive) colors.primary.copy(alpha = 0.3f) else p.faint,
            content = if (ui.sleepActive) colors.primary else p.cream,
        )
        Spacer(Modifier.width(12.dp))
        Pill(at(R.drawable.at_more_vert), "More", ui.onMore, p, Modifier.weight(1f))
    }
}

@Composable
private fun Pill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    p: Pal,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(47.dp).bounceClick(pressedScale = 0.97f, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = p.faint,
        contentColor = p.cream,
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 16.sp, maxLines = 1)
        }
    }
}


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
                    Text(text = ui.title,
                        color = text.copy(alpha = 0.95f),
                        fontSize = (62f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.basicMarquee(),
                    )
                    Spacer(Modifier.height((10f * scale).dp))
                    if (ui.mapper.isNotBlank()) {
                        Text(text = stringResource(R.string.mapped_by, ui.mapper),
                            color = text.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onMapper),
                        )
                    }
                    Text(text = stringResource(R.string.by_artist, ui.artist),
                        color = text.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
                    )
                }
                Spacer(Modifier.width((16f * scale).dp))
                Text(text = timeText,
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


@Composable
private fun ExpressiveDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    val colors = MaterialTheme.colorScheme
    DesignColumn(ui, horizontal = 0.dp) {
        PlayerHeader(ui, p)
        ArtBox(ui, Modifier.weight(1f), hPad = 32.dp)
        Spacer(Modifier.height(14.dp))
        TitleRow(ui, p, hPad = 32.dp, titleSize = 28.sp) {
            Btn(
                at(R.drawable.at_share), "Share", ui.onShare, size = 42.dp, iconSize = 22.dp, container = p.faint, content = p.cream,
                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Btn(
                favoriteIcon(ui), "Favorite", ui.onFavorite, size = 42.dp, iconSize = 22.dp, container = p.faint,
                content = if (ui.isFavorite) colors.error else p.cream,
            )
            Spacer(Modifier.width(8.dp))
            Btn(
                at(R.drawable.at_more_horiz), stringResource(R.string.more_options), ui.onMore, size = 42.dp, iconSize = 22.dp, container = p.faint, content = p.cream,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 50.dp, bottomEnd = 50.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        SeekM3(seek, p, hPad = 28.dp)
        SeekTimes(seek, p.muted, hPad = 28.dp)
        Spacer(Modifier.height(8.dp))
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            shape = RoundedCornerShape(28.dp),
            color = p.faint,
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TrayKey(
                    at(R.drawable.at_skip_previous), "Previous", ui.onPrevious,
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                    Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                PlayKey(ui, 88.dp, 80.dp, RoundedCornerShape(30.dp), p.cream, p.onCream, iconSize = 42.dp)
                Spacer(Modifier.width(8.dp))
                TrayKey(
                    at(R.drawable.at_skip_next), "Next", ui.onNext,
                    RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Btn(
                shuffleIcon(), "Shuffle", ui.onShuffle, size = 40.dp, iconSize = 20.dp,
                container = if (ui.shuffle) colors.primary.copy(alpha = 0.28f) else p.faint,
                content = if (ui.shuffle) colors.primary else p.muted,
            )
            Spacer(Modifier.width(16.dp))
            Btn(
                repeatIcon(ui), "Repeat", ui.onRepeat, size = 40.dp, iconSize = 20.dp,
                container = if (ui.repeat != RepeatState.OFF) colors.primary.copy(alpha = 0.28f) else p.faint,
                content = if (ui.repeat != RepeatState.OFF) colors.primary else p.muted,
            )
        }
        BottomPills(ui, p)
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
    val c = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .height(56.dp)
                .bounceClick(pressedScale = 0.96f) {
                    haptics.tick()
                    onClick()
                },
        shape = shape,
        color = c.secondaryContainer,
        contentColor = c.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(28.dp))
        }
    }
}


@Composable
private fun BlurFadeArt(ui: PlayerUi) {
    Box(Modifier.fillMaxSize()) {
        FullBleedArt(ui, Modifier.fillMaxSize())
        FullBleedArt(
            ui,
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0.30f to Color.Transparent, 0.62f to Color.Black),
                        blendMode = BlendMode.DstIn,
                    )
                }.blur(32.dp, BlurredEdgeTreatment.Unbounded),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.28f),
                    0.2f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.5f),
                ),
            ),
        )
    }
}

@Composable
private fun ImmersiveDesign(ui: PlayerUi, extended: Boolean) {
    val p = pal()
    val seek = rememberSeek(ui)
    val white = Color.White
    val whiteMuted = Color.White.copy(alpha = 0.72f)
    Box(Modifier.fillMaxSize()) {
        if (extended) {
            FullBleedArt(ui, Modifier.fillMaxSize().blur(46.dp, BlurredEdgeTreatment.Unbounded))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        } else {
            BlurFadeArt(ui)
        }
        DesignColumn(ui, horizontal = 0.dp) {
            if (extended) {
                PlayerHeader(ui, p, onDark = true)
                ArtBox(ui, Modifier.weight(1f), hPad = 22.dp)
            } else {
                Text(text = stringResource(R.string.playing_from, ui.title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = white,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick(pressedScale = 0.98f, onClick = ui.onClose),
                )
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            TitleRow(ui, p, hPad = if (extended) 22.dp else 28.dp, titleSize = 30.sp, titleColor = white, subColor = whiteMuted) {
                if (extended) {
                    Btn(
                        at(R.drawable.at_more_vert), stringResource(R.string.more_options), ui.onMore, size = 48.dp, iconSize = 24.dp,
                        container = Color.White.copy(alpha = 0.2f), content = white,
                    )
                    Spacer(Modifier.width(12.dp))
                    Btn(
                        favoriteIcon(ui), "Favorite", ui.onFavorite, size = 48.dp, iconSize = 24.dp,
                        container = Color.White.copy(alpha = 0.3f), content = white,
                    )
                } else {
                    PlainBtn(favoriteIcon(ui), "Favorite", ui.onFavorite, size = 48.dp, iconSize = 30.dp, tint = white)
                    PlainBtn(at(R.drawable.at_more_horiz), stringResource(R.string.more_options), ui.onMore, size = 48.dp, iconSize = 30.dp, tint = white)
                }
            }
            Spacer(Modifier.height(12.dp))
            val hPad = if (extended) 22.dp else 25.dp
            LineSeek(
                fraction = seek.fraction, onScrub = seek.onScrub, onFinish = seek.onFinish,
                height = if (extended) 9.dp else 5.dp, active = white, inactive = Color.White.copy(alpha = 0.32f),
                modifier = Modifier.padding(horizontal = hPad),
            )
            SeekTimes(seek, whiteMuted, hPad = hPad, remaining = !extended) {
                if (extended && !ui.fileInfo.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(at(R.drawable.at_graphic_eq), contentDescription = null, tint = whiteMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ui.fileInfo.uppercase(), fontSize = 13.sp, color = whiteMuted, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (ui.compact) 6.dp else 18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                SkipKey(ui, false, white, size = 96.dp, iconSize = 48.dp)
                PlainBtn(playIcon(ui), if (ui.isPlaying) "Pause" else "Play", ui.onPlayPause, size = 96.dp, iconSize = 52.dp, tint = white)
                SkipKey(ui, true, white, size = 96.dp, iconSize = 48.dp)
            }
            Spacer(Modifier.height(if (ui.compact) 4.dp else 14.dp))
            if (ui.showVolume) PlayerVolumeBar(Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            if (extended) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val glass = Color.White.copy(alpha = 0.16f)
                    Btn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, size = 42.dp, iconSize = 22.dp, container = glass, content = white)
                    Spacer(Modifier.width(8.dp))
                    Btn(shuffleIcon(), "Shuffle", ui.onShuffle, size = 42.dp, iconSize = 22.dp, container = if (ui.shuffle) Color.White.copy(alpha = 0.4f) else glass, content = white)
                    Spacer(Modifier.width(8.dp))
                    Btn(
                        at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, size = 42.dp, iconSize = 22.dp,
                        container = if (ui.sleepActive) Color.White.copy(alpha = 0.4f) else glass, content = white,
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.height(47.dp).bounceClick(pressedScale = 0.97f, onClick = ui.onRepeat),
                        shape = RoundedCornerShape(24.dp), color = glass, contentColor = white,
                    ) {
                        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(repeatIcon(ui), contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(when (ui.repeat) {
                                    RepeatState.OFF -> "Repeat off"
                                    RepeatState.ONE -> "Repeat one"
                                    else -> "Repeat all"
                                },
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    PlainBtn(at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, size = 48.dp, iconSize = 28.dp, tint = if (ui.sleepActive) MaterialTheme.colorScheme.primary else white)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(24.dp), color = Color.Black.copy(alpha = 0.28f), contentColor = white) {
                            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                PlainBtn(shuffleIcon(), "Shuffle", ui.onShuffle, size = 46.dp, iconSize = 24.dp, tint = if (ui.shuffle) MaterialTheme.colorScheme.primary else white)
                                Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.25f)))
                                PlainBtn(repeatIcon(ui), "Repeat", ui.onRepeat, size = 46.dp, iconSize = 24.dp, tint = if (ui.repeat != RepeatState.OFF) MaterialTheme.colorScheme.primary else white)
                            }
                        }
                        Text(ui.fileInfo?.takeIf { it.isNotBlank() } ?: stringResource(R.string.kozu),
                            fontSize = 14.sp, color = whiteMuted, modifier = Modifier.padding(top = 4.dp), maxLines = 1,
                        )
                    }
                    PlainBtn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, size = 48.dp, iconSize = 28.dp, tint = white)
                }
            }
        }
    }
}


@Composable
private fun MaterialExtendedDesign(ui: PlayerUi) {
    val p = pal()
    val seek = rememberSeek(ui)
    val colors = MaterialTheme.colorScheme
    val container = colors.surfaceContainerHigh
    DesignColumn(ui, horizontal = 0.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = if (ui.compact) 4.dp else 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Btn(at(R.drawable.at_expand_more), stringResource(R.string.close_player), ui.onClose, size = 56.dp, iconSize = 28.dp, container = container, content = p.cream)
            Text(text = stringResource(R.string.now_playing),
                fontSize = 24.sp, color = p.cream, maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Btn(
                at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, size = 56.dp, iconSize = 26.dp,
                container = if (ui.sleepActive) colors.primary.copy(alpha = 0.3f) else container,
                content = if (ui.sleepActive) colors.primary else p.cream,
            )
            Spacer(Modifier.width(12.dp))
            Btn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, size = 56.dp, iconSize = 26.dp, container = container, content = p.cream)
        }
        ArtBox(ui, Modifier.weight(1f), hPad = 27.dp, shape = RoundedCornerShape(36.dp), original = false)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                TitleText(ui, 31.sp, p.cream, FontWeight.Normal)
                Spacer(Modifier.height(2.dp))
                SubLine(ui, p.muted, size = 22.sp)
            }
            Spacer(Modifier.width(8.dp))
            PlainBtn(
                favoriteIcon(ui), "Favorite", ui.onFavorite, size = 52.dp, iconSize = 34.dp,
                tint = if (ui.isFavorite) colors.error else p.cream,
            )
        }
        Spacer(Modifier.height(6.dp))
        LineSeek(
            fraction = seek.fraction, onScrub = seek.onScrub, onFinish = seek.onFinish,
            height = 4.dp, active = p.cream, inactive = p.cream.copy(alpha = 0.22f), thumb = 14.dp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        SeekTimes(seek, p.muted, hPad = 24.dp)
        Spacer(Modifier.height(if (ui.compact) 6.dp else 16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val keyShape = RoundedCornerShape(42.dp)
            Btn(
                at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, modifier = Modifier.weight(1f), size = 78.dp, iconSize = 34.dp,
                container = container, content = p.cream, shape = keyShape,
            )
            PlayKey(ui, 97.dp, 78.dp, keyShape, p.cream, p.onCream, modifier = Modifier.weight(1f), iconSize = 38.dp)
            Btn(
                at(R.drawable.at_skip_next), "Next", ui.onNext, modifier = Modifier.weight(1f), size = 78.dp, iconSize = 34.dp,
                container = container, content = p.cream, shape = keyShape,
            )
        }
        if (ui.showVolume) {
            Spacer(Modifier.height(10.dp))
            PlayerVolumeBar(Modifier.padding(horizontal = 28.dp), tint = p.cream, track = p.cream.copy(alpha = 0.22f), iconTint = p.muted)
        }
        Spacer(Modifier.height(if (ui.compact) 6.dp else 16.dp))
        Surface(
            Modifier.padding(horizontal = 40.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(40.dp),
            color = p.faint,
        ) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SegmentKey(
                    shuffleIcon(), "Shuffle", ui.onShuffle, ui.shuffle, p,
                    RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 8.dp, bottomEnd = 8.dp), Modifier.weight(1f),
                )
                SegmentKey(repeatIcon(ui), "Repeat", ui.onRepeat, ui.repeat != RepeatState.OFF, p, RoundedCornerShape(8.dp), Modifier.weight(1f))
                SegmentKey(
                    at(R.drawable.at_more_horiz), "More", ui.onMore, false, p,
                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 32.dp, bottomEnd = 32.dp), Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SegmentKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean,
    p: Pal,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(56.dp).bounceClick(pressedScale = 0.96f, onClick = onClick),
        shape = shape,
        color = if (active) colors.primary.copy(alpha = 0.28f) else p.faint,
        contentColor = if (active) colors.primary else p.cream.copy(alpha = 0.8f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(26.dp))
        }
    }
}


@Composable
private fun EditorialDesign(ui: PlayerUi) {
    val colors = MaterialTheme.colorScheme
    val seek = rememberSeek(ui)
    val accent = colors.primary
    val onAccent = colors.onPrimary
    val bg = colors.primaryContainer
    val key = accent
    Box(Modifier.fillMaxSize().background(bg)) {
        DesignColumn(ui, horizontal = 0.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = if (ui.compact) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Btn(at(R.drawable.at_expand_more), stringResource(R.string.close_player), ui.onClose, size = 44.dp, iconSize = 26.dp, container = key, content = onAccent)
                Spacer(Modifier.weight(1f))
                PlainBtn(
                    at(R.drawable.at_bedtime), stringResource(R.string.sleep_timer), ui.onSleep, size = 44.dp, iconSize = 26.dp,
                    tint = if (ui.sleepActive) colors.onPrimaryContainer else accent,
                )
                Spacer(Modifier.width(12.dp))
                Btn(at(R.drawable.at_queue_music), stringResource(R.string.queue), ui.onQueue, size = 44.dp, iconSize = 24.dp, container = key, content = onAccent)
                Spacer(Modifier.width(12.dp))
                Btn(at(R.drawable.at_more_vert), stringResource(R.string.more_options), ui.onMore, size = 44.dp, iconSize = 24.dp, container = key, content = onAccent)
            }
            ArtBox(
                ui, Modifier.weight(1f), hPad = 60.dp,
                shape = if (ui.useOriginalArtwork) ui.artShape else KozuCookies.Flower8, original = false,
            )
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(text = ui.title,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (ui.compact) 34.sp else 52.sp,
                    lineHeight = if (ui.compact) 38.sp else 56.sp,
                    color = accent,
                    maxLines = if (ui.compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = ui.artist.uppercase(),
                        fontSize = 15.sp,
                        letterSpacing = 3.sp,
                        color = accent.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onArtist),
                    )
                    if (ui.mapper.isNotBlank()) {
                        Text("  •  ", fontSize = 15.sp, color = accent.copy(alpha = 0.5f))
                        Text(text = ui.mapper,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = accent.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(8.dp)).bounceClick(pressedScale = 0.97f, onClick = ui.onMapper),
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                val haptics = rememberKozuHaptics()
                Surface(
                    modifier = Modifier.weight(1f).height(80.dp).bounceClick(pressedScale = 0.97f) {
                        haptics.tick()
                        ui.onPlayPause()
                    },
                    shape = RoundedCornerShape(40.dp), color = key, contentColor = onAccent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (ui.isPlaying) "PAUSE" else "PLAY", fontSize = 22.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Btn(at(R.drawable.at_skip_next), "Next", ui.onNext, size = 80.dp, iconSize = 34.dp, container = key, content = onAccent)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn(at(R.drawable.at_skip_previous), "Previous", ui.onPrevious, size = 80.dp, iconSize = 34.dp, container = key, content = onAccent)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    LineSeek(
                        fraction = seek.fraction, onScrub = seek.onScrub, onFinish = seek.onFinish,
                        height = 4.dp, active = accent, inactive = accent.copy(alpha = 0.3f), thumb = 8.dp,
                    )
                    SeekTimes(seek, accent.copy(alpha = 0.75f), hPad = 0.dp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                val soft = accent.copy(alpha = 0.15f)
                Btn(favoriteIcon(ui), "Favorite", ui.onFavorite, size = 47.dp, iconSize = 24.dp, container = soft, content = if (ui.isFavorite) colors.error else accent)
                Spacer(Modifier.width(24.dp))
                Btn(shuffleIcon(), "Shuffle", ui.onShuffle, size = 47.dp, iconSize = 24.dp, container = soft, content = if (ui.shuffle) colors.onPrimaryContainer else accent)
            }
        }
    }
}
