package vn.kozu.domain.model

import vn.kozu.data.local.History
import vn.kozu.util.dayKey
import java.util.Calendar


data class ListeningStats(
    val totalMs: Long = 0L,
    val plays: Int = 0,
    val avgPerDayMs: Long = 0L,
    val topTrack: Beatmap? = null,
    val topTrackPlays: Int = 0,

    val dayMs: List<Long> = List(7) { 0L },

    val todayIndex: Int = 0
) {
    companion object {
        fun from(history: History, now: Long = System.currentTimeMillis()): ListeningStats {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val todayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, -todayIndex)
            val weekStart = cal.timeInMillis

            val dayMs = List(7) { index ->
                val day = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
                history.dailyMs[dayKey(day.timeInMillis)] ?: 0L
            }
            val total = dayMs.sum()
            val weekEvents = history.events.filter { it.at >= weekStart }
            val top = weekEvents.groupingBy { it.beatmapId }.eachCount().maxByOrNull { it.value }

            return ListeningStats(
                totalMs = total,
                plays = weekEvents.size,
                avgPerDayMs = total / (todayIndex + 1),
                topTrack = top?.let { history.tracks[it.key] },
                topTrackPlays = top?.value ?: 0,
                dayMs = dayMs,
                todayIndex = todayIndex
            )
        }
    }
}
