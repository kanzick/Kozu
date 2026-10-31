package vn.kozu.game.mania.gamestage.engine

/**
 * Mốc đồng bộ giữa thời gian của file nhạc và đồng hồ hệ thống (System.nanoTime).
 * Với MediaPlayer.getTimestamp() đây chính là (anchorMediaTime, anchorSystemNanoTime, mediaClockRate).
 */
class AudioAnchor(
    val mediaTimeMs: Double,
    val systemNano: Long,
    val rate: Double = 1.0,
    /** false = chỉ có getCurrentPosition() thô (nhảy theo từng cục), nên được tin ít hơn. */
    val precise: Boolean = true,
) {
    /** Thời gian trong bài nhạc tại thời điểm hệ thống [nano]. */
    fun mediaTimeAt(nano: Long): Double = mediaTimeMs + (nano - systemNano) / 1_000_000.0 * rate
}

/**
 * Đồng hồ trò chơi. Mục tiêu: chuyển động mượt (không khựng) và khớp với âm thanh thật.
 *
 *  - Thời gian chạy theo System.nanoTime (mượt, độ phân giải ns) chứ KHÔNG đọc thẳng vị trí MediaPlayer,
 *    vì getCurrentPosition() chỉ cập nhật theo từng cục vài chục ms.
 *  - Âm thanh là "slave": mỗi khi có [AudioAnchor] mới, đồng hồ được kéo dần về đúng mốc đó
 *    với bước hiệu chỉnh giới hạn để timestamp bất thường không làm thời gian chart nhảy lùi.
 *  - Trước khi nhạc thật sự phát (thời gian âm = lead-in), đồng hồ chạy tự do từ [start].
 *    Nếu tới t = 0 mà nhạc chưa phát thì giữ ở 0 cho đến khi anchor đầu tiên tới (hoặc [giveUpOnAudio]),
 *    nhờ vậy không bao giờ bị lệch khoảng độ trễ khởi động của MediaPlayer.
 *
 * Tất cả hàm gọi trên một luồng (main thread).
 */
class GameClock(private val nanoSource: () -> Long = System::nanoTime) {
    private var baseNano = 0L
    private var baseTimeMs = 0.0
    private var started = false
    private var paused = false
    private var pausedTimeMs = 0.0
    private var audioLocked = false
    private var audioMode = true
    private var audioStartRequested = false

    val isAudioLocked: Boolean get() = audioLocked
    val isWaitingForAudio: Boolean get() = started && audioMode && !audioLocked && rawTime(nanoSource()) > 0.0

    /** Bắt đầu chạy từ thời điểm [startTimeMs] (thường âm = đoạn dạo đầu). */
    fun start(startTimeMs: Double, withAudio: Boolean = true) {
        baseNano = nanoSource()
        baseTimeMs = startTimeMs
        started = true
        audioMode = withAudio
        audioLocked = false
        audioStartRequested = false
    }

    private fun rawTime(nano: Long): Double = baseTimeMs + (nano - baseNano) / 1_000_000.0

    /** Thời gian bài nhạc (ms) tại thời điểm hệ thống [nano]; dùng để quy đổi timestamp của sự kiện chạm. */
    fun timeAt(nano: Long): Double {
        if (!started) return baseTimeMs
        if (paused) return pausedTimeMs
        val raw = rawTime(nano)
        return if (audioMode && !audioLocked && raw > 0.0) 0.0 else raw
    }

    fun now(): Double = timeAt(nanoSource())

    /** true đúng một lần, khi đã tới lúc phải gọi start() của trình phát nhạc. */
    fun shouldStartAudio(leadMs: Double = 0.0): Boolean {
        if (!started || !audioMode || audioStartRequested) return false
        if (rawTime(nanoSource()) < -leadMs) return false
        audioStartRequested = true
        return true
    }

    /** Nhạc không phát được: bỏ chế độ chờ, đồng hồ tiếp tục chạy tự do từ thời điểm hiện tại. */
    fun giveUpOnAudio() {
        if (!started || audioLocked) return
        val nano = nanoSource()
        val current = timeAt(nano)
        baseNano = nano
        baseTimeMs = current
        audioMode = false
    }

    fun pause() {
        if (!started || paused) return
        pausedTimeMs = timeAt(nanoSource())
        paused = true
    }

    fun resume() {
        if (!started || !paused) return
        baseTimeMs = pausedTimeMs
        baseNano = nanoSource()
        paused = false
    }

    /** Nạp mốc đồng bộ mới từ trình phát nhạc. */
    fun onAudioAnchor(anchor: AudioAnchor) {
        if (!started || !audioMode) return
        val nano = nanoSource()
        val desired = anchor.mediaTimeAt(nano)
        if (!audioLocked) {
            val raw = rawTime(nano)
            audioLocked = true
            if (raw > 0.0) {
                // đang bị giữ ở 0: nhảy thẳng tới thời gian thật của nhạc (liên tục với giá trị đang hiển thị = 0)
                baseNano = nano
                baseTimeMs = desired
                return
            }
        }
        val error = desired - rawTime(nano)
        val gain = if (anchor.precise) GAIN_PRECISE else GAIN_COARSE
        val limit = if (anchor.precise) MAX_STEP_PRECISE_MS else MAX_STEP_COARSE_MS
        val shift = (error * gain).coerceIn(-limit, limit)
        baseTimeMs += shift
    }

    companion object {
        private const val GAIN_PRECISE = 0.15
        private const val GAIN_COARSE = 0.03
        private const val MAX_STEP_PRECISE_MS = 1.5
        private const val MAX_STEP_COARSE_MS = 0.7
    }
}
