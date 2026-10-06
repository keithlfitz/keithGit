package com.ridesandshares.passenger.demo

import java.util.Locale

/**
 * Sample fleet numbers bundled in the app. The admin screens read only this.
 */
object AdminMock {
    const val FLEET_ONLINE = "128"
    const val IMPRESSIONS_TODAY = "48,291"
    const val SCANS_TODAY = "3,104"
    const val ACTIVE_CAMPAIGNS = "5"

    data class Campaign(
        val businessName: String,
        val impressions: Int,
        val scanRate: String,
    ) {
        fun impressionLabel(): String = String.format(Locale.US, "%,d", impressions)
    }

    data class Tablet(
        val id: String,
        val neighborhood: String,
        val online: Boolean,
        val nowShowing: String,
    )

    data class Ride(
        val destination: String,
        val miles: String,
        val arrival: String?,
        val status: String,
    )

    val campaigns = listOf(
        Campaign("Harbor & Rye", 12_480, "6.4%"),
        Campaign("Northline Eats", 9_220, "5.1%"),
        Campaign("Lumen Hotel", 8_640, "7.2%"),
        Campaign("Pike Street Books", 4_110, "4.4%"),
        Campaign("Cedar Dental", 3_841, "3.9%"),
    )

    val tablets = listOf(
        Tablet("RS-0142", "Downtown", online = true, nowShowing = "Harbor & Rye"),
        Tablet("RS-0143", "Capitol Hill", online = true, nowShowing = "Northline Eats"),
        Tablet("RS-0201", "Ballard", online = false, nowShowing = "Lumen Hotel"),
        Tablet("RS-0208", "Fremont", online = true, nowShowing = "Pike Street Books"),
    )

    val rides = listOf(
        Ride("Pike Place Market", "4.2 mi", "12 min", "In progress"),
        Ride("Seattle-Tacoma Airport", "18.4 mi", "34 min", "In progress"),
        Ride("Fremont Troll", "2.1 mi", null, "Arrived"),
        Ride("Kerry Park", "6.8 mi", "19 min", "In progress"),
    )
}
