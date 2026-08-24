package com.stockmaster.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    /** 读取压缩图片并按 EXIF 方向（旋转+镜像）纠正后保存至应用私有目录，返回 file:/// 格式路径。 */
    suspend fun saveCompressedImage(
        context: Context,
        uri: Uri,
        maxSize: Int = 800,
        quality: Int = 80
    ): String? = withContext(Dispatchers.IO) {
        try {
            val exifOrientation = readExifOrientation(context, uri)
            val swapped = orientationSwapsDimensions(exifOrientation)

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

            val oriented = applyExifOrientation(decoded, exifOrientation)

            var scaled = oriented
            if (maxOf(oriented.width, oriented.height) > maxSize) {
                val ratio = maxSize.toFloat() / maxOf(oriented.width, oriented.height)
                scaled = oriented.scale(
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

    /** 按最大边长下采样解码并按 EXIF 方向纠正，供相册识码等场景使用。 */
    fun decodeSampledFromUriWithExif(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        val bitmap = decodeSampledFromUri(context, uri, maxDim) ?: return null
        return applyExifOrientation(bitmap, readExifOrientation(context, uri))
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

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /** 该 EXIF 方向是否交换宽高（旋转 90°/270° 及其镜像变体）。 */
    private fun orientationSwapsDimensions(orientation: Int): Boolean =
        orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

    /**
     * 按 EXIF 方向变换位图（含旋转与镜像），覆盖全部 8 种方向；
     * 前置摄像头常见的 TRANSPOSE/TRANSVERSE 此前被忽略导致照片横倒+镜像。
     */
    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (result !== bitmap) bitmap.recycle()
            result
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * 解码 data URL base64 图片；两段式解码按 [maxDim] 下采样，
     * 避免 4000×3000 级 legacy 图片全尺寸解码造成 ~48MB 内存峰值。
     */
    fun decodeDataUrl(dataUrl: String, maxDim: Int = 1600): Bitmap? {
        return try {
            val comma = dataUrl.indexOf(',')
            val b64 = if (comma > 0) dataUrl.substring(comma + 1) else dataUrl
            val bytes = Base64.decode(b64, Base64.NO_WRAP)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (e: Exception) {
            null
        }
    }
}
