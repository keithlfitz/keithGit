package com.ridesandshares.driver

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import com.ridesandshares.trip.TripProgress
import com.ridesandshares.trip.formatTripDistance
import com.ridesandshares.trip.formatTripDuration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Keeps asking Google for the remaining driving distance while the driver
 * follows the same destination in the Google Maps app.
 */
class TripShareService : Service() {
    private val locationThread = HandlerThread("trip-location")
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private val latestLocation = AtomicReference<Location?>(null)
    private val pollGeneration = AtomicInteger()
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharing()
            return START_NOT_STICKY
        }
        val destination = intent?.getStringExtra(EXTRA_DESTINATION)?.trim().orEmpty()
        val host = intent?.getStringExtra(EXTRA_HOST)?.trim().orEmpty()
        val port = intent?.getIntExtra(EXTRA_PORT, com.ridesandshares.trip.TripLink.PORT)
            ?: com.ridesandshares.trip.TripLink.PORT
        if (destination.isEmpty() || host.isEmpty()) {
            TripShareStatus.update("Enter the tablet address and the destination.")
            stopSharing()
            return START_NOT_STICKY
        }
        startInForeground(getString(R.string.notification_sharing))
        watchLocation()
        pollRoute(destination, host, port)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        pollGeneration.incrementAndGet()
        worker?.interrupt()
        locationListener?.let { listener -> locationManager?.removeUpdates(listener) }
        if (locationThread.isAlive) locationThread.quitSafely()
        super.onDestroy()
    }

    private fun pollRoute(destination: String, host: String, port: Int) {
        val generation = pollGeneration.incrementAndGet()
        worker?.interrupt()
        val next = Thread {
            val client = DirectionsClient(BuildConfig.MAPS_API_KEY)
            while (pollGeneration.get() == generation && !Thread.currentThread().isInterrupted) {
                val location = latestLocation.get()
                if (location == null) {
                    TripShareStatus.update("Waiting for a location fix on this phone.")
                } else {
                    publish(client, location, destination, host, port)
                }
                try {
                    Thread.sleep(POLL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        next.isDaemon = true
        next.name = "trip-poll"
        worker = next
        next.start()
    }

    private fun publish(
        client: DirectionsClient,
        location: Location,
        destination: String,
        host: String,
        port: Int,
    ) {
        try {
            val leg = client.route(location.latitude, location.longitude, destination)
            sendTrip(
                host,
                port,
                TripProgress(
                    destination = destination,
                    distanceMeters = leg.distanceMeters,
                    durationSeconds = leg.durationSeconds,
                ),
            )
            val summary = "${formatTripDistance(leg.distanceMeters)} · ${formatTripDuration(leg.durationSeconds)}"
            TripShareStatus.update("Sent $summary to the tablet")
            notifyPassenger(summary)
        } catch (error: Exception) {
            TripShareStatus.update(error.message ?: "Could not reach Google Maps")
        }
    }

    private fun watchLocation() {
        if (!locationThread.isAlive) locationThread.start()
        val manager = getSystemService(LocationManager::class.java)
        locationManager = manager
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            TripShareStatus.update("Location permission is required.")
            return
        }
        locationListener?.let { manager.removeUpdates(it) }
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            TripShareStatus.update("Turn on location, then start sharing again.")
            return
        }
        val listener = LocationListener { location -> latestLocation.set(location) }
        locationListener = listener
        manager.getLastKnownLocation(provider)?.let { latestLocation.set(it) }
        manager.requestLocationUpdates(provider, 5_000L, 5f, listener, locationThread.looper)
    }

    private fun startInForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyPassenger(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "trip-share"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        getString(R.string.notification_channel),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
        return Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun stopSharing() {
        pollGeneration.incrementAndGet()
        worker?.interrupt()
        locationListener?.let { listener -> locationManager?.removeUpdates(listener) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_STOP = "com.ridesandshares.driver.STOP"
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        private const val NOTIFICATION_ID = 41
        private const val POLL_MS = 10_000L
    }
}
