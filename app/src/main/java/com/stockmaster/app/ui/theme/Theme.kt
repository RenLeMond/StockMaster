package com.stockmaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = Color.White,
    primaryContainer = GreenTint,
    onPrimaryContainer = GreenDark,
    secondary        = BlueAccent,
    onSecondary      = Color.White,
    secondaryContainer = BlueLightBg,
    onSecondaryContainer = TextPrimary,
    tertiary         = RedPrimary,
    onTertiary       = Color.White,
    tertiaryContainer = RedLight,
    onTertiaryContainer = RedDark,
    background       = BgMain,
    onBackground     = TextPrimary,
    surface          = BgCard,
    onSurface        = TextPrimary,
    surfaceVariant   = BlueLightBg,
    onSurfaceVariant = TextSecondary,
    outline          = BorderLight,
    outlineVariant   = BorderLight,
    error            = RedPrimary,
    onError          = Color.White,
    errorContainer   = RedLight,
    onErrorContainer = RedDark
)

@Composable
fun StockMasterTheme(
    content: @Composable () -> Unit
) {
    // 固定亮色设计系统，不跟随系统暗色模式
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}