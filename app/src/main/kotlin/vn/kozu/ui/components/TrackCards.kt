package vn.kozu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.Beatmap

/**
 * Cover card component displaying artwork, rank badge, optional equalizer overlay, and track details.
 */
@Composable
fun CoverCard(
    beatmap: Beatmap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
    shape: Shape = RoundedCornerShape(30.dp),
    large: Boolean = false,
    badge: String? = null,
    rank: Int? = null,
    showPlay: Boolean = false
) {
    val nowPlaying = LocalNowPlaying.current
    val isCurrent = nowPlaying.beatmapId == beatmap.id

    Column(
        modifier = modifier
            .width(size)
            .bounceClick(onClick = onClick)
    ) {
        Box {
            BeatmapArt(beatmap, Modifier.size(size), shape = shape, large = large)
            if (rank != null) {
                HitCircle(number = rank, modifier = Modifier.padding(10.dp))
            }
            if (!badge.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
                    Text(
                        text = badge.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (isCurrent) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        EqualizerBars(
                            isPlaying = nowPlaying.isPlaying,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else if (showPlay) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play ${beatmap.title}",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = beatmap.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = beatmap.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/** Horizontal track pill component for compact lists */
@Composable
fun TrackPill(
    beatmap: Beatmap,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .bounceClick(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeatmapArt(beatmap, Modifier.size(44.dp), shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.width(132.dp)) {
                Text(
                    text = beatmap.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = beatmap.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
