package com.yaarapp.app.util

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorage {
    fun saveToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "product_images").apply { mkdirs() }
            val destFile = File(dir, "${UUID.randomUUID()}.jpg")
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            bitmap.recycle()
            if (destFile.length() > 0L) destFile.absolutePath else null
        } catch (_: Exception) { null }
    }

    fun resolveImageModel(context: Context, imageUrl: String): Any? {
        return when {
            imageUrl.startsWith("res:") -> context.resources.getIdentifier(imageUrl.removePrefix("res:"), "drawable", context.packageName)
            imageUrl.startsWith("/") -> File(imageUrl)
            imageUrl.startsWith("file://") -> File(imageUrl.removePrefix("file://"))
            imageUrl.startsWith("content://") -> Uri.parse(imageUrl)
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            else -> imageUrl.takeIf { it.isNotBlank() }
        }
    }
}
