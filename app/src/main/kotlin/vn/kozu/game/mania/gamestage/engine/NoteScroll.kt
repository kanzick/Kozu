package vn.kozu.game.mania.gamestage.engine

object NoteScroll {
    const val MIN_SPEED = 0.1
    const val MAX_SPEED = 15.0
    const val BASE_TRAVEL_MS = 6000.0

    fun clampSpeed(speed: Double): Double = speed.coerceIn(MIN_SPEED, MAX_SPEED)

    fun travelTimeMs(speed: Double): Double = BASE_TRAVEL_MS / clampSpeed(speed)

    fun yAt(eventTimeMs: Double, nowMs: Double, travelMs: Double, judgeY: Float): Float =
        judgeY - ((eventTimeMs - nowMs) / travelMs * judgeY).toFloat()
}
