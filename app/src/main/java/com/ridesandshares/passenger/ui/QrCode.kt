package com.ridesandshares.passenger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ridesandshares.passenger.qr.QrEncoder
import kotlin.math.floor

/**
 * Draws a QR symbol in module-sized rectangles so the tablet does not blur it.
 * The matrix already includes a four-module quiet zone; the white padding
 * around the canvas keeps that zone off the warm paper rail.
 */
@Composable
fun QrCode(
    contents: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(contents) { QrEncoder.matrix(contents) }
    Canvas(
        modifier
            .background(Color.White)
            .padding(16.dp)
            .background(Color.White)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val modules = matrix.width
        if (modules == 0) return@Canvas
        val cell = floor(size.minDimension / modules.toFloat())
        if (cell < 1f) return@Canvas
        val drawn = cell * modules
        val left = (size.width - drawn) / 2f
        val top = (size.height - drawn) / 2f
        drawRect(Color.White)
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!matrix.get(x, y)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(left + x * cell, top + y * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
