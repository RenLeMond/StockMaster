package com.stockmaster.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.components.FieldLabel
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.QuantityStepper
import com.stockmaster.app.ui.components.QuickStepRow
import com.stockmaster.app.ui.components.SMDropdownMenu
import com.stockmaster.app.ui.components.SMDropdownMenuItem
import com.stockmaster.app.ui.components.SMNumberField
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.theme.BgMain
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

/** 快捷出入库对话框（库存列表/商品详情入口）。 */
@Composable
fun QuickTransactionDialog(
    item: InventoryItem,
    mode: TxType,
    locations: List<String>,
    onConfirm: (quantity: Int, price: Double, location: String, reason: String, size: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableStateOf(if (mode == TxType.IN) 10 else 1) }
    var price by remember { mutableStateOf(if (mode == TxType.IN) item.unitCost else item.unitPrice) }
    var location by remember { mutableStateOf(item.location) }
    var reason by remember { mutableStateOf(if (mode == TxType.IN) "采购到货上架" else "销售快速出库") }
    var locationMenuOpen by remember { mutableStateOf(false) }
    var selectedSize by remember { mutableStateOf(item.sizeVariants.firstOrNull()?.size) }

    val accent = if (mode == TxType.IN) GreenPrimary else RedPrimary
    val tintBg = if (mode == TxType.IN) GreenTint else RedTint
    val tintBorder = if (mode == TxType.IN) GreenBorder else RedBorder

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = BgMain,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val titleBadgeShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(titleBadgeShape)
                                    .background(tintBg)
                                    .border(1.dp, tintBorder, titleBadgeShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (mode == TxType.IN) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "快捷${if (mode == TxType.IN) "入库" else "出库"}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val closeBtnShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(closeBtnShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 商品快照
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, BorderLight.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemImage(
                            imageUrl = item.imageUrl,
                            modifier = Modifier.size(48.dp),
                            iconSize = 22.dp
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
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "SKU: ${item.sku} · 当前库存 ${item.stock} ${item.unit}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 尺码选择
                    if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        FieldLabel("操作尺码")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item.sizeVariants.forEach { v ->
                                val isSelected = selectedSize == v.size
                                val sizeChipShape = RoundedCornerShape(10.dp)
                                Row(
                                    modifier = Modifier
                                        .clip(sizeChipShape)
                                        .background(
                                            if (isSelected) accent else Color.White
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) accent else BorderLight.copy(alpha = 0.5f),
                                            sizeChipShape
                                        )
                                        .clickable { selectedSize = v.size }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        v.size,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.size(5.dp))
                                    Text(
                                        "(余${v.stock})",
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 数量
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, BorderLight.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${if (mode == TxType.IN) "入库" else "出库"}数量 (${item.unit})",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        QuantityStepper(
                            value = quantity,
                            onValueChange = { quantity = maxOf(1, it) },
                            accent = accent
                        )
                        Spacer(Modifier.height(10.dp))
                        QuickStepRow(
                            steps = listOf(1, 5, 10, 50, 100),
                            current = quantity,
                            onSelect = { quantity = it }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel(if (mode == TxType.IN) "进货单价 (¥)" else "销售单价 (¥)")
                            SMNumberField(
                                value = price.toString(),
                                onValueChange = { input ->
                                    price = input.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                },
                                decimal = true,
                                bold = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("存放库位")
                            val locShape = RoundedCornerShape(10.dp)
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(locShape)
                                        .background(Color.White)
                                        .border(1.dp, BorderLight.copy(alpha = 0.6f), locShape)
                                        .clickable { locationMenuOpen = true }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            location,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SMDropdownMenu(
                                    expanded = locationMenuOpen,
                                    onDismissRequest = { locationMenuOpen = false }
                                ) {
                                    (locations.ifEmpty { listOf("默认主仓库") }).forEach { loc ->
                                        SMDropdownMenuItem(
                                            text = loc,
                                            selected = location == loc,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = if (location == loc) GreenPrimary else TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                location = loc
                                                locationMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    FieldLabel("出入库事由 / 备注")
                    SMTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = "如: 采购验收入库、电商发货..."
                    )

                    Spacer(Modifier.height(16.dp))

                    val confirmBtnShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(confirmBtnShape)
                            .background(accent)
                            .clickable {
                                // 出库库存校验：不足时提示，不做静默截断
                                if (mode == TxType.OUT) {
                                    val avail = if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
                                        item.sizeVariants.firstOrNull { it.size == selectedSize }?.stock ?: 0
                                    } else item.stock
                                    if (avail <= 0) {
                                        Toast.makeText(context, "该商品当前库存为 0，无法出库", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (quantity > avail) {
                                        Toast.makeText(context, "库存仅剩 $avail ${item.unit}，请调整出库数量", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                }
                                onConfirm(
                                    quantity,
                                    price,
                                    location.ifBlank { item.location },
                                    reason.ifBlank { if (mode == TxType.IN) "快捷入库" else "快捷出库" },
                                    selectedSize
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "确认${if (mode == TxType.IN) "入库" else "出库"} ${quantity} ${item.unit}" +
                                    (selectedSize?.let { " · $it" } ?: ""),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}