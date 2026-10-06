package com.ridesandshares.trip

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TripProgressJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(trip: TripProgress): String = json.encodeToString(Wire.serializer(), Wire.from(trip))

    fun decode(text: String): TripProgress? {
        val wire = try {
            json.decodeFromString(Wire.serializer(), text)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (wire.destination.isBlank() || wire.distanceMeters < 0 || wire.durationSeconds < 0) {
            return null
        }
        return wire.toProgress()
    }
}

@Serializable
private data class Wire(
    val destination: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
) {
    fun toProgress(): TripProgress = TripProgress(
        destination = destination.trim(),
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
    )

    companion object {
        fun from(trip: TripProgress): Wire = Wire(
            destination = trip.destination,
            distanceMeters = trip.distanceMeters,
            durationSeconds = trip.durationSeconds,
        )
    }
}
