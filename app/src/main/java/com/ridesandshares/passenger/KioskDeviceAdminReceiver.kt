package com.ridesandshares.passenger

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin entry used only so this package can be the tablet's device
 * owner. The slideshow calls the policy manager itself after that.
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver()
