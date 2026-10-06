package com.ridesandshares.trip

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RouteLeg(
    val distanceMeters: Int,
    val durationSeconds: Int,
)

private val directionsJson = Json { ignoreUnknownKeys = true }

/**
 * Reads the first leg of a Google Directions API response.
 * Traffic duration wins when Google includes it.
 */
fun parseDirectionsRoute(body: String): RouteLeg? {
    val root = try {
        directionsJson.parseToJsonElement(body).jsonObject
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (root["status"]?.jsonPrimitive?.contentOrNull != "OK") return null
    val leg = root["routes"]?.jsonArray?.firstOrNull()?.jsonObject
        ?.get("legs")?.jsonArray?.firstOrNull()?.jsonObject
        ?: return null
    val distance = leg["distance"]?.jsonObject?.get("value")?.jsonPrimitive?.intOrNull ?: return null
    val duration = leg["duration_in_traffic"]?.jsonObject?.get("value")?.jsonPrimitive?.intOrNull
        ?: leg["duration"]?.jsonObject?.get("value")?.jsonPrimitive?.intOrNull
        ?: return null
    if (distance < 0 || duration < 0) return null
    return RouteLeg(distanceMeters = distance, durationSeconds = duration)
}

fun directionsErrorMessage(body: String): String {
    val root = try {
        directionsJson.parseToJsonElement(body).jsonObject
    } catch (_: IllegalArgumentException) {
        return "Google Maps did not return a route"
    }
    val status = root["status"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
    val detail = root["error_message"]?.jsonPrimitive?.contentOrNull
    return if (detail.isNullOrBlank()) "Google Maps route failed ($status)" else detail
}
