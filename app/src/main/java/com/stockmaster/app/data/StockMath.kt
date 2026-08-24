package com.stockmaster.app.data

/**
 * 出入库的纯库存计算，无 Android 依赖，便于单元测试与 ViewModel 解耦。
 * 约定：所有写入路径统一经过这里，保证多尺码明细与总库存的一致性口径。
 */
object StockMath {

    /** 校验一笔出库是否可行；返回错误描述，null 表示允许。 */
    fun checkOut(
        item: InventoryItem,
        quantity: Int,
        size: String?,
        sizeBreakdown: List<SizeBreakdown>
    ): String? {
        if (!item.hasSizes || item.sizeVariants.isEmpty()) {
            return if (item.stock < quantity) "库存不足（当前余量 ${item.stock}）" else null
        }
        val knownSizes = item.sizeVariants.map { it.size }.toSet()
        if (sizeBreakdown.isNotEmpty()) {
            // 尺码矩阵中不存在的尺码直接拒绝，与 IN 方向保持对称，避免流水与库存永久对不上
            val unknown = sizeBreakdown.firstOrNull { it.size !in knownSizes }
            if (unknown != null) return "尺码「${unknown.size}」不属于该商品的尺码矩阵"
            val insufficient = sizeBreakdown.firstOrNull { b ->
                (item.sizeVariants.firstOrNull { it.size == b.size }?.stock ?: 0) < b.quantity
            }
            if (insufficient != null) return "尺码「${insufficient.size}」库存不足"
            return null
        }
        if (size != null) {
            if (size !in knownSizes) return "尺码「$size」不属于该商品的尺码矩阵"
            val cur = item.sizeVariants.firstOrNull { it.size == size }?.stock ?: 0
            return if (cur < quantity) "尺码「$size」库存不足（当前余量 $cur）" else null
        }
        return "多尺码商品请指定尺码或按尺码配比出库"
    }

    /** 校验一笔入库是否合法（主要是尺码归属）；返回错误描述，null 表示允许。 */
    fun checkIn(
        item: InventoryItem,
        @Suppress("UNUSED_PARAMETER") quantity: Int,
        size: String?,
        sizeBreakdown: List<SizeBreakdown>
    ): String? {
        if (!item.hasSizes || item.sizeVariants.isEmpty()) return null
        val knownSizes = item.sizeVariants.map { it.size }.toSet()
        sizeBreakdown.firstOrNull { it.size !in knownSizes }?.let {
            return "尺码「${it.size}」不属于该商品的尺码矩阵"
        }
        if (size != null && size !in knownSizes) return "尺码「$size」不属于该商品的尺码矩阵"
        return null
    }

    /**
     * 对商品列表应用一笔出入库，返回更新后的列表。
     * 总库存始终以尺码明细之和为准（多尺码商品），杜绝账实分离。
     */
    fun applyToItems(
        items: List<InventoryItem>,
        itemId: String,
        isIn: Boolean,
        quantity: Int,
        size: String?,
        sizeBreakdown: List<SizeBreakdown>,
        newLocation: String?,
        nowIso: String
    ): List<InventoryItem> = items.map { item ->
        if (item.id != itemId) return@map item

        var updatedVariants = item.sizeVariants
        if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
            updatedVariants = when {
                sizeBreakdown.isNotEmpty() -> {
                    val map = sizeBreakdown.associate { it.size to it.quantity }
                    item.sizeVariants.map { v ->
                        val qty = map[v.size] ?: 0
                        val delta = if (isIn) qty else -qty
                        v.copy(stock = maxOf(0, v.stock + delta))
                    }
                }
                size != null -> item.sizeVariants.map { v ->
                    if (v.size == size) {
                        val delta = if (isIn) quantity else -quantity
                        v.copy(stock = maxOf(0, v.stock + delta))
                    } else v
                }
                else -> item.sizeVariants
            }
        }

        val finalStock = if (item.hasSizes && updatedVariants.isNotEmpty()) {
            updatedVariants.sumOf { it.stock }
        } else {
            val delta = if (isIn) quantity else -quantity
            maxOf(0, item.stock + delta)
        }

        item.copy(
            stock = finalStock,
            sizeVariants = updatedVariants,
            // 仅入库顺带迁移库位；出库不应改变商品的在库位置
            location = if (isIn && !newLocation.isNullOrEmpty()) newLocation else item.location,
            updatedAt = nowIso
        )
    }
}
