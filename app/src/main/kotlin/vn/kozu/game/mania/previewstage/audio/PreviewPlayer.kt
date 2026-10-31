package vn.kozu.game.mania.previewstage.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.io.File

/** Phát đoạn preview của bài mà không chặn luồng giao diện. */
class PreviewPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioThread = HandlerThread("PreviewPlayer").apply { start() }
    private val audioHandler = Handler(audioThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Chỉ đọc/ghi trên audioHandler.
    private var player: MediaPlayer? = null

    /**
     * @param previewTimeMs PreviewTime trong .osu; < 0 (không có) thì bắt đầu ở 40% độ dài bài.
     */
    fun play(audio: File, previewTimeMs: Int, onError: (String) -> Unit = {}) {
        playSource(audio.name, previewTimeMs, onError) { player ->
            player.setDataSource(audio.absolutePath)
        }
    }

    fun playUrl(url: String, headers: Map<String, String> = emptyMap(), onError: (String) -> Unit = {}) {
        playSource(url, 0, onError) { player ->
            player.setDataSource(appContext, Uri.parse(url), headers)
        }
    }

    private fun playSource(
        sourceName: String,
        previewTimeMs: Int,
        onError: (String) -> Unit,
        setDataSource: (MediaPlayer) -> Unit,
    ) {
        audioHandler.post {
            releaseCurrent()
            val mp = MediaPlayer()
            player = mp
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(mp)
                mp.setOnPreparedListener { p ->
                    if (player !== p) return@setOnPreparedListener
                    val duration = p.duration
                    val start = if (previewTimeMs in 0 until duration) previewTimeMs else (duration * 0.4).toInt()
                    p.setOnSeekCompleteListener { sp -> if (player === sp) sp.start() }
                    p.seekTo(start)
                }
                mp.setOnCompletionListener { p -> if (player === p) releaseCurrent() }
                mp.setOnErrorListener { p, what, extra ->
                    if (player === p) {
                        releaseCurrent()
                        mainHandler.post {
                            onError("MediaPlayer error (what=$what, extra=$extra) while playing $sourceName")
                        }
                    }
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                if (player === mp) releaseCurrent()
                val message = e.message ?: e.javaClass.simpleName
                mainHandler.post { onError(message) }
            }
        }
    }

    fun stop() {
        audioHandler.post { releaseCurrent() }
    }

    private fun releaseCurrent() {
        val mp = player ?: return
        player = null
        runCatching { mp.setOnPreparedListener(null); mp.setOnSeekCompleteListener(null) }
        runCatching { mp.setOnCompletionListener(null); mp.setOnErrorListener(null) }
        runCatching { mp.release() }
    }
}
