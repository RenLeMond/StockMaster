package com.stockmaster.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class TxType { IN, OUT }

@Immutable
@Serializable
data class SizeVariant(
    val size: String,
    val stock: Int,
    val minStock: Int = 0
)

@Immutable
@Serializable
data class SizeBreakdown(
    val size: String,
    val quantity: Int
)

// 纯值语义的不可变模型，标注 @Immutable 让 Compose 跳过优化生效
@Immutable
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

    // 未设置容量上限时不随 stock 水涨船高（否则 stock>=50 时进度条恒钉 50%，失去预警意义）
    val effectiveMaxCapacity: Int
        get() = maxCapacity ?: maxOf(minStock * 4, 100)

    fun stockPercent(): Int =
        (stock.toDouble() / effectiveMaxCapacity * 100).toInt().coerceIn(0, 100)
}

@Immutable
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