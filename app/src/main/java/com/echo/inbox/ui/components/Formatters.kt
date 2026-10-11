package com.echo.inbox.ui.components

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

fun fullDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(timestamp))

fun formatTime(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(timestamp))

fun formatDateHeader(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val messageCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = now }

    val sameYear = messageCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR)
    val dayOfYearDiff = nowCal.get(java.util.Calendar.DAY_OF_YEAR) - messageCal.get(java.util.Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayOfYearDiff == 0 -> "Today"
        sameYear && dayOfYearDiff == 1 -> "Yesterday"
        sameYear && dayOfYearDiff in 2..6 -> {
            java.text.SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(timestamp))
        }
        sameYear -> {
            java.text.SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            java.text.SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1_024) return "$bytes B"
    val kb = bytes / 1_024.0
    if (kb < 1_024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1_024.0
    if (mb < 1_024) return String.format(Locale.getDefault(), "%.1f MB", mb)
    return String.format(Locale.getDefault(), "%.2f GB", mb / 1_024.0)
}
