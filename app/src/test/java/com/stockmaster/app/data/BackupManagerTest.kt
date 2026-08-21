package com.stockmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupManagerTest {

    private fun sampleItem(id: String) = InventoryItem(
        id = id,
        sku = "SKU-$id",
        barcode = "690$id",
        name = "商品$id",
        category = "饮料",
        stock = 3,
        minStock = 1,
        unitCost = 2.0,
        unitPrice = 3.0,
        location = "主仓",
        unit = "瓶",
        description = "测试",
        updatedAt = "2026-01-01T00:00:00"
    )

    private fun sampleTx(id: String) = TransactionRecord(
        id = id,
        itemId = "item-1",
        itemName = "商品1",
        sku = "SKU-item-1",
        type = TxType.IN,
        quantity = 2,
        unitPrice = 2.0,
        totalPrice = 4.0,
        reason = "初始建档录入",
        location = "主仓",
        timestamp = "2026-01-01T00:00:00"
    )

    @Test
    fun `备份编码后解析可完整还原`() {
        val bundle = BackupManager.createBackupBundle(
            items = listOf(sampleItem("1"), sampleItem("2")),
            transactions = listOf(sampleTx("t1")),
            categories = listOf("饮料", "零食"),
            locations = listOf("主仓")
        )
        val json = BackupManager.encodeBackupBundle(bundle)
        val parsed = BackupManager.parseBackupBundle(json)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.items.size)
        assertEquals(1, parsed.transactions.size)
        assertEquals("SKU-1", parsed.items[0].sku)
        assertEquals("主仓", parsed.locations[0])
    }

    @Test
    fun `非法 JSON 解析返回 null`() {
        assertNull(BackupManager.parseBackupBundle("not a json"))
    }
}
