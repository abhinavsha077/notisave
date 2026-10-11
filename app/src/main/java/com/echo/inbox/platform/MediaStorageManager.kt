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
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val file = File(mediaDir, fileName)
            FileOutputStream(file).use { out ->
                // Compress to JPEG with high quality and manageable size
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        }.getOrNull()
    }

    fun saveIcon(icon: Icon): String? {
        if (Build.VERSION.SDK_INT < 23) return null
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
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                saveBitmap(bitmap)
            }
        }.getOrNull()
    }

    fun getFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { File(path).delete() }.getOrDefault(false)
    }

    fun clearAll(): Boolean {
        return runCatching {
            mediaDir.listFiles()?.forEach { it.delete() }
            true
        }.getOrDefault(false)
    }
}
