package com.ridesandshares.trip

/**
 * Remaining distance on the driver's Google route, as shown to the passenger.
 *
 * [receivedAtEpochMs] is set by the tablet when the packet arrives. The wire
 * format does not include it, so a clock difference between the two phones
 * cannot make a fresh update look stale.
 */
data class TripProgress(
    val destination: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val receivedAtEpochMs: Long = 0L,
) {
    fun receivedNow(nowMs: Long = System.currentTimeMillis()): TripProgress =
        copy(receivedAtEpochMs = nowMs)

    fun isFresh(nowMs: Long, maxAgeMs: Long = TripLink.STALE_AFTER_MS): Boolean {
        if (receivedAtEpochMs <= 0L) return false
        val age = nowMs - receivedAtEpochMs
        return age in 0..maxAgeMs
    }
}

object TripLink {
    const val PORT = 8787
    const val STALE_AFTER_MS = 45_000L
}
