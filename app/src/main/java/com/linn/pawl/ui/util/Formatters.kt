package com.linn.pawl.ui.util

import java.util.Locale

fun formatPathForDisplay(path: String): String {
    return path.replace("/", "/\u200B")
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "—"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatDateTime(timestampMs: Long): String {
    if (timestampMs <= 0) return "—"
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US)
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(java.time.Instant.ofEpochMilli(timestampMs))
}

fun formatAspectRatio(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "—"
    val divisor = gcd(width, height)
    return "${width / divisor}:${height / divisor}"
}

private fun gcd(a: Int, b: Int): Int {
    var x = kotlin.math.abs(a)
    var y = kotlin.math.abs(b)
    while (y != 0) {
        val remainder = x % y
        x = y
        y = remainder
    }
    return x
}
