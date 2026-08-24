package com.stockmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ---------- 修复回归测试 ----------

    @Test
    fun `旧版备份缺少 categories 或 locations 键仍可解析`() {
        val legacyJson = """
            {
              "backupTime": "2025-01-01 00:00:00",
              "itemCount": 1,
              "transactionCount": 1,
              "items": [${"""{"id":"1","sku":"S","barcode":"","name":"n","category":"c","stock":1,"minStock":0,"unitCost":1.0,"unitPrice":2.0,"location":"l","updatedAt":"2025-01-01T00:00:00"}"""}],
              "transactions": []
            }
        """.trimIndent()
        val parsed = BackupManager.parseBackupBundle(legacyJson)
        assertNotNull("旧备份必须可解析", parsed)
        assertEquals(1, parsed!!.items.size)
        assertTrue(parsed.categories.isEmpty())
        assertTrue(parsed.locations.isEmpty())
    }

    @Test
    fun `版本过新的备份被拒绝并给出明确错误`() {
        val future = """
            {"version":99,"backupTime":"t","itemCount":0,"transactionCount":0,"items":[],"transactions":[],"categories":[],"locations":[]}
        """.trimIndent()
        var message: String? = null
        val parsed = BackupManager.parseBackupBundle(future) { message = it }
        assertNull(parsed)
        assertNotNull(message)
        assertTrue(message!!.contains("99"))
    }

    @Test
    fun `计数与实际不一致时给出告警但仍恢复`() {
        val tampered = """
            {"version":1,"backupTime":"t","itemCount":7,"transactionCount":7,
             "items":[{"id":"1","sku":"S","barcode":"","name":"n","category":"c","stock":1,"minStock":0,"unitCost":1.0,"unitPrice":2.0,"location":"l","updatedAt":"2025-01-01T00:00:00"}],
             "transactions":[],"categories":[],"locations":[]}
        """.trimIndent()
        var warning: String? = null
        val parsed = BackupManager.parseBackupBundle(tampered) { warning = it }
        assertNotNull("截断备份应尽量恢复", parsed)
        assertEquals(1, parsed!!.items.size)
        assertNotNull(warning)
    }

    @Test
    fun `缺少计数字段的旧备份不触发数量告警`() {
        val legacy = """
            {"version":1,"backupTime":"t",
             "items":[{"id":"1","sku":"S","barcode":"","name":"n","category":"c","stock":1,"minStock":0,"unitCost":1.0,"unitPrice":2.0,"location":"l","updatedAt":"2025-01-01T00:00:00"}],
             "transactions":[],"categories":["c"],"locations":["l"]}
        """.trimIndent()
        var warning: String? = null
        val parsed = BackupManager.parseBackupBundle(legacy) { warning = it }
        assertNotNull("缺计数字段属正常历史格式，必须可恢复", parsed)
        assertEquals(1, parsed!!.items.size)
        assertEquals("缺键 ≠ 声明 0，不应误报数量校验失败", null, warning)
    }
}
