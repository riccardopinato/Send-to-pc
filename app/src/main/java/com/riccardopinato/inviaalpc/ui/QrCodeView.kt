package com.riccardopinato.inviaalpc.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCodeView(
    text: String,
    modifier: Modifier = Modifier
) {
    val bitmap =
        remember(text) {
            generateQrCode(
                text = text,
                size = 768
            )
        }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR code della sessione",
        modifier = modifier
    )
}

private fun generateQrCode(
    text: String,
    size: Int
): Bitmap {
    val hints =
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )

    val matrix =
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

    val pixels = IntArray(size * size)

    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] =
                if (matrix[x, y]) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
        }
    }

    return Bitmap
        .createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )
        .apply {
            setPixels(
                pixels,
                0,
                size,
                0,
                0,
                size,
                size
            )
        }
}
