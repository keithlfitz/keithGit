package com.ridesandshares.driver

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ridesandshares.trip.parseTabletAddress

class DriverActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("trip", MODE_PRIVATE) }
    private val sharing = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { granted ->
            val locationOk = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!locationOk) {
                TripShareStatus.update("Location permission is required.")
                sharing.value = false
                return@registerForActivityResult
            }
            sharing.value = startFromSavedFields()
        }
        setContent {
            var host by remember { mutableStateOf(prefs.getString(KEY_HOST, "") ?: "") }
            var destination by remember { mutableStateOf(prefs.getString(KEY_DESTINATION, "") ?: "") }
            var status by remember { mutableStateOf(TripShareStatus.text) }
            val isSharing by sharing
            DisposableEffect(Unit) {
                val previous = TripShareStatus.listener
                TripShareStatus.listener = { status = it }
                onDispose { if (TripShareStatus.listener != null) TripShareStatus.listener = previous }
            }
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Share the route", fontSize = 32.sp)
                    Text(
                        "Google Maps navigates on this phone. Every few seconds the remaining driving distance is sent to the passenger tablet.",
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tablet address") },
                        placeholder = { Text("192.168.1.20") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Destination") },
                        placeholder = { Text("Pike Place Market") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            prefs.edit()
                                .putString(KEY_HOST, host.trim())
                                .putString(KEY_DESTINATION, destination.trim())
                                .apply()
                            val needed = permissionsStillNeeded()
                            if (needed.isEmpty()) {
                                sharing.value = startFromSavedFields()
                            } else {
                                permissionLauncher.launch(needed)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start and open Google Maps")
                    }
                    if (isSharing) {
                        TextButton(
                            onClick = {
                                startService(
                                    Intent(this@DriverActivity, TripShareService::class.java)
                                        .setAction(TripShareService.ACTION_STOP),
                                )
                                sharing.value = false
                                TripShareStatus.update("Sharing stopped.")
                            },
                        ) {
                            Text("Stop sharing")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(status)
                }
            }
        }
    }

    private fun permissionsStillNeeded(): Array<String> {
        val wanted = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
        }
        return wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    private fun startFromSavedFields(): Boolean {
        val host = prefs.getString(KEY_HOST, "").orEmpty()
        val destination = prefs.getString(KEY_DESTINATION, "").orEmpty()
        val parsed = parseTabletAddress(host)
        if (parsed == null) {
            TripShareStatus.update("Enter the address shown on the passenger tablet.")
            return false
        }
        if (destination.isBlank()) {
            TripShareStatus.update("Enter the destination.")
            return false
        }
        val (tabletHost, port) = parsed
        ContextCompat.startForegroundService(
            this,
            Intent(this, TripShareService::class.java)
                .putExtra(TripShareService.EXTRA_HOST, tabletHost)
                .putExtra(TripShareService.EXTRA_PORT, port)
                .putExtra(TripShareService.EXTRA_DESTINATION, destination),
        )
        try {
            openGoogleMapsNavigation(this, destination)
        } catch (_: ActivityNotFoundException) {
            TripShareStatus.update("Google Maps is not installed. Distance sharing is still running.")
        }
        return true
    }

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_DESTINATION = "destination"
    }
}
