package com.ridesandshares.passenger.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Encodes an advertisement info URL as a QR symbol.
 *
 * [QUIET_ZONE_MODULES] is the ISO quiet zone (four modules of white on every
 * side). The slideshow draws that margin and then extra white padding so the
 * code still scans against the tablet bezel.
 */
object QrEncoder {
    const val QUIET_ZONE_MODULES = 4

    fun matrix(contents: String, quietZoneModules: Int = QUIET_ZONE_MODULES): BitMatrix {
        require(contents.isNotEmpty()) { "QR contents must not be empty" }
        val hints = mapOf(
            EncodeHintType.MARGIN to quietZoneModules,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        return com.google.zxing.qrcode.QRCodeWriter().encode(
            contents,
            BarcodeFormat.QR_CODE,
            0,
            0,
            hints,
        )
    }
}
