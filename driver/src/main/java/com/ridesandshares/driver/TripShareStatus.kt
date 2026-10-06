package com.ridesandshares.driver

import android.os.Handler
import android.os.Looper

object TripShareStatus {
    @Volatile
    var text: String = "Enter the tablet address and the destination."

    var listener: ((String) -> Unit)? = null

    fun update(value: String) {
        text = value
        val callback = listener ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(value)
        } else {
            Handler(Looper.getMainLooper()).post { listener?.invoke(value) }
        }
    }
}
