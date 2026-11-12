package vn.kozu.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vn.kozu.R

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

/** Localized "1h 5m" style text; the plain overload above stays for callers that have no Context. */
fun formatDuration(
    context: Context,
    milliseconds: Long,
): String {
    val totalMinutes = (milliseconds / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        context.getString(R.string.duration_hours_minutes, hours, minutes)
    } else {
        context.getString(R.string.duration_minutes, minutes)
    }
}

fun dayKey(timeMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMillis))
