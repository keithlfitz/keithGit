package com.ridesandshares.passenger.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.ridesandshares.passenger.KioskDeviceAdminReceiver
import com.ridesandshares.passenger.MainActivity

/**
 * Pins the slideshow when the tablet allows it.
 *
 * Device owner is the setup we want in a car: lock task with no system
 * chrome, keyguard off, and this activity as the home app. If some other
 * policy already whitelisted the package, we only enter lock task. Otherwise
 * we still call [Activity.startLockTask], which asks the passenger to confirm
 * screen pinning when that setting is on, and does nothing harmful when it
 * is not.
 */
class KioskController(
    private val activity: Activity,
    private val devicePolicyManager: DevicePolicyManager =
        activity.getSystemService(DevicePolicyManager::class.java),
) {
    fun enter() {
        val mode = KioskMode.resolve(
            isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(activity.packageName),
            isLockTaskPermitted = devicePolicyManager.isLockTaskPermitted(activity.packageName),
        )
        if (mode == KioskMode.DEVICE_OWNER) {
            configureDeviceOwner()
        }
        try {
            activity.startLockTask()
        } catch (error: SecurityException) {
            Log.i(TAG, "Lock task is not available; staying in immersive fullscreen", error)
        }
    }

    private fun configureDeviceOwner() {
        val admin = ComponentName(activity, KioskDeviceAdminReceiver::class.java)
        ownerAction("setLockTaskPackages") {
            devicePolicyManager.setLockTaskPackages(admin, arrayOf(activity.packageName))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ownerAction("setLockTaskFeatures") {
                devicePolicyManager.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE,
                )
            }
        }
        ownerAction("setStatusBarDisabled") {
            devicePolicyManager.setStatusBarDisabled(admin, true)
        }
        ownerAction("setKeyguardDisabled") {
            devicePolicyManager.setKeyguardDisabled(admin, true)
        }
        ownerAction("addPersistentPreferredActivity") {
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            devicePolicyManager.addPersistentPreferredActivity(
                admin,
                home,
                ComponentName(activity, MainActivity::class.java),
            )
        }
    }

    private fun ownerAction(name: String, block: () -> Unit) {
        try {
            block()
        } catch (error: SecurityException) {
            Log.e(TAG, "Device-owner step failed: $name", error)
        }
    }

    companion object {
        private const val TAG = "KioskController"
    }
}
