package com.stockmaster.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.stockmaster.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.TxType
import com.stockmaster.app.ui.screens.AddProductScreen
import com.stockmaster.app.ui.screens.CategoryLocationScreen
import com.stockmaster.app.ui.screens.DashboardScreen
import com.stockmaster.app.ui.screens.GlobalSearchScreen
import com.stockmaster.app.ui.screens.HistoryScreen
import com.stockmaster.app.ui.screens.InventoryScreen
import com.stockmaster.app.ui.screens.ProductDetailScreen
import com.stockmaster.app.ui.screens.QuickTransactionDialog
import com.stockmaster.app.ui.screens.ScanScreen
import com.stockmaster.app.ui.screens.SettingsScreen
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderBlue
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.GreenTint
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import androidx.compose.ui.graphics.Brush

private data class Nav(val screen: String, val param: String? = null, val extra: String? = null)

/** 应用根导航：主 Tab 页 + 全屏 overlay 页 + 快捷出入库对话框。 */
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var navStack by remember { mutableStateOf(listOf(Nav("main"))) }
    val currentNav = navStack.lastOrNull() ?: Nav("main")

    // 外接扫码枪待处理请求：用 token 区分每次扫描，避免重复压栈
    var scanRequest by remember { mutableStateOf<Pair<String, Long>?>(null) }

    // 快捷出入库对话框状态
    var quickTxItem by remember { mutableStateOf<InventoryItem?>(null) }
    var quickTxMode by remember { mutableStateOf(TxType.IN) }

    fun navigateTo(dest: Nav) {
        if (dest.screen == "main") {
            navStack = listOf(Nav("main"))
        } else {
            navStack = navStack + dest
        }
    }

    fun popBack(): Boolean {
        if (quickTxItem != null) {
            quickTxItem = null
            return true
        }
        if (navStack.size > 1) {
            navStack = navStack.dropLast(1)
            return true
        }
        return false
    }

    BackHandler(enabled = navStack.size > 1 || quickTxItem != null) {
        popBack()
    }

    // 外接扫码枪：扫到即暂存待处理请求（token 随时间变化，每次扫描只触发一次）
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.stockmaster.app.util.ScannerGun.onScanned = { code ->
            scanRequest = code to System.nanoTime()
        }
        onDispose {
            com.stockmaster.app.util.ScannerGun.onScanned = null
        }
    }

    // 收到扫码请求时进入 / 复用扫码工作台（不重复压栈）
    androidx.compose.runtime.LaunchedEffect(scanRequest) {
        if (scanRequest != null && currentNav.screen != "scan") {
            navigateTo(Nav("scan", TxType.IN.name))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgMain)) {
        when (currentNav.screen) {
            "main" -> Column(modifier = Modifier.fillMaxSize()) {
                // ── 品牌精工顶栏 (Hardware Precision Titlebar) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .statusBarsPadding()
                        .border(0.5.dp, BorderLight.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 品牌标识区
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    setImageResource(R.mipmap.ic_launcher)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "货本",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "StockMaster",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            Text(
                                when (tab) {
                                    0 -> "极速出入库 · 单机安全存储"
                                    1 -> "在库商品与多尺码矩阵"
                                    2 -> "出入库流水核算与追溯"
                                    else -> "个人卖家库存记账工具"
                                },
                                color = TextSecondary.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 右侧功能区（全局快速搜索 Pill + 上下文动作）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 全局搜索触发展开胶囊
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
                                .clickable { navigateTo(Nav("search")) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "搜索",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "搜索",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (tab) {
                        0 -> DashboardScreen(
                            items = items,
                            transactions = transactions,
                            onNavigateTab = { tab = it },
                            onStartScan = { mode -> navigateTo(Nav("scan", mode.name)) },
                            onSelectItem = { id -> navigateTo(Nav("detail", id)) }
                        )
                        1 -> InventoryScreen(
                            items = items,
                            categories = categories,
                            locations = locations,
                            onSelectItem = { id -> navigateTo(Nav("detail", id)) },
                            onOpenAdd = { navigateTo(Nav("add")) },
                            onQuickIn = { item ->
                                quickTxItem = item
                                quickTxMode = TxType.IN
                            },
                            onQuickOut = { item ->
                                quickTxItem = item
                                quickTxMode = TxType.OUT
                            },
                            onManageCategoriesLocations = { navigateTo(Nav("catloc")) },
                            onImportItems = { viewModel.importItems(it) }
                        )
                        2 -> HistoryScreen(
                            transactions = transactions,
                            onImportTransactions = { viewModel.importTransactions(it) }
                        )
                    }
                }

                // ── 底部浮岛导航栏 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(1.dp, BorderLight.copy(alpha = 0.6f))
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 概览
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Dashboard,
                        label = "概览",
                        selected = tab == 0,
                        onClick = { tab = 0 }
                    )
                    // 库存
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Inventory2,
                        label = "库存",
                        selected = tab == 1,
                        onClick = { tab = 1 }
                    )
                    // 中央扫码浮岛 CTA
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GreenPrimary, Color(0xFF00A369))
                                    )
                                )
                                .clickable { navigateTo(Nav("scan", TxType.IN.name)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "扫码",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "扫码",
                            color = GreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // 流水
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "流水",
                        selected = tab == 2,
                        onClick = { tab = 2 }
                    )
                    // 搜索（快捷入口）
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Settings,
                        label = "设置",
                        selected = false,
                        onClick = { navigateTo(Nav("settings")) }
                    )
                }
            }

            "scan" -> ScanScreen(
                viewModel = viewModel,
                items = items,
                locations = locations,
                defaultMode = TxType.valueOf(currentNav.param ?: "IN"),
                initialCode = currentNav.extra,
                pendingScanCode = scanRequest?.first,
                pendingScanToken = scanRequest?.second,
                onScanConsumed = { scanRequest = null },
                onClose = { popBack() },
                onAddNewProductWithBarcode = { barcode -> navigateTo(Nav("add", barcode)) },
                onScanCompletedWithItem = { itemId ->
                    popBack()
                    navigateTo(Nav("detail", itemId))
                }
            )

            "add" -> AddProductScreen(
                categories = categories,
                locations = locations,
                presetBarcode = currentNav.param,
                onSave = { viewModel.addItem(it) },
                onClose = { popBack() },
                onSaved = {
                    popBack()
                    Toast.makeText(context, "商品档案已创建", Toast.LENGTH_SHORT).show()
                },
                onAddCategory = { viewModel.addCategory(it) },
                onAddLocation = { viewModel.addLocation(it) }
            )

            "detail" -> {
                val item = items.firstOrNull { it.id == currentNav.param }
                if (item == null) {
                    androidx.compose.runtime.LaunchedEffect(Unit) { popBack() }
                } else {
                    ProductDetailScreen(
                        item = item,
                        categories = categories,
                        locations = locations,
                        history = transactions.filter { it.itemId == item.id || it.sku == item.sku },
                        onUpdate = { viewModel.updateItem(it) },
                        onDelete = {
                            viewModel.deleteItem(item.id)
                            popBack()
                            Toast.makeText(context, "商品档案已删除", Toast.LENGTH_SHORT).show()
                        },
                        onQuickIn = {
                            quickTxItem = item
                            quickTxMode = TxType.IN
                        },
                        onQuickOut = {
                            quickTxItem = item
                            quickTxMode = TxType.OUT
                        },
                        onClose = { popBack() }
                    )
                }
            }

            "search" -> GlobalSearchScreen(
                items = items,
                onSelectItem = { id -> navigateTo(Nav("detail", id)) },
                onClose = { popBack() }
            )

            "settings" -> SettingsScreen(
                items = items,
                transactions = transactions,
                categories = categories,
                locations = locations,
                onImportItems = { viewModel.importItems(it) },
                onImportTransactions = { viewModel.importTransactions(it) },
                onRestoreBackup = { json, cb -> viewModel.restoreFromBackupJson(json, cb) },
                onClearAll = {
                    viewModel.clearAll()
                    Toast.makeText(context, "全部数据已清空", Toast.LENGTH_SHORT).show()
                },
                onOpenCategoryLocation = { navigateTo(Nav("catloc")) },
                onClose = { popBack() }
            )

            "catloc" -> CategoryLocationScreen(
                categories = categories,
                locations = locations,
                items = items,
                onAddCategory = { viewModel.addCategory(it) },
                onAddLocation = { viewModel.addLocation(it) },
                onRenameCategory = { old, new -> viewModel.renameCategory(old, new) },
                onRenameLocation = { old, new -> viewModel.renameLocation(old, new) },
                onDeleteCategory = { name -> viewModel.updateCategories(categories.filter { it != name }) },
                onDeleteLocation = { name -> viewModel.updateLocations(locations.filter { it != name }) },
                onClose = { popBack() }
            )
        }

        // 快捷出入库对话框
        quickTxItem?.let { item ->
            QuickTransactionDialog(
                item = item,
                mode = quickTxMode,
                locations = locations,
                onConfirm = { qty, price, location, reason, size ->
                    val ok = viewModel.recordTransaction(
                        TxDraft(
                            itemId = item.id,
                            itemName = item.name,
                            sku = item.sku,
                            type = quickTxMode,
                            quantity = qty,
                            unitPrice = price,
                            totalPrice = qty * price,
                            reason = reason,
                            location = location,
                            imageUrl = item.imageUrl,
                            size = size
                        )
                    )
                    if (!ok) {
                        Toast.makeText(context, "库存不足，出库数量超出当前可用库存", Toast.LENGTH_SHORT).show()
                        return@QuickTransactionDialog
                    }
                    quickTxItem = null
                    com.stockmaster.app.util.BeepPlayer.play(com.stockmaster.app.util.BeepPlayer.BeepType.SUCCESS)
                    Toast.makeText(
                        context,
                        "已${if (quickTxMode == TxType.IN) "入库" else "出库"} $qty ${item.unit}${size?.let { " · $it" } ?: ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { quickTxItem = null }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (selected) GreenPrimary.copy(alpha = 0.1f) else Color.Transparent,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) GreenPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (selected) GreenPrimary else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}