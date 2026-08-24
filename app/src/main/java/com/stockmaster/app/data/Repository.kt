package com.stockmaster.app.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 本地 JSON 文件持久化仓库。
 * 对应原 Web 版 localStorage + IndexedDB 的存储语义。
 */
class Repository(private val context: Context) {

    companion object {
        private const val TAG = "Repository"
        private const val ITEMS_FILE = "items.json"
        private const val TRANSACTIONS_FILE = "transactions.json"
        private const val CATEGORIES_FILE = "categories.json"
        private const val LOCATIONS_FILE = "locations.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // 容忍显式 null（非法 null 替换为默认值），提升旧备份/外部数据兼容性
        coerceInputValues = true
    }

    private val store = JsonFileStore(
        dir = context.filesDir,
        json = json,
        onError = { message, error -> Log.e(TAG, message, error) }
    )

    private val itemsFile: File get() = File(context.filesDir, ITEMS_FILE)
    private val transactionsFile: File get() = File(context.filesDir, TRANSACTIONS_FILE)
    private val categoriesFile: File get() = File(context.filesDir, CATEGORIES_FILE)
    private val locationsFile: File get() = File(context.filesDir, LOCATIONS_FILE)

    @Synchronized
    fun loadItems(): List<InventoryItem> =
        store.readList(ITEMS_FILE, InventoryItem.serializer()) ?: emptyList()

    @Synchronized
    fun saveItems(items: List<InventoryItem>) =
        store.writeList(ITEMS_FILE, items, InventoryItem.serializer())

    @Synchronized
    fun loadTransactions(): List<TransactionRecord> =
        store.readList(TRANSACTIONS_FILE, TransactionRecord.serializer()) ?: emptyList()

    @Synchronized
    fun saveTransactions(transactions: List<TransactionRecord>) =
        store.writeList(TRANSACTIONS_FILE, transactions, TransactionRecord.serializer())

    @Synchronized
    fun loadCategories(): List<String> {
        // 空白/缺失/损坏均视为「无数据」，播种预设分类，保证语义统一
        store.readList(CATEGORIES_FILE, String.serializer())?.let { return it }
        saveCategories(PRESET_CATEGORIES)
        return PRESET_CATEGORIES
    }

    @Synchronized
    fun saveCategories(categories: List<String>) {
        val unique = categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        store.writeList(CATEGORIES_FILE, unique, String.serializer())
    }

    @Synchronized
    fun loadLocations(): List<String> {
        store.readList(LOCATIONS_FILE, String.serializer())?.let { return it }
        saveLocations(PRESET_LOCATIONS)
        return PRESET_LOCATIONS
    }

    @Synchronized
    fun saveLocations(locations: List<String>) {
        val unique = locations.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        store.writeList(LOCATIONS_FILE, unique, String.serializer())
    }

    /** 供 clearAll 等场景清理商品图片目录。 */
    fun imagesDir(): File = File(context.filesDir, "images")
}
