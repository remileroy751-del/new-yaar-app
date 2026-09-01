package com.yaarapp.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copie une photo choisie dans la galerie (via le sélecteur de photos Android,
 * qui ne nécessite pas de permission de stockage) vers le stockage interne de
 * l'application, afin que la photo du produit reste accessible même après un
 * redémarrage du téléphone.
 */
object ImageStorage {

    fun saveToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val productImagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
            val destFile = File(productImagesDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Traduit une valeur `Product.imageUrl` en modèle utilisable par Coil
     * (identifiant de ressource pour les produits de démo, fichier local pour
     * les photos importées, chaîne brute sinon - ex. URL distante).
     */
    fun resolveImageModel(context: Context, imageUrl: String): Any {
        return when {
            imageUrl.startsWith("res:") -> {
                val name = imageUrl.removePrefix("res:")
                context.resources.getIdentifier(name, "drawable", context.packageName)
            }
            imageUrl.startsWith("/") -> File(imageUrl)
            imageUrl.startsWith("file://") -> File(imageUrl.removePrefix("file://"))
            else -> imageUrl
        }
    }
}
