package vn.kozu.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.model.Beatmap
import vn.kozu.ui.components.BeatmapCover
import vn.kozu.ui.components.formatTime

@Composable
fun MiniPlayer(
    beatmap: Beatmap,
    isPlaying: Boolean,
    currentPositionState: State<Long>,
    durationState: State<Long>,
    progressColor: Color,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPosition by currentPositionState
    val duration by durationState

    val progressFraction = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = progressColor.copy(alpha = 0.2f),
                spotColor = progressColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapCover(
                    beatmap = beatmap,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = beatmap.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = beatmap.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPrevious, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPause),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(3.dp)
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
fun PlayerScreen(
    beatmap: Beatmap,
    isPlaying: Boolean,
    isFavorite: Boolean,
    currentPositionState: State<Long>,
    durationState: State<Long>,
    progressColor: Color,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit
) {
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var showLyrics by remember { mutableStateOf(false) }

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playerArtworkScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "beatPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isPlaying) 0.35f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        progressColor.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM BEATMAPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Speaker,
                            contentDescription = "Audio output",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.08f)
                    .clip(RoundedCornerShape(38.dp))
                    .background(progressColor.copy(alpha = pulseAlpha))
            )

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                BeatmapCover(
                    beatmap = beatmap,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(32.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beatmap.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        lineHeight = 28.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${beatmap.artist} • ${beatmap.mapper.ifBlank { "osu! mapper" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (showLyrics) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { showLyrics = !showLyrics }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = "Lyrics / Visualizer",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "48.0 kHz • 320 kbps • OSU AUDIO • ${beatmap.bpm} BPM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        PlayerProgress(
            currentPositionState = currentPositionState,
            durationState = durationState,
            progressColor = progressColor,
            onSeek = onSeek
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(onClick = onPrevious),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous song",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1.3f)
                    .height(78.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable(onClick = onPlayPause),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(onClick = onNext),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next song",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isShuffle = !isShuffle }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { repeatMode = (repeatMode + 1) % 3 }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            2 -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val heartScale by animateFloatAsState(
                    targetValue = if (isFavorite) 1.25f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "playerHeartScale"
                )
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.scale(heartScale)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgress(
    currentPositionState: State<Long>,
    durationState: State<Long>,
    progressColor: Color,
    onSeek: (Long) -> Unit
) {
    val currentPosition by currentPositionState
    val duration by durationState
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = currentPosition.coerceIn(0L, duration.coerceAtLeast(0L)).toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "playerProgress"
    )
    val progress = if (isSeeking) seekPosition else animatedProgress

    val maxDuration = duration.coerceAtLeast(1L).toFloat()

    Slider(
        value = progress,
        onValueChange = {
            isSeeking = true
            seekPosition = it
            onSeek(it.toLong())
        },
        valueRange = 0f..maxDuration,
        onValueChangeFinished = { isSeeking = false },
        enabled = duration > 0L,
        colors = SliderDefaults.colors(
            thumbColor = progressColor,
            activeTrackColor = progressColor,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledThumbColor = progressColor.copy(alpha = 0.45f),
            disabledActiveTrackColor = progressColor.copy(alpha = 0.45f)
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(currentPosition),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
