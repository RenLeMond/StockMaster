package com.stockmaster.app.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/**
 * 使用 ZXing 生成 CODE128 条形码位图（对应 Web 版 JsBarcode）。
 * 宽度自适应：CODE_128 的矩阵宽度随内容增长，固定 640px 在长内容时编码失败。
 */
object BarcodeBitmap {

    private val WIDTH_CANDIDATES = intArrayOf(640, 1024, 2048, 4096)

    fun generate(
        value: String,
        height: Int = 120,
        width: Int = 640,
        barColor: Int = Color.rgb(0x0b, 0x1c, 0x30),
        bgColor: Int = Color.TRANSPARENT
    ): Bitmap? {
        if (value.isBlank()) return null
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        // 候选宽度 = 请求宽度 ∪ 预设档位（升序去重），从请求宽度起逐级放大，
        // 找到能容纳该内容的最小候选；避免 candidate < width 时用同一宽度空转重试
        var matrix: BitMatrix? = null
        var actualWidth = width
        val candidates = (WIDTH_CANDIDATES.toList() + width).distinct().sorted()
        for (w in candidates) {
            try {
                matrix = MultiFormatWriter().encode(value, BarcodeFormat.CODE_128, w, height, hints)
                actualWidth = w
                break
            } catch (_: Exception) {
                continue
            }
        }
        val m = matrix ?: return null

        return createBitmap(actualWidth, height, Bitmap.Config.ARGB_8888).apply {
            val px = IntArray(actualWidth * height)
            for (y in 0 until height) {
                val offset = y * actualWidth
                for (x in 0 until actualWidth) {
                    px[offset + x] = if (m[x, y]) barColor else bgColor
                }
            }
            setPixels(px, 0, actualWidth, 0, 0, actualWidth, height)
        }
    }
}
