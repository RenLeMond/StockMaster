package com.stockmaster.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    /** 读取压缩图片并按 EXIF 方向旋转后保存至应用私有目录，返回 file:/// 格式路径。 */
    suspend fun saveCompressedImage(
        context: Context,
        uri: Uri,
        maxSize: Int = 800,
        quality: Int = 80
    ): String? = withContext(Dispatchers.IO) {
        try {
            val exifRotation = readExifRotation(context, uri)
            val swapped = exifRotation == 90 || exifRotation == 270

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val boundW = if (swapped) bounds.outHeight else bounds.outWidth
            val boundH = if (swapped) bounds.outWidth else bounds.outHeight

            var sample = 1
            while (maxOf(boundW, boundH) / (sample * 2) >= maxSize) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null

            val oriented = if (exifRotation != 0) {
                val matrix = Matrix().apply { postRotate(exifRotation.toFloat()) }
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated !== decoded) decoded.recycle()
                rotated
            } else decoded

            var scaled = oriented
            if (maxOf(oriented.width, oriented.height) > maxSize) {
                val ratio = maxSize.toFloat() / maxOf(oriented.width, oriented.height)
                scaled = Bitmap.createScaledBitmap(
                    oriented,
                    (oriented.width * ratio).toInt().coerceAtLeast(1),
                    (oriented.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled !== oriented) oriented.recycle()
            }

            val imageDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val imageFile = File(imageDir, fileName)

            FileOutputStream(imageFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }
            if (scaled !== oriented && !scaled.isRecycled) scaled.recycle()
            "file://${imageFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 按最大边长下采样解码，避免大图 OOM。 */
    fun decodeSampledFromUri(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) } ?: return 0
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /** 解码 data URL base64 图片。 */
    fun decodeDataUrl(dataUrl: String): Bitmap? {
        return try {
            val comma = dataUrl.indexOf(',')
            val b64 = if (comma > 0) dataUrl.substring(comma + 1) else dataUrl
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
