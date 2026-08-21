package com.stockmaster.app.ui.screens

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.components.AppCard
import com.stockmaster.app.ui.components.DoubleBezelCard
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.MoneyDisplay
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
fun DashboardScreen(
    items: List<InventoryItem>,
    transactions: List<TransactionRecord>,
    onNavigateTab: (Int) -> Unit,
    onStartScan: (TxType) -> Unit,
    onSelectItem: (String) -> Unit
) {
    val totalValue = items.sumOf { it.stock * it.unitCost }
    val totalUnits = items.sumOf { it.stock.toLong() }
    val avgValue = if (items.isEmpty()) 0.0 else totalValue / items.size

    val currentMonthPrefix = java.time.LocalDate.now().toString().take(7) // yyyy-MM
    val monthlyProfit = transactions
        .filter { it.type == TxType.OUT && it.timestamp.startsWith(currentMonthPrefix) }
        .sumOf { tx ->
            val item = items.firstOrNull { it.id == tx.itemId || it.sku == tx.sku }
            val cost = item?.unitCost ?: (tx.unitPrice * 0.7)
            val profitPerUnit = maxOf(0.0, tx.unitPrice - cost)
            profitPerUnit * tx.quantity
        }

    val lowStockItems = items.filter { it.minStock > 0 && it.stock <= it.minStock }
    val recent = transactions.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── 核心资产总览卡 (Executive Asset Card - High-End Mesh) ──
        item {
            val outerShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(outerShape)
                    .background(Color(0xFF003824).copy(alpha = 0.15f))
                    .border(0.5.dp, GreenPrimary.copy(alpha = 0.3f), outerShape)
                    .padding(3.dp)
            ) {
                val innerShape = RoundedCornerShape(21.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(innerShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF006C47), Color(0xFF00452C))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // 顶部 Eyebrow Tag 行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF34D399), RoundedCornerShape(50))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "LIVE INVENTORY ASSETS",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "共 ${items.size} 种品类",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "在库总估值 (¥)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        // 金融级货币排版
                        MoneyDisplay(
                            amount = totalValue,
                            fontSize = 36.sp,
                            color = Color.White,
                            symbolColor = Color.White.copy(alpha = 0.85f),
                            decimalColor = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        // 次要指标 3 格卡
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("在库总件数", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    Fmt.int(totalUnits),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("平均单品货值", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "¥" + Fmt.moneyRaw(avgValue),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("低库存预警", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${lowStockItems.size} 件",
                                    color = if (lowStockItems.isEmpty()) Color.White else Color(0xFFFBBF24),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 双核心指标卡：本月利润 + 低库存预警 ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // 本月毛利卡
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BlueLightBg)
                        .border(1.dp, BorderBlue, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BlueAccent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("本月毛利", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(10.dp))
                    MoneyDisplay(
                        amount = monthlyProfit,
                        showPlus = true,
                        fontSize = 20.sp,
                        color = BlueAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("出库销售毛利核算", color = BlueAccent.copy(alpha = 0.6f), fontSize = 10.sp)
                }

                // 低库存预警卡
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (lowStockItems.isEmpty()) GreenTint else RedTint)
                        .border(
                            1.dp,
                            if (lowStockItems.isEmpty()) GreenBorder else RedBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onNavigateTab(1) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (lowStockItems.isEmpty()) GreenPrimary.copy(alpha = 0.1f)
                                        else RedPrimary.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = if (lowStockItems.isEmpty()) GreenPrimary else RedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("库存预警", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = if (lowStockItems.isEmpty()) GreenPrimary else RedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (lowStockItems.isEmpty()) "库存充裕" else "${lowStockItems.size} 件品类",
                        color = if (lowStockItems.isEmpty()) GreenPrimary else RedPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (lowStockItems.isNotEmpty()) "请及时安排采购补货" else "当前各品类库存正常",
                        color = if (lowStockItems.isEmpty()) GreenPrimary.copy(alpha = 0.6f) else RedPrimary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // ── 扫码快捷操作卡 ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScanActionButton(
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(Color(0xFF007A50), Color(0xFF00A369)),
                    iconTint = Color.White,
                    textColor = Color.White,
                    title = "扫码入库",
                    subtitle = "快速增加库存",
                    onClick = { onStartScan(TxType.IN) }
                )
                ScanActionButton(
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(Color(0xFFDC2626), Color(0xFFEF4444)),
                    iconTint = Color.White,
                    textColor = Color.White,
                    title = "扫码出库",
                    subtitle = "快速扣减库存",
                    onClick = { onStartScan(TxType.OUT) }
                )
            }
        }

        // ── 最近动态标题行 ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("最近动态", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("最近 ${recent.size} 笔出入库单据", color = TextMuted, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BlueLightBg)
                        .border(1.dp, BorderBlue, RoundedCornerShape(50))
                        .clickable { onNavigateTab(2) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("查看全部", color = BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // ── 动态流水卡片 ──
        if (recent.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("暂无出入库动态", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("点击上方扫码按钮开始录入", color = TextMuted.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
                ) {
                    recent.forEachIndexed { index, tx ->
                        RecentTxRow(
                            tx = tx,
                            fallbackImage = items.firstOrNull { it.id == tx.itemId || it.sku == tx.sku }?.imageUrl,
                            onClick = {
                                items.firstOrNull { it.id == tx.itemId || it.sku == tx.sku }?.let {
                                    onSelectItem(it.id)
                                }
                            }
                        )
                        if (index < recent.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
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

@Composable
private fun ScanActionButton(
    modifier: Modifier,
    gradientColors: List<Color>,
    iconTint: Color,
    textColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val outerShape = RoundedCornerShape(20.dp)
    val innerShape = RoundedCornerShape(17.dp)
    Box(
        modifier = modifier
            .clip(outerShape)
            .background(gradientColors.first().copy(alpha = 0.12f))
            .border(0.5.dp, gradientColors.first().copy(alpha = 0.25f), outerShape)
            .clickable(onClick = onClick)
            .padding(2.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(innerShape)
                .background(Brush.verticalGradient(gradientColors))
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = textColor.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RecentTxRow(
    tx: TransactionRecord,
    fallbackImage: String?,
    onClick: () -> Unit
) {
    val isIn = tx.type == TxType.IN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图片
        ItemImage(
            imageUrl = tx.imageUrl.ifEmpty { fallbackImage.orEmpty() },
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp)),
            iconSize = 22.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.itemName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${Fmt.formattedTime(tx.timestamp)} · ${tx.reason}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isIn) "+" else "-") + tx.quantity,
                color = if (isIn) GreenPrimary else RedPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isIn) GreenTint else RedTint,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (isIn) GreenBorder else RedBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    if (isIn) "入库" else "出库",
                    color = if (isIn) GreenPrimary else RedPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}