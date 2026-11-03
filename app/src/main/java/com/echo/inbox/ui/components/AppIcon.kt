package com.echo.inbox.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AppIconCache = object : LruCache<String, ImageBitmap>(120) {}

@Composable
fun AppIcon(
    packageName: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val context = LocalContext.current
    val cacheKey = "$packageName:${size.value.toInt()}"
    val image by produceState<ImageBitmap?>(initialValue = AppIconCache.get(cacheKey), cacheKey) {
        if (value != null) return@produceState
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .safeBitmap(size)
                    .asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) {
            AppIconCache.put(cacheKey, loaded)
            value = loaded
        }
    }
    val shape = RoundedCornerShape(size / 4)
    if (image != null) {
        Image(
            painter = BitmapPainter(requireNotNull(image)),
            contentDescription = "$displayName icon",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName.trim().take(1).uppercase().ifBlank { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun Drawable.safeBitmap(size: Dp) = toBitmap(
    width = (size.value * 2).toInt().coerceAtLeast(48),
    height = (size.value * 2).toInt().coerceAtLeast(48),
)
