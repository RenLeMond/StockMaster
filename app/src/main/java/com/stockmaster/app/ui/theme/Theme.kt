package com.stockmaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassColors = darkColorScheme(
    primary               = GreenPrimary,
    onPrimary             = Color(0xFF032015),
    primaryContainer      = Color(0xFF0B3B2A),
    onPrimaryContainer    = Color(0xFF9FF0CD),
    secondary             = BlueAccent,
    onSecondary           = Color(0xFF061224),
    secondaryContainer    = Color(0xFF12294D),
    onSecondaryContainer  = Color(0xFFBEDCFF),
    tertiary              = RedPrimary,
    onTertiary            = Color(0xFF2A0708),
    tertiaryContainer     = Color(0xFF461213),
    onTertiaryContainer   = Color(0xFFFFC7C5),
    background            = BgMain,
    onBackground          = TextPrimary,
    surface               = DeepPanel,
    onSurface             = TextPrimary,
    surfaceVariant        = Color(0xFF1B2740),
    onSurfaceVariant      = TextSecondary,
    surfaceContainerLowest = DeepBase,
    surfaceContainerLow   = DeepElevated,
    surfaceContainer      = DeepPanel,
    surfaceContainerHigh  = Color(0xFF16233C),
    surfaceContainerHighest = Color(0xFF1B2946),
    outline               = GlassHairline,
    outlineVariant        = Color(0x1FFFFFFF),
    inverseSurface        = Color(0xFFF8FAFC),
    inverseOnSurface      = Color(0xFF10192B),
    error                 = RedPrimary,
    onError               = Color(0xFF2A0708),
    errorContainer        = Color(0xFF461213),
    onErrorContainer      = Color(0xFFFFC7C5),
    scrim                 = ScrimDeep
)

@Composable
fun StockMasterTheme(
    content: @Composable () -> Unit
) {
    // 固定深空玻璃设计系统，不跟随系统亮暗模式
    MaterialTheme(
        colorScheme = GlassColors,
        content = content
    )
}
