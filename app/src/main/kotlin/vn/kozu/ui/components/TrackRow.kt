package vn.kozu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kozu.data.download.DownloadStatus
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.R
import vn.kozu.ui.theme.isCompactLayout

@Composable
fun TrackRow(
    beatmap: Beatmap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    compact: Boolean = isCompactLayout,
    trailing: (@Composable () -> Unit)? = null,
    details: (@Composable () -> Unit)? = null,
    showDownloadBadge: Boolean = true,
    subtitleSuffix: String? = null,
    onArtworkClick: (() -> Unit)? = null,
) {
    val nowPlaying = LocalNowPlaying.current
    val favorites = LocalFavoriteIds.current
    val actions = LocalTrackActions.current
    val downloads = LocalDownloads.current
    val isCurrent = nowPlaying.beatmapId == beatmap.id
    val isFavorite = beatmap.id in favorites
    val artworkSize = if (compact) 44.dp else 56.dp
    val artworkShape = if (compact) RoundedCornerShape(14.dp) else RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.975f, onClick = onClick),
        shape = shape,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else containerColor
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (compact) 8.dp else 10.dp,
                end = 4.dp,
                top = if (compact) 6.dp else 10.dp,
                bottom = if (compact) 6.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(artworkSize)) {
                BeatmapArt(beatmap, Modifier.size(artworkSize), shape = artworkShape)
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(artworkSize)
                            .background(Color.Black.copy(alpha = 0.38f), artworkShape),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerBars(
                            isPlaying = nowPlaying.isPlaying,
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onArtworkClick != null) {
                    Surface(
                        onClick = onArtworkClick,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play beatmap",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)) {
                Text(text = beatmap.displayTitle(LocalAppSettings.current.preferUnicode),
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = beatmap.displayArtist(LocalAppSettings.current.preferUnicode) + subtitleSuffix.orEmpty(),
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showDownloadBadge && !compact) {
                    DownloadBadge(
                        isDownloaded = beatmap.id in downloads.downloaded,
                        isOutdated = beatmap.id in downloads.outdated,
                        status = downloads.active[beatmap.id],
                        mapper = if (LocalAppSettings.current.showMapper) beatmap.mapper else ""
                    )
                } else if (showDownloadBadge && (downloads.active[beatmap.id] != null || beatmap.id in downloads.downloaded)) {
                    DownloadBadge(
                        isDownloaded = beatmap.id in downloads.downloaded,
                        isOutdated = beatmap.id in downloads.outdated,
                        status = downloads.active[beatmap.id],
                        mapper = ""
                    )
                }
                details?.invoke()
            }
            if (trailing != null) {
                trailing()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    if (!compact) {
                        IconButton(onClick = { actions.toggleFavorite(beatmap) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from liked" else "Add to liked",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { actions.openMenu(beatmap) }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

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
                Text(stringResource(R.string.downloading), style = MaterialTheme.typography.labelSmall, color = muted)
            }
            status is DownloadStatus.Queued -> {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.queued), style = MaterialTheme.typography.labelSmall, color = muted)
            }
            status is DownloadStatus.Failed -> {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.download_failed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            isDownloaded -> {
                Icon(
                    Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isOutdated) "Offline · update available" else stringResource(R.string.offline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            mapper.isNotBlank() -> Text(text = stringResource(R.string.mapped_by, mapper),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
