package com.stockmaster.app.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.stockmaster.app.ui.theme.BgMain
import com.stockmaster.app.ui.theme.DeepBase
import com.stockmaster.app.ui.theme.DeepPanel
import com.stockmaster.app.ui.theme.GlowAzure
import com.stockmaster.app.ui.theme.GlowEmerald
import com.stockmaster.app.ui.theme.GlowViolet
import com.stockmaster.app.ui.theme.GlassBorderHi
import com.stockmaster.app.ui.theme.GlassBorderLo
import com.stockmaster.app.ui.theme.GlassFill01
import com.stockmaster.app.ui.theme.GlassFill03

/** 毛玻璃渐变描边：左上高光 → 右下暗部，塑造透光玻璃边缘。 */
fun Modifier.glassBorder(
    cornerRadius: Dp,
    width: Dp = 1.dp,
    topColor: Color = GlassBorderHi,
    bottomColor: Color = GlassBorderLo
): Modifier = drawBehind {
    val strokePx = width.toPx()
    val radiusPx = CornerRadius(cornerRadius.toPx())
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(topColor, bottomColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        topLeft = Offset(strokePx / 2f, strokePx / 2f),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = radiusPx,
        style = Stroke(width = strokePx)
    )
}

object GlassDefaults {
    val cardFill = listOf(GlassFill03, GlassFill01)

    val crystalEmerald = listOf(
        Color(0xF20E4430),
        Color(0xE6082C1F)
    )

    val crystalAmber = listOf(
        Color(0xE64D3A0A),
        Color(0xCC2E2205)
    )

    val panelFill = listOf(
        Color(0xF21A2743),
        Color(0xF2101B30)
    )

    val hudFill = listOf(
        Color(0xF0152036),
        Color(0xE60D1626)
    )
}

/** 全局环境背景：深空基底 + 多重柔和径向光晕，为玻璃卡片提供折射载体。 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(BgMain)) {
        Canvas(Modifier.fillMaxSize()) {
            fun glow(center: Offset, radius: Float, color: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            val w = size.width
            val h = size.height
            glow(Offset(w * 0.12f, h * 0.05f), w * 0.85f, GlowEmerald.copy(alpha = 0.20f))
            glow(Offset(w * 0.98f, h * 0.28f), w * 0.95f, GlowAzure.copy(alpha = 0.18f))
            glow(Offset(w * 0.25f, h * 0.78f), w * 1.05f, GlowViolet.copy(alpha = 0.12f))
            glow(Offset(w * 0.90f, h * 0.98f), w * 0.80f, GlowEmerald.copy(alpha = 0.10f))
        }
    }
}

/** 可复用毛玻璃卡片容器。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    fill: List<Color> = GlassDefaults.cardFill,
    borderColor: Color = Color.Transparent,
    glow: Color? = null,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .then(
                if (glow != null) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Transparent,
                        spotColor = glow
                    )
                } else Modifier
            )
            .clip(shape)
            .background(Brush.verticalGradient(fill))
            .glassBorder(cornerRadius)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, shape)
                } else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding)
    ) {
        content()
    }
}

/** 玻璃药丸按钮：常态为半透明彩色晶体，filled 时切换为实心发光渐变。 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = GlowEmerald,
    filled: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bg: Brush = if (filled) {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.92f), accent.copy(alpha = 0.68f)))
    } else {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.10f)))
    }
    val contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .glassBorder(cornerRadius)
            .then(
                if (!filled) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.45f), shape)
                } else Modifier
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** 发光状态徽章（入库/充足/缺货等）。 */
@Composable
fun GlassGlowBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    leadingDot: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.10f))))
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingDot) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(accent, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

/** 全屏毛玻璃对话框面板：深色遮罩 + Android 12+ 背景模糊降级兼容。 */
@Composable
fun GlassDialogPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    horizontalMargin: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        DisposableEffect(Unit) {
            val window = try {
                (view.parent as? DialogWindowProvider)?.window
            } catch (_: Exception) {
                null
            }
            var applied = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                try {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = window.attributes
                    attrs.blurBehindRadius = (52 * view.resources.displayMetrics.density).toInt()
                    window.attributes = attrs
                    applied = true
                } catch (_: Exception) {
                }
            }
            onDispose {
                if (applied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        val attrs = window.attributes
                        attrs.blurBehindRadius = 0
                        window.attributes = attrs
                    } catch (_: Exception) {
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Brush.verticalGradient(GlassDefaults.panelFill))
                    .glassBorder(cornerRadius)
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/** 底部浮层毛玻璃面板（配合 ScrimDeep 遮罩使用）。 */
@Composable
fun GlassBottomSheet(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    fill: List<Color> = GlassDefaults.hudFill,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            .background(Brush.verticalGradient(fill))
            .glassBorder(cornerRadius),
        content = content
    )
}

/** 玻璃胶囊标签（Glass Pill Tag）。 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = GlowEmerald,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.70f)))
    } else {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.06f)))
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) accent else accent.copy(alpha = 0.35f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) leadingIcon()
        Text(
            text = text,
            color = if (selected) Color(0xFF032015) else accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** 玻璃微渐变分割线。 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.12f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        color,
                        color,
                        Color.Transparent
                    )
                )
            )
    )
}

