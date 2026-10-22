package vn.kozu.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapCover
import vn.kozu.ui.components.formatTime

@Composable
internal fun MiniPlayer(
    beatmap: Beatmap,
    isPlaying: Boolean,
    currentPositionState: State<Long>,
    durationState: State<Long>,
    progressColor: Color,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    val currentPosition by currentPositionState
    val duration by durationState

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapCover(beatmap = beatmap, modifier = Modifier.size(46.dp))
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(beatmap.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        beatmap.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous song",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(29.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next song",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Slider(
                value = currentPosition.coerceIn(0L, duration.coerceAtLeast(0L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = progressColor,
                    activeTrackColor = progressColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                    disabledThumbColor = progressColor.copy(alpha = 0.45f),
                    disabledActiveTrackColor = progressColor.copy(alpha = 0.45f)
                )
            )
        }
    }
}

@Composable
internal fun PlayerScreen(
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
    val currentPosition by currentPositionState
    val duration by durationState
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.97f,
        animationSpec = tween(durationMillis = 320),
        label = "playerArtworkScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        progressColor.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp).clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "KOZU PLAYER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(27.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                },
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            BeatmapCover(beatmap = beatmap, modifier = Modifier.fillMaxSize())
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beatmap.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = beatmap.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Slider(
            value = currentPosition.coerceIn(0L, duration.coerceAtLeast(0L)).toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = duration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = progressColor,
                activeTrackColor = progressColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledThumbColor = progressColor.copy(alpha = 0.45f),
                disabledActiveTrackColor = progressColor.copy(alpha = 0.45f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous song",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clickable(onClick = onPlayPause),
                shape = RoundedCornerShape(27.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next song",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "osu! beatmap collection · demo playback",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
    }
}
