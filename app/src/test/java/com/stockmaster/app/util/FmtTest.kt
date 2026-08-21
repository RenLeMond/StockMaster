package com.stockmaster.app.util

import org.junit.Assert.assertEquals
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
}
