package com.stockmaster.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner as ComposeLocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.SizeBreakdown
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.MainViewModel
import com.stockmaster.app.ui.components.ItemImage
import com.stockmaster.app.ui.components.SMNumberField
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.GreenLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.ScanBg
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.BeepPlayer
import java.util.concurrent.Executors

private class LastScanRef {
    var code: String = ""
    var time: Long = 0

    fun accept(newCode: String): Boolean {
        val now = System.currentTimeMillis()
        if (newCode == code && now - time < 1200) return false
        code = newCode
        time = now
        return true
    }
}

data class ScanSuccessCelebrationData(
    val itemId: String,
    val mode: TxType,
    val itemName: String,
    val quantity: Int,
    val unit: String,
    val newStock: Int,
    val totalAmount: Double
)

/** 全屏扫码工作台（对应 Web ScanView）。 */
@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    items: List<InventoryItem>,
    locations: List<String>,
    defaultMode: TxType,
    initialCode: String? = null,
    pendingScanCode: String? = null,
    pendingScanToken: Long? = null,
    onScanConsumed: (() -> Unit)? = null,
    onClose: () -> Unit,
    onAddNewProductWithBarcode: (String) -> Unit,
    onScanCompletedWithItem: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(defaultMode) }
    var useFront by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    // 锁焦动效与出入库完成动效浮层
    var scanLockTrigger by remember { mutableStateOf(false) }
    var successCelebration by remember { mutableStateOf<ScanSuccessCelebrationData?>(null) }

    LaunchedEffect(scanLockTrigger) {
        if (scanLockTrigger) {
            delay(380)
            scanLockTrigger = false
        }
    }

    LaunchedEffect(successCelebration) {
        if (successCelebration != null) {
            delay(1600)
            val targetItemId = successCelebration?.itemId
            successCelebration = null
            if (!targetItemId.isNullOrBlank() && onScanCompletedWithItem != null) {
                onScanCompletedWithItem(targetItemId)
            } else {
                onClose()
            }
        }
    }

    // 识别结果
    var activeItem by remember { mutableStateOf<InventoryItem?>(null) }
    var unrecognizedBarcode by remember { mutableStateOf<String?>(null) }
    var showResultDrawer by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf(1) }
    var location by remember { mutableStateOf(locations.firstOrNull() ?: "默认主仓库") }
    var unitCost by remember { mutableStateOf(0.0) }
    var unitPrice by remember { mutableStateOf(0.0) }
    var reason by remember { mutableStateOf("扫码录入") }

    // 尺码
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var isBatchSizeScan by remember { mutableStateOf(false) }
    var scanSizeBreakdown by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasPermission = true
            permissionDenied = false
            cameraError = null
        } else {
            permissionDenied = true
        }
    }
    LaunchedEffect(hasPermission) {
        if (!hasPermission && !permissionDenied) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 相机闪光灯状态同步（CameraInfo.torchState 为 LiveData）
    val torchState = cameraInfo?.torchState?.observeAsState()
    LaunchedEffect(torchState?.value) {
        torchState?.value?.let { torchOn = it == TorchState.ON }
    }

    fun handleBarcodeDetected(scannedText: String) {
        if (scannedText.isBlank() || successCelebration != null) return
        val clean = scannedText.trim()
        val matched = items.firstOrNull {
            it.barcode.lowercase() == clean.lowercase() || it.sku.lowercase() == clean.lowercase()
        }
        scanLockTrigger = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (matched != null) {
            BeepPlayer.play(BeepPlayer.BeepType.SCAN)
            activeItem = matched
            quantity = if (mode == TxType.IN) 10 else 1
            location = matched.location
            unitCost = matched.unitCost
            unitPrice = matched.unitPrice
            reason = if (mode == TxType.IN) "采购到货上架" else "销售快速出库"
            unrecognizedBarcode = null

            if (matched.hasSizes && matched.sizeVariants.isNotEmpty()) {
                selectedSize = matched.sizeVariants.first().size
                scanSizeBreakdown = matched.sizeVariants.associate { it.size to 0 }
                isBatchSizeScan = false
            }
            showResultDrawer = true
        } else {
            BeepPlayer.play(BeepPlayer.BeepType.ALERT)
            unrecognizedBarcode = clean
            activeItem = null
            showResultDrawer = true
        }
    }

    // 外部（扫码枪）传入的条码：与摄像头识别走同一流程
    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrBlank()) {
            handleBarcodeDetected(initialCode)
        }
    }

    // 外接扫码枪通过 AppRoot 中转的待处理条码（token 变化触发）
    LaunchedEffect(pendingScanToken) {
        if (pendingScanToken != null && !pendingScanCode.isNullOrBlank()) {
            handleBarcodeDetected(pendingScanCode)
            onScanConsumed?.invoke()
        }
    }

    val galleryScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
        )
    }
    DisposableEffect(Unit) {
        onDispose { galleryScanner.close() }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bitmap = com.stockmaster.app.util.ImageUtils.decodeSampledFromUri(context, uri)
            if (bitmap != null) {
                galleryScanner.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { results ->
                        val text = results.firstOrNull()?.rawValue
                        if (text != null) {
                            handleBarcodeDetected(text)
                        } else {
                            Toast.makeText(context, "未能从该图片中识别到清晰的条形码或二维码", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "未能从该图片中识别到清晰的条形码或二维码", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "未能从该图片中识别到清晰的条形码或二维码", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalBatchQuantity = if (isBatchSizeScan) scanSizeBreakdown.values.sum() else quantity

    fun submitTransaction() {
        val item = activeItem ?: return
        val finalPrice = if (mode == TxType.IN) unitCost else unitPrice
        var finalQty = quantity
        var sizePayloadSize: String? = null
        var sizePayloadBreakdown: List<SizeBreakdown> = emptyList()

        if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
            if (isBatchSizeScan) {
                val breakdown = scanSizeBreakdown.filter { it.value > 0 }.map { SizeBreakdown(it.key, it.value) }
                val total = breakdown.sumOf { it.quantity }
                if (total <= 0) {
                    Toast.makeText(context, "请至少为一个尺码输入大于 0 的数量", Toast.LENGTH_SHORT).show()
                    return
                }
                if (mode == TxType.OUT) {
                    val over = breakdown.any { b ->
                        (item.sizeVariants.firstOrNull { it.size == b.size }?.stock ?: 0) < b.quantity
                    }
                    if (over) {
                        Toast.makeText(context, "部分尺码库存不足，请核对配比数量", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                finalQty = total
                sizePayloadBreakdown = breakdown
            } else {
                if (selectedSize.isNullOrEmpty()) {
                    Toast.makeText(context, "请选择需要操作的尺码", Toast.LENGTH_SHORT).show()
                    return
                }
                if (mode == TxType.OUT) {
                    val avail = item.sizeVariants.firstOrNull { it.size == selectedSize }?.stock ?: 0
                    if (avail <= 0) {
                        Toast.makeText(context, "该尺码当前库存为 0，无法出库", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (quantity > avail) {
                        Toast.makeText(context, "该尺码库存仅剩 $avail 件，请调整出库数量", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                sizePayloadSize = selectedSize
            }
        } else if (mode == TxType.OUT) {
            if (item.stock <= 0) {
                Toast.makeText(context, "该商品当前库存为 0，无法出库", Toast.LENGTH_SHORT).show()
                return
            }
            if (quantity > item.stock) {
                Toast.makeText(context, "库存仅剩 ${item.stock} ${item.unit}，请调整出库数量", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val finalStock = if (mode == TxType.IN) item.stock + finalQty else maxOf(0, item.stock - finalQty)
        val totalAmt = finalQty * finalPrice

        viewModel.recordTransaction(
            com.stockmaster.app.ui.TxDraft(
                itemId = item.id,
                itemName = item.name,
                sku = item.sku,
                type = mode,
                quantity = finalQty,
                unitPrice = finalPrice,
                totalPrice = totalAmt,
                reason = reason.ifBlank { if (mode == TxType.IN) "扫码入库" else "扫码出库" },
                location = location.ifBlank { item.location },
                imageUrl = item.imageUrl,
                size = sizePayloadSize,
                sizeBreakdown = sizePayloadBreakdown
            )
        )

        BeepPlayer.play(BeepPlayer.BeepType.SUCCESS)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showResultDrawer = false
        activeItem = null
        successCelebration = ScanSuccessCelebrationData(
            itemId = item.id,
            mode = mode,
            itemName = item.name,
            quantity = finalQty,
            unit = item.unit,
            newStock = finalStock,
            totalAmount = totalAmt
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanBg)
    ) {
        // 相机预览
        if (hasPermission && !permissionDenied) {
            CameraScannerView(
                useFront = useFront,
                onBarcode = { handleBarcodeDetected(it) },
                onCameraReady = { control, info ->
                    cameraControl = control
                    cameraInfo = info
                    hasFlash = info.hasFlashUnit()
                },
                onCameraError = { cameraError = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 取景框与遮罩
        var viewfinderRect by remember { mutableStateOf<Rect?>(null) }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(286.dp)
                .height(224.dp)
                .onGloballyPositioned { viewfinderRect = it.boundsInRoot() }
        ) {
            // 角落标记 + 动态激光与扫码光晕
            CornerMark(mode = mode, isLocked = scanLockTrigger)
        }
        Canvas(Modifier.fillMaxSize()) {
            val rect = viewfinderRect
            if (rect != null) {
                val path = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRect(rect)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(path, Color.Black.copy(alpha = 0.55f))
            }
        }
        // 提示文字
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 140.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✦", color = GreenLight, fontSize = 13.sp)
                Spacer(Modifier.size(6.dp))
                Text(
                    "将条形码 / 二维码置于框内 · 自动对焦识别",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        // 相机错误 / 权限被拒
        if (cameraError != null || permissionDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.size(16.dp))
                Text("摄像头未就绪", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (permissionDenied)
                        "未获得相机权限。请在系统设置中允许本应用使用摄像头。"
                    else cameraError ?: "相机启动失败，请重试。",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(24.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .clickable { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) {
                        Text("上传条码图片", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.size(12.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable { showManualInput = true }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) {
                        Text("手动输入条码", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (permissionDenied) {
                    Spacer(Modifier.size(16.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) {
                        Text("前往系统设置开启权限", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = Icons.Filled.Close,
                onClick = onClose
            )
            Box(
                modifier = Modifier
                    .background(
                        if (mode == TxType.IN) GreenLight else RedPrimary,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (mode == TxType.IN) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle,
                        contentDescription = null,
                        tint = if (mode == TxType.IN) Color(0xFF003822) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        if (mode == TxType.IN) "扫码入库" else "扫码出库",
                        color = if (mode == TxType.IN) Color(0xFF003822) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIconButton(
                    icon = if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    tint = if (torchOn) Color(0xFFFFC107) else Color.White,
                    onClick = {
                        val control = cameraControl
                        if (control != null && hasFlash) {
                            control.enableTorch(!torchOn)
                        } else {
                            Toast.makeText(context, "当前摄像头未开放物理闪光灯控制", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                RoundIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    onClick = { useFront = !useFront }
                )
                RoundIconButton(
                    icon = Icons.Filled.Image,
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
                RoundIconButton(
                    icon = Icons.Filled.Keyboard,
                    bg = if (showManualInput) GreenPrimary else Color.White.copy(alpha = 0.15f),
                    tint = if (showManualInput) Color.White else Color.White,
                    onClick = { showManualInput = !showManualInput }
                )
            }
        }

        // 手动输入
        AnimatedVisibility(
            visible = showManualInput,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                SMTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = "输入或粘贴条形码 / SKU 编码...",
                    mono = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .background(GreenPrimary, RoundedCornerShape(12.dp))
                        .clickable {
                            if (manualInput.isNotBlank()) {
                                handleBarcodeDetected(manualInput.trim())
                                manualInput = ""
                                showManualInput = false
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("识别", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 底部模式切换
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(4.dp)
                    .width(256.dp)
            ) {
                ModePill(
                    modifier = Modifier.weight(1f),
                    text = "扫码入库 (IN)",
                    selected = mode == TxType.IN,
                    selectedBg = GreenLight,
                    selectedColor = Color(0xFF00422B),
                    onClick = { mode = TxType.IN }
                )
                ModePill(
                    modifier = Modifier.weight(1f),
                    text = "扫码出库 (OUT)",
                    selected = mode == TxType.OUT,
                    selectedBg = RedLight,
                    selectedColor = Color(0xFF93000A),
                    onClick = { mode = TxType.OUT }
                )
            }
        }

        // 结果抽屉
        AnimatedVisibility(
            visible = showResultDrawer,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ScanResultDrawer(
                mode = mode,
                item = activeItem,
                unrecognizedBarcode = unrecognizedBarcode,
                locations = locations,
                quantity = quantity,
                onQuantityChange = { quantity = it },
                location = location,
                onLocationChange = { location = it },
                unitCost = unitCost,
                unitPrice = unitPrice,
                onCostChange = { unitCost = it },
                onPriceChange = { unitPrice = it },
                reason = reason,
                onReasonChange = { reason = it },
                selectedSize = selectedSize,
                onSelectedSize = { selectedSize = it },
                isBatchSizeScan = isBatchSizeScan,
                onToggleBatch = { isBatchSizeScan = it },
                sizeBreakdown = scanSizeBreakdown,
                onSizeBreakdownChange = { scanSizeBreakdown = it },
                totalQty = totalBatchQuantity,
                onConfirm = { submitTransaction() },
                onDismiss = {
                    showResultDrawer = false
                    activeItem = null
                    unrecognizedBarcode = null
                },
                onAddNewProduct = {
                    showResultDrawer = false
                    unrecognizedBarcode?.let(onAddNewProductWithBarcode)
                }
            )
        }

        // 扫码出入库成功全屏动效浮层
        AnimatedVisibility(
            visible = successCelebration != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(250), initialScale = 0.82f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier.fillMaxSize()
        ) {
            successCelebration?.let { data ->
                ScanCelebrationCard(
                    data = data,
                    onCardClick = {
                        val targetItemId = data.itemId
                        successCelebration = null
                        if (!targetItemId.isBlank() && onScanCompletedWithItem != null) {
                            onScanCompletedWithItem(targetItemId)
                        } else {
                            onClose()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfettiFireworksEffect(
    modifier: Modifier = Modifier,
    isIncoming: Boolean = true
) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    val particles = remember {
        val colors = if (isIncoming) {
            listOf(
                Color(0xFF10B981), Color(0xFF34D399), Color(0xFFF59E0B),
                Color(0xFFFFD700), Color(0xFF3B82F6), Color(0xFFEC4899), Color.White
            )
        } else {
            listOf(
                Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFF59E0B),
                Color(0xFFFFD700), Color(0xFF8B5CF6), Color(0xFF06B6D4), Color.White
            )
        }
        val random = java.util.Random()
        (0 until 80).map {
            val angle = random.nextDouble() * 2 * Math.PI
            val speed = 900f + random.nextFloat() * 1600f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat() - 450f
            ConfettiParticle(
                vx = vx,
                vy = vy,
                color = colors[random.nextInt(colors.size)],
                size = 10f + random.nextFloat() * 18f,
                rotation = random.nextFloat() * 360f,
                rotSpeed = (random.nextFloat() - 0.5f) * 720f,
                isSquare = random.nextBoolean()
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        val centerX = size.width / 2f
        val centerY = size.height * 0.42f
        val gravity = 1400f

        particles.forEach { p ->
            val curX = centerX + p.vx * t
            val curY = centerY + p.vy * t + 0.5f * gravity * t * t
            val alpha = (1f - t * 0.95f).coerceIn(0f, 1f)
            val curRotation = p.rotation + p.rotSpeed * t
            val curSize = p.size * (1f - 0.2f * t)

            if (alpha > 0.01f) {
                drawContext.canvas.save()
                drawContext.canvas.translate(curX, curY)
                drawContext.canvas.rotate(curRotation)

                if (p.isSquare) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = androidx.compose.ui.geometry.Offset(-curSize / 2, -curSize / 2),
                        size = androidx.compose.ui.geometry.Size(curSize, curSize)
                    )
                } else {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = curSize / 2f
                    )
                }
                drawContext.canvas.restore()
            }
        }
    }
}

private data class ConfettiParticle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotSpeed: Float,
    val isSquare: Boolean
)

@Composable
private fun ScanCelebrationCard(
    data: ScanSuccessCelebrationData,
    onCardClick: () -> Unit
) {
    val isIncoming = data.mode == TxType.IN
    val accentColor = if (isIncoming) GreenPrimary else Color(0xFFDC2626)
    val accentLight = if (isIncoming) GreenLight else Color(0xFFFF6B6B)
    val titleText = if (isIncoming) "扫码入库成功" else "扫码出库成功"
    val deltaSign = if (isIncoming) "+" else "-"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onCardClick),
        contentAlignment = Alignment.Center
    ) {
        // 满屏烟花粒子连发特效
        ConfettiFireworksEffect(
            modifier = Modifier.fillMaxSize(),
            isIncoming = isIncoming
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 动效微徽章
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(2.dp, accentLight.copy(alpha = 0.6f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = accentLight,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 标题
            Text(
                titleText,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // 变动数量大字
            Text(
                "$deltaSign${data.quantity} ${data.unit}",
                color = accentLight,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(10.dp))

            // 商品名称与库存剩余
            Text(
                data.itemName,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "变动后在库: ${data.newStock} ${data.unit} · 金额: ¥${com.stockmaster.app.util.Fmt.moneyRaw(data.totalAmount)}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .background(accentColor, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "查看商品详情 ➔",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    bg: Color = Color.White.copy(alpha = 0.15f),
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ModePill(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    selectedBg: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) selectedBg else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) selectedColor else Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CornerMark(
    mode: TxType,
    isLocked: Boolean
) {
    val isIncoming = mode == TxType.IN
    val cornerColor = if (isIncoming) GreenLight else Color(0xFFFF5252)
    val beamPrimaryColor = if (isIncoming) GreenLight else Color(0xFFFF4D4D)
    val len = 26.dp

    // 锁焦脉冲透明度
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isLocked) 0.9f else 0.12f,
        animationSpec = tween(durationMillis = if (isLocked) 120 else 500)
    )

    Box(Modifier.fillMaxSize()) {
        // 外围发光光晕框（锁焦呼吸）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isLocked) 2.5.dp else 1.dp,
                    color = beamPrimaryColor.copy(alpha = pulseAlpha),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // 四角精密工业角标
        Box(
            Modifier
                .align(Alignment.TopStart)
                .width(len)
                .height(len)
                .border(4.dp, cornerColor, RoundedCornerShape(topStart = 16.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(len)
                .height(len)
                .border(4.dp, cornerColor, RoundedCornerShape(topEnd = 16.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .width(len)
                .height(len)
                .border(4.dp, cornerColor, RoundedCornerShape(bottomStart = 16.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(len)
                .height(len)
                .border(4.dp, cornerColor, RoundedCornerShape(bottomEnd = 16.dp))
        )

        // 动态激光扫描线 + 纵向扇面拖尾光晕
        val transition = rememberInfiniteTransition()
        val laserY by transition.animateFloat(
            initialValue = 8f,
            targetValue = 210f,
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        // 纵向扇面拖尾光晕 (Volumetric Laser Aura)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = maxOf(0f, laserY - 32f) }
                .height(34.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            beamPrimaryColor.copy(alpha = 0.08f),
                            beamPrimaryColor.copy(alpha = 0.25f)
                        )
                    )
                )
        )

        // 主激光束
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = laserY }
                .height(2.5.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            beamPrimaryColor.copy(alpha = 0.4f),
                            Color.White,
                            beamPrimaryColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/** CameraX + ML Kit 扫码。 */
@Composable
fun CameraScannerView(
    useFront: Boolean,
    onBarcode: (String) -> Unit,
    onCameraReady: (CameraControl, CameraInfo) -> Unit,
    onCameraError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = ComposeLocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var disposed by remember { mutableStateOf(false) }

    DisposableEffect(useFront, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
        )
        val lastRef = LastScanRef()

        val preview = Preview.Builder().build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { proxy: ImageProxy ->
            val mediaImage = proxy.image
            if (mediaImage != null) {
                val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                scanner.process(input)
                    .addOnSuccessListener { results ->
                        val text = results.firstOrNull()?.rawValue
                        if (text != null && lastRef.accept(text)) {
                            onBarcode(text)
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            } else {
                proxy.close()
            }
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (disposed) return@Runnable
            val provider = try {
                providerFuture.get()
            } catch (e: Exception) {
                onCameraError("无法启动相机。请确保已在系统设置中允许本应用访问摄像头权限。")
                return@Runnable
            }
            val selector = CameraSelector.Builder()
                .requireLensFacing(if (useFront) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
                .build()
            try {
                provider.unbindAll()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                onCameraReady(camera.cameraControl, camera.cameraInfo)
                onCameraError(null)
            } catch (e: Exception) {
                onCameraError("无法启动相机。请确保已在系统设置中允许本应用访问摄像头权限。")
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            try {
                if (providerFuture.isDone) {
                    providerFuture.get()?.unbindAll()
                }
            } catch (e: Exception) {
            }
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}