package com.ridesandshares.passenger

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ridesandshares.passenger.data.AdRepository
import com.ridesandshares.passenger.demo.DemoController
import com.ridesandshares.passenger.demo.RideDemo
import com.ridesandshares.passenger.kiosk.KioskController
import com.ridesandshares.passenger.trip.TripReceiver
import com.ridesandshares.passenger.ui.DemoHome
import com.ridesandshares.passenger.ui.theme.RidesAndSharesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        applyImmersive()
        val catalog = AdRepository(assets).load()
        val ride = RideDemo()
        val demo = DemoController()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    demo.backToTablet()
                }
            },
        )
        setContent {
            DisposableEffect(Unit) {
                val receiver = TripReceiver { update -> ride.applyProgress(update) }
                receiver.start()
                onDispose { receiver.stop() }
            }
            RidesAndSharesTheme {
                DemoHome(
                    catalog = catalog,
                    ride = ride,
                    controller = demo,
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
