package com.stockmaster.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/**
 * 使用 ZXing 生成 CODE128 条形码位图（对应 Web 版 JsBarcode）。
 */
object BarcodeBitmap {

    fun generate(
        value: String,
        height: Int = 120,
        width: Int = 640,
        barColor: Int = Color.rgb(0x0b, 0x1c, 0x30),
        bgColor: Int = Color.TRANSPARENT
    ): Bitmap? {
        if (value.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix: BitMatrix = MultiFormatWriter().encode(
                value, BarcodeFormat.CODE_128, width, height, hints
            )
            val px = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    px[offset + x] = if (matrix[x, y]) barColor else bgColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(px, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }
}