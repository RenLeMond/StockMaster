package com.stockmaster.app.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontFamily
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GlassBorderLo
import com.stockmaster.app.ui.theme.GlassHairline
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.RedPrimary
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.Fmt
import kotlinx.coroutines.delay

/** 毛玻璃圆角卡片。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(GlassDefaults.cardFill))
            .glassBorder(cornerRadius)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, shape)
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/** 双层嵌合毛玻璃卡片 (Glass Double-Bezel Card)。 */
@Composable
fun DoubleBezelCard(
    modifier: Modifier = Modifier,
    outerColor: Color = Color.White.copy(alpha = 0.05f),
    outerBorderColor: Color = GlassHairline,
    innerBorderColor: Color = GlassBorderLo,
    outerRadius: Dp = 20.dp,
    innerRadius: Dp = 16.dp,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val outerShape = RoundedCornerShape(outerRadius)
    val innerShape = RoundedCornerShape(innerRadius)
    Box(
        modifier = modifier
            .clip(outerShape)
            .background(outerColor)
            .glassBorder(outerRadius, topColor = outerBorderColor, bottomColor = outerBorderColor.copy(alpha = 0.3f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(innerShape)
                .background(Color.White.copy(alpha = 0.07f))
                .glassBorder(innerRadius, topColor = innerBorderColor, bottomColor = innerBorderColor.copy(alpha = 0.4f))
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

/** 金融级货币与度量衡分级排版组件 (Tabular Currency Hierarchy)。 */
@Composable
fun MoneyDisplay(
    amount: Double,
    modifier: Modifier = Modifier,
    sign: String = "¥",
    showPlus: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    color: Color = TextPrimary,
    symbolColor: Color = color.copy(alpha = 0.75f),
    decimalColor: Color = color.copy(alpha = 0.65f),
    bold: Boolean = true
) {
    val rawStr = Fmt.moneyRaw(kotlin.math.abs(amount))
    val parts = rawStr.split(".")
    val integerPart = parts.getOrNull(0) ?: "0"
    val decimalPart = parts.getOrNull(1) ?: "00"
    val isNegative = amount < 0
    val isPositive = amount > 0 && showPlus
    val signPrefix = if (isNegative) "-$sign" else if (isPositive) "+$sign" else sign

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = signPrefix,
            color = symbolColor,
            fontSize = (fontSize.value * 0.55).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = (fontSize.value * 0.08).dp)
        )
        Spacer(Modifier.width(1.5.dp))
        Text(
            text = integerPart,
            color = color,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = ".$decimalPart",
            color = decimalColor,
            fontSize = (fontSize.value * 0.58).sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = (fontSize.value * 0.05).dp)
        )
    }
}

/** 嵌套式纽扣按钮 (Glass CTA)。 */
@Composable
fun ButtonInButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0B7A55),
    textColor: Color = Color.White,
    iconBadgeColor: Color = Color.White.copy(alpha = 0.2f),
    iconTint: Color = Color.White,
    borderColor: Color = Color.Transparent,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    .background(iconBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** 可选中玻璃胶囊 chip。 */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = GreenPrimary,
    selectedTextColor: Color = Color(0xFF04241A),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(selectedColor.copy(alpha = 0.95f), selectedColor.copy(alpha = 0.72f)))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f)))
                }
            )
            .border(
                1.dp,
                if (selected) selectedColor else Color.White.copy(alpha = 0.16f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (leadingIcon != null) leadingIcon()
        Text(
            text = text,
            color = if (selected) selectedTextColor else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

/** 数量步进器：± 按钮 + 中央可直接键入，编辑时可自由删除清空，失焦自动归一。 */
@Composable
fun QuantityStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = Int.MAX_VALUE,
    accent: Color = GreenPrimary,
    boxSize: Dp = 46.dp
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value.toString()) }

    fun commit(text: String) {
        val n = text.filter { it.isDigit() }.take(6).toIntOrNull()
        when {
            n != null -> {
                val clamped = n.coerceIn(min, max)
                // 越界输入（如 min=1 时键入 0）立即把草稿吸附到合法值，
                // 避免「编辑中显示 0、失焦后跳变 1」的观感断裂
                if (clamped.toString() != text) draft = clamped.toString()
                onValueChange(clamped)
            }
            text.isEmpty() -> onValueChange(min)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StepButton(icon = Icons.Filled.Remove, size = boxSize, tint = TextPrimary) {
            editing = false
            onValueChange((value - 1).coerceAtLeast(min))
        }
        val centerShape = RoundedCornerShape(12.dp)
        androidx.compose.foundation.text.BasicTextField(
            value = if (editing) draft else value.toString(),
            onValueChange = { input ->
                draft = input.filter { it.isDigit() }.take(6)
                editing = true
                commit(input)
            },
            modifier = Modifier
                .size(width = 112.dp, height = boxSize)
                .clip(centerShape)
                .background(accent.copy(alpha = 0.14f))
                .border(1.5.dp, accent, centerShape)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        editing = true
                        draft = value.toString()
                    } else {
                        editing = false
                    }
                },
            textStyle = TextStyle(
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            cursorBrush = SolidColor(accent),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    innerTextField()
                }
            }
        )
        StepButton(icon = Icons.Filled.Add, size = boxSize, tint = TextPrimary) {
            editing = false
            onValueChange((value + 1).coerceAtMost(max))
        }
    }
}

