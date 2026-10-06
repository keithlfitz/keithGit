package com.ridesandshares.passenger.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ridesandshares.trip.TripProgress
import java.util.Locale

/**
 * On-device ride shared by the passenger slideshow and the driver-phone demo.
 * Nothing here talks to a server. Pairing is a stand-in for Bluetooth.
 */
class RideDemo {
    var paired by mutableStateOf(false)
        private set
    var destination by mutableStateOf("")
        private set
    var milesRemaining by mutableStateOf<Double?>(null)
        private set
    var remainingSeconds by mutableStateOf(PREVIEW_SECONDS)
        private set
    var rideActive by mutableStateOf(false)
        private set

    private var routeMiles = 0.0
    private var milesAtSend = 0.0
    private var arrivalTotalSeconds = PREVIEW_SECONDS

    val progress: Float
        get() {
            if (!rideActive || routeMiles <= 0.0) return 0.08f
            val remaining = milesRemaining ?: return 0.08f
            return (1.0 - remaining / routeMiles).toFloat().coerceIn(0.08f, 1f)
        }

    fun pair() {
        paired = true
    }

    fun send(destination: String, miles: Double, arrivalMinutes: Int) {
        val safeMiles = miles.coerceAtLeast(0.0)
        val seconds = (arrivalMinutes.coerceAtLeast(1) * 60)
        this.destination = destination.trim()
        milesAtSend = safeMiles
        milesRemaining = safeMiles
        routeMiles = if (safeMiles == 0.0) 1.0 else safeMiles / 0.55
        arrivalTotalSeconds = seconds
        remainingSeconds = seconds
        rideActive = true
        paired = true
    }

    fun applyProgress(trip: TripProgress) {
        val miles = trip.distanceMeters / 1609.344
        val minutes = ((trip.durationSeconds + 59) / 60).coerceAtLeast(1)
        send(trip.destination, miles, minutes)
    }

    fun tick() {
        if (remainingSeconds > 0) remainingSeconds -= 1
        if (!rideActive || arrivalTotalSeconds <= 0) return
        val fraction = remainingSeconds.toDouble() / arrivalTotalSeconds.toDouble()
        milesRemaining = milesAtSend * fraction
    }

    fun statusLabel(): String = when {
        !rideActive -> "Waiting for the driver"
        remainingSeconds <= 0 -> "Arrived"
        else -> "Ride in progress"
    }

    fun countdownLabel(): String = formatCountdown(remainingSeconds)

    fun milesLabel(): String? {
        val miles = milesRemaining ?: return null
        return String.format(Locale.US, "%.1f mi left", miles)
    }

    fun destinationLabel(): String? {
        if (!rideActive || destination.isBlank()) return null
        return "to $destination"
    }

    companion object {
        const val TABLET_ID = "RS-0142"
        private const val PREVIEW_SECONDS = 12 * 60
    }
}

fun formatCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", safe / 60, safe % 60)
}
