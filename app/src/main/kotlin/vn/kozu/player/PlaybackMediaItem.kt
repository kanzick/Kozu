package vn.kozu.player

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import vn.kozu.domain.model.Beatmap

internal fun Beatmap.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(PlaybackController.KEY_ID, id)
        putString(PlaybackController.KEY_MAPPER, mapper)
        putString(PlaybackController.KEY_STATUS, status)
        putLong(PlaybackController.KEY_UPDATED, updatedAt)
        putIntArray(PlaybackController.KEY_MODES, modes.toIntArray())
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(thumbUrl.toUri())
        .setExtras(extras)
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri("${PlaybackController.SET_SCHEME}://$id")
        .setMediaMetadata(metadata)
        .build()
}

internal fun MediaItem.toBeatmap(): Beatmap? {
    val extras = mediaMetadata.extras ?: return null
    return Beatmap(
        id = extras.getLong(PlaybackController.KEY_ID),
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        mapper = extras.getString(PlaybackController.KEY_MAPPER).orEmpty(),
        status = extras.getString(PlaybackController.KEY_STATUS).orEmpty(),
        updatedAt = extras.getLong(PlaybackController.KEY_UPDATED),
        modes = extras.getIntArray(PlaybackController.KEY_MODES)?.toSet().orEmpty()
    )
}
