package com.ridesandshares.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripProgressTest {
    @Test
    fun roundTripsTheWireFormat() {
        val encoded = TripProgressJson.encode(
            TripProgress("Pike Place Market", 4828, 780, receivedAtEpochMs = 99),
        )
        val decoded = TripProgressJson.decode(encoded)
        assertEquals("Pike Place Market", decoded?.destination)
        assertEquals(4828, decoded?.distanceMeters)
        assertEquals(780, decoded?.durationSeconds)
        assertEquals(0L, decoded?.receivedAtEpochMs)
    }

    @Test
    fun rejectsABlankDestination() {
        assertNull(TripProgressJson.decode("""{"destination":"  ","distanceMeters":1,"durationSeconds":1}"""))
    }

    @Test
    fun freshnessUsesTheTabletClock() {
        val trip = TripProgress("Home", 100, 60).receivedNow(1_000)
        assertTrue(trip.isFresh(1_000 + 10_000))
        assertFalse(trip.isFresh(1_000 + TripLink.STALE_AFTER_MS + 1))
    }

    @Test
    fun formatsMilesAndMinutes() {
        assertEquals("3.0 mi", formatTripDistance(4828))
        assertEquals("328 ft", formatTripDistance(100))
        assertEquals("13 min", formatTripDuration(780))
        assertEquals("Under 1 min", formatTripDuration(40))
        assertEquals("1 hr 5 min", formatTripDuration(3900))
    }

    @Test
    fun parsesATabletAddress() {
        assertEquals("192.168.1.20" to TripLink.PORT, parseTabletAddress("192.168.1.20"))
        assertEquals("192.168.1.20" to 9000, parseTabletAddress("192.168.1.20:9000"))
        assertNull(parseTabletAddress("   "))
    }
}
