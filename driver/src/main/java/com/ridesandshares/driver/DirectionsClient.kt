package com.ridesandshares.driver

import com.ridesandshares.trip.RouteLeg
import com.ridesandshares.trip.directionsErrorMessage
import com.ridesandshares.trip.parseDirectionsRoute
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DirectionsClient(private val apiKey: String) {
    fun route(originLat: Double, originLng: Double, destination: String): RouteLeg {
        if (apiKey.isBlank()) {
            error("Add MAPS_API_KEY to local.properties, then rebuild the driver app.")
        }
        val url = buildString {
            append("https://maps.googleapis.com/maps/api/directions/json")
            append("?origin=").append(originLat).append(',').append(originLng)
            append("&destination=").append(URLEncoder.encode(destination, Charsets.UTF_8.name()))
            append("&mode=driving&departure_time=now")
            append("&key=").append(URLEncoder.encode(apiKey, Charsets.UTF_8.name()))
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        val body = try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
        return parseDirectionsRoute(body) ?: error(directionsErrorMessage(body))
    }
}
