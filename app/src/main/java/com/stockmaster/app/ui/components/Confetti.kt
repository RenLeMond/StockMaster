package com.stockmaster.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private data class ConfettiParticle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotSpeed: Float,
    val isSquare: Boolean
)

/**
 * 全屏烟花纸屑动效：从画面中心（偏上）爆开并受重力下落，约 1.5s 淡出。
 * isIncoming 区分配色：入库/成功用绿金系，出库用红紫系。
 */
@Composable
fun ConfettiFireworksEffect(
    modifier: Modifier = Modifier,
    isIncoming: Boolean = true
) {
    val progress = remember { Animatable(0f) }
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
                        topLeft = Offset(-curSize / 2, -curSize / 2),
                        size = Size(curSize, curSize)
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
