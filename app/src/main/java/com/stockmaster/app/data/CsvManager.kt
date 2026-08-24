package com.stockmaster.app.data

import java.io.InputStream
import java.security.MessageDigest
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CSV 导入导出，与 Web 版格式完全一致（UTF-8 BOM）。
 */
object CsvManager {

    fun exportItemsCsv(items: List<InventoryItem>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM
        sb.appendLine("SKU,产品名称,条形码,分类,当前库存,预警阈值,单位成本(元),单位售价(元),存放库位,计量单位,说明")
        items.forEach { item ->
            sb.appendLine(
                "${csv(item.sku)},${csv(item.name)},${csv(item.barcode)},${csv(item.category)},${item.stock}," +
                    "${item.minStock},${fmtNum(item.unitCost)},${fmtNum(item.unitPrice)}," +
                    "${csv(item.location)},${csv(item.unit)},${csv(item.description)}"
            )
        }
        return sb.toString()
    }

    fun exportTransactionsCsv(transactions: List<TransactionRecord>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("流水单号,类型,时间,商品名称,SKU,变动数量,单价(元),总额(元),库位,业务事由")
        transactions.forEach { tx ->
            val typeStr = if (tx.type == TxType.IN) "入库" else "出库"
            sb.appendLine(
                "${csv(tx.id)},${csv(typeStr)},${csv(tx.timestamp)},${csv(tx.itemName)},${csv(tx.sku)}," +
                    "${tx.quantity},${fmtNum(tx.unitPrice)},${fmtNum(tx.totalPrice)}," +
                    "${csv(tx.location)},${csv(tx.reason)}"
            )
        }
        return sb.toString()
    }

