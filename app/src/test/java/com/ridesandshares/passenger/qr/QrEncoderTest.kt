package com.ridesandshares.passenger.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrEncoderTest {
    @Test
    fun roundTripsTheInfoUrl() {
        val url = "https://ridesandshares.example/a/harbor-rye"
        val decoded = decode(url)
        assertEquals(url, decoded)
    }

    @Test
    fun leavesAFourModuleQuietZone() {
        val matrix = QrEncoder.matrix("https://ridesandshares.example/a/lumen-hotel")
        val quiet = QrEncoder.QUIET_ZONE_MODULES
        assertTrue(matrix.width > quiet * 2)
        assertEquals(matrix.width, matrix.height)
        for (i in 0 until quiet) {
            for (x in 0 until matrix.width) {
                assertFalse("top quiet zone", matrix.get(x, i))
                assertFalse("bottom quiet zone", matrix.get(x, matrix.height - 1 - i))
                assertFalse("left quiet zone", matrix.get(i, x))
                assertFalse("right quiet zone", matrix.get(matrix.width - 1 - i, x))
            }
        }
        val inside = quiet
        var hasInk = false
        for (y in inside until matrix.height - inside) {
            for (x in inside until matrix.width - inside) {
                if (matrix.get(x, y)) hasInk = true
            }
        }
        assertTrue(hasInk)
    }

    @Test
    fun differentUrlsEncodeDifferentSymbols() {
        val first = QrEncoder.matrix("https://ridesandshares.example/a/harbor-rye")
        val second = QrEncoder.matrix("https://ridesandshares.example/a/cedar-dental")
        assertFalse(same(first, second))
    }

    private fun decode(contents: String): String {
        val matrix = QrEncoder.matrix(contents)
        val scale = 8
        val pad = 16
        val width = matrix.width * scale + pad * 2
        val height = matrix.height * scale + pad * 2
        val pixels = IntArray(width * height) { 0xFFFFFFFF.toInt() }
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix.get(x, y)) continue
                val originX = pad + x * scale
                val originY = pad + y * scale
                for (dy in 0 until scale) {
                    val row = (originY + dy) * width
                    for (dx in 0 until scale) {
                        pixels[row + originX + dx] = 0xFF000000.toInt()
                    }
                }
            }
        }
        val source = RGBLuminanceSource(width, height, pixels)
        return MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    private fun same(first: com.google.zxing.common.BitMatrix, second: com.google.zxing.common.BitMatrix): Boolean {
        if (first.width != second.width || first.height != second.height) return false
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (first.get(x, y) != second.get(x, y)) return false
            }
        }
        return true
    }
}
