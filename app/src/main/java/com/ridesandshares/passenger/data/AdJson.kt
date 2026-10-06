package com.ridesandshares.passenger.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object AdJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<Advertisement> {
        return try {
            json.decodeFromString(text)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Advertisement catalog is not a JSON list", error)
        }
    }
}
