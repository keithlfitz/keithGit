package com.ridesandshares.passenger.data

import android.content.res.AssetManager
import android.util.Log
import java.io.IOException

class AdRepository(private val assets: AssetManager) {
    fun load(): Catalog {
        return try {
            val text = assets.open(CATALOG).bufferedReader().use { it.readText() }
            Catalog.from(AdJson.parse(text))
        } catch (error: IOException) {
            Log.e(TAG, "Could not read $CATALOG", error)
            Catalog.Invalid(listOf("Could not read $CATALOG"))
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Could not parse $CATALOG", error)
            Catalog.Invalid(listOf(error.message ?: "Could not parse $CATALOG"))
        }
    }

    companion object {
        const val CATALOG = "ads.json"
        private const val TAG = "AdRepository"
    }
}
