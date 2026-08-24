package com.stockmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.components.FieldLabel
import com.stockmaster.app.ui.components.GlassDefaults
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.QuantityStepper
import com.stockmaster.app.ui.components.QuantityStepperField
import com.stockmaster.app.ui.components.QuickStepRow
import com.stockmaster.app.ui.components.SMDropdownMenu
import com.stockmaster.app.ui.components.SMDropdownMenuItem
import com.stockmaster.app.ui.components.SMNumberField
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.components.glassBorder
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.GlassHairline
import com.stockmaster.app.ui.theme.GreenLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedBorder
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.ScrimDrawer
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.Fmt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultDrawer(
    mode: TxType,
    item: InventoryItem?,
    unrecognizedBarcode: String?,
    locations: List<String>,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    unitCost: Double,
    unitPrice: Double,
    onCostChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    selectedSize: String?,
    onSelectedSize: (String?) -> Unit,
    isBatchSizeScan: Boolean,
    onToggleBatch: (Boolean) -> Unit,
    sizeBreakdown: Map<String, Int>,
    onSizeBreakdownChange: (Map<String, Int>) -> Unit,
    totalQty: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAddNewProduct: () -> Unit
) {
    // 输入框的原始编辑文本：避免「删空/中间态被立即解析回写为 0」吞掉正在输入的内容
    var costEdit by remember(mode, item?.id) { mutableStateOf<String?>(null) }
    var priceEdit by remember(mode, item?.id) { mutableStateOf<String?>(null) }

    // 出库时 0 库存尺码被隐藏，其配比条目若残留在 sizeBreakdown 中，
    // 会污染确认按钮的合计与 VM 校验；随可见集合变化清理不可见尺码的条目
    val currentBreakdown by rememberUpdatedState(sizeBreakdown)
    val currentOnBreakdownChange by rememberUpdatedState(onSizeBreakdownChange)
    LaunchedEffect(mode, item?.id) {
        if (mode == TxType.OUT && item != null) {
            val keep = item.sizeVariants.filter { it.stock > 0 }.map { it.size }.toSet()
            val cleaned = currentBreakdown.filterKeys { it in keep }
            if (cleaned.size != currentBreakdown.size) currentOnBreakdownChange(cleaned)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 深色模糊感遮罩（Scrim Backdrop）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x59040A16), ScrimDrawer)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Brush.verticalGradient(GlassDefaults.hudFill))
                .glassBorder(26.dp)
        ) {
            // 拖拽指示条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(50))
            )

        // 头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (mode == TxType.IN) GreenPrimary else RedPrimary,
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (unrecognizedBarcode != null && item == null) "发现新条形码"
                    else if (mode == TxType.IN) "确认入库录入" else "确认出库扣减",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            val closeBtnShape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(closeBtnShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), closeBtnShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // 内容
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            if (unrecognizedBarcode != null && item == null) {
                // 未识别条码
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0x2EFBBF24), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x59FBBF24), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("条码未录入库存", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("识别条码: ", color = TextSecondary, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                unrecognizedBarcode,
                                color = GreenPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "该条码尚未关联任何商品。您可以立即为其新建商品档案。",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    val newProductBtnShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(newProductBtnShape)
                            .background(GreenPrimary)
                            .clickable(onClick = onAddNewProduct)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("为该条码录入新商品", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (item != null) {
                // 已识别商品快照
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemImage(
                        imageUrl = item.imageUrl,
                        modifier = Modifier.size(60.dp),
                        iconSize = 28.dp
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(GreenLight.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("已识别对应商品", color = Color(0xFF00422B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (item.hasSizes) {
                                Spacer(Modifier.size(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(GreenPrimary.copy(alpha = 0.15f), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Checkroom,
                                            contentDescription = null,
                                            tint = Color(0xFF00422B),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.size(3.dp))
                                        Text("多尺码", color = Color(0xFF00422B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(5.dp))
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
                            "SKU: ${item.sku} · 库位: ${item.location}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("当前在库: ", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                "${item.stock} ${item.unit}",
                                color = GreenPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 尺码选择（出库时隐藏 0 库存尺码；入库保留全部以便补货）
                if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
                    val visibleVariants = if (mode == TxType.OUT) {
                        item.sizeVariants.filter { it.stock > 0 }
                    } else {
                        item.sizeVariants
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Checkroom,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.size(5.dp))
                                Text("选择操作尺码", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.clickable { onToggleBatch(!isBatchSizeScan) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Layers,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.size(3.dp))
                                Text(
                                    if (isBatchSizeScan) "切换为单码录入" else "多码批量配比",
                                    color = BlueAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        if (!isBatchSizeScan) {
                            if (visibleVariants.isEmpty()) {
                                Text(
                                    "所有尺码均无库存，无法出库",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleVariants) { v ->
                                    val isSelected = selectedSize == v.size
                                    val isOut = v.stock <= 0
                                    val chipShape = RoundedCornerShape(12.dp)
                                    Row(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .background(
                                                when {
                                                    isSelected -> GreenPrimary
                                                    isOut -> RedLight.copy(alpha = 0.3f)
                                                    else -> Color.White.copy(alpha = 0.08f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected -> GreenPrimary
                                                    isOut -> RedBorder
                                                    else -> Color.White.copy(alpha = 0.16f)
                                                },
                                                chipShape
                                            )
                                            .clickable { onSelectedSize(v.size) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            v.size,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            "(余${v.stock})",
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    }
                                }
                            }
                        } else {
                            // 批量配比（出库时同样只列出有库存的尺码）
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visibleVariants.forEach { v ->
                                    Column(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(v.size, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("余${v.stock}", color = TextMuted, fontSize = 10.sp)
                                        Spacer(Modifier.height(6.dp))
                                        QuantityStepperField(
                                            value = sizeBreakdown[v.size] ?: 0,
                                            onValueChange = { num ->
                                                onSizeBreakdownChange(sizeBreakdown + (v.size to num))
                                            },
                                            height = 34.dp,
                                            accent = BlueAccent,
                                            modifier = Modifier.width(96.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 数量
                if (!isBatchSizeScan) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "本次${if (mode == TxType.IN) "入库" else "出库"}数量 " +
                                (selectedSize?.let { "(尺码: $it)" } ?: "(${item.unit})"),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        QuantityStepper(
                            value = quantity,
                            onValueChange = { onQuantityChange(maxOf(1, it)) },
                            accent = if (mode == TxType.IN) GreenPrimary else RedPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        QuickStepRow(
                            steps = listOf(1, 5, 10, 50, 100),
                            current = quantity,
                            onSelect = { onQuantityChange(it) }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 价格与库位
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FieldLabel(if (mode == TxType.IN) "进货单价 (¥)" else "销售单价 (¥)")
                        SMNumberField(
                            value = if (mode == TxType.IN) (costEdit ?: unitCost.toString())
                                else (priceEdit ?: unitPrice.toString()),
                            onValueChange = { input ->
                                if (mode == TxType.IN) costEdit = input else priceEdit = input
                                input.toDoubleOrNull()?.let {
                                    if (mode == TxType.IN) onCostChange(it.coerceAtLeast(0.0))
                                    else onPriceChange(it.coerceAtLeast(0.0))
                                }
                            },
                            decimal = true,
                            bold = true,
                            fontSize = 13.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FieldLabel("存放库位")
                        var menuOpen by remember { mutableStateOf(false) }
                        val locShape = RoundedCornerShape(10.dp)
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(locShape)
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, GlassHairline, locShape)
                                    .clickable { menuOpen = true }
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
                            SMDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                                            onLocationChange(loc)
                                            menuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 事由
                FieldLabel("出入库事由 / 备注")
                SMTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    placeholder = "如: 采购验收入库、电商发货、样品领用..."
                )
                Spacer(Modifier.height(18.dp))

                // 确认按钮
                val accent = if (mode == TxType.IN) GreenPrimary else RedPrimary
                val confirmBtnShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 14.dp,
                            shape = confirmBtnShape,
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = accent.copy(alpha = if (totalQty > 0) 0.40f else 0f)
                        )
                        .clip(confirmBtnShape)
                        .background(
                            if (totalQty <= 0) {
                                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f)))
                            } else {
                                Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.70f)))
                            }
                        )
                        .glassBorder(16.dp)
                        .border(
                            1.dp,
                            if (totalQty <= 0) Color.Transparent else accent,
                            confirmBtnShape
                        )
                        .clickable(enabled = totalQty > 0, onClick = onConfirm)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (totalQty <= 0) Color.Transparent else Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (totalQty <= 0) Color.White.copy(alpha = 0.40f) else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            "确认${if (mode == TxType.IN) "入库" else "出库"} ($totalQty ${item.unit}" +
                                (if (selectedSize != null && !isBatchSizeScan) " · $selectedSize" else "") +
                                " · 总计 ${Fmt.money(totalQty * (if (mode == TxType.IN) unitCost else unitPrice))})",
                            color = if (totalQty <= 0) Color.White.copy(alpha = 0.40f) else Color.White,
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
