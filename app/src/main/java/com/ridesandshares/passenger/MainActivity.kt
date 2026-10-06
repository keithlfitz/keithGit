package com.ridesandshares.passenger

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ridesandshares.passenger.data.AdRepository
import com.ridesandshares.passenger.kiosk.KioskController
import com.ridesandshares.passenger.trip.TripReceiver
import com.ridesandshares.passenger.trip.localIpv4
import com.ridesandshares.passenger.ui.SlideshowScreen
import com.ridesandshares.passenger.ui.theme.RidesAndSharesTheme
import com.ridesandshares.trip.TripProgress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        applyImmersive()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )
        val catalog = AdRepository(assets).load()
        val tabletAddress = localIpv4()
        setContent {
            var trip by mutableStateOf<TripProgress?>(null)
            DisposableEffect(Unit) {
                val receiver = TripReceiver { update -> trip = update }
                receiver.start()
                onDispose { receiver.stop() }
            }
            RidesAndSharesTheme {
                SlideshowScreen(
                    catalog = catalog,
                    trip = trip,
                    tabletAddress = tabletAddress,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersive()
        KioskController(this).enter()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    private fun applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
