package com.stockmaster.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.CsvManager
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.util.Fmt
import com.stockmaster.app.ui.components.EmptyState
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.SMDropdownMenu
import com.stockmaster.app.ui.components.SMDropdownMenuItem
import com.stockmaster.app.ui.components.SelectChip
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderLight
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

@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    categories: List<String>,
    locations: List<String>,
    onSelectItem: (String) -> Unit,
    onOpenAdd: () -> Unit,
    onQuickIn: (InventoryItem) -> Unit,
    onQuickOut: (InventoryItem) -> Unit,
    onManageCategoriesLocations: () -> Unit,
    onImportItems: ((List<InventoryItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var onlyLowStock by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf("全部") }
    var locationMenuOpen by remember { mutableStateOf(false) }


    val allCategories = remember(items, categories) {
        (listOf("全部") + categories).distinct().filter { it.isNotBlank() }
    }
    val allLocations = remember(items, locations) {
        (listOf("全部") + locations).distinct().filter { it.isNotBlank() }
    }

    val filtered = remember(items, searchQuery, selectedCategory, selectedLocation, onlyLowStock) {
        items.filter { item ->
            val q = searchQuery.trim().lowercase()
            val matchSearch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.sku.lowercase().contains(q) ||
                item.barcode.contains(q) ||
                item.location.lowercase().contains(q)
            val matchCat = selectedCategory == "全部" || item.category == selectedCategory
            val matchLoc = selectedLocation == "全部" || item.location == selectedLocation
            val matchLow = !onlyLowStock || (item.minStock > 0 && item.stock <= item.minStock)
            matchSearch && matchCat && matchLoc && matchLow
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── 第 1 行：主标题 + 统计胶囊 + 录入/管理动作区 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("库存清单", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(BlueLightBg, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${items.size} 种品类",
                                color = BlueAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (filtered.size != items.size) {
                            Box(
                                modifier = Modifier
                                    .background(GreenTint, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "匹配 ${filtered.size} 件",
                                    color = GreenPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 右侧动作：管理分类/库位 + 录入商品
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val manageShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .clip(manageShape)
                                .background(Color(0xFFF1F5F9))
                                .border(0.5.dp, BorderLight, manageShape)
                                .clickable(onClick = onManageCategoriesLocations)
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Layers,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("分类库位", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        val addBtnShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .clip(addBtnShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(GreenPrimary, Color(0xFF00A369))
                                    )
                                )
                                .clickable(onClick = onOpenAdd)
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("录入商品", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── 第 2 行：搜索框 + 库位选择下拉胶囊 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 搜索框
                    com.stockmaster.app.ui.components.SMTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "搜索名称、SKU、条码或库位...",
                        height = 38.dp,
                        fontSize = 12.sp,
                        leading = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 库位快选胶囊
                    val isLocFiltered = selectedLocation != "全部"
                    val locPillShape = RoundedCornerShape(10.dp)
                    Box {
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(locPillShape)
                                .background(if (isLocFiltered) GreenLight.copy(alpha = 0.2f) else Color.White)
                                .border(
                                    1.dp,
                                    if (isLocFiltered) GreenPrimary else BorderLight.copy(alpha = 0.6f),
                                    locPillShape
                                )
                                .clickable { locationMenuOpen = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (isLocFiltered) GreenPrimary else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                if (selectedLocation == "全部") "库位: 全部" else selectedLocation,
                                color = if (isLocFiltered) GreenPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isLocFiltered) FontWeight.Bold else FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isLocFiltered) GreenPrimary else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        SMDropdownMenu(
                            expanded = locationMenuOpen,
                            onDismissRequest = { locationMenuOpen = false }
                        ) {
                            allLocations.forEach { loc ->
                                SMDropdownMenuItem(
                                    text = if (loc == "全部") "全部库位" else loc,
                                    selected = selectedLocation == loc,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = if (selectedLocation == loc) GreenPrimary else TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    onClick = {
                                        selectedLocation = loc
                                        locationMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ── 第 3 行：横向滚动分类与预警滑轨 ──
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        SelectChip(
                            text = "全部 (${items.size})",
                            selected = selectedCategory == "全部" && !onlyLowStock,
                            onClick = {
                                selectedCategory = "全部"
                                onlyLowStock = false
                            }
                        )
                    }
                    item {
                        val lowCount = items.count { it.minStock > 0 && it.stock <= it.minStock }
                        SelectChip(
                            text = "缺货预警 ($lowCount)",
                            selected = onlyLowStock,
                            onClick = { onlyLowStock = !onlyLowStock },
                            selectedColor = RedPrimary,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = if (onlyLowStock) Color.White else RedPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                    }
                    items(categories.filter { it.isNotBlank() }, key = { it }) { cat ->
                        SelectChip(
                            text = cat,
                            selected = selectedCategory == cat && !onlyLowStock,
                            onClick = {
                                selectedCategory = cat
                                onlyLowStock = false
                            }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "未找到符合条件的商品",
                    subtitle = "可尝试清空搜索条件，或点击上方新建商品档案",
                    actionText = "立即录入商品",
                    onAction = onOpenAdd
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ProductCard(
                    item = item,
                    onClick = { onSelectItem(item.id) },
                    onQuickIn = { onQuickIn(item) },
                    onQuickOut = { onQuickOut(item) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onQuickIn: () -> Unit,
    onQuickOut: () -> Unit
) {
    val isLow = item.isLowStock
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(
                1.dp,
                if (isLow) RedBorder else BorderLight,
                cardShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // ── 顶部主信息行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                ItemImage(
                    imageUrl = item.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    iconSize = 28.dp
                )
            }
            Spacer(Modifier.width(12.dp))
            // 中间信息区
            Column(modifier = Modifier.weight(1f)) {
                // 标签行：分类 + 多尺码 + SKU
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BlueLightBg, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.category, color = BlueAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (item.hasSizes) {
                        Box(
                            modifier = Modifier
                                .background(GreenTint, RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("多尺码", color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        item.sku,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(5.dp))
                // 商品名
                Text(
                    item.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // 库位 + 单价
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        item.location,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        " · ¥" + Fmt.moneyRaw(item.unitPrice),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // 右侧库存数字 + 预警标签
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        item.stock.toString(),
                        color = if (isLow) RedPrimary else GreenPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        item.unit,
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (isLow) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(RedTint, RoundedCornerShape(7.dp))
                            .border(0.5.dp, RedBorder, RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("缺货预警", color = RedPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 多尺码分布胶囊预览 ──
        if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.sizeVariants.take(6).forEach { v ->
                    val vLow = v.minStock > 0 && v.stock <= v.minStock
                    Box(
                        modifier = Modifier
                            .background(
                                if (vLow) RedTint else Color(0xFFF1F5F9),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                0.5.dp,
                                if (vLow) RedBorder else BorderLight,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${v.size}: ${v.stock}",
                            color = if (vLow) RedPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (item.sizeVariants.size > 6) {
                    Text("+${item.sizeVariants.size - 6}", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // ── 底部：进度条 + 快捷出入库按鈕 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 库存容量进度条
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(Color(0xFFEFF2F8), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            minOf(1f, item.stockPercent() / 100f)
                        )
                        .height(5.dp)
                        .background(
                            if (isLow) RedPrimary else GreenPrimary,
                            RoundedCornerShape(50)
                        )
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 入库按鈕
                val inBtnShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(inBtnShape)
                        .background(GreenTint)
                        .border(1.dp, GreenBorder, inBtnShape)
                        .clickable(onClick = onQuickIn)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(GreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text("入库", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // 出库按鈕
                val outBtnShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(outBtnShape)
                        .background(RedTint)
                        .border(1.dp, RedBorder, outBtnShape)
                        .clickable(onClick = onQuickOut)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(RedPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.RemoveCircleOutline,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text("出库", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}