package vn.kozu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap

/**
 * Standard list row displaying track artwork, title, artist, download status badge, favorite, and overflow menu.
 */
@Composable
fun TrackRow(
    beatmap: Beatmap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    trailing: (@Composable () -> Unit)? = null
) {
    val nowPlaying = LocalNowPlaying.current
    val favorites = LocalFavoriteIds.current
    val actions = LocalTrackActions.current
    val downloads = LocalDownloads.current
    val isCurrent = nowPlaying.beatmapId == beatmap.id
    val isFavorite = beatmap.id in favorites

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.975f, onClick = onClick),
        shape = shape,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else containerColor
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(56.dp)) {
                BeatmapArt(beatmap, Modifier.size(56.dp), shape = RoundedCornerShape(18.dp))
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerBars(
                            isPlaying = nowPlaying.isPlaying,
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = beatmap.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = beatmap.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DownloadBadge(
                    isDownloaded = beatmap.id in downloads.downloaded,
                    isOutdated = beatmap.id in downloads.outdated,
                    status = downloads.active[beatmap.id],
                    mapper = beatmap.mapper
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { actions.toggleFavorite(beatmap) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from liked" else "Add to liked",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { actions.openMenu(beatmap) }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Badge showing offline download status or mapper information */
@Composable
private fun DownloadBadge(
    isDownloaded: Boolean,
    isOutdated: Boolean,
    status: DownloadStatus?,
    mapper: String
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            status is DownloadStatus.Running -> {
                if (status.progress < 0f) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("Downloading", style = MaterialTheme.typography.labelSmall, color = muted)
            }
            status is DownloadStatus.Queued -> {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("Queued", style = MaterialTheme.typography.labelSmall, color = muted)
            }
            status is DownloadStatus.Failed -> {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(6.dp))
                Text("Download failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            isDownloaded -> {
                Icon(
                    Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isOutdated) "Offline · update available" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            mapper.isNotBlank() -> Text(
                text = "Mapped by $mapper",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
