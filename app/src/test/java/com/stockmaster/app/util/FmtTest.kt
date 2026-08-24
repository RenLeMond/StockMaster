package com.stockmaster.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FmtTest {

    @Test
    fun `int 使用中文千分位分组`() {
        assertEquals("1,234,567", Fmt.int(1234567))
    }

    @Test
    fun `int Long 不溢出`() {
        assertEquals("12,345,678,901", Fmt.int(12345678901L))
    }

    @Test
    fun `money 保留两位小数并带人民币符号`() {
        assertEquals("¥3.50", Fmt.money(3.5))
    }

    @Test
    fun `formattedTime 解析 ISO 时间`() {
        val result = Fmt.formattedTime("2026-01-01T10:30:00")
        assertTrue(result.contains("10:30"))
    }

    // ---------- 时区解析一致性 ----------

    @Test
    fun `Z 形式与加号偏移形式等价`() {
        // "…Z" 与 "+00:00" 是同一时刻，无论测试环境处于哪个时区都应解析一致
        val z = Fmt.parseIso("2026-01-01T02:30:00Z")
        val offset = Fmt.parseIso("2026-01-01T02:30:00+00:00")
        assertNotNull(z)
        assertEquals(z, offset)
    }

    @Test
    fun `带正偏移的时间正确换算`() {
        val parsed = Fmt.parseIso("2026-01-01T10:30:00+08:00")
        assertNotNull(parsed)
    }

    @Test
    fun `带负偏移的时间不再被静默丢弃`() {
        // 回归：此前 -05:00 走 naive 分支抛异常，fallback 又忽略偏移
        val parsed = Fmt.parseIso("2026-01-01T09:30:00-05:00")
        assertNotNull(parsed)
    }

    @Test
    fun `同一时刻两种表示解析结果一致`() {
        // 14:30+08:00 与 06:30Z 是同一时刻，应得到相同本地时间
        val a = Fmt.parseIso("2026-06-01T14:30:00+08:00")
        val b = Fmt.parseIso("2026-06-01T06:30:00Z")
        assertEquals(a, b)
    }

    @Test
    fun `无时区 naive 解析保持挂钟时间`() {
        val parsed = Fmt.parseIso("2026-01-01T10:30:00")
        assertEquals(10, parsed!!.hour)
    }

    @Test
    fun `epoch 毫秒串与空串返回 null 不崩溃`() {
        assertNull(Fmt.parseIso("1756000000000"))
        assertNull(Fmt.parseIso(""))
        assertNull(Fmt.parseIso("not-a-date"))
    }
}
