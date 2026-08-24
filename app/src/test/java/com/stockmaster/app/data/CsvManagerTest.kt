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

    // ---------- 修复回归测试 ----------

    @Test
    fun `大额金额导出不带千分位且可无损往返`() {
        val sample = listOf(
            InventoryItem(
                id = "item-1", sku = "SKU1", name = "数码相机", barcode = "", category = "数码",
                stock = 3, minStock = 1, unitCost = 1234.5, unitPrice = 2999.99, location = "主仓",
                unit = "台", description = "", updatedAt = "2026-01-01T00:00:00"
            )
        )
        val csv = CsvManager.exportItemsCsv(sample)
        // 关键回归：千分位会破坏列结构并导致再导入时价格归零
        assertTrue(!csv.contains("1,234"))
        assertTrue(!csv.contains("2,999"))
        val parsed = CsvManager.parseItemsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1234.5, parsed[0].unitCost, 0.001)
        assertEquals(2999.99, parsed[0].unitPrice, 0.001)
    }

    @Test
    fun `含逗号引号换行的字段导出后可完整往返`() {
        val trickyName = "名称,带\"引号\"\n和换行"
        val sample = listOf(
            InventoryItem(
                id = "item-1", sku = "S", name = trickyName, barcode = "", category = "",
                stock = 1, minStock = 0, unitCost = 0.0, unitPrice = 0.0, location = "",
                unit = "", description = "第一行\r\n第二行", updatedAt = "2026-01-01T00:00:00"
            )
        )
        val csv = CsvManager.exportItemsCsv(sample)
        val parsed = CsvManager.parseItemsCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1, parsed.size)
        assertEquals(trickyName, parsed[0].name)
        assertEquals("第一行\r\n第二行", parsed[0].description)
    }

    @Test
    fun `导入端剥离 UTF-8 BOM`() {
        val csv = "\uFEFFSKU,产品名称,条形码,分类,当前库存,预警阈值,单位成本(元),单位售价(元),存放库位,计量单位,说明\r\nA,B,C,D,1,0,0,0,E,F,G"
        val items = CsvManager.parseItemsCsv(csv)
        assertEquals(1, items.size)
        assertEquals("A", items[0].sku)
        assertTrue(!items[0].sku.startsWith("\uFEFF"))
    }

    @Test
    fun `负数与脏数值被钳制而非放大`() {
        val csv = buildString {
            appendLine("流水单号,类型,时间,商品名称,SKU,变动数量,单价(元),总额(元),库位,业务事由")
            appendLine("\"\",\"出库\",\"t\",\"脏数据\",\"S\",-5,-3,-15,\"主仓\",\"x\"")
        }
        val txs = CsvManager.parseTransactionsCsv(csv)
        assertEquals(1, txs.size)
        assertEquals(0, txs[0].quantity)
        assertEquals(0.0, txs[0].unitPrice, 0.0001)
    }

    @Test
    fun `stableTxId 对分隔符歧义内容不碰撞`() {
        val rowA = listOf("", "入库", "t", "a|b", "c", "1", "0", "0", "", "")
        val rowB = listOf("", "入库", "t", "a", "b|c", "1", "0", "0", "", "")
        val idA = reflectStableTxId(rowA)
        val idB = reflectStableTxId(rowB)
        assertTrue(idA != idB)
    }

    @Test
    fun `randomEan13 具有合法校验位`() {
        repeat(20) {
            val code = CsvManager.randomEan13()
            assertEquals(13, code.length)
            assertTrue(code.startsWith("697"))
            val body = code.substring(0, 12)
            val sum = body.mapIndexed { idx, c -> (c - '0') * (if (idx % 2 == 0) 1 else 3) }.sum()
            val check = (10 - sum % 10) % 10
            assertEquals(check, (code[12] - '0'))
        }
    }

    /** 通过解析路径间接访问 private stableTxId：空单号行走 stableTxId 分支。 */
    private fun reflectStableTxId(values: List<String>): String =
        parseTxRowsForId(values)

    companion object {
        private fun parseTxRowsForId(values: List<String>): String {
            val body = values.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            val header = "流水单号,类型,时间,商品名称,SKU,变动数量,单价(元),总额(元),库位,业务事由"
            val parsed = CsvManager.parseTransactionsCsv("$header\r\n$body")
            return parsed.first().id
        }
    }
}
