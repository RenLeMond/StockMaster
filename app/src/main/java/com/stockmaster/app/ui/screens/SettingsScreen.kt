package com.stockmaster.app.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.BuildConfig
import com.stockmaster.app.data.CsvManager
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.ui.components.ConfirmDialog
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GlassHairline
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore

import com.stockmaster.app.data.BackupManager
import com.stockmaster.app.data.BackupBundle
import com.stockmaster.app.util.ImageUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 设置页（对应 Web SettingsDrawer）。 */

/** 恢复备份的文件大小上限（200MB，含内嵌图片），防止误选超大文件一次性读入内存导致 OOM。 */
private const val MAX_BACKUP_BYTES: Long = 200L * 1024 * 1024

private fun backupFileSize(context: Context, uri: Uri): Long? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }
} catch (_: Exception) {
    null
}

@Composable
fun SettingsScreen(
    items: List<InventoryItem>,
    transactions: List<TransactionRecord>,
    categories: List<String> = emptyList(),
    locations: List<String> = emptyList(),
    onImportItems: (List<InventoryItem>) -> Unit,
    onImportTransactions: ((List<TransactionRecord>) -> Unit)? = null,
    onRestoreBackup: (String, (Result<BackupBundle>) -> Unit) -> Unit = { _, _ -> },
    onClearAll: () -> Unit,
    onOpenCategoryLocation: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportJsonBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            // NonCancellable：SAF 写盘不随页面退出取消，避免导出文件被截断成损坏文件
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    val payloads = ImageUtils.collectImagePayloads(
                        imagesDir = ImageUtils.imagesDir(context),
                        referencedUrls = items.map { it.imageUrl } +
                            transactions.map { it.imageUrl }
                    )
                    val bundle = BackupManager.createBackupBundle(
                        items = items,
                        transactions = transactions,
                        categories = categories,
                        locations = locations,
                        // 内嵌商品图片本体：跨设备/重装后恢复备份时图片不丢
                        imagePayloads = payloads.entries
                    )
                    val jsonText = BackupManager.encodeBackupBundle(bundle)
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(jsonText.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        if (payloads.skippedCount > 0) {
                            Toast.makeText(
                                context,
                                "备份已导出；因体积限制 ${payloads.skippedCount} 张图片未打包，其余数据完整",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "全量数据备份已成功导出！", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // NonCancellable：读取过程不随页面退出取消
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    val size = backupFileSize(context, it)
                    if (size != null && size > MAX_BACKUP_BYTES) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                String.format(
                                    java.util.Locale.US,
                                    "备份文件过大（%.1f MB），超过 %d MB 上限，已取消还原",
                                    size / 1048576.0, MAX_BACKUP_BYTES / 1048576
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                    if (!content.isNullOrBlank()) {
                        // VM 内部已在 IO 解析并把回调切回主线程
                        onRestoreBackup(content) { result ->
                            if (result.isSuccess) {
                                Toast.makeText(context, "全量数据备份已成功还原！", Toast.LENGTH_SHORT).show()
                            } else {
                                val reason = result.exceptionOrNull()?.message ?: "备份文件解析失败，请确认选择的是正确的 JSON 备份文件"
                                Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "备份文件内容为空或读取失败", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "还原出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val exportItemsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(CsvManager.exportItemsCsv(items).toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "商品档案表格已导出", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val exportTxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(CsvManager.exportTransactionsCsv(transactions).toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "出入库流水表格已导出", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    val imported = context.contentResolver.openInputStream(it)?.let { stream ->
                        CsvManager.parseItemsCsv(stream)
                    } ?: emptyList()
                    if (imported.isNotEmpty()) {
                        // VM 契约：状态写入收敛主线程，导入的读-改-写不能与主线程并发
                        withContext(Dispatchers.Main.immediate) {
                            onImportItems(imported)
                            Toast.makeText(context, "成功从表格导入/合并 ${imported.size} 条商品数据！", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "表格格式解析失败或内容为空，请检查文件格式。", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导入出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importTxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    val imported = context.contentResolver.openInputStream(it)?.let { stream ->
                        CsvManager.parseTransactionsCsv(stream)
                    } ?: emptyList()
                    if (imported.isNotEmpty()) {
                        withContext(Dispatchers.Main.immediate) {
                            onImportTransactions?.invoke(imported)
                            Toast.makeText(context, "成功从表格导入 ${imported.size} 条出入库流水！", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "流水表格格式解析失败或内容为空。", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导入出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清空全部数据",
            message = "此操作将删除全部商品档案与出入库流水记录，且无法恢复。建议先导出备份文件。确定继续吗？",
            confirmText = "全部清空",
            danger = true,
            onConfirm = {
                showClearConfirm = false
                onClearAll()
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showExportDialog) {
        com.stockmaster.app.ui.components.GlassDialogPanel(
            onDismissRequest = { showExportDialog = false }
        ) {
            Text("选择导出内容", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportImportOption(title = "全量数据备份", desc = "备份所有档案、流水与分类库位", icon = Icons.Filled.Save, color = Color(0xFF059669)) {
                    showExportDialog = false
                    exportJsonBackupLauncher.launch("StockMaster_全量备份_${CsvManager.today()}.json")
                }
                ExportImportOption(title = "商品档案表格", desc = "导出商品资料与在库数量表", icon = Icons.Filled.Download, color = BlueAccent) {
                    showExportDialog = false
                    exportItemsLauncher.launch("商品档案_${CsvManager.today()}.csv")
                }
                ExportImportOption(title = "出入库流水表格", desc = "导出所有历史出入库单据明细", icon = Icons.AutoMirrored.Filled.ReceiptLong, color = Color(0xFF0EA5E9)) {
                    showExportDialog = false
                    exportTxLauncher.launch("出入库流水_${CsvManager.today()}.csv")
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { showExportDialog = false }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showImportDialog) {
        com.stockmaster.app.ui.components.GlassDialogPanel(
            onDismissRequest = { showImportDialog = false }
        ) {
            Text("选择导入内容", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportImportOption(title = "全量数据恢复", desc = "一键恢复所有档案与历史流水", icon = Icons.Filled.SettingsBackupRestore, color = Color(0xFF4F46E5)) {
                    showImportDialog = false
                    restoreJsonLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
                }
                ExportImportOption(title = "商品档案表格", desc = "批量导入或更新商品档案", icon = Icons.Filled.Upload, color = Color(0xFF7C3AED)) {
                    showImportDialog = false
                    importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                }
                ExportImportOption(title = "出入库流水表格", desc = "导入历史出入库流水记录", icon = Icons.Filled.Upload, color = Color(0xFF0284C7)) {
                    showImportDialog = false
                    importTxLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { showImportDialog = false }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            Text("设置与数据管理", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("数据导入与导出", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            SettingRow(
                icon = Icons.Filled.Upload,
                iconBg = Color(0xFF4F46E5),
                title = "数据导出",
                subtitle = "将数据导出为全量备份或表格",
                onClick = { showExportDialog = true }
            )
            SettingRow(
                icon = Icons.Filled.Download,
                iconBg = Color(0xFF059669),
                title = "数据导入",
                subtitle = "从备份文件恢复，或导入外部数据",
                onClick = { showImportDialog = true }
            )

            Spacer(Modifier.height(4.dp))
            Text("基础数据管理", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            SettingRow(
                icon = Icons.Filled.Layers,
                iconBg = Color(0xFF0EA5E9),
                title = "分类与库位管理",
                subtitle = "新增、重命名或删除分类与存放库位",
                onClick = onOpenCategoryLocation
            )
            SettingRow(
                icon = Icons.Filled.DeleteForever,
                iconBg = RedPrimary,
                title = "清空全部数据",
                subtitle = "删除所有商品档案与流水记录",
                danger = true,
                onClick = { showClearConfirm = true }
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            setImageResource(R.mipmap.ic_launcher)
                        }
                    },
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "货本 StockMaster v${BuildConfig.VERSION_NAME} · 原生 Android 版",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (danger) RedPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f), rowShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) RedPrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExportImportOption(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                desc,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
