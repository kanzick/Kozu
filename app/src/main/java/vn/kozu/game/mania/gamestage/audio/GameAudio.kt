package vn.kozu.game.mania.gamestage.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import vn.kozu.game.mania.gamestage.engine.AudioAnchor
import java.io.File

/**
 * Phát nhạc bài và cung cấp [AudioAnchor] chính xác cho đồng hồ game.
 *
 * Mọi thao tác MediaPlayer chạy trên một luồng riêng, kể cả việc hỏi MediaPlayer.getTimestamp()
 * (một lời gọi binder) mỗi ~10ms, nên luồng giao diện không bao giờ bị chặn. Kết quả được công bố
 * qua trường [anchor] (volatile) để khung hình đọc.
 *
 * getTimestamp() trả về cặp (media time, System.nanoTime) lấy từ AudioTrack nên phản ánh âm thanh đã
 * thật sự ra loa, không phải vị trí đọc file. Độ trễ của tai nghe Bluetooth thì không có trong đó:
 * dùng cài đặt offset để bù.
 */
class GameAudio {
    private val thread = HandlerThread("GameAudio").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null // chỉ đụng trên luồng handler
    private var startedNano = 0L
    private var gotTimestamp = false
    private var polling = false

    @Volatile var anchor: AudioAnchor? = null
        private set
    @Volatile var failure: String? = null
        private set
    @Volatile var completed: Boolean = false
        private set

    /** Nạp nhạc (không chặn). [onDone] chạy trên main thread; tham số = thông báo lỗi hoặc null. */
    fun prepare(file: File, volume: Float, onDone: (String?) -> Unit) {
        handler.post {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                mp.setVolume(volume, volume)
                mp.setOnCompletionListener { completed = true }
                mp.setOnErrorListener { _, what, extra ->
                    failure = "MediaPlayer error (what=$what, extra=$extra)"
                    true
                }
                player = mp
                main.post { onDone(null) }
            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                main.post { onDone(msg) }
            }
        }
    }

    /** Bắt đầu phát từ đầu bài. Anchor đầu tiên xuất hiện khi âm thanh thật sự bắt đầu ra loa. */
    fun start() {
        handler.post {
            val mp = player ?: return@post
            try {
                mp.seekTo(0)
                startedNano = System.nanoTime()
                mp.start()
                if (!polling) { polling = true; handler.post(poll) }
            } catch (e: Exception) {
                failure = e.message ?: e.javaClass.simpleName
            }
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            val mp = player
            if (mp == null) { polling = false; return }
            try {
                val ts = mp.timestamp
                if (ts != null && ts.mediaClockRate > 0f) {
                    gotTimestamp = true
                    anchor = AudioAnchor(
                        mediaTimeMs = ts.anchorMediaTimeUs / 1000.0,
                        systemNano = ts.anchorSystemNanoTime,
                        rate = ts.mediaClockRate.toDouble(),
                        precise = true,
                    )
                } else if (!gotTimestamp && System.nanoTime() - startedNano > 600_000_000L && mp.isPlaying) {
                    // máy không hỗ trợ getTimestamp: dùng vị trí thô, đồng hồ sẽ tin nó ít hơn
                    anchor = AudioAnchor(mp.currentPosition.toDouble(), System.nanoTime(), 1.0, precise = false)
                }
            } catch (e: Exception) {
                // IllegalStateException khi đang đổi trạng thái: bỏ qua lần này
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    fun release() {
        handler.post {
            polling = false
            handler.removeCallbacksAndMessages(null)
            player?.let { runCatching { it.setOnCompletionListener(null); it.setOnErrorListener(null); it.release() } }
            player = null
            thread.quitSafely()
        }
    }

    private companion object { const val POLL_MS = 10L }
}