/**
 * 紧凑型数量步进输入框（± 按钮 + 可直接键入），用于表单中的整数数量字段。
 * 编辑期间保留原始草稿文本可自由删除清空，失焦后回落为归一化数值。
 */
@Composable
fun QuantityStepperField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
    height: Dp = 40.dp,
    accent: Color = GreenPrimary,
    placeholderText: String = "0"
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value.toString()) }
    val btnSide = (height.value * 0.74f).dp
    val btnIcon = (btnSide.value * 0.55f).dp

    fun commit(text: String) {
        val n = text.filter { it.isDigit() }.take(6).toIntOrNull()
        when {
            n != null -> {
                val clamped = n.coerceIn(min, max)
                // 越界输入（如 min=1 时键入 0）立即把草稿吸附到合法值，
                // 避免「编辑中显示 0、失焦后跳变 1」的观感断裂
                if (clamped.toString() != text) draft = clamped.toString()
                onValueChange(clamped)
            }
            text.isEmpty() -> onValueChange(min)
        }
    }

    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassHairline, RoundedCornerShape(11.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(icon = Icons.Filled.Remove, size = btnSide, tint = TextPrimary, iconSize = btnIcon) {
            editing = false
            onValueChange((value - 1).coerceAtLeast(min))
        }
        androidx.compose.foundation.text.BasicTextField(
            value = if (editing) draft else value.toString(),
            onValueChange = { input ->
                draft = input.filter { it.isDigit() }.take(6)
                editing = true
                commit(input)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        editing = true
                        draft = value.toString()
                    } else {
                        editing = false
                    }
                },
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            cursorBrush = SolidColor(accent),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!editing && value <= 0 && placeholderText.isNotEmpty()) {
                        Text(
                            text = placeholderText,
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    } else {
                        innerTextField()
                    }
                }
            }
        )
        StepButton(icon = Icons.Filled.Add, size = btnSide, tint = TextPrimary, iconSize = btnIcon) {
            editing = false
            onValueChange((value + 1).coerceAtMost(max))
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    size: Dp,
    tint: Color,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** 快捷数量 chip 行。 */
@Composable
fun QuickStepRow(
    steps: List<Int>,
    current: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { step ->
            val shape = RoundedCornerShape(8.dp)
            val isCurrent = current == step
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (isCurrent) {
                            Brush.verticalGradient(listOf(GreenPrimary.copy(alpha = 0.95f), GreenPrimary.copy(alpha = 0.7f)))
                        } else {
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isCurrent) GreenPrimary else Color.White.copy(alpha = 0.16f),
                        shape
                    )
                    .clickable { onSelect(step) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = step.toString(),
                    color = if (isCurrent) Color(0xFF04241A) else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** 空状态提示。 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(GlassDefaults.cardFill))
            .glassBorder(20.dp)
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GreenPrimary.copy(alpha = 0.14f))
                .border(1.dp, GreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GreenPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            val btnShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(btnShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    actionText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** 确认对话框（替代 web alert/confirm）。 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    cancelText: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogPanel(onDismissRequest = onDismiss) {
        Column {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (cancelText.isNotBlank()) {
                    TextButton(onClick = onDismiss) {
                        Text(cancelText, color = TextSecondary)
                    }
                    Spacer(Modifier.size(8.dp))
                }
                TextButton(
                    onClick = onConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (danger) RedPrimary else GreenPrimary
                    )
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** 统一文本输入对话框。 */
@Composable
fun InputDialog(
    title: String,
    message: String = "",
    placeholder: String = "请输入内容...",
    initialValue: String = "",
    confirmText: String = "确定",
    cancelText: String = "取消",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    GlassDialogPanel(onDismissRequest = onDismiss) {
        Column {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.height(14.dp))
            SMTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        val clean = text.trim()
                        if (clean.isNotEmpty()) {
                            onConfirm(clean)
                        }
                    }
                )
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (cancelText.isNotBlank()) {
                    TextButton(onClick = onDismiss) {
                        Text(cancelText, color = TextSecondary)
                    }
                    Spacer(Modifier.size(8.dp))
                }
                TextButton(
                    onClick = {
                        val clean = text.trim()
                        if (clean.isNotEmpty()) {
                            onConfirm(clean)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = GreenPrimary
                    )
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** 统一现代风格的下拉菜单。 */
@Composable
fun SMDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(Color(0xFF152036), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF152036),
        shadowElevation = 12.dp,
        content = content
    )
}

@Composable
fun SMDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (selected) GreenPrimary else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        onClick = onClick,
        leadingIcon = leadingIcon,
        trailingIcon = if (selected) {
            {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** 简易信息/警告对话框。 */
@Composable
fun AlertDialog(
    title: String,
    message: String,
    confirmText: String = "知道了",
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        cancelText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}