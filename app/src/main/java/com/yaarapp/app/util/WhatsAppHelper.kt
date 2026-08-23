package com.yaarapp.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.Interest
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import java.net.URLEncoder

object WhatsAppHelper {

    /**
     * Le vendeur ouvre directement la discussion WhatsApp avec un acheteur qui s'est dit
     * intéressé par un produit (bouton "Écrire aux clients sur WhatsApp" dans les notifications).
     */
    fun contactInterestedBuyer(context: Context, interest: Interest) {
        val message = "Bonjour ${interest.buyerFirstName}, vous avez montré de l'intérêt pour " +
            "\"${interest.productName}\" sur Yaar-App. Je vous contacte à ce sujet."
        openWhatsApp(context, interest.buyerWhatsappNumber, message)
    }

    /** Commande directe d'un seul produit (bouton "Acheter" sur la fiche produit). */
    fun orderProduct(context: Context, product: Product, shop: Shop) {
        val message = "Bonjour, je souhaite commander le produit \"${product.name}\" " +
            "(${formatPrice(product.price)}) publié sur votre boutique ${shop.name} sur Yaar-App."
        openWhatsApp(context, shop.whatsappNumber, message)
    }

    /**
     * Commande du panier. Comme chaque produit peut venir d'une boutique différente,
     * on regroupe les articles par boutique et on envoie un message WhatsApp distinct
     * à chaque vendeur concerné.
     */
    fun orderCartGroupedByShop(context: Context, items: List<CartItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "Votre panier est vide", Toast.LENGTH_SHORT).show()
            return
        }
        val byShop = items.groupBy { it.shopId }
        byShop.values.forEach { shopItems ->
            val shopName = shopItems.first().shopName
            val shopNumber = shopItems.first().shopWhatsappNumber
            val sb = StringBuilder()
            sb.append("Bonjour, je souhaite commander sur votre boutique $shopName (via Yaar-App) :\n\n")
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

    private fun formatPrice(value: Double): String = "${value.toLong()} FCFA"

    /**
     * Les numéros sont stockés au format "00" + indicatif + numéro (ex : "0022890000000"),
     * pratique pour les envois automatiques. Un lien wa.me a besoin du numéro SANS le "00"
     * ni le "+" (ex : "22890000000").
     */
    private fun toWaLinkNumber(number: String): String =
        number.trim().removePrefix("+").let { if (it.startsWith("00")) it.substring(2) else it }

    private fun openWhatsApp(context: Context, number: String, message: String) {
        val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        val url = "https://wa.me/${toWaLinkNumber(number)}?text=$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp n'est pas installé", Toast.LENGTH_SHORT).show()
        }
    }
}
