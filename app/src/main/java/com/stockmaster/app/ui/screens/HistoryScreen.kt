package com.stockmaster.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.CsvManager
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.components.EmptyState
import com.stockmaster.app.ui.components.MoneyDisplay
import com.stockmaster.app.ui.components.SMDropdownMenu
import com.stockmaster.app.ui.components.SMDropdownMenuItem
import com.stockmaster.app.ui.components.SelectChip
import com.stockmaster.app.ui.components.glassBorder
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderBlue
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.DividerColor
import com.stockmaster.app.ui.theme.GreenBorder
import com.stockmaster.app.ui.theme.GreenLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.GreenTint
import com.stockmaster.app.ui.theme.RedBorder
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.RedTint
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.Fmt

@Composable
fun HistoryScreen(
    transactions: List<TransactionRecord>,
    onImportTransactions: ((List<TransactionRecord>) -> Unit)? = null
) {
    val context = LocalContext.current
    var filterType by remember { mutableStateOf("ALL") }
    var selectedMonth by remember { mutableStateOf("ALL") }
    var monthMenuOpen by remember { mutableStateOf(false) }

    val availableMonths = remember(transactions) {
        val months = transactions.mapNotNull { Fmt.monthLabel(it.timestamp).ifEmpty { null } }.distinct()
        listOf("ALL") + months
    }

    val filtered = remember(transactions, filterType, selectedMonth) {
        transactions.filter { tx ->
            val matchType = filterType == "ALL" || tx.type.name == filterType
            val matchMonth = selectedMonth == "ALL" || Fmt.monthLabel(tx.timestamp) == selectedMonth
            matchType && matchMonth
        }
    }

    val totalInCount = filtered.filter { it.type == TxType.IN }.sumOf { it.quantity.toLong() }
    val totalOutCount = filtered.filter { it.type == TxType.OUT }.sumOf { it.quantity.toLong() }
    val totalInAmount = filtered.filter { it.type == TxType.IN }.sumOf {
        it.totalPrice.let { p -> if (p > 0) p else it.quantity * it.unitPrice }
    }
    val totalOutAmount = filtered.filter { it.type == TxType.OUT }.sumOf {
        it.totalPrice.let { p -> if (p > 0) p else it.quantity * it.unitPrice }
    }

    val grouped = remember(filtered) {
        val map = LinkedHashMap<String, MutableList<TransactionRecord>>()
        filtered.forEach { tx ->
            val label = Fmt.dateGroupLabel(tx.timestamp).ifEmpty { "未知日期" }
            map.getOrPut(label) { mutableListOf() }.add(tx)
        }
        map.toList()
    }



    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── 标题栏 + 月份选择 ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("出入库流水", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "共记录 ${transactions.size} 笔单据",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 月份筛选
                    if (availableMonths.size > 1) {
                        val monthBtnShape = RoundedCornerShape(12.dp)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(monthBtnShape)
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), monthBtnShape)
                                    .clickable { monthMenuOpen = true }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (selectedMonth == "ALL") "全部月份" else selectedMonth,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            SMDropdownMenu(
                                expanded = monthMenuOpen,
                                onDismissRequest = { monthMenuOpen = false }
                            ) {
                                availableMonths.forEach { m ->
                                    SMDropdownMenuItem(
                                        text = if (m == "ALL") "全部月份流水" else m,
                                        selected = selectedMonth == m,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                tint = if (selectedMonth == m) GreenPrimary else TextMuted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        onClick = {
                                            selectedMonth = m
                                            monthMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }

        // ── 统计双卡（发光晶体玻璃） ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 总入库卡
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(18.dp),
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = Color(0xFF34D399).copy(alpha = 0.30f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))))
                        .glassBorder(18.dp)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text("总入库", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "+${Fmt.int(totalInCount)}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("成本 ¥${Fmt.moneyRaw(totalInAmount)}", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                }

                // 总出库卡
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(18.dp),
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = Color(0xFFF87171).copy(alpha = 0.30f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C))))
                        .glassBorder(18.dp)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text("总出库", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "-${Fmt.int(totalOutCount)}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("销售 ¥${Fmt.moneyRaw(totalOutAmount)}", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }
        }

        // ── 类型筛选 chips ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    text = "全部 (${transactions.size})",
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" }
                )
                SelectChip(
                    text = "入库 (${transactions.count { it.type == TxType.IN }})",
                    selected = filterType == "IN",
                    onClick = { filterType = "IN" },
                    selectedColor = GreenPrimary
                )
                SelectChip(
                    text = "出库 (${transactions.count { it.type == TxType.OUT }})",
                    selected = filterType == "OUT",
                    onClick = { filterType = "OUT" },
                    selectedColor = RedPrimary
                )
            }
        }

        // ── 分组流水列表 ──
        if (grouped.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "暂无匹配的流水记录",
                    subtitle = "通过扫码或快速出入库即可自动生成单据"
                )
            }
        } else {
            grouped.forEach { (label, txList) ->
                // 日期分组标题
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                    }
                }
                // 当天流水卡片列表
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))))
                            .glassBorder(18.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        txList.forEachIndexed { index, tx ->
                            HistoryRow(tx)
                            if (index < txList.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .height(1.dp)
                                        .background(DividerColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(tx: TransactionRecord) {
    val isIn = tx.type == TxType.IN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 类型图标
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isIn) GreenTint else RedTint,
                    RoundedCornerShape(13.dp)
                )
                .border(
                    1.dp,
                    if (isIn) GreenBorder else RedBorder,
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isIn) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = if (isIn) GreenPrimary else RedPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tx.itemName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    (if (isIn) "+" else "-") + tx.quantity,
                    color = if (isIn) GreenPrimary else RedPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${Fmt.formattedTime(tx.timestamp)} · ${tx.reason}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "SKU: ${tx.sku} · ${tx.location}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MoneyDisplay(
                    amount = if (tx.totalPrice > 0) tx.totalPrice else tx.quantity * tx.unitPrice,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    symbolColor = TextSecondary,
                    decimalColor = TextMuted
                )
            }
        }
    }
}