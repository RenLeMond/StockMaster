package com.stockmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockMathTest {

    private fun item(
        stock: Int = 10,
        hasSizes: Boolean = false,
        variants: List<SizeVariant> = emptyList()
    ) = InventoryItem(
        id = "i1",
        sku = "S1",
        barcode = "",
        name = "测试商品",
        category = "分类",
        stock = stock,
        minStock = 0,
        unitCost = 1.0,
        unitPrice = 2.0,
        location = "默认主仓库",
        hasSizes = hasSizes,
        sizeVariants = variants,
        updatedAt = "2026-01-01T00:00:00"
    )

    // ---------- checkOut ----------

    @Test
    fun `单规格出库库存不足被拒`() {
        val error = StockMath.checkOut(item(stock = 5), quantity = 6, size = null, sizeBreakdown = emptyList())
        assertNotNull(error)
        assertTrue(error!!.contains("不足"))
    }

    @Test
    fun `单规格出库数量恰好等于库存放行`() {
        assertNull(StockMath.checkOut(item(stock = 5), quantity = 5, size = null, sizeBreakdown = emptyList()))
    }

    @Test
    fun `多尺码出库未指定尺码防御性拒绝`() {
        val it = item(hasSizes = true, variants = listOf(SizeVariant("M", 10)))
        assertNotNull(StockMath.checkOut(it, 3, size = null, sizeBreakdown = emptyList()))
    }

    @Test
    fun `多尺码出库某尺码不足被拒`() {
        val it = item(hasSizes = true, variants = listOf(SizeVariant("M", 3), SizeVariant("L", 9)))
        val breakdown = listOf(SizeBreakdown("M", 4), SizeBreakdown("L", 1))
        assertNotNull(StockMath.checkOut(it, 5, size = null, sizeBreakdown = breakdown))
    }

    @Test
    fun `多尺码出库含未知尺码被拒`() {
        val it = item(hasSizes = true, variants = listOf(SizeVariant("M", 10)))
        assertNotNull(StockMath.checkOut(it, 2, size = "XL", sizeBreakdown = emptyList()))
        assertNotNull(StockMath.checkOut(it, 2, size = null, sizeBreakdown = listOf(SizeBreakdown("XL", 2))))
    }

    // ---------- checkIn ----------

    @Test
    fun `入库未知尺码与出库同样被拒（对称性）`() {
        val it = item(hasSizes = true, variants = listOf(SizeVariant("M", 0)))
        assertNotNull(StockMath.checkIn(it, 5, size = "XL", sizeBreakdown = emptyList()))
        assertNotNull(StockMath.checkIn(it, 5, size = null, sizeBreakdown = listOf(SizeBreakdown("XL", 5))))
        assertNull(StockMath.checkIn(it, 5, size = "M", sizeBreakdown = emptyList()))
    }

    // ---------- applyToItems ----------

    @Test
    fun `单规格入库累加总库存`() {
        val updated = StockMath.applyToItems(
            listOf(item(stock = 4)), "i1", isIn = true,
            quantity = 6, size = null, sizeBreakdown = emptyList(),
            newLocation = null, nowIso = "2026-01-02T00:00:00"
        )
        assertEquals(10, updated[0].stock)
    }

    @Test
    fun `单规格出库钳制不为负`() {
        val updated = StockMath.applyToItems(
            listOf(item(stock = 3)), "i1", isIn = false,
            quantity = 10, size = null, sizeBreakdown = emptyList(),
            newLocation = null, nowIso = "2026-01-02T00:00:00"
        )
        assertEquals(0, updated[0].stock)
    }

    @Test
    fun `多尺码按配比扣减且总库存等于明细之和`() {
        val origin = item(hasSizes = true, variants = listOf(SizeVariant("M", 5), SizeVariant("L", 7)))
        val breakdown = listOf(SizeBreakdown("M", 3), SizeBreakdown("L", 4))
        val updated = StockMath.applyToItems(
            listOf(origin), "i1", isIn = false, quantity = 7,
            size = null, sizeBreakdown = breakdown,
            newLocation = null, nowIso = "2026-01-02T00:00:00"
        )
        assertEquals(listOf(SizeVariant("M", 2), SizeVariant("L", 3)), updated[0].sizeVariants)
        assertEquals(5, updated[0].stock)
    }

    @Test
    fun `多尺码单码入库只动目标尺码`() {
        val origin = item(hasSizes = true, variants = listOf(SizeVariant("M", 2), SizeVariant("L", 3)))
        val updated = StockMath.applyToItems(
            listOf(origin), "i1", isIn = true, quantity = 4,
            size = "M", sizeBreakdown = emptyList(),
            newLocation = null, nowIso = "2026-01-02T00:00:00"
        )
        assertEquals(6, updated[0].sizeVariants.first { it.size == "M" }.stock)
        assertEquals(3, updated[0].sizeVariants.first { it.size == "L" }.stock)
        assertEquals(9, updated[0].stock)
    }

    @Test
    fun `仅入库顺带迁移库位 出库不改变在库位置`() {
        val single = listOf(item(stock = 5))
        val afterIn = StockMath.applyToItems(single, "i1", true, 1, null, emptyList(), "货架 2 层", "t")
        assertEquals("货架 2 层", afterIn[0].location)

        val afterOut = StockMath.applyToItems(single, "i1", false, 1, null, emptyList(), "货架 3 层", "t")
        assertEquals("默认主仓库", afterOut[0].location)
    }

    @Test
    fun `其他商品不受影响`() {
        val a = item(stock = 1)
        val b = item(stock = 2).copy(id = "i2")
        val updated = StockMath.applyToItems(listOf(a, b), "i1", false, 1, null, emptyList(), null, "t")
        assertEquals(0, updated[0].stock)
        assertEquals(2, updated[1].stock)
    }
}
