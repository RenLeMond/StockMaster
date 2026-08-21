package com.stockmaster.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.SizeVariant
import com.stockmaster.app.data.COMMON_UNITS
import com.stockmaster.app.data.SIZE_PRESET_LABELS
import com.stockmaster.app.data.SIZE_PRESETS
import com.stockmaster.app.ui.components.AppCard
import com.stockmaster.app.ui.components.InputDialog
import com.stockmaster.app.ui.components.FieldLabel
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.PhotoViewerDialog
import com.stockmaster.app.ui.components.SMNumberField
import com.stockmaster.app.ui.components.SMTextArea
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.components.SelectChip
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GreenLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.Fmt
import com.stockmaster.app.util.ImageUtils
import java.io.File

import java.util.Locale
import java.util.UUID
/** 全新设计的录入商品界面 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProductScreen(
    categories: List<String>,
    locations: List<String>,
    presetBarcode: String?,
    onSave: (InventoryItem) -> Unit,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onAddCategory: ((String) -> Unit)? = null,
    onAddLocation: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    var customCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var customLocations by remember { mutableStateOf<List<String>>(emptyList()) }

    val allCategories = remember(categories, customCategories) {
        (categories + customCategories).distinct().filter { it.isNotBlank() }
    }
    val allLocations = remember(locations, customLocations) {
        (locations + customLocations).distinct().filter { it.isNotBlank() }
    }

    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(presetBarcode ?: "") }
    var category by remember(categories) { mutableStateOf(categories.firstOrNull() ?: "") }
    var location by remember(locations) { mutableStateOf(locations.firstOrNull() ?: "") }
    var unit by remember { mutableStateOf("件") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }

    if (showPhotoViewer && !imageBase64.isNullOrEmpty()) {
        PhotoViewerDialog(
            imageUrl = imageBase64,
            title = name.ifEmpty { "商品实物照片" },
            subtitle = if (barcode.isNotBlank()) "条码: $barcode" else "",
            onDismiss = { showPhotoViewer = false }
        )
    }

    var hasSizes by remember { mutableStateOf(false) }
    var variants by remember { mutableStateOf<List<SizeVariant>>(emptyList()) }
    var stock by remember { mutableStateOf("0") }
    var minStock by remember { mutableStateOf("0") }
    var maxCapacity by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("0") }
    var unitPrice by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }

    // 拍照
    val scope = rememberCoroutineScope()
    val tempPhoto = remember { File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg").apply { if (!exists()) createNewFile() } }
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

    val costVal = unitCost.toDoubleOrNull() ?: 0.0
    val priceVal = unitPrice.toDoubleOrNull() ?: 0.0
    val profitPerUnit = priceVal - costVal
    val profitMargin = if (priceVal > 0) (profitPerUnit / priceVal * 100) else 0.0

    fun save() {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            Toast.makeText(context, "请填写商品名称", Toast.LENGTH_SHORT).show()
            return
        }
        val finalBarcode = barcode.trim().ifEmpty { "697${(100000000L + (0..899999999L).random())}" }
        val finalSku = sku.trim().ifEmpty { finalBarcode }
        val validVariants = if (hasSizes) variants.filter { it.size.isNotBlank() } else emptyList()
        val finalStock = if (hasSizes) validVariants.sumOf { it.stock } else (stock.toIntOrNull() ?: 0)

        onSave(
            InventoryItem(
                id = UUID.randomUUID().toString(),
                sku = finalSku,
                barcode = finalBarcode,
                name = cleanName,
                category = category.trim().ifEmpty { "默认分类" },
                stock = finalStock,
                minStock = minStock.toIntOrNull() ?: 0,
                maxCapacity = maxCapacity.toIntOrNull()?.takeIf { it > 0 },
                unitCost = costVal,
                unitPrice = priceVal,
                location = location.trim().ifEmpty { "默认库位" },
                imageUrl = imageBase64.orEmpty(),
                unit = unit.trim().ifEmpty { "件" },
                description = description.trim(),
                hasSizes = hasSizes,
                sizeVariants = validVariants,
                updatedAt = java.time.LocalDateTime.now().toString()
            )
        )
        onSaved()
    }

    if (showAddCategoryDialog) {
        InputDialog(
            title = "新建自定义分类",
            message = "请输入新商品分类名称：",
            placeholder = "如: 办公文具、当季服饰...",
            confirmText = "添加分类",
            onConfirm = { clean ->
                customCategories = customCategories + clean
                category = clean
                onAddCategory?.invoke(clean)
                showAddCategoryDialog = false
            },
            onDismiss = {
                showAddCategoryDialog = false
            }
        )
    }

    if (showAddLocationDialog) {
        InputDialog(
            title = "新建自定义库位",
            message = "请输入新存放库位名称：",
            placeholder = "如: C区-货架03、阁楼仓库...",
            confirmText = "添加库位",
            onConfirm = { clean ->
                customLocations = customLocations + clean
                location = clean
                onAddLocation?.invoke(clean)
                showAddLocationDialog = false
            },
            onDismiss = {
                showAddLocationDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgMain)) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .border(0.5.dp, BorderLight.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(backShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("录入新商品", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("建立商品档案并初始化库存账目", color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            val saveShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(saveShape)
                    .background(GreenPrimary)
                    .clickable(onClick = { save() })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
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
            // 1. 首屏图文英雄卡片 (Hero Card)
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 图片展示与更换
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
                        Text("商品照片", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("拍摄或上传实物照片，便于极速找货与视觉核对", color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
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
                                    Spacer(Modifier.size(4.dp))
                                    Text("拍照", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
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
                                    Spacer(Modifier.size(4.dp))
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
                        Spacer(Modifier.size(6.dp))
                        Text("基础档案信息", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        FieldLabel("商品名称 *")
                        SMTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "如: 夏季纯棉宽松短袖 T 恤 / 蓝牙降噪耳机",
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
                                placeholder = "留空自动生成",
                                mono = true,
                                height = 40.dp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("条形码 (Barcode)")
                            SMTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                placeholder = "留空自动生成",
                                mono = true,
                                height = 40.dp,
                                leading = if (presetBarcode != null && barcode == presetBarcode) {
                                    {
                                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
                                    }
                                } else null
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
                                Spacer(Modifier.size(6.dp))
                                Text("所属分类", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(BlueLightBg)
                                    .clickable { showAddCategoryDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.size(3.dp))
                                    Text("自定义分类", color = BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (allCategories.isEmpty()) {
                            Text("暂无分类，点击右上角新建", color = TextMuted, fontSize = 12.sp)
                        } else {
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
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F4F9)))

                    // 库位
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("存放库位 / 货架", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(GreenLight.copy(alpha = 0.2f))
                                    .clickable { showAddLocationDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.size(3.dp))
                                    Text("自定义库位", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (allLocations.isEmpty()) {
                            Text("暂无库位，点击右上角新建", color = TextMuted, fontSize = 12.sp)
                        } else {
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
                            Spacer(Modifier.size(6.dp))
                            Text("规格与库存模式", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // 单码 / 多尺码切换
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF0F4F9), RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (!hasSizes) GreenPrimary else Color.Transparent
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (hasSizes) GreenPrimary else Color.Transparent
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
                        // 单规格库存录入
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("初始在库数量 ($unit)")
                                SMNumberField(
                                    value = stock,
                                    onValueChange = { stock = it },
                                    placeholder = "0",
                                    bold = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("缺货预警阈值 ($unit)")
                                SMNumberField(
                                    value = minStock,
                                    onValueChange = { minStock = it },
                                    placeholder = "0 (0不预警)"
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
                                        .background(BgMain, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderLight.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
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
                                    SMNumberField(
                                        value = v.stock.toString(),
                                        onValueChange = { input ->
                                            variants = variants.toMutableList().apply {
                                                this[index] = this[index].copy(stock = input.toIntOrNull() ?: 0)
                                            }
                                        },
                                        placeholder = "库存",
                                        height = 36.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, BorderLight.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .clickable { variants = variants + SizeVariant("新尺码", 0) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.size(4.dp))
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
                        Spacer(Modifier.size(6.dp))
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
                            Spacer(Modifier.size(6.dp))
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
                            SMNumberField(
                                value = maxCapacity,
                                onValueChange = { maxCapacity = it },
                                placeholder = "不限制"
                            )
                        }
                        if (hasSizes) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("默认预警阈值 ($unit)")
                                SMNumberField(
                                    value = minStock,
                                    onValueChange = { minStock = it },
                                    placeholder = "0 (0不预警)"
                                )
                            }
                        }
                    }
                }
            }

            // 底部大保存按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenPrimary)
                    .clickable(onClick = { save() })
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("保存并创建商品档案", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}