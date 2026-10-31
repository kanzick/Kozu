package vn.kozu.game.mania.gamestage.engine

/**
 * Project Sekai-style scroll speed: the player chooses a constant speed,
 * independent of BPM or chart SV changes. Higher speeds make notes fall faster.
 *
 * Time for a note to fall from the top of the screen to the judgement line:
 *     travelMs = BASE_TRAVEL_MS / speed
 *
 * BASE_TRAVEL_MS = 6000 means speed 10 -> 600ms, speed 15 -> 400ms, speed 1 -> 6s, speed 0.1 -> 60s.
 * This is a chosen constant, not a measurement from Project Sekai; adjust it here to change the speed feel.
 */
object NoteScroll {
    const val MIN_SPEED = 0.1
    const val MAX_SPEED = 15.0
    const val BASE_TRAVEL_MS = 6000.0

    fun clampSpeed(speed: Double): Double = speed.coerceIn(MIN_SPEED, MAX_SPEED)

    fun travelTimeMs(speed: Double): Double = BASE_TRAVEL_MS / clampSpeed(speed)

    /**
     * The y-coordinate of an event at [eventTimeMs] when the current time is [nowMs].
     * y = 0 at the top edge, y = [judgeY] at the judgement line, increasing as the note falls.
     * This is linear over time, so movement is uniform and independent of the frame rate.
     */
    fun yAt(eventTimeMs: Double, nowMs: Double, travelMs: Double, judgeY: Float): Float =
        judgeY - ((eventTimeMs - nowMs) / travelMs * judgeY).toFloat()
}
