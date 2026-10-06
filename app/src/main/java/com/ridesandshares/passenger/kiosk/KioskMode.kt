package com.ridesandshares.passenger.kiosk

enum class KioskMode {
    DEVICE_OWNER,
    SCREEN_PIN,
    IMMERSIVE_ONLY,
    ;

    companion object {
        fun resolve(isDeviceOwner: Boolean, isLockTaskPermitted: Boolean): KioskMode {
            return when {
                isDeviceOwner -> DEVICE_OWNER
                isLockTaskPermitted -> SCREEN_PIN
                else -> IMMERSIVE_ONLY
            }
        }
    }
}