    /**
     * 双引号包裹并转义内部引号，保证含逗号/引号字段不破坏 CSV 结构。
     * 同时中和 Excel/WPS 公式注入：以 = + - @ Tab CR 开头的单元格前置单引号（OWASP 建议）。
     */
    private fun csv(v: String): String {
        val safe = if (v.isNotEmpty() && v[0] in "=+-@\t\r") "'$v" else v
        return "\"${safe.replace("\"", "\"\"")}\""
    }

    fun parseItemsCsv(stream: InputStream): List<InventoryItem> {
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parseItemsCsv(text)
    }

    fun parseTransactionsCsv(stream: InputStream): List<TransactionRecord> {
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parseTransactionsCsv(text)
    }

    fun parseTransactionsCsv(text: String): List<TransactionRecord> {
        val rows = parseCsv(text)
        if (rows.size <= 1) return emptyList()

        val list = mutableListOf<TransactionRecord>()

        for (i in 1 until rows.size) {
            val values = rows[i]
            if (values.size >= 4) {
                val id = values.getOrNull(0).orEmpty().ifEmpty { stableTxId(values) }
                val typeStr = values.getOrNull(1).orEmpty()
                val type = if (typeStr.contains("出") || typeStr.equals("OUT", ignoreCase = true)) TxType.OUT else TxType.IN
                val timestamp = values.getOrNull(2).orEmpty().ifEmpty { LocalDateTime.now().toString() }
                val itemName = values.getOrNull(3).orEmpty()
                val sku = values.getOrNull(4).orEmpty()
                // 脏数据不放大：数量解析失败按 0 处理并钳制非负，而不是默认记一笔真实入库
                val quantity = (values.getOrNull(5)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val unitPrice = (values.getOrNull(6)?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                val totalPrice = values.getOrNull(7)?.toDoubleOrNull()?.coerceAtLeast(0.0)
                    ?: (quantity * unitPrice)
                val location = values.getOrNull(8).orEmpty().ifEmpty { "默认主仓库" }
                val reason = values.getOrNull(9).orEmpty().ifEmpty { if (type == TxType.IN) "CSV导入入库" else "CSV导入出库" }

                if (itemName.isNotBlank() || sku.isNotBlank()) {
                    list.add(
                        TransactionRecord(
                            id = id,
                            itemId = "",
                            itemName = itemName,
                            sku = sku,
                            type = type,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            totalPrice = totalPrice,
                            reason = reason,
                            location = location,
                            timestamp = timestamp,
                            formattedTime = com.stockmaster.app.util.Fmt.formattedTime(timestamp)
                        )
                    )
                }
            }
        }
        return list
    }

    fun parseItemsCsv(text: String): List<InventoryItem> {
        val rows = parseCsv(text)
        if (rows.size <= 1) return emptyList()

        val items = mutableListOf<InventoryItem>()
        val now = System.currentTimeMillis()
        val nowIso = LocalDateTime.now().toString()

        for (i in 1 until rows.size) {
            val values = rows[i]
            if (values.size >= 2 && values[1].isNotEmpty()) {
                val barcode = values.getOrNull(2).orEmpty().ifEmpty { randomEan13() }
                val sku = values[0].ifEmpty { barcode }
                val name = values[1]
                val category = values.getOrNull(3).orEmpty().ifEmpty { PRESET_CATEGORIES.first() }
                // 钳制非负，避免手编 CSV 制造负库存
                val stock = (values.getOrNull(4)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val minStock = (values.getOrNull(5)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val unitCost = (values.getOrNull(6)?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                val unitPrice = (values.getOrNull(7)?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                val location = values.getOrNull(8).orEmpty().ifEmpty { PRESET_LOCATIONS.first() }
                val unit = values.getOrNull(9).orEmpty().ifEmpty { "件" }
                val description = values.getOrNull(10).orEmpty()

                items.add(
                    InventoryItem(
                        id = "item-$now-$i",
                        sku = sku,
                        name = name,
                        barcode = barcode,
                        category = category,
                        stock = stock,
                        minStock = minStock,
                        maxCapacity = null,
                        unitCost = unitCost,
                        unitPrice = unitPrice,
                        location = location,
                        unit = unit,
                        description = description,
                        imageUrl = "",
                        // 与系统其余路径统一为 ISO 格式，保证时间显示与排序正确
                        updatedAt = nowIso
                    )
                )
            }
        }
        return items
    }

    /**
     * 完整 CSV 解析：支持双引号包裹字段（含逗号/换行）及 "" 转义，兼容 CRLF/LF。
     */
    private fun parseCsv(text: String): List<List<String>> {
        // 剥离 UTF-8 BOM，避免污染首个字段
        val body = text.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var insideQuote = false
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '"' && !insideQuote && field.isEmpty() -> insideQuote = true
                c == '"' && insideQuote -> {
                    if (i + 1 < body.length && body[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else insideQuote = false
                }
                c == ',' && !insideQuote -> {
                    row.add(field.toString())
                    field.clear()
                }
                (c == '\n' || c == '\r') && !insideQuote -> {
                    if (c == '\r' && i + 1 < body.length && body[i + 1] == '\n') i++
                    row.add(field.toString())
                    if (row.any { it.isNotEmpty() }) rows.add(row)
                    row = mutableListOf()
                    field.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotEmpty() }) rows.add(row)
        }
        return rows
    }

    /**
     * 由行内容生成稳定的流水 ID（不含时间戳），便于重复导入时去重/幂等重放。
     * 用「长度前缀 + 单元分隔符」拼接消除分隔歧义；SHA-256 截断 128 位降低碰撞概率。
     */
    private fun stableTxId(values: List<String>): String {
        val key = values.joinToString("\u001F") { "${it.trim().length}:${it.trim()}" }
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return "tx-" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /** 生成带合法 EAN-13 校验位的 697 开头条码。 */
    fun randomEan13(): String {
        val body = "697" + (100_000_000L..899_999_999L).random()
        val sum = body.mapIndexed { idx, c -> (c - '0') * (if (idx % 2 == 0) 1 else 3) }.sum()
        val check = (10 - sum % 10) % 10
        return body + check
    }

    fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private fun fmtNum(v: Double): String =
        // 禁用千分位分组：带逗号的金额会破坏 CSV 列结构，且再导入时 toDoubleOrNull 解析失败
        NumberFormat.getNumberInstance(Locale.US).apply {
            isGroupingUsed = false
            maximumFractionDigits = 2
        }.format(v)
}