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
    val backupTime: String = "",
    // 可空：旧版/Web 版备份可能不含计数字段，缺键（null）与「声明 0 但实际非空」是两回事，
    // 前者是正常历史格式，后者才可能是截断/篡改
    val itemCount: Int? = null,
    val transactionCount: Int? = null,
    val items: List<InventoryItem> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    // 给默认值：旧版本/Web 版备份可能不含这两个键，缺失时按空集处理而非解析失败
    val categories: List<String> = emptyList(),
    val locations: List<String> = emptyList()
)

object BackupManager {

    /** 当前 App 支持的备份格式版本。 */
    const val SUPPORTED_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
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
     * 解析全量 JSON 备份包。
     * [onError] 用于向用户暴露可行动的错误分类（版本过新 / 格式错误 / 计数不一致）。
     */
    fun parseBackupBundle(jsonString: String, onError: (String) -> Unit = {}): BackupBundle? {
        val bundle = try {
            json.decodeFromString<BackupBundle>(jsonString)
        } catch (e: Exception) {
            onError("无法解析该备份文件，格式可能不正确")
            null
        } ?: return null

        if (bundle.version > SUPPORTED_VERSION) {
            onError("备份版本过新（v${bundle.version}），请先升级 App 再恢复")
            return null
        }

        // 声明了计数且与实际内容不一致，说明备份可能被截断或篡改；仍恢复但明确告知
        val countMismatch = (bundle.itemCount != null && bundle.itemCount != bundle.items.size) ||
            (bundle.transactionCount != null && bundle.transactionCount != bundle.transactions.size)
        if (countMismatch) {
            val declared = "${bundle.itemCount ?: "?"}/${bundle.transactionCount ?: "?"}"
            onError("警告：备份数量校验不一致（声明 $declared，实际 ${bundle.items.size}/${bundle.transactions.size}），已尽量恢复")
        }
        return bundle
    }
}
