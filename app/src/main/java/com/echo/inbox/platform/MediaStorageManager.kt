package com.echo.inbox.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaStorageManager(private val context: Context) {
    private val mediaDir: File by lazy {
        File(context.filesDir, "captured_media").apply { if (!exists()) mkdirs() }
    }

    fun saveBitmap(bitmap: Bitmap): String? {
        return runCatching {
            val maxDimension = 1280
            val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val file = File(mediaDir, fileName)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            if (scaled !== bitmap) {
                scaled.recycle()
            }
            file.absolutePath
        }.getOrNull()
    }

    fun saveIcon(icon: Icon): String? {
        return runCatching {
            val drawable = icon.loadDrawable(context)
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                saveBitmap(drawable.bitmap)
            } else {
                null
            }
        }.getOrNull()
    }

    fun saveUri(uriString: String): String? {
        return runCatching {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "content" && scheme != "android.resource") return null

            // First decode bounds only to avoid loading unbounded resolution into memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return null

            val targetDim = 1280
            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= targetDim && options.outHeight / (sampleSize * 2) >= targetDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions) ?: return null
                saveBitmap(bitmap)
            }
        }.getOrNull()
    }

    fun getFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val file = File(path)
            val canonicalFile = file.canonicalFile
            val canonicalBase = mediaDir.canonicalFile
            // Verify path is within media directory to prevent directory traversal
            if (canonicalFile.path.startsWith(canonicalBase.path + File.separator) && file.exists()) {
                file
            } else {
                null
            }
        }.getOrNull()
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching {
            val file = File(path)
            val canonicalFile = file.canonicalFile
            val canonicalBase = mediaDir.canonicalFile
            if (canonicalFile.path.startsWith(canonicalBase.path + File.separator) && file.exists()) {
                file.delete()
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun clearAll(): Boolean {
        return runCatching {
            mediaDir.listFiles()?.forEach { it.delete() }
            true
        }.getOrDefault(false)
    }
}
