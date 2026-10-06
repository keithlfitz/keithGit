package com.ridesandshares.trip

import java.util.Locale
import kotlin.math.roundToInt

fun formatTripDistance(meters: Int): String {
    val miles = meters / 1609.344
    return if (miles >= 0.1) {
        String.format(Locale.US, "%.1f mi", miles)
    } else {
        "${(meters * 3.28084).roundToInt()} ft"
    }
}

fun formatTripDuration(seconds: Int): String {
    if (seconds < 60) return "Under 1 min"
    val minutes = (seconds + 30) / 60
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "$hours hr" else "$hours hr $remainder min"
}

fun parseTabletAddress(raw: String): Pair<String, Int>? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val colon = trimmed.lastIndexOf(':')
    if (colon <= 0 || colon == trimmed.lastIndex) return trimmed to TripLink.PORT
    val host = trimmed.substring(0, colon)
    val port = trimmed.substring(colon + 1).toIntOrNull() ?: return null
    if (host.isBlank() || port !in 1..65535) return null
    return host to port
}
