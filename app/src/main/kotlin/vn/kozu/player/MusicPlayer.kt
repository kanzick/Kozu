package vn.kozu.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import vn.kozu.model.Beatmap

class MusicPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val fallbackUri = "android.resource://${appContext.packageName}/raw/test"
                try {
                    player.setMediaItem(MediaItem.fromUri(fallbackUri))
                    player.prepare()
                    player.play()
                } catch (_: Exception) {}
            }
        })
    }

    fun playBeatmap(beatmap: Beatmap, fallbackResourceId: Int) {
        val streamUrl = beatmap.previewUrl
            ?: beatmap.beatmapsetId?.let { "https://b.ppy.sh/preview/$it.mp3" }

        if (!streamUrl.isNullOrBlank()) {
            player.setMediaItem(MediaItem.fromUri(streamUrl))
        } else {
            player.setMediaItem(
                MediaItem.fromUri(
                    "android.resource://${appContext.packageName}/$fallbackResourceId"
                )
            )
        }
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        player.release()
    }
}
