package com.stockmaster.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupBundle(
    val version: Int = 1,
    val appName: String = "StockMaster",
    val backupTime: String,
    val itemCount: Int,
    val transactionCount: Int,
    val items: List<InventoryItem>,
    val transactions: List<TransactionRecord>,
    val categories: List<String>,
    val locations: List<String>
)

object BackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun createBackupBundle(
        items: List<InventoryItem>,
        transactions: List<TransactionRecord>,
        categories: List<String>,
        locations: List<String>
    ): BackupBundle {
        return BackupBundle(
            backupTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            itemCount = items.size,
            transactionCount = transactions.size,
            items = items,
            transactions = transactions,
            categories = categories,
            locations = locations
        )
    }

    fun encodeBackupBundle(bundle: BackupBundle): String {
        return json.encodeToString(bundle)
    }

    /**
     * 解析全量 JSON 备份包
     */
    fun parseBackupBundle(jsonString: String): BackupBundle? {
        return try {
            json.decodeFromString<BackupBundle>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
