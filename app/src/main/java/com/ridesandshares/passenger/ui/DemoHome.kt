package com.ridesandshares.passenger.ui

import androidx.compose.runtime.Composable
import com.ridesandshares.passenger.data.Catalog
import com.ridesandshares.passenger.demo.DemoController
import com.ridesandshares.passenger.demo.DemoMode
import com.ridesandshares.passenger.demo.RideDemo

@Composable
fun DemoHome(
    catalog: Catalog,
    ride: RideDemo,
    controller: DemoController,
) {
    RideClock(ride)
    when (controller.mode) {
        DemoMode.Tablet -> SlideshowScreen(
            catalog = catalog,
            ride = ride,
            runClock = false,
            onOpenDriver = { controller.open(DemoMode.Driver) },
            onOpenAdmin = { controller.open(DemoMode.Admin) },
        )
        DemoMode.Driver -> DriverPhoneScreen(
            ride = ride,
            onOpenTablet = { controller.open(DemoMode.Tablet) },
            onOpenAdmin = { controller.open(DemoMode.Admin) },
        )
        DemoMode.Admin -> AdminScreen(
            onOpenTablet = { controller.open(DemoMode.Tablet) },
            onOpenDriver = { controller.open(DemoMode.Driver) },
        )
    }
}
