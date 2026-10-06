package com.ridesandshares.passenger.demo

import com.ridesandshares.trip.TripProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideDemoTest {
    @Test
    fun countdownTicksUntilARideIsSent() {
        val ride = RideDemo()
        assertFalse(ride.rideActive)
        assertEquals("Waiting for the driver", ride.statusLabel())
        assertEquals("12:00", ride.countdownLabel())
        assertNull(ride.milesLabel())
        ride.tick()
        assertEquals("11:59", ride.countdownLabel())
        assertFalse(ride.rideActive)
    }

    @Test
    fun sendingARideMovesTheCarAndResetsTheCountdown() {
        val ride = RideDemo()
        ride.tick()
        val waiting = ride.progress
        ride.pair()
        ride.send("Pike Place Market", 4.2, 12)
        assertTrue(ride.paired)
        assertTrue(ride.rideActive)
        assertEquals("Ride in progress", ride.statusLabel())
        assertEquals("12:00", ride.countdownLabel())
        assertEquals("4.2 mi left", ride.milesLabel())
        assertEquals("to Pike Place Market", ride.destinationLabel())
        assertTrue(ride.progress > waiting)
        val started = ride.progress
        ride.tick()
        assertEquals("11:59", ride.countdownLabel())
        assertTrue(ride.progress > started)
    }

    @Test
    fun aDriverPacketFillsTheSameRide() {
        val ride = RideDemo()
        ride.applyProgress(
            TripProgress(
                destination = "Kerry Park",
                distanceMeters = 10_944,
                durationSeconds = 1_140,
            ),
        )
        assertEquals("6.8 mi left", ride.milesLabel())
        assertEquals("19:00", ride.countdownLabel())
        assertEquals("to Kerry Park", ride.destinationLabel())
    }
}

class AdminMockTest {
    @Test
    fun analyticsCoverTheFiveSampleBusinesses() {
        assertEquals(
            listOf(
                "Harbor & Rye",
                "Northline Eats",
                "Lumen Hotel",
                "Pike Street Books",
                "Cedar Dental",
            ),
            AdminMock.campaigns.map { it.businessName },
        )
        assertEquals("48,291", AdminMock.IMPRESSIONS_TODAY)
        assertEquals("12,480", AdminMock.campaigns.first().impressionLabel())
        assertTrue(AdminMock.tablets.any { !it.online })
        assertTrue(AdminMock.rides.any { it.status == "Arrived" })
    }
}
