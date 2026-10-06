package com.ridesandshares.passenger.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DemoMode {
    Tablet,
    Driver,
    Admin,
}

/**
 * Which demo screen is showing. The passenger slideshow is the start screen,
 * and Back from the other screens returns there.
 */
class DemoController {
    var mode by mutableStateOf(DemoMode.Tablet)
        private set

    fun open(next: DemoMode) {
        mode = next
    }

    fun backToTablet() {
        if (mode != DemoMode.Tablet) mode = DemoMode.Tablet
    }
}
