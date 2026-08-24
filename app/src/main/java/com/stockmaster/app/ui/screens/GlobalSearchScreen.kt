package com.stockmaster.app.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.ui.components.EmptyState
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GlassHairline
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary

/** 全局搜索页。 */
@Composable
fun GlobalSearchScreen(
    items: List<InventoryItem>,
    onSelectItem: (String) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val results = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            items.filter {
                it.name.lowercase().contains(q) ||
                    it.sku.lowercase().contains(q) ||
                    it.barcode.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.location.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.07f))
                .statusBarsPadding()
                .border(0.5.dp, GlassHairline)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(backShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            SMTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索名称 / SKU / 条码 / 分类 / 库位...",
                leading = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                },
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.isBlank()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "输入关键词开始搜索",
                        subtitle = "可搜索商品名称、SKU 编码、条形码、分类或库位"
                    )
                }
            } else if (results.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "未找到相关商品",
                        subtitle = "换一个关键词试试，或在库存页录入新商品"
                    )
                }
            } else {
                item {
                    Text(
                        "找到 ${results.size} 件商品",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(results, key = { it.id }) { item ->
                    val rowShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), rowShape)
                            .clickable { onSelectItem(item.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemImage(
                            imageUrl = item.imageUrl,
                            modifier = Modifier.size(52.dp),
                            iconSize = 24.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "SKU: ${item.sku} · ${item.category} · ${item.location}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                item.stock.toString(),
                                color = if (item.isLowStock) RedPrimary else GreenPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(item.unit, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}