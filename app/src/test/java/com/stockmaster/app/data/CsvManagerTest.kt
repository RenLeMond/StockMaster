package com.stockmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvManagerTest {

    @Test
    fun `parseItemsCsv 不 trim 字段，保留原始空格`() {
        val csv = buildString {
            append('\uFEFF')
            appendLine("SKU,产品名称,条形码,分类,当前库存,预警阈值,单位成本(元),单位售价(元),存放库位,计量单位,说明")
            appendLine("\" sku1 \",\" 名称A \",\" 690123 \",\" 饮料 \",10,2,3.5,5.0,\" 主仓 \",\" 瓶 \",\" 备注 \"")
        }
        val items = CsvManager.parseItemsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1, items.size)
        val it = items[0]
        assertEquals(" sku1 ", it.sku)
        assertEquals(" 名称A ", it.name)
        assertEquals(" 690123 ", it.barcode)
        assertEquals(" 饮料 ", it.category)
        assertEquals(" 主仓 ", it.location)
        assertEquals(" 瓶 ", it.unit)
        assertEquals(" 备注 ", it.description)
        assertEquals(10, it.stock)
    }

    @Test
    fun `items 导出后再解析保持数量一致`() {
        val sample = listOf(
            InventoryItem(
                id = "item-1", sku = "SKU1", name = "可乐", barcode = "690001", category = "饮料",
                stock = 5, minStock = 1, unitCost = 2.0, unitPrice = 3.0, location = "主仓",
                unit = "瓶", description = "测试", updatedAt = "2026-01-01T00:00:00"
            )
        )
        val csv = CsvManager.exportItemsCsv(sample)
        val parsed = CsvManager.parseItemsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1, parsed.size)
        assertEquals("SKU1", parsed[0].sku)
        assertEquals("可乐", parsed[0].name)
        assertEquals(5, parsed[0].stock)
    }

    @Test
    fun `transactions 稳定 id 支持重复导入去重`() {
        val csv = buildString {
            append('\uFEFF')
            appendLine("流水单号,类型,时间,商品名称,SKU,变动数量,单价(元),总额(元),库位,业务事由")
            appendLine("\"\",\"入库\",\"2026-01-01T10:00:00\",\"可乐\",\"SKU1\",3,2.0,6.0,\"主仓\",\"CSV导入入库\"")
        }
        val first = CsvManager.parseTransactionsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        val second = CsvManager.parseTransactionsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1, first.size)
        assertEquals(first[0].id, second[0].id)
    }
}
