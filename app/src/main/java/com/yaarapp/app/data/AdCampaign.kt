package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Campagne de mise en avant payante d'un produit ("Promouvoir mes produits").
 *
 * Le moteur d'exposition (voir [YaarRepository.recordAppOpenExposure]) décrémente
 * [remainingExpositions] d'une unité à chaque ouverture de l'application par un
 * acheteur, tant que la campagne est active. La campagne se termine — et le produit
 * redevient normal (non sponsorisé) — dès que [remainingExpositions] atteint 0 OU que
 * la date [endsAt] est dépassée, selon ce qui arrive en premier.
 *
 * NOTE : dans cette version locale (sans backend), une "exposition" correspond à une
 * ouverture de l'application sur CET appareil. Une fois Firebase branché, il suffira de
 * déplacer ce compteur côté serveur (Cloud Function déclenchée à chaque ouverture d'app,
 * tous utilisateurs confondus) pour un comptage réellement partagé.
 */
@Entity(tableName = "ad_campaigns")
data class AdCampaign(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val productName: String,
    val shopId: Int,
    val totalExpositions: Int,
    val remainingExpositions: Int,
    val durationDays: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val endsAt: Long,
    val priceFcfa: Int,
    val isActive: Boolean = true
) {
    fun remainingDays(now: Long = System.currentTimeMillis()): Int {
        if (!isActive || now >= endsAt) return 0
        val dayMs = 24L * 60L * 60L * 1000L
        return (((endsAt - now) + dayMs - 1) / dayMs).toInt().coerceAtLeast(0)
    }

    fun progressFraction(): Float =
        if (totalExpositions <= 0) 0f
        else ((totalExpositions - remainingExpositions).toFloat() / totalExpositions).coerceIn(0f, 1f)
}
