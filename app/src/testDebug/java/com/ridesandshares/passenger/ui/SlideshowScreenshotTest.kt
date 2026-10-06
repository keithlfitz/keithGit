package com.ridesandshares.passenger.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.ridesandshares.passenger.data.AdRepository
import com.ridesandshares.passenger.data.Catalog
import com.ridesandshares.passenger.ui.theme.RidesAndSharesTheme
import com.ridesandshares.trip.TripProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * Renders the passenger slideshow composable — the same UI the tablet activity
 * shows — and can write one landscape frame per sample ad.
 *
 * Pass -PSLIDE_OUT and -PFRAME_OUT to keep the PNGs and the frames used for a
 * preview video. Without those properties the test still renders and checks
 * each slide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class SlideshowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersEachSampleSlide() {
        val catalog = AdRepository(RuntimeEnvironment.getApplication().assets).load()
        assertTrue(catalog is Catalog.Ready)
        val ads = (catalog as Catalog.Ready).ads
        assertEquals(
            listOf(
                "harbor-rye",
                "northline-eats",
                "lumen-hotel",
                "pike-street-books",
                "cedar-dental",
            ),
            ads.map { it.id },
        )

        val slideOut = File(System.getenv("SLIDE_OUT").orEmpty()).takeIf { it.path.isNotEmpty() }
        val frameOut = File(System.getenv("FRAME_OUT").orEmpty()).takeIf { it.path.isNotEmpty() }
        slideOut?.mkdirs()
        frameOut?.mkdirs()
        var frame = 0

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            RidesAndSharesTheme {
                Box(
                    Modifier
                        .size(1280.dp, 800.dp)
                        .fillMaxSize(),
                ) {
                    SlideshowScreen(catalog)
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        ads.indices.forEach { index ->
            val ad = ads[index]
            composeRule.onNodeWithText("Waiting for the driver's route").assertIsDisplayed()
            composeRule.onNodeWithText(ad.businessName).assertIsDisplayed()
            composeRule.onNodeWithText(ad.tagline).assertIsDisplayed()
            composeRule.onNodeWithText("Scan for details").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Advertisement for ${ad.businessName}")
                .assertIsDisplayed()
            composeRule.onNodeWithContentDescription(
                "QR code for ${ad.businessName}. Scan to open this business.",
            ).assertIsDisplayed()

            val bitmap = snapshot()
            assertRenderedSlide(bitmap)
            slideOut?.let { writePng(File(it, SLIDE_FILES[index]), bitmap) }
            if (frameOut != null) {
                writePng(File(frameOut, "frame_%03d.png".format(frame++)), bitmap)
                repeat(30) {
                    composeRule.mainClock.advanceTimeBy(500)
                    composeRule.waitForIdle()
                    writePng(File(frameOut, "frame_%03d.png".format(frame++)), snapshot())
                }
                composeRule.mainClock.advanceTimeBy(500)
                composeRule.waitForIdle()
            } else if (index < ads.lastIndex) {
                composeRule.mainClock.advanceTimeBy(15_500)
                composeRule.waitForIdle()
            }
        }
    }

    @Test
    fun rendersRemainingDistance() {
        val catalog = AdRepository(RuntimeEnvironment.getApplication().assets).load()
        val trip = TripProgress(
            destination = "Pike Place Market",
            distanceMeters = 6759,
            durationSeconds = 720,
            receivedAtEpochMs = System.currentTimeMillis(),
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            RidesAndSharesTheme {
                Box(
                    Modifier
                        .size(1280.dp, 800.dp)
                        .fillMaxSize(),
                ) {
                    SlideshowScreen(
                        catalog = catalog,
                        trip = trip,
                        tabletAddress = "192.168.4.21",
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("4.2 mi  ·  12 min").assertIsDisplayed()
        composeRule.onNodeWithText("to Pike Place Market").assertIsDisplayed()
        composeRule.onNodeWithText("Harbor & Rye").assertIsDisplayed()
        val tripOut = System.getenv("TRIP_OUT").orEmpty()
        if (tripOut.isNotEmpty()) {
            writePng(File(tripOut), snapshot())
        }
    }

    private fun snapshot(): Bitmap {
        val activity = (composeRule as AndroidComposeTestRule<*, *>).activity
        val view = activity.window.decorView
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun assertRenderedSlide(bitmap: Bitmap) {
        assertTrue("expected a landscape tablet frame, was ${bitmap.width}x${bitmap.height}", bitmap.width > bitmap.height)
        assertTrue(bitmap.width >= 1000)
        assertTrue(bitmap.height >= 700)
        var dark = 0
        var light = 0
        val stride = 24
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                if (luminance < 48) dark += 1
                if (luminance > 210) light += 1
                x += stride
            }
            y += stride
        }
        assertTrue("slide looked blank (dark=$dark light=$light)", dark > 20 && light > 20)
    }

    private fun writePng(file: File, bitmap: Bitmap) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Could not write $file" }
        }
    }

    companion object {
        private val SLIDE_FILES = listOf(
            "slide-harbor-and-rye.png",
            "slide-northline-eats.png",
            "slide-lumen-hotel.png",
            "slide-pike-street-books.png",
            "slide-cedar-dental.png",
        )
    }
}
