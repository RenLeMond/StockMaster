package com.stockmaster.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 纯 JVM 的 JSON 列表文件存储核心（不依赖 Android 类，便于单元测试）。
 *
 * 原子写入策略：写 .tmp → fsync → rename 替换。
 * 恢复策略：
 * - 主文件缺失但 tmp 存在：tmp 是 rename 失败前的最新数据，解析成功则提升为主文件；
 * - 主文件存在且 tmp 更新（上次 rename 失败）：择优恢复；tmp 解析失败只隔离 tmp，绝不触碰完好的主文件；
 * - 主文件损坏：隔离为 .corrupt-<ts>-<uuid> 留底，返回 null 由上层兜底；
 * - 主文件存在但内容空白：视为缺失返回 null（与「文件不存在」语义一致，保证预设播种等上层行为统一）。
 */
class JsonFileStore(
    private val dir: File,
    private val json: Json,
    private val onError: (message: String, error: Throwable?) -> Unit = { _, _ -> }
) {

    fun <T> readList(name: String, itemSerializer: KSerializer<T>): List<T>? {
        val file = File(dir, name)
        val tempFile = File(dir, "$name.tmp")
        return try {
            if (!file.exists()) {
                if (tempFile.exists()) {
                    val recovered = parseTemp(tempFile, itemSerializer)
                    if (recovered != null) {
                        moveReplace(tempFile, file)
                        return recovered
                    }
                }
                return null
            }
            // 上次 rename 失败时 tmp 可能比主文件更新，择优恢复
            if (tempFile.exists() && tempFile.lastModified() > file.lastModified()) {
                val recovered = parseTemp(tempFile, itemSerializer)
                if (recovered != null) {
                    moveReplace(tempFile, file)
                    return recovered
                }
                // tmp 自身损坏：只隔离 tmp，主文件保持完好
                onError("临时文件损坏已隔离: ${tempFile.name}", null)
                isolateCorrupt(tempFile)
            }
            val text = file.readText()
            if (text.isBlank()) return null
            decodeTolerant(text, itemSerializer)
        } catch (e: Exception) {
            onError("读取失败: $name", e)
            isolateCorrupt(file)
            null
        }
    }

    /**
     * 逐元素容错解码：单条记录缺字段/枚举未知时跳过该条并上报，
     * 而不是让整份文件报废进隔离流程。整体 JSON 语法错误仍抛出（走隔离）。
     */
    private fun <T> decodeTolerant(text: String, itemSerializer: KSerializer<T>): List<T> {
        val array: JsonArray = json.parseToJsonElement(text).jsonArray
        val result = ArrayList<T>(array.size)
        for ((index, element) in array.withIndex()) {
            try {
                result.add(json.decodeFromJsonElement(itemSerializer, element))
            } catch (e: Exception) {
                onError("跳过无法解析的第 ${index + 1} 条记录", e)
            }
        }
        return result
    }

    fun <T> writeList(name: String, list: List<T>, itemSerializer: KSerializer<T>) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                onError("目录创建失败: $dir", null)
                return
            }
            val tempFile = File(dir, "$name.tmp")
            val data = json.encodeToString(ListSerializer(itemSerializer), list)
            FileOutputStream(tempFile).use { fos ->
                fos.write(data.toByteArray(Charsets.UTF_8))
                fos.fd.sync() // 掉电耐久：数据落盘后再原子替换
            }
            if (!moveReplace(tempFile, File(dir, name))) {
                onError("写入失败，数据保留在 ${tempFile.absolutePath}，下次读取时将自动恢复", null)
            }
        } catch (e: Exception) {
            onError("保存失败: $name", e)
        }
    }

    /** 解析 tmp 内容；任何失败返回 null（由调用方决定隔离 tmp）。 */
    private fun <T> parseTemp(tempFile: File, itemSerializer: KSerializer<T>): List<T>? = try {
        val text = tempFile.readText()
        if (text.isNotBlank()) json.decodeFromString(ListSerializer(itemSerializer), text) else null
    } catch (e: Exception) {
        onError("恢复临时文件失败: ${tempFile.name}", e)
        null
    }

    /**
     * 隔离损坏文件留底。命名带 UUID 避免同毫秒撞名顶掉上一份留底；
     * rename 失败时上报而不静默——调用方据此禁止覆盖写回，保护现场。
     */
    private fun isolateCorrupt(file: File) {
        try {
            if (file.exists()) {
                val backup = File(
                    file.parentFile,
                    "${file.name}.corrupt-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
                )
                if (!file.renameTo(backup)) {
                    onError("损坏文件隔离失败（原样保留）: ${file.name}", null)
                }
            }
        } catch (e: Exception) {
            onError("保留损坏文件失败: ${file.name}", e)
        }
    }

    /**
     * 替换式移动：Android (Linux) 下 rename 原子替换目标；
     * 若个别设备/桌面 JVM 不支持替换语义，回退为删旧再改名（非原子，仅作兜底）。
     */
    private fun moveReplace(src: File, dst: File): Boolean {
        return try {
            if (src.renameTo(dst)) return true
            if (dst.exists() && !dst.delete()) return false
            src.renameTo(dst)
        } catch (e: Exception) {
            onError("移动文件失败: ${src.name} -> ${dst.name}", e)
            false
        }
    }
}
