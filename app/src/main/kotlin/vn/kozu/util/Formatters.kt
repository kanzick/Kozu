package vn.kozu.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%d:%02d".format(Locale.US, totalSeconds / 60, totalSeconds % 60)
}


fun formatDuration(milliseconds: Long): String {
    val totalMinutes = (milliseconds / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}


fun dayKey(timeMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMillis))
