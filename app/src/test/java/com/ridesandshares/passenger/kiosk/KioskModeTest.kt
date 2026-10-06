package com.ridesandshares.passenger.kiosk

import org.junit.Assert.assertEquals
import org.junit.Test

class KioskModeTest {
    @Test
    fun deviceOwnerPinsWithoutAPrompt() {
        assertEquals(KioskMode.DEVICE_OWNER, KioskMode.resolve(isDeviceOwner = true, isLockTaskPermitted = false))
        assertEquals(KioskMode.DEVICE_OWNER, KioskMode.resolve(isDeviceOwner = true, isLockTaskPermitted = true))
    }

    @Test
    fun aWhitelistedPackageUsesScreenPin() {
        assertEquals(
            KioskMode.SCREEN_PIN,
            KioskMode.resolve(isDeviceOwner = false, isLockTaskPermitted = true),
        )
    }

    @Test
    fun otherwiseTheSlideshowStaysImmersive() {
        assertEquals(
            KioskMode.IMMERSIVE_ONLY,
            KioskMode.resolve(isDeviceOwner = false, isLockTaskPermitted = false),
        )
    }
}
