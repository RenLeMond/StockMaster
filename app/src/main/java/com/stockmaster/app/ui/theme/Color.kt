package com.stockmaster.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── 深空环境底色（Ambient Backdrop 基底）──────────────────────
val DeepBase        = Color(0xFF070C16)   // 全局深石墨蓝基底
val DeepElevated    = Color(0xFF0E1728)   // 抬升面板基色
val DeepPanel       = Color(0xFF111D33)   // 对话框/抽屉近实心基色
val ScanBg          = Color(0xFF05080F)   // 扫码工作台极深背景

// ── 环境光晕（Mesh Gradient Glow Sources）────────────────────
val GlowEmerald     = Color(0xFF34D399)   // 翡翠绿环境光
val GlowAzure       = Color(0xFF38BDF8)   // 科技蓝环境光
val GlowViolet      = Color(0xFF818CF8)   // 远景紫罗兰微光
val GlowAmber       = Color(0xFFFBBF24)   // 预警琥珀光

// ── 毛玻璃表面材质（Glass Surface Fills）─────────────────────
val GlassFill01     = Color(0x0DFFFFFF)   // 5% 白 · 最轻浮层
val GlassFill02     = Color(0x14FFFFFF)   // 8% 白 · 列表内嵌块
val GlassFill03     = Color(0x1FFFFFFF)   // 12% 白 · 标准卡面顶
val GlassFill04     = Color(0x2BFFFFFF)   // 17% 白 · 强调卡面顶
val GlassHairline   = Color(0x24FFFFFF)   // 14% 白 · 分割线/弱边框
val GlassBorderHi   = Color(0x61FFFFFF)   // 38% 白 · 边框高光端
val GlassBorderLo   = Color(0x12FFFFFF)   // 7% 白 · 边框暗部端
val ScrimDeep       = Color(0xA3050A14)   // 弹窗深色模糊遮罩
val ScrimDrawer     = Color(0x8C040913)   // 底部抽屉遮罩

// ── 主题品牌色（暗色底适配提亮）───────────────────────────────
val GreenPrimary    = Color(0xFF34D399)   // 主色（图标、文字高亮、成功）
val GreenDark       = Color(0xFF10B981)   // 渐变深端
val GreenLight      = Color(0xFF6EE7B7)   // 辅助亮色
val GreenTint       = Color(0x2434D399)   // 半透明翡翠底（徽章）
val GreenBorder     = Color(0x5934D399)   // 翡翠发光边框
val GreenInkOnSolid = Color(0xFF03301F)   // 实心绿上的墨色文字

// 信号红 · 警示、出库（暗色底提亮为 coral）
val RedPrimary      = Color(0xFFF87171)
val RedDark         = Color(0xFFDC2626)   // 实心按钮渐变深端
val RedBright       = Color(0xFFEF4444)   // 实心按钮渐变亮端
val RedLight        = Color(0x26F87171)   // 半透明红底
val RedTint         = Color(0x24F87171)
val RedBorder       = Color(0x59F87171)

// 数据蓝 · 利润、链接、分类标签
val BlueAccent      = Color(0xFF60A5FA)
val BlueLightBg     = Color(0x2460A5FA)
val BorderBlue      = Color(0x5960A5FA)

// ── 背景与表面（Material Scheme 映射）────────────────────────
val BgMain          = DeepBase            // 全局底色
val BgCard          = DeepElevated        // 卡片基色（Material surface）

// ── 文字阶梯（严守 ≥4.5:1）──────────────────────────────────
val TextPrimary     = Color(0xFFF8FAFC)   // 主数据 · 近纯白
val TextSecondary   = Color(0xFFCBD5E1)   // 副标题 · 浅板岩
val TextMuted       = Color(0xFF94A3B8)   // 辅助说明（深底上 ≈6.9:1）

// ── 边框与分割线 ────────────────────────────────────────────
val BorderLight     = GlassHairline
val DividerColor    = Color(0x0FFFFFFF)

// ── 玻璃拟态预设渐变材质（Glass Brushes）─────────────────────
val GlassCardFillGradient = listOf(
    Color(0x24FFFFFF), // 14%
    Color(0x0AFFFFFF)  // 4%
)

val GlassElevatedFillGradient = listOf(
    Color(0x2BFFFFFF), // 17%
    Color(0x12FFFFFF)  // 7%
)

val GlassPanelFillGradient = listOf(
    Color(0xF018253D),
    Color(0xF00F1A2D)
)

val GlassHUDFillGradient = listOf(
    Color(0xF2131F35),
    Color(0xE60A1221)
)

val GlassCrystalEmeraldGradient = listOf(
    Color(0x4034D399),
    Color(0x14059669)
)

val GlassCrystalAzureGradient = listOf(
    Color(0x4060A5FA),
    Color(0x142563EB)
)

val GlassCrystalRedGradient = listOf(
    Color(0x40F87171),
    Color(0x14DC2626)
)

val GlassCrystalAmberGradient = listOf(
    Color(0x40FBBF24),
    Color(0x14D97706)
)

// ── 兼容旧浅色字面量（历史引用点统一映射为玻璃材质）─────────
val LegacySubtleFill = GlassFill02

