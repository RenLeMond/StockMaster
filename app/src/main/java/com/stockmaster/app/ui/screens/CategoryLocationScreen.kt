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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.ui.components.ConfirmDialog
import com.stockmaster.app.ui.components.EmptyState
import com.stockmaster.app.ui.components.InputDialog
import com.stockmaster.app.ui.components.SMTextField
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.BlueAccent
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedBorder
import com.stockmaster.app.ui.theme.RedLight
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.RedTint
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary

/** 分类 / 库位管理页。 */
@Composable
fun CategoryLocationScreen(
    categories: List<String>,
    locations: List<String>,
    items: List<InventoryItem>,
    onAddCategory: (String) -> Unit,
    onAddLocation: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onRenameLocation: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var tab by remember { mutableStateOf(0) } // 0 分类 1 库位
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    val currentList = if (tab == 0) categories else locations

    fun addNew() {
        val clean = newName.trim()
        if (clean.isEmpty()) {
            Toast.makeText(context, "请输入${if (tab == 0) "分类" else "库位"}名称", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentList.contains(clean)) {
            Toast.makeText(context, "「$clean」已存在", Toast.LENGTH_SHORT).show()
            return
        }
        if (tab == 0) onAddCategory(clean) else onAddLocation(clean)
        newName = ""
        focusManager.clearFocus()
        Toast.makeText(context, "已添加${if (tab == 0) "分类" else "库位"}「$clean」", Toast.LENGTH_SHORT).show()
    }

    renameTarget?.let { target ->
        InputDialog(
            title = "重命名${if (tab == 0) "分类" else "库位"}",
            message = "重命名「$target」后，使用该${if (tab == 0) "分类" else "库位"}的商品档案将同步更新。",
            placeholder = "请输入新名称...",
            initialValue = target,
            confirmText = "保存",
            onConfirm = { newVal ->
                val clean = newVal.trim()
                if (clean.isNotEmpty() && clean != target) {
                    if (currentList.contains(clean)) {
                        Toast.makeText(context, "「$clean」已存在", Toast.LENGTH_SHORT).show()
                    } else {
                        if (tab == 0) onRenameCategory(target, clean)
                        else onRenameLocation(target, clean)
                        Toast.makeText(context, "已重命名为「$clean」", Toast.LENGTH_SHORT).show()
                    }
                }
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        val usedCount = if (tab == 0) items.count { it.category == target } else items.count { it.location == target }
        ConfirmDialog(
            title = "删除${if (tab == 0) "分类" else "库位"}",
            message = "确定删除「$target」吗？${if (usedCount > 0) "当前有 $usedCount 件商品正在使用该${if (tab == 0) "分类" else "库位"}，删除后商品档案不受影响。" else "该${if (tab == 0) "分类" else "库位"}下暂无商品。"}",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                if (tab == 0) {
                    onDeleteCategory(target)
                } else {
                    onDeleteLocation(target)
                }
                deleteTarget = null
                Toast.makeText(context, "已删除「$target」", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { deleteTarget = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgMain)) {
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("分类与库位管理", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        // Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.dp, BorderLight.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            listOf("商品分类" to Icons.Filled.Layers, "存放库位" to Icons.Filled.LocationOn).forEachIndexed { index, (label, icon) ->
                val tabShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(tabShape)
                        .background(
                            if (tab == index) GreenPrimary else Color.Transparent
                        )
                        .clickable { tab = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (tab == index) Color.White else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(
                            label,
                            color = if (tab == index) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 新增
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SMTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = if (tab == 0) "输入新分类名称..." else "输入新库位名称...",
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { addNew() })
            )
            Spacer(Modifier.width(10.dp))
            val btnShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(btnShape)
                    .background(GreenPrimary)
                    .clickable(onClick = { addNew() })
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("添加", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentList.isEmpty()) {
                item {
                    EmptyState(
                        icon = if (tab == 0) Icons.Filled.Layers else Icons.Filled.LocationOn,
                        title = if (tab == 0) "暂无自定义分类" else "暂无自定义库位",
                        subtitle = "输入名称点击添加，或直接在录入商品页使用预设项"
                    )
                }
            } else {
                items(currentList, key = { it }) { name ->
                    val usedCount = if (tab == 0) items.count { it.category == name } else items.count { it.location == name }
                    val itemShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(Color.White)
                            .border(1.dp, BorderLight.copy(alpha = 0.4f), itemShape)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BlueLightBg, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (tab == 0) Icons.Filled.Layers else Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("使用中商品: $usedCount 件", color = TextMuted, fontSize = 11.sp)
                        }
                        val editBtnShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(editBtnShape)
                                .background(BlueLightBg)
                                .border(0.5.dp, BorderLight.copy(alpha = 0.6f), editBtnShape)
                                .clickable {
                                    renameTarget = name
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "重命名", tint = BlueAccent, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        val delBtnShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(delBtnShape)
                                .background(RedTint)
                                .border(0.5.dp, RedBorder.copy(alpha = 0.5f), delBtnShape)
                                .clickable { deleteTarget = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = RedPrimary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}