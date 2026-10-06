package com.ridesandshares.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectionsRouteTest {
    @Test
    fun prefersTrafficDuration() {
        val leg = parseDirectionsRoute(
            """
            {
              "status": "OK",
              "routes": [{
                "legs": [{
                  "distance": {"value": 4828, "text": "3.0 mi"},
                  "duration": {"value": 540, "text": "9 mins"},
                  "duration_in_traffic": {"value": 780, "text": "13 mins"}
                }]
              }]
            }
            """.trimIndent(),
        )
        assertEquals(4828, leg?.distanceMeters)
        assertEquals(780, leg?.durationSeconds)
    }

    @Test
    fun reportsARejectedKey() {
        val body = """{"status":"REQUEST_DENIED","error_message":"The provided API key is invalid."}"""
        assertNull(parseDirectionsRoute(body))
        assertEquals("The provided API key is invalid.", directionsErrorMessage(body))
    }
}
