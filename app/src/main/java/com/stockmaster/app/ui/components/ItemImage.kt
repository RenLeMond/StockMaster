package com.stockmaster.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stockmaster.app.ui.theme.BlueLightBg
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.util.ImageUtils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ZoomIn

/**
 * 商品图片：支持 base64 data URL、http(s) URL、无图占位。
 * 支持点击放大预览。
 */
@Composable
fun ItemImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    onTap: (() -> Unit)? = null
) {
    val dataBitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = if (!imageUrl.isNullOrEmpty() && imageUrl.startsWith("data:")) {
            withContext(Dispatchers.Default) { ImageUtils.decodeDataUrl(imageUrl) }
        } else null
    }

    val hasImage = !imageUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .background(BlueLightBg)
            .border(0.5.dp, BorderLight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onTap != null && hasImage) Modifier.clickable(onClick = onTap) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = dataBitmap
        when {
            bmp != null -> {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                if (onTap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "查看大图",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            hasImage -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                if (onTap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "查看大图",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            else -> Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = GreenPrimary.copy(alpha = 0.65f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}