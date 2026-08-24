package com.stockmaster.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.SizeVariant
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.data.TxType
import com.stockmaster.app.data.COMMON_UNITS
import com.stockmaster.app.data.SIZE_PRESET_LABELS
import com.stockmaster.app.data.SIZE_PRESETS
import com.stockmaster.app.ui.components.AppCard
import com.stockmaster.app.ui.components.ButtonInButton
import com.stockmaster.app.ui.components.ConfirmDialog
import com.stockmaster.app.ui.components.DoubleBezelCard
import com.stockmaster.app.ui.components.FieldLabel
import com.stockmaster.app.ui.components.InputDialog
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.MoneyDisplay
import com.stockmaster.app.ui.components.PhotoViewerDialog
import com.stockmaster.app.ui.components.QuantityStepperField
import com.stockmaster.app.ui.components.SMNumberField
import com.stockmaster.app.ui.components.SMTextArea
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.components.SelectChip
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.DividerColor
import com.stockmaster.app.ui.theme.GlassHairline
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
import com.stockmaster.app.util.BarcodeBitmap
import com.stockmaster.app.util.Fmt
import com.stockmaster.app.util.ImageUtils
import java.io.File
import java.time.LocalDateTime
import java.util.Locale

import kotlinx.coroutines.launch

/** 商品详情 / 编辑页。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    item: InventoryItem,
    categories: List<String>,
    locations: List<String>,
    history: List<TransactionRecord>,
    onUpdate: (InventoryItem) -> Boolean,
    onDelete: () -> Unit,
    onQuickIn: () -> Unit,
    onQuickOut: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var editMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除商品档案",
            message = "确定删除「${item.name}」吗？该商品的历史流水仍会保留，但库存记录将被移除。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (!editMode) {
        DetailView(
            item = item,
            history = history,
            onEdit = { editMode = true },
            onDelete = { showDeleteConfirm = true },
            onQuickIn = onQuickIn,
            onQuickOut = onQuickOut,
            onClose = onClose
        )
    } else {
        EditView(
            item = item,
            categories = categories,
            locations = locations,
            onSave = { updated ->
                if (onUpdate(updated)) {
                    editMode = false
                } else {
                    Toast.makeText(context, "SKU 或条码已与其他商品冲突，请修改后保存", Toast.LENGTH_LONG).show()
                }
            },
            onCancel = { editMode = false }
        )
    }
}

/** 现代重构版商品详情展示视图 */
@Composable
private fun DetailView(
    item: InventoryItem,
    history: List<TransactionRecord>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickIn: () -> Unit,
    onQuickOut: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val barcodeBitmap: Bitmap? = remember(item.barcode) {
        if (item.barcode.isNotBlank()) BarcodeBitmap.generate(item.barcode) else null
    }
    val isLow = item.isLowStock

    val profit = item.unitPrice - item.unitCost
    val profitMargin = if (item.unitPrice > 0) (profit / item.unitPrice * 100) else 0.0

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label 已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    var showPhotoViewer by remember { mutableStateOf(false) }

    if (showPhotoViewer && !item.imageUrl.isNullOrEmpty()) {
        PhotoViewerDialog(
            imageUrl = item.imageUrl,
            title = item.name,
            subtitle = item.sku,
            onDismiss = { showPhotoViewer = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("商品档案详情", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val actionShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(actionShape)
                    .background(BlueLightBg)
                    .border(1.dp, BorderLight.copy(alpha = 0.6f), actionShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑商品", tint = BlueAccent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(actionShape)
                    .background(RedTint)
                    .border(1.dp, RedBorder.copy(alpha = 0.5f), actionShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "删除商品", tint = RedPrimary, modifier = Modifier.size(17.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 商品展台主卡片 (Hero Card - Double-Bezel)
            DoubleBezelCard(
                outerBorderColor = if (isLow) RedBorder.copy(alpha = 0.8f) else BorderLight.copy(alpha = 0.6f),
                outerRadius = 20.dp,
                innerRadius = 17.dp,
                innerPadding = 14.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        ItemImage(
                            imageUrl = item.imageUrl,
                            modifier = Modifier.size(76.dp),
                            iconSize = 34.dp,
                            onTap = if (!item.imageUrl.isNullOrEmpty()) { { showPhotoViewer = true } } else null
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(BlueLightBg, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(item.category, color = BlueAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                if (item.hasSizes) {
                                    Box(
                                        modifier = Modifier
                                            .background(GreenTint, RoundedCornerShape(4.dp))
                                            .border(0.5.dp, GreenBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("多尺码", color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                item.name,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    item.location,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("·", color = TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.sku,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 缺货预警横幅
                    if (isLow) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RedLight, RoundedCornerShape(10.dp))
                                .border(1.dp, RedBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "当前库存 ${item.stock} ${item.unit}，已低于预警线 (${item.minStock} ${item.unit})，请尽快补货！",
                                    color = RedPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. 财务与库存核算指标网格 (4-Grid Highlights - 严格等高)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGridCard(
                    title = "当前在库总量",
                    value = "${item.stock} ${item.unit}",
                    valueColor = if (isLow) RedPrimary else GreenPrimary,
                    subtext = if (item.maxCapacity != null) {
                        "容量: ${item.stockPercent()}% / ${item.maxCapacity}${item.unit}"
                    } else {
                        "预警线: ${if (item.minStock > 0) "${item.minStock}${item.unit}" else "未设置"}"
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MetricGridCard(
                    title = "单件毛利 / 利率",
                    value = (if (profit >= 0) "+¥" else "¥") + Fmt.moneyRaw(profit),
                    valueColor = if (profit >= 0) GreenPrimary else RedPrimary,
                    subtext = "毛利率 ${String.format(Locale.CHINA, "%.1f", profitMargin)}%",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGridCard(
                    title = "单位进货成本",
                    value = "¥" + Fmt.moneyRaw(item.unitCost),
                    valueColor = TextPrimary,
                    subtext = "成本核算基准",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MetricGridCard(
                    title = "单位销售标价",
                    value = "¥" + Fmt.moneyRaw(item.unitPrice),
                    valueColor = BlueAccent,
                    subtext = "建议零售标价",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // 3. 快捷出入库操作按钮 (Button-in-Button 嵌套纽扣架构)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val inBtnShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(inBtnShape)
                        .background(GreenPrimary)
                        .clickable(onClick = onQuickIn)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("快捷入库", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val outBtnShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(outBtnShape)
                        .background(RedTint)
                        .border(1.dp, RedBorder, outBtnShape)
                        .clickable(onClick = onQuickOut)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                .background(RedPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text("快捷出库", color = RedPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. 多尺码库存明细表 (若有多尺码)
            if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
                AppCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Checkroom, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("尺码库存矩阵", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("共 ${item.sizeVariants.size} 种规格", color = TextMuted, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        // 表头
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("尺码规格", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                            Text("当前在库", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("预警线", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                            Text("状态", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                        }

                        item.sizeVariants.forEachIndexed { index, v ->
                            val vLow = v.minStock > 0 && v.stock <= v.minStock
                            val isOut = v.stock <= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    v.size,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(80.dp)
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${v.stock}",
                                        color = if (vLow || isOut) RedPrimary else GreenPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.unit, color = TextMuted, fontSize = 11.sp)
                                }
                                Text(
                                    if (v.minStock > 0) "${v.minStock}" else "—",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(50.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isOut) RedLight else if (vLow) RedLight.copy(alpha = 0.6f) else GreenLight.copy(alpha = 0.18f)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isOut) "售罄" else if (vLow) "缺货" else "充足",
                                        color = if (isOut || vLow) RedPrimary else GreenPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (index < item.sizeVariants.size - 1) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
                            }
                        }
                    }
                }
            }

            // 5. 条形码与商品备注 (Barcode & Notes)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("条码与档案信息", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (item.barcode.isNotBlank()) {
                            val copyShape = RoundedCornerShape(6.dp)
                            Row(
                                modifier = Modifier
                                    .clip(copyShape)
                                    .background(BlueLightBg)
                                    .border(0.5.dp, BorderLight.copy(alpha = 0.6f), copyShape)
                                    .clickable { copyToClipboard(item.barcode, "条形码") }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("复制条码", color = BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (barcodeBitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "条形码",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.barcode,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (item.description.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("规格备注说明:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(item.description, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            // 6. 最近出入库流水 (Recent Stock Movements Timeline)
            AppCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("近期出入库动态", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("共 ${history.size} 笔记录", color = TextMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无该商品的流水单据记录", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        history.take(10).forEachIndexed { index, tx ->
                            val isIn = tx.type == TxType.IN
                            val badgeShape = RoundedCornerShape(10.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(badgeShape)
                                        .background(if (isIn) GreenTint else RedTint)
                                        .border(1.dp, if (isIn) GreenBorder else RedBorder, badgeShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isIn) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.RemoveCircleOutline,
                                        contentDescription = null,
                                        tint = if (isIn) GreenPrimary else RedPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        tx.reason.ifEmpty { if (isIn) "常规入库" else "销售出库" },
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${Fmt.formattedTime(tx.timestamp)} · ${tx.location}" + (tx.size?.let { " · 尺码 $it" } ?: ""),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        (if (isIn) "+" else "-") + tx.quantity,
                                        color = if (isIn) GreenPrimary else RedPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        Fmt.money(if (tx.totalPrice > 0) tx.totalPrice else tx.quantity * tx.unitPrice),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (index != history.lastIndex && index < 9) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(DividerColor)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricGridCard(
    title: String,
    value: String,
    valueColor: Color,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            subtext,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** 现代重构版商品编辑视图（与 AddProductScreen 保持 100% 结构和视觉统一） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditView(
    item: InventoryItem,
    categories: List<String>,
    locations: List<String>,
    onSave: (InventoryItem) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var customCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var customLocations by remember { mutableStateOf<List<String>>(emptyList()) }

    val allCategories = remember(categories, customCategories, item.category) {
        (categories + customCategories + item.category).distinct().filter { it.isNotBlank() }
    }
    val allLocations = remember(locations, customLocations, item.location) {
        (locations + customLocations + item.location).distinct().filter { it.isNotBlank() }
    }

    var imageBase64 by remember { mutableStateOf<String?>(item.imageUrl) }
    var name by remember { mutableStateOf(item.name) }
    var sku by remember { mutableStateOf(item.sku) }
    var barcode by remember { mutableStateOf(item.barcode) }
    var category by remember { mutableStateOf(item.category) }
    var location by remember { mutableStateOf(item.location) }
    var unit by remember { mutableStateOf(item.unit) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }

    var hasSizes by remember { mutableStateOf(item.hasSizes) }
    var variants by remember { mutableStateOf(item.sizeVariants) }
    var stock by remember { mutableStateOf(item.stock.toString()) }
    var minStock by remember { mutableStateOf(item.minStock.toString()) }
    var maxCapacity by remember { mutableStateOf(item.maxCapacity?.toString() ?: "") }
    var unitCost by remember { mutableStateOf(item.unitCost.toString()) }
    var unitPrice by remember { mutableStateOf(item.unitPrice.toString()) }
    var description by remember { mutableStateOf(item.description) }

    // 拍照
    val scope = rememberCoroutineScope()
    // 惰性创建：不在组合期做磁盘 IO；离开编辑时清理残留临时文件
    val tempPhoto = remember { File(context.cacheDir, "edit_capture_${System.currentTimeMillis()}.jpg") }
    androidx.compose.runtime.DisposableEffect(tempPhoto) {
        onDispose { if (tempPhoto.exists()) tempPhoto.delete() }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            scope.launch {
                val path = ImageUtils.saveCompressedImage(context, Uri.fromFile(tempPhoto))
                if (path != null) imageBase64 = path
                tempPhoto.delete()
            }
        }
    }
    // 相册选取
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = ImageUtils.saveCompressedImage(context, uri)
                if (path != null) imageBase64 = path
            }
        }
    }

    fun applySizePreset(preset: List<String>) {
        variants = preset.map { SizeVariant(size = it, stock = 0, minStock = 0) }
    }

    // 记录进入编辑时的库存快照：保存前检测外部并发写入（如扫码出入库），
    // 避免用户未改库存字段时用陈旧值静默覆盖新数据
    val enteredStockText = remember(item.id) { item.stock.toString() }
    val enteredVariants = remember(item.id) { item.sizeVariants }

    val costVal = unitCost.toDoubleOrNull() ?: 0.0
    val priceVal = unitPrice.toDoubleOrNull() ?: 0.0
    val profitPerUnit = priceVal - costVal
    val profitMargin = if (priceVal > 0) (profitPerUnit / priceVal * 100) else 0.0

    if (showAddCategoryDialog) {
        InputDialog(
            title = "自定义新分类",
            placeholder = "请输入分类名称 (如: 上衣 / 裤装)",
            confirmText = "添加",
            onConfirm = { newCat ->
                showAddCategoryDialog = false
                val clean = newCat.trim()
                if (clean.isNotEmpty()) {
                    customCategories = (customCategories + clean).distinct()
                    category = clean
                    Toast.makeText(context, "已新增分类: $clean", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    if (showAddLocationDialog) {
        InputDialog(
            title = "自定义新库位",
            placeholder = "请输入库位名称 (如: A区-01架 / 仓库前柜)",
            confirmText = "添加",
            onConfirm = { newLoc ->
                showAddLocationDialog = false
                val clean = newLoc.trim()
                if (clean.isNotEmpty()) {
                    customLocations = (customLocations + clean).distinct()
                    location = clean
                    Toast.makeText(context, "已新增库位: $clean", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddLocationDialog = false }
        )
    }

    fun save() {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            Toast.makeText(context, "请填写商品名称", Toast.LENGTH_SHORT).show()
            return
        }
        val finalSku = sku.trim().ifEmpty { item.sku }
        val finalBarcode = barcode.trim().ifEmpty { item.barcode }
        val validVariants = if (hasSizes) variants.filter { it.size.isNotBlank() } else emptyList()

        // 重名尺码会导致下游按尺码折叠/扣减时账实漂移，保存前拦截
        if (hasSizes && validVariants.isNotEmpty()) {
            val sizeNames = validVariants.map { it.size.trim() }
            if (sizeNames.size != sizeNames.distinct().size) {
                Toast.makeText(context, "存在重复的尺码名称，请合并或修改后再保存", Toast.LENGTH_LONG).show()
                return
            }
        }

        // 外部并发写入保护：库存字段未被用户改动、但商品库存已在其他入口更新时拦截
        val stockUntouched = if (hasSizes) variants == enteredVariants else stock == enteredStockText
        val externalChanged = if (hasSizes) item.sizeVariants != enteredVariants else item.stock.toString() != enteredStockText
        if (stockUntouched && externalChanged) {
            Toast.makeText(context, "商品库存在其他入口被更新过，请退出编辑后重新进入再保存", Toast.LENGTH_LONG).show()
            return
        }

        val finalStock = if (hasSizes) validVariants.sumOf { it.stock } else (stock.toIntOrNull() ?: item.stock)

        onSave(
            item.copy(
                name = cleanName,
                sku = finalSku,
                barcode = finalBarcode,
                category = category.trim().ifEmpty { item.category },
                location = location.trim().ifEmpty { item.location },
                unit = unit.trim().ifEmpty { item.unit },
                hasSizes = hasSizes,
                sizeVariants = validVariants,
                stock = finalStock,
                minStock = minStock.toIntOrNull() ?: 0,
                maxCapacity = maxCapacity.toIntOrNull()?.takeIf { it > 0 },
                unitCost = costVal,
                unitPrice = priceVal,
                description = description.trim(),
                imageUrl = imageBase64 ?: "",
                updatedAt = LocalDateTime.now().toString()
            )
        )
    }

    var showPhotoViewer by remember { mutableStateOf(false) }

    if (showPhotoViewer && !imageBase64.isNullOrEmpty()) {
        PhotoViewerDialog(
            imageUrl = imageBase64,
            title = name.ifEmpty { "商品实物照片" },
            subtitle = sku,
            onDismiss = { showPhotoViewer = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部操作栏
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
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("编辑商品档案", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("修改商品档案信息、库位与价格设置", color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            val saveTopShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(saveTopShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), saveTopShape)
                    .clickable { save() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 主体表单区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 照片管理卡片 (Hero Card)
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BlueLightBg)
                            .border(1.dp, BorderLight.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBase64 != null) {
                            ItemImage(
                                imageUrl = imageBase64,
                                modifier = Modifier.fillMaxSize(),
                                iconSize = 36.dp,
                                onTap = { showPhotoViewer = true }
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { imageBase64 = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = BlueAccent.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("暂无图片", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("商品照片维护", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("拍摄或重新上传实物照片，便于极速找货", color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val camShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(camShape)
                                    .background(GreenLight.copy(alpha = 0.2f))
                                    .clickable {
                                        takePictureLauncher.launch(
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                tempPhoto
                                            )
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("拍照", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            val galShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(galShape)
                                    .background(BlueLightBg)
                                    .clickable {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("相册", color = BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. 基础信息卡片 (Basic Info)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("基础档案信息", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        FieldLabel("商品名称 *")
                        SMTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "商品名称",
                            height = 42.dp,
                            fontSize = 14.sp,
                            bold = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("SKU 编码")
                            SMTextField(
                                value = sku,
                                onValueChange = { sku = it },
                                placeholder = "SKU 编码",
                                mono = true,
                                height = 40.dp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("条形码 (Barcode)")
                            SMTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                placeholder = "条形码",
                                mono = true,
                                height = 40.dp
                            )
                        }
                    }

                    Column {
                        FieldLabel("规格型号 / 商品备注")
                        SMTextArea(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = "选填，如颜色、材质、供应商、货源批次等...",
                            minHeight = 70.dp
                        )
                    }
                }
            }

            // 3. 分类与存放库位卡片 (Category & Location)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 分类
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Layers, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("所属分类", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            val addCatShape = RoundedCornerShape(50)
                            Box(
                                modifier = Modifier
                                    .clip(addCatShape)
                                    .background(BlueLightBg)
                                    .clickable { showAddCategoryDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("自定义分类", color = BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allCategories) { cat ->
                                SelectChip(
                                    text = cat,
                                    selected = category == cat,
                                    selectedColor = GreenPrimary,
                                    onClick = { category = cat }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

                    // 库位
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("存放库位 / 货架", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            val addLocShape = RoundedCornerShape(50)
                            Box(
                                modifier = Modifier
                                    .clip(addLocShape)
                                    .background(GreenLight.copy(alpha = 0.2f))
                                    .clickable { showAddLocationDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("自定义库位", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allLocations) { loc ->
                                SelectChip(
                                    text = loc,
                                    selected = location == loc,
                                    selectedColor = BlueAccent,
                                    onClick = { location = loc }
                                )
                            }
                        }
                    }
                }
            }

            // 4. 规格与库存矩阵卡片 (Specs & Stock Matrix)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Checkroom, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("规格与库存模式", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // 单码 / 多尺码切换
                        Row(
                            modifier = Modifier
                                .background(Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            val singleShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .clip(singleShape)
                                    .background(
                                        if (!hasSizes) Color(0xFF0B7A55) else Color.Transparent
                                    )
                                    .clickable {
                                        hasSizes = false
                                        variants = emptyList()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "单规格",
                                    color = if (!hasSizes) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val multiShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .clip(multiShape)
                                    .background(
                                        if (hasSizes) Color(0xFF0B7A55) else Color.Transparent
                                    )
                                    .clickable {
                                        hasSizes = true
                                        if (variants.isEmpty()) {
                                            applySizePreset(SIZE_PRESETS[0])
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "多尺码矩阵",
                                    color = if (hasSizes) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 计量单位快速选择
                    Column {
                        FieldLabel("计量单位")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(COMMON_UNITS) { u ->
                                SelectChip(
                                    text = u,
                                    selected = unit == u,
                                    selectedColor = GreenPrimary,
                                    onClick = { unit = u }
                                )
                            }
                        }
                    }

                    if (!hasSizes) {
                        // 单规格库存
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("当前在库数量 ($unit)")
                                QuantityStepperField(
                                    value = stock.toIntOrNull() ?: 0,
                                    onValueChange = { stock = it.toString() },
                                    placeholderText = "0"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("缺货预警阈值 ($unit)")
                                QuantityStepperField(
                                    value = minStock.toIntOrNull() ?: 0,
                                    onValueChange = { minStock = it.toString() },
                                    placeholderText = "0 (0不预警)"
                                )
                            }
                        }
                    } else {
                        // 多尺码矩阵
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FieldLabel("常用尺码模板快速填充")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(SIZE_PRESET_LABELS.size) { idx ->
                                    val label = SIZE_PRESET_LABELS[idx]
                                    SelectChip(
                                        text = label,
                                        selected = false,
                                        selectedColor = BlueAccent,
                                        onClick = { applySizePreset(SIZE_PRESETS[idx]) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text("尺码库存明细表", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            variants.forEachIndexed { index, v ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SMTextField(
                                        value = v.size,
                                        onValueChange = { newSize ->
                                            variants = variants.toMutableList().apply {
                                                this[index] = this[index].copy(size = newSize)
                                            }
                                        },
                                        placeholder = "尺码名",
                                        height = 36.dp,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    QuantityStepperField(
                                        value = v.stock,
                                        onValueChange = { newStock ->
                                            variants = variants.toMutableList().apply {
                                                this[index] = this[index].copy(stock = newStock)
                                            }
                                        },
                                        height = 36.dp,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val delVarShape = RoundedCornerShape(8.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(delVarShape)
                                            .background(RedLight)
                                            .clickable {
                                                variants = variants.filterIndexed { i, _ -> i != index }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "删除", tint = RedPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            // 添加新尺码按钮
                            val addRowShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(addRowShape)
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, GreenPrimary.copy(alpha = 0.40f), addRowShape)
                                    .clickable { variants = variants + SizeVariant("新尺码", 0) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("添加一行尺码", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 合计库存横幅
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GreenLight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("各尺码合计总在库:", color = Color(0xFF00422B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${variants.sumOf { it.stock }} $unit",
                                        color = GreenPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. 成本、售价与毛利测算 (Pricing & Profit)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("价格核算与容量控制", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("单位进货成本 (¥)")
                            SMNumberField(
                                value = unitCost,
                                onValueChange = { unitCost = it },
                                placeholder = "0.00",
                                decimal = true,
                                bold = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("单位销售标价 (¥)")
                            SMNumberField(
                                value = unitPrice,
                                onValueChange = { unitPrice = it },
                                placeholder = "0.00",
                                decimal = true,
                                bold = true
                            )
                        }
                    }

                    // 实时毛利测算提示条
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (profitPerUnit >= 0) GreenLight.copy(alpha = 0.18f) else RedLight,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (profitPerUnit >= 0) GreenPrimary else RedPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (priceVal > 0) {
                                    "单件预估毛利: " + (if (profitPerUnit >= 0) "+¥" else "¥") +
                                        Fmt.moneyRaw(profitPerUnit) +
                                        " · 毛利率 ${String.format(Locale.CHINA, "%.1f", profitMargin)}%"
                                } else "输入进价与售价将自动核算单件毛利率",
                                color = if (profitPerUnit >= 0) Color(0xFF00422B) else Color(0xFFBA1A1A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("存放容量上限（可选）")
                            QuantityStepperField(
                                value = maxCapacity.toIntOrNull() ?: 0,
                                onValueChange = { maxCapacity = if (it <= 0) "" else it.toString() },
                                placeholderText = "不限制"
                            )
                        }
                        if (hasSizes) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("默认预警阈值 ($unit)")
                                QuantityStepperField(
                                    value = minStock.toIntOrNull() ?: 0,
                                    onValueChange = { minStock = it.toString() },
                                    placeholderText = "0 (0不预警)"
                                )
                            }
                        }
                    }
                }
            }

            // 6. 底部大保存按钮
            val saveBottomShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(saveBottomShape)
                    .background(GreenPrimary)
                    .clickable { save() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("保存修改后的商品档案", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}