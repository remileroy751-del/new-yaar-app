package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ownerId: Int,
    /** UID Firebase du propriétaire, stable entre téléphones. */
    val ownerUid: String? = null,
    val name: String,
    val whatsappNumber: String,
    /** Pays et ville de la boutique — repris automatiquement du profil du vendeur à la création. */
    val country: Country,
    val city: String,
    /** Logo de la boutique (facultatif) — chemin local de la photo importée depuis la galerie. */
    val logoUrl: String? = null,
    /** Description libre de l'activité de la boutique (ex : "Vente de vêtements et accessoires femme"). */
    val activityDescription: String = "",
    /** Jusqu'à 3 catégories choisies parmi [ShopCategories.defaultCategories] pour décrire la boutique. */
    val categories: List<String> = emptyList(),
    /**
     * Capacité supplémentaire de produits actifs achetée (voir [ShopLimits]).
     * 0 = boutique encore au forfait gratuit (5 produits actifs) ;
     * [ShopLimits.EXTRA_PACK_PRODUCTS] = capacité de 20 produits actifs après paiement.
     * Utilisez la propriété [maxProducts] pour connaître la limite effective.
     */
    val extraProductSlots: Int = 0,
    // ---------- Certification de la boutique ----------
    val certificationStatus: CertificationStatus = CertificationStatus.NONE,
    /** Photos recto/verso de la pièce d'identité envoyées lors de la demande de certification. */
    val idCardFrontUrl: String? = null,
    val idCardBackUrl: String? = null,
    val certificationRequestedAt: Long? = null,
    /** Date du dernier paiement de certification. */
    val certificationPaidAt: Long? = null,
    /** Fin de la période mensuelle payée (30 jours). */
    val certificationExpiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Identifiant du document Firestore correspondant (auto-généré par Firestore,
     * globalement unique) une fois cette boutique synchronisée en ligne. `null` tant
     * que la synchronisation n'a pas encore eu lieu (ex. pas de réseau) — l'app reste
     * pleinement fonctionnelle en local dans ce cas, la synchro réessaiera plus tard.
     */
    val remoteId: String? = null
)
