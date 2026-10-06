package com.ridesandshares.passenger.data

import com.ridesandshares.passenger.qr.QrEncoder
import com.ridesandshares.passenger.slideshow.SlideshowTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdsCatalogTest {
    @Test
    fun slideshowAdvancesEveryFifteenSeconds() {
        assertEquals(15_000, SlideshowTiming.ADVANCE_EVERY_MS)
    }

    @Test
    fun bundledCatalogIsValidAndImagesExist() {
        val catalogFile = locate("src/main/assets/ads.json")
        val catalog = Catalog.from(AdJson.parse(catalogFile.readText()))
        assertTrue(catalog is Catalog.Ready)
        val ads = (catalog as Catalog.Ready).ads
        assertTrue("Sample ads are bundled so the tablet runs with no backend", ads.size >= 3)
        assertEquals(ads.size, ads.map { it.id }.distinct().size)
        assertEquals(ads.size, ads.map { it.infoUrl }.distinct().size)

        val assets = catalogFile.parentFile
        ads.forEach { ad ->
            assertTrue(ad.infoUrl.startsWith("https://"))
            val image = File(assets, ad.image)
            assertTrue("Missing creative ${image.path}", image.isFile && image.length() > 0L)
            val matrix = QrEncoder.matrix(ad.infoUrl)
            assertTrue(matrix.width > QrEncoder.QUIET_ZONE_MODULES * 2)
        }
    }

    private fun locate(relative: String): File {
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.isFile }
            ?: error("$relative not found from ${File(".").absoluteFile}")
    }
}
