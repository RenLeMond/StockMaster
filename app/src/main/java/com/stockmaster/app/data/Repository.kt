package com.stockmaster.app.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 本地 JSON 文件持久化仓库。
 * 对应原 Web 版 localStorage + IndexedDB 的存储语义。
 */
class Repository(private val context: Context) {

    companion object {
        private const val TAG = "Repository"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val itemsFile: File get() = File(context.filesDir, "items.json")
    private val transactionsFile: File get() = File(context.filesDir, "transactions.json")
    private val categoriesFile: File get() = File(context.filesDir, "categories.json")
    private val locationsFile: File get() = File(context.filesDir, "locations.json")

    @Synchronized
    fun loadItems(): List<InventoryItem> =
        readList<InventoryItem>(itemsFile) ?: emptyList()

    @Synchronized
    fun saveItems(items: List<InventoryItem>) =
        writeList(itemsFile, items)

    @Synchronized
    fun loadTransactions(): List<TransactionRecord> =
        readList<TransactionRecord>(transactionsFile) ?: emptyList()

    @Synchronized
    fun saveTransactions(transactions: List<TransactionRecord>) =
        writeList(transactionsFile, transactions)

    @Synchronized
    fun loadCategories(): List<String> {
        readList<String>(categoriesFile)?.let { return it }
        saveCategories(PRESET_CATEGORIES)
        return PRESET_CATEGORIES
    }

    @Synchronized
    fun saveCategories(categories: List<String>) {
        val unique = categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        writeList(categoriesFile, unique)
    }

    @Synchronized
    fun loadLocations(): List<String> {
        readList<String>(locationsFile)?.let { return it }
        saveLocations(PRESET_LOCATIONS)
        return PRESET_LOCATIONS
    }

    @Synchronized
    fun saveLocations(locations: List<String>) {
        val unique = locations.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        writeList(locationsFile, unique)
    }

    private inline fun <reified T> readList(file: File): List<T>? {
        return try {
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            if (!file.exists()) {
                if (tempFile.exists()) {
                    val text = tempFile.readText()
                    if (text.isNotBlank()) {
                        val parsed = json.decodeFromString<List<T>>(text)
                        moveReplace(tempFile, file)
                        return parsed
                    }
                }
                return null
            }
            // 上次 rename 失败时 tmp 可能比主文件更新，择优恢复
            if (tempFile.exists() && tempFile.lastModified() > file.lastModified()) {
                val text = tempFile.readText()
                if (text.isNotBlank()) {
                    val parsed = json.decodeFromString<List<T>>(text)
                    moveReplace(tempFile, file)
                    return parsed
                }
            }
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            json.decodeFromString<List<T>>(text)
        } catch (e: Exception) {
            Log.e(TAG, "读取失败: ${file.name}", e)
            // 文件损坏时保留现场，避免下次保存覆盖丢失数据
            preserveCorruptFile(file)
            null
        }
    }

    private fun preserveCorruptFile(file: File) {
        try {
            if (file.exists()) {
                val backup = File(file.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}")
                file.renameTo(backup)
            }
        } catch (e: Exception) {
            Log.e(TAG, "保留损坏文件失败: ${file.name}", e)
        }
    }

    private inline fun <reified T> writeList(file: File, list: List<T>) {
        try {
            val parent = file.parentFile ?: return
            if (!parent.exists() && !parent.mkdirs()) {
                Log.e(TAG, "目录创建失败: $parent")
                return
            }
            val tempFile = File(parent, "${file.name}.tmp")
            tempFile.writeText(json.encodeToString(list))
            if (!moveReplace(tempFile, file)) {
                Log.e(TAG, "写入失败，数据保留在 ${tempFile.absolutePath}，下次读取时将自动恢复")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存失败: ${file.name}", e)
        }
    }

    /**
     * 替换式移动：Android (Linux) 下 rename 原子替换目标；
     * 若个别设备不支持替换语义，回退为删旧再改名。
     */
    private fun moveReplace(src: File, dst: File): Boolean {
        return try {
            if (src.renameTo(dst)) return true
            if (dst.exists() && !dst.delete()) return false
            src.renameTo(dst)
        } catch (e: Exception) {
            Log.e(TAG, "移动文件失败: ${src.name} -> ${dst.name}", e)
            false
        }
    }
}
