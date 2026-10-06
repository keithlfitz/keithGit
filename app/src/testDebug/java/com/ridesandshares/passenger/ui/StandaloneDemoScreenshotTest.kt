package com.ridesandshares.passenger.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ridesandshares.passenger.data.AdRepository
import com.ridesandshares.passenger.data.Catalog
import com.ridesandshares.passenger.demo.DemoController
import com.ridesandshares.passenger.demo.RideDemo
import com.ridesandshares.passenger.ui.theme.RidesAndSharesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.io.FileOutputStream

/**
 * Walks the standalone demo: passenger slideshow, driver phone, then admin.
 * STANDALONE_OUT receives the three stills and a frame sequence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StandaloneDemoScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchesBetweenTabletDriverAndAdmin() {
        val catalog = AdRepository(RuntimeEnvironment.getApplication().assets).load()
        check(catalog is Catalog.Ready)
        val ride = RideDemo()
        val controller = DemoController()
        val out = File(System.getenv("STANDALONE_OUT").orEmpty()).takeIf { it.path.isNotEmpty() }
        val frames = out?.let { File(it, "standalone-frames").also { dir -> dir.mkdirs() } }
        var frame = 0

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            RidesAndSharesTheme {
                Box(
                    Modifier
                        .size(1280.dp, 800.dp)
                        .fillMaxSize(),
                ) {
                    DemoHome(catalog = catalog, ride = ride, controller = controller)
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Waiting for the driver").assertIsDisplayed()
        composeRule.onNodeWithText("12:00").assertIsDisplayed()
        composeRule.onNodeWithText("Harbor & Rye").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Car on the route").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "QR code for Harbor & Rye. Scan to open this business.",
        ).assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("11:59").assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.onNodeWithTag("demo-driver").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Pair tablet").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Paired with tablet RS-0142").assertIsDisplayed()
        composeRule.onNodeWithText("Pike Place Market").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Miles left").assertIsDisplayed()
        composeRule.onNodeWithText("Arrival").assertIsDisplayed()
        shoot(frames, out?.let { File(it, "standalone-driver.png") }) { frame++ }

        composeRule.onNodeWithTag("send-ride").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sent to the tablet.").assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.onNodeWithTag("demo-tablet").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ride in progress").assertIsDisplayed()
        composeRule.onNodeWithText("4.2 mi left").assertIsDisplayed()
        composeRule.onNodeWithText("to Pike Place Market").assertIsDisplayed()
        composeRule.onNodeWithText("12:00").assertIsDisplayed()
        composeRule.onNodeWithText("Harbor & Rye").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Car on the route").assertIsDisplayed()
        shoot(frames, out?.let { File(it, "standalone-tablet.png") }) { frame++ }
        repeat(3) {
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            shoot(frames) { frame++ }
        }
        composeRule.mainClock.advanceTimeBy(15_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Northline Eats").assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.onNodeWithTag("demo-admin").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fleet online").assertIsDisplayed()
        composeRule.onNodeWithText("128").assertIsDisplayed()
        composeRule.onNodeWithText("Impressions today").assertIsDisplayed()
        composeRule.onNodeWithText("48,291").assertIsDisplayed()
        composeRule.onNodeWithText("Scans today").assertIsDisplayed()
        composeRule.onNodeWithText("3,104").assertIsDisplayed()
        composeRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeRule.onNodeWithText("12,480 impressions · 6.4% scans").assertIsDisplayed()
        shoot(frames, out?.let { File(it, "standalone-admin.png") }) { frame++ }

        composeRule.onNodeWithTag("admin-Campaigns").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Campaign performance").assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.onNodeWithTag("admin-Tablets").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("RS-0142 Downtown").assertIsDisplayed()
        composeRule.onNodeWithText("Offline").assertIsDisplayed()
        composeRule.onNodeWithText("Tablet fleet").assertIsDisplayed()
        shoot(frames) { frame++ }

        composeRule.onNodeWithTag("admin-Rides").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fremont Troll").assertIsDisplayed()
        composeRule.onNodeWithText("Arrived").assertIsDisplayed()
        shoot(frames) { frame++ }
    }

    private fun shoot(frames: File?, still: File? = null, next: () -> Int) {
        val bitmap = snapshot()
        if (still != null) writePng(still, bitmap)
        if (frames != null) writePng(File(frames, "frame_%03d.png".format(next())), bitmap)
    }

    private fun snapshot(): Bitmap {
        val activity = (composeRule as AndroidComposeTestRule<*, *>).activity
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun writePng(file: File, bitmap: Bitmap) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Could not write $file" }
        }
    }
}
