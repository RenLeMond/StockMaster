package com.stockmaster.app.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {

    private val moneyFormat = NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val intFormat = NumberFormat.getNumberInstance(Locale.CHINA)

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    fun money(v: Double): String = "¥" + moneyFormat.format(v)

    fun moneyRaw(v: Double): String = moneyFormat.format(v)

    fun int(v: Int): String = intFormat.format(v)

    fun int(v: Long): String = intFormat.format(v)

    fun parseIso(iso: String): LocalDateTime? {
        return try {
            if (iso.contains("Z") || iso.contains("+")) {
                LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault())
            } else {
                LocalDateTime.parse(iso)
            }
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(iso, isoFormatter)
            } catch (e2: Exception) {
                null
            }
        }
    }

    /** 相对时间展示：今天显示"今天 HH:mm"，历史日期显示"x年x月x日 HH:mm"。 */
    fun formattedTime(timestamp: String): String {
        val dt = parseIso(timestamp) ?: return ""
        val now = LocalDateTime.now()
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
        val today = LocalDateTime.now()
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
        return "${dt.year}年 ${dt.monthValue}月"
    }
}