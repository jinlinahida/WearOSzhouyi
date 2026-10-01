package com.boompala.ai.sync

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 轻量二维码位图生成器，基于 ZXing Core。
 * 纯内存生成，零文件落地，安全高效。
 */
object QrCodeGenerator {
    fun generateBitMatrix(content: String, sizePx: Int = 240): com.google.zxing.common.BitMatrix? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            )
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        }.getOrNull()
    }

    fun generateQrBitmap(content: String, sizePx: Int = 240): ImageBitmap? {
        val matrix = generateBitMatrix(content, sizePx) ?: return null
        return runCatching {
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            val black = 0xFF000000.toInt()
            val white = 0xFFFFFFFF.toInt()
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) black else white
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}
