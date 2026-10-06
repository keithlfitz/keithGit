package com.ridesandshares.driver

import com.ridesandshares.trip.TripProgress
import com.ridesandshares.trip.TripProgressJson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun sendTrip(host: String, port: Int, trip: TripProgress) {
    val bytes = TripProgressJson.encode(trip).toByteArray(Charsets.UTF_8)
    DatagramSocket().use { socket ->
        val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port)
        socket.send(packet)
    }
}
