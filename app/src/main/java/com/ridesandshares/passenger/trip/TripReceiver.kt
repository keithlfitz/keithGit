package com.ridesandshares.passenger.trip

import android.os.Handler
import android.os.Looper
import com.ridesandshares.trip.TripLink
import com.ridesandshares.trip.TripProgress
import com.ridesandshares.trip.TripProgressJson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class TripReceiver(
    private val onTrip: (TripProgress) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var thread: Thread? = null
    private var socket: DatagramSocket? = null

    @Volatile
    private var stopped = false

    fun start() {
        if (thread != null) return
        stopped = false
        val udp = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(TripLink.PORT))
        }
        socket = udp
        val worker = Thread {
            val buffer = ByteArray(2048)
            while (!stopped && !Thread.currentThread().isInterrupted) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    udp.receive(packet)
                } catch (_: Exception) {
                    break
                }
                val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                val trip = TripProgressJson.decode(text)?.receivedNow() ?: continue
                main.post {
                    if (!stopped) onTrip(trip)
                }
            }
        }
        worker.isDaemon = true
        worker.name = "trip-receiver"
        thread = worker
        worker.start()
    }

    fun stop() {
        stopped = true
        thread?.interrupt()
        thread = null
        socket?.close()
        socket = null
        main.removeCallbacksAndMessages(null)
    }
}
