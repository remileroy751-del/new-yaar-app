package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durée d'exposition gratuite d'un produit avant désactivation automatique. */
const val FREE_LISTING_DURATION_DAYS = 14
const val FREE_LISTING_DURATION_MS = FREE_LISTING_DURATION_DAYS * 24L * 60L * 60L * 1000L

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shopId: Int,
    /** Nom de la boutique au moment de la publication — affiché en miniature sous le produit. */
    val shopName: String,
    val name: String,
    val description: String,
    val price: Double,
    /**
     * Peut être :
     * - "res:nom_du_drawable" pour les produits de démonstration intégrés à l'application
     * - un chemin de fichier local (photo importée par le vendeur depuis sa galerie)
     * - une URL http(s) si un backend distant est branché plus tard
     */
    val imageUrl: String,
    val category: String,
    /** Pays et ville où le produit est disponible (copiés de la boutique à la publication). */
    val country: Country,
    val city: String,
    /**
     * true = produit visible par les acheteurs dans "Acheter".
     * false = désactivé (automatiquement après 14 jours, ou manuellement par le vendeur).
     * Un produit désactivé n'est jamais supprimé automatiquement : il reste visible
     * par le responsable de la boutique jusqu'à ce qu'il le remette en vente ou le supprime.
     */
    val isActive: Boolean = true,
    /** Date de première publication (ne change jamais). */
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Date de (re)mise en vente : réinitialisée à chaque republication.
     * La désactivation automatique se déclenche 14 jours après cette date.
     */
    val activatedAt: Long = System.currentTimeMillis(),
    /**
     * true tant qu'une campagne publicitaire active fait de la mise en avant pour ce
     * produit (voir [AdCampaign]) — piloté entièrement par [YaarRepository], jamais
     * modifié directement ailleurs.
     */
    val isPromoted: Boolean = false,
    /**
     * Identifiant du document Firestore correspondant une fois ce produit synchronisé
     * en ligne (voir [Shop.remoteId] pour le principe complet).
     */
    val remoteId: String? = null
) {
    fun expiresAt(): Long = activatedAt + FREE_LISTING_DURATION_MS
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = isActive && now >= expiresAt()
}
