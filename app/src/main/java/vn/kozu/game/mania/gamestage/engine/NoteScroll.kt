package vn.kozu.game.mania.gamestage.engine

/**
 * Cơ chế tốc độ rơi kiểu Project Sekai: tốc độ là một hằng số do người chơi chọn,
 * KHÔNG phụ thuộc BPM hay thay đổi SV trong chart. Tốc độ càng lớn nốt càng rơi nhanh.
 *
 * Thời gian một nốt rơi từ mép trên màn hình xuống vạch phán định:
 *     travelMs = BASE_TRAVEL_MS / speed
 *
 * BASE_TRAVEL_MS = 6000 nghĩa là speed 10 -> 600ms, speed 15 -> 400ms, speed 1 -> 6s, speed 0.1 -> 60s.
 * Đây là hằng số tự chọn (không phải số đo từ Project Sekai), chỉnh ở đây nếu muốn đổi cảm giác tốc độ.
 */
object NoteScroll {
    const val MIN_SPEED = 0.1
    const val MAX_SPEED = 15.0
    const val BASE_TRAVEL_MS = 6000.0

    fun clampSpeed(speed: Double): Double = speed.coerceIn(MIN_SPEED, MAX_SPEED)

    fun travelTimeMs(speed: Double): Double = BASE_TRAVEL_MS / clampSpeed(speed)

    /**
     * Toạ độ y của một sự kiện ở thời điểm [eventTimeMs] khi thời gian hiện tại là [nowMs].
     * y = 0 tại mép trên, y = [judgeY] tại vạch phán định, tăng dần khi nốt rơi xuống.
     * Hàm tuyến tính theo thời gian nên chuyển động đều, không phụ thuộc số khung hình.
     */
    fun yAt(eventTimeMs: Double, nowMs: Double, travelMs: Double, judgeY: Float): Float =
        judgeY - ((eventTimeMs - nowMs) / travelMs * judgeY).toFloat()
}
