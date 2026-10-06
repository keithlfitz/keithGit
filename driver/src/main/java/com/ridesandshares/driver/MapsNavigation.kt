package com.ridesandshares.driver

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openGoogleMapsNavigation(context: Context, destination: String) {
    val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
