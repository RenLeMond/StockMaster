package com.stockmaster.app.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {

    private val moneyFormat = NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val intFormat = NumberFormat.getNumberInstance(Locale.CHINA)

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    fun money(v: Double): String = "¥" + moneyFormat.format(v)

    fun moneyRaw(v: Double): String = moneyFormat.format(v)

    fun int(v: Int): String = intFormat.format(v)

    fun int(v: Long): String = intFormat.format(v)

    fun parseIso(iso: String): LocalDateTime? {
        val value = iso.trim()
        // 带时区（Z 或 ±hh:mm）→ 统一换算系统时区，保证同一时刻的两种表示显示一致
        runCatching {
            return LocalDateTime.ofInstant(OffsetDateTime.parse(value).toInstant(), ZoneId.systemDefault())
        }
        // 兜底：纯 UTC Instant 形式
        runCatching {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault())
        }
        // 无时区信息：按本地挂钟时间解析
        return runCatching { LocalDateTime.parse(value) }.getOrNull()
    }

    /** 相对时间展示：今天显示"今天 HH:mm"，历史日期显示"x年x月x日 HH:mm"。 */
    fun formattedTime(timestamp: String): String {
        // 统一使用系统时区比较，避免跨时区备份恢复后日期错位
        val dt = parseIso(timestamp) ?: return ""
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val time = timeFormatter.format(dt)
        return if (dt.toLocalDate() == now.toLocalDate()) {
            "今天 $time"
        } else {
            "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 $time"
        }
    }

    /** 历史分组的日期标题。 */
    fun dateGroupLabel(timestamp: String): String {
        val dt = parseIso(timestamp) ?: return ""
        val today = LocalDateTime.now(ZoneId.systemDefault())
        val yesterday = today.minusDays(1)
        val date = dt.toLocalDate()
        val label = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日"
        return when (date) {
            today.toLocalDate() -> "今天 (${dt.monthValue}月${dt.dayOfMonth}日)"
            yesterday.toLocalDate() -> "昨天 (${dt.monthValue}月${dt.dayOfMonth}日)"
            else -> label
        }
    }

    /** 月份选项：2026年 8月 */
    fun monthLabel(timestamp: String): String {
        val dt = parseIso(timestamp) ?: return ""
        // parseIso 已换算至 systemDefault，此处无需再转区
        return "${dt.year}年 ${dt.monthValue}月"
    }
}