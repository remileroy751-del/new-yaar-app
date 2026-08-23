package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InterestStatus(val label: String) {
    PENDING("En attente de réponse"),
    AVAILABLE("Produit disponible"),
    UNAVAILABLE("Produit indisponible")
}

/**
 * Notification créée quand un acheteur clique sur "Je suis intéressé" sur un produit.
 * Elle apparaît dans les notifications du compte du vendeur (shopOwnerId), qui peut alors
 * répondre "Oui disponible" / "Non indisponible", ou écrire directement au client sur
 * WhatsApp (son numéro est déjà connu puisque tout le monde s'inscrit avec son WhatsApp).
 *
 * Pour l'instant, cette notification est uniquement stockée en local et visible dans
 * l'onglet "Ma boutique" (icône cloche). Une fois le backend Firebase branché, la création
 * d'une ligne ici déclenchera aussi une notification push (Firebase Cloud Messaging) vers
 * le téléphone du vendeur, à condition que son compte ait activé les notifications
 * (voir User.notificationsEnabled, réglable dans "Mon profil").
 */
@Entity(tableName = "interests")
data class Interest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val productName: String,
    val productImageUrl: String,
    val shopId: Int,
    val shopOwnerId: Int,
    val buyerId: Int,
    val buyerFirstName: String,
    val buyerWhatsappNumber: String,
    val status: InterestStatus = InterestStatus.PENDING,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
