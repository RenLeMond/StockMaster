package com.stockmaster.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.stockmaster.app.data.BackupImageEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    /** 应用私有图片目录的唯一来源：保存、备份导出与恢复物化必须共用同一目录。 */
    fun imagesDir(context: Context): File = File(context.filesDir, "images")

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
                // 等比缩放：先用四舍五入避免向 0 截断，再钳制非零
                val ratio = maxSize.toFloat() / maxOf(oriented.width, oriented.height)
                val targetW = (oriented.width * ratio + 0.5f).toInt().coerceAtLeast(1)
                val targetH = (oriented.height * ratio + 0.5f).toInt().coerceAtLeast(1)
                scaled = oriented.scale(targetW, targetH, true)
                if (scaled !== oriented) oriented.recycle()
            }

            val imageDir = imagesDir(context).apply { if (!exists()) mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val imageFile = File(imageDir, fileName)

            FileOutputStream(imageFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }
            if (scaled !== oriented && !scaled.isRecycled) scaled.recycle()
            "file://${imageFile.absolutePath}"
        } catch (e: Exception) {
            android.util.Log.w("ImageUtils", "saveCompressedImage failed", e)
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
            android.util.Log.w("ImageUtils", "decodeSampledFromUri failed", e)
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
     * 图片载荷打包结果：[entries] 为实际嵌入备份的图片，[skippedCount] 为因超出总体积预算被跳过的数量。
     */
    data class ImagePayloadResult(
        val entries: List<BackupImageEntry>,
        val skippedCount: Int
    )

    /**
     * 打包备份用图片载荷：收集 [referencedUrls] 中指向 [imagesDir] 的文件并 Base64 编码。
     * 仅嵌入存在且不超过单图上限的文件；同时受 [maxTotalBytes] 总预算约束（按 Base64 编码后体积计），
     * 超出的图片跳过并计入 skippedCount，避免大量商品图一次性全量进内存导致 OOM。
     * 涉及磁盘 IO，须在 IO 线程调用。
     */
    fun collectImagePayloads(
        imagesDir: File?,
        referencedUrls: Collection<String>,
        maxBytesPerImage: Long = 4L * 1024 * 1024,
        maxTotalBytes: Long = 64L * 1024 * 1024
    ): ImagePayloadResult {
        val dir = imagesDir?.takeIf { it.exists() } ?: return ImagePayloadResult(emptyList(), 0)
        val names = referencedUrls
            .filter { it.startsWith("file://") }
            .mapNotNull { url -> File(url.removePrefix("file://")).name }
            .toSet()
        if (names.isEmpty()) return ImagePayloadResult(emptyList(), 0)
        var usedBytes = 0L
        var skipped = 0
        val entries = ArrayList<BackupImageEntry>()
        for (name in names) {
            val entry = runCatching {
                val f = File(dir, name)
                if (f.exists() && f.length() in 1..maxBytesPerImage) {
                    BackupImageEntry(
                        name = name,
                        b64 = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
                    )
                } else null
            }.getOrNull() ?: continue
            // 预算按编码后体积计（b64.length 即编码后字节数，NO_WRAP 纯 ASCII），这才是真正写入备份文件的量
            if (usedBytes + entry.b64.length > maxTotalBytes) {
                skipped++
                continue
            }
            usedBytes += entry.b64.length
            entries.add(entry)
        }
        return ImagePayloadResult(entries, skipped)
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
