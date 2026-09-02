package com.yaarapp.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.Interest
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WhatsAppHelper {
    fun contactInterestedBuyer(context: Context, interest: Interest) {
        val message = "Bonjour ${interest.buyerFirstName}, vous avez montré de l'intérêt pour \"${interest.productName}\" sur Yaar-App. Je vous contacte à ce sujet."
        openWhatsApp(context, interest.buyerWhatsappNumber, message)
    }

    fun orderProduct(context: Context, product: Product, shop: Shop) {
        val message = "Bonjour, je souhaite commander le produit \"${product.name}\" (${formatPrice(product.price)}) publié sur votre boutique ${shop.name} sur Yaar-App."
        openWhatsApp(context, shop.whatsappNumber, message)
    }

    /**
     * Prépare une discussion WhatsApp avec la photo du produit comme média partagé.
     * Android/WhatsApp ne permet pas de préselectionner simultanément un numéro wa.me
     * et une pièce jointe via l'API publique : on cible donc WhatsApp avec la photo et
     * le message prérempli, puis l'utilisateur choisit la conversation du fournisseur.
     */
    fun discussProduct(context: Context, product: Product, shop: Shop) {
        val message = "Bonjour, je suis intéressé(e) par le produit \"${product.name}\" (${formatPrice(product.price)}) publié par ${shop.name} à ${product.city} sur Yaar-App."
        val image = product.imageUrl
        if (image.isBlank() || image.startsWith("res:")) {
            openWhatsApp(context, shop.whatsappNumber, message)
            return
        }
        Thread {
            try {
                val file = downloadImage(context, image)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage("com.whatsapp")
                }
                Handler(Looper.getMainLooper()).post {
                    try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (_: Exception) { openWhatsApp(context, shop.whatsappNumber, message) }
                }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post { openWhatsApp(context, shop.whatsappNumber, message) }
            }
        }.start()
    }

    fun orderCartGroupedByShop(context: Context, items: List<CartItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "Votre panier est vide", Toast.LENGTH_SHORT).show()
            return
        }
        items.groupBy { it.shopId }.values.forEach { shopItems ->
            val shopName = shopItems.first().shopName
            val shopNumber = shopItems.first().shopWhatsappNumber
            val sb = StringBuilder("Bonjour, je souhaite commander sur votre boutique $shopName (via Yaar-App) :\n\n")
            var total = 0.0
            shopItems.forEach { item ->
                val lineTotal = item.price * item.quantity
                total += lineTotal
                sb.append("• ${item.productName} x${item.quantity} - ${formatPrice(lineTotal)}\n")
            }
            sb.append("\nTotal : ${formatPrice(total)}")
            openWhatsApp(context, shopNumber, sb.toString())
        }
    }

    private fun downloadImage(context: Context, imageUrl: String): File {
        val dir = File(context.cacheDir, "whatsapp_share").apply { mkdirs() }
        val file = File(dir, "product_${System.currentTimeMillis()}.jpg")
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
        }
        connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        connection.disconnect()
        if (!file.exists() || file.length() == 0L) throw IllegalStateException("Image indisponible")
        return file
    }

    private fun formatPrice(value: Double): String = "${value.toLong()} FCFA"

    private fun toWaLinkNumber(number: String): String = number.trim().removePrefix("+").let { if (it.startsWith("00")) it.substring(2) else it }

    private fun openWhatsApp(context: Context, number: String, message: String) {
        val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        val url = "https://wa.me/${toWaLinkNumber(number)}?text=$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "WhatsApp n'est pas installé", Toast.LENGTH_SHORT).show() }
    }
}
