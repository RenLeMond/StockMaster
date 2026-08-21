package com.stockmaster.app.data

import kotlinx.serialization.Serializable

enum class TxType { IN, OUT }

@Serializable
data class SizeVariant(
    val size: String,
    val stock: Int,
    val minStock: Int = 0
)

@Serializable
data class SizeBreakdown(
    val size: String,
    val quantity: Int
)

@Serializable
data class InventoryItem(
    val id: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val category: String,
    val stock: Int,
    val minStock: Int,
    val maxCapacity: Int? = null,
    val unitCost: Double,
    val unitPrice: Double,
    val location: String,
    val imageUrl: String = "",
    val unit: String = "件",
    val description: String = "",
    val hasSizes: Boolean = false,
    val sizeVariants: List<SizeVariant> = emptyList(),
    val updatedAt: String
) {
    val isLowStock: Boolean
        get() = minStock > 0 && stock <= minStock

    val effectiveMaxCapacity: Int
        get() = maxCapacity ?: maxOf(stock * 2, minStock * 4, 100)

    fun stockPercent(): Int =
        (stock.toDouble() / effectiveMaxCapacity * 100).toInt().coerceIn(0, 100)
}

@Serializable
data class TransactionRecord(
    val id: String,
    val itemId: String,
    val itemName: String,
    val sku: String,
    val type: TxType,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val reason: String,
    val location: String,
    val size: String? = null,
    val sizeBreakdown: List<SizeBreakdown> = emptyList(),
    val timestamp: String,
    val formattedTime: String? = null,
    val imageUrl: String = ""
)

val PRESET_LOCATIONS = listOf(
    "货架 1 层",
    "货架 2 层",
    "货架 3 层",
    "储物间",
    "次卧",
    "主卧",
    "货架阳台",
    "默认主仓库"
)

val PRESET_CATEGORIES = listOf(
    "服饰",
    "鞋靴",
    "首饰",
    "潮玩盲盒",
    "日用百货",
    "美妆个护",
    "数码配件",
    "箱包皮具"
)