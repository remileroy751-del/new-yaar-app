package com.yaarapp.app.data

/**
 * Capacité de produits actifs d'une boutique.
 *
 * Offre actuelle (une seule pour le moment) : passer de 5 à 20 produits actifs pour
 * 5 000 FCFA, en un seul paiement (achat unique, pas d'abonnement). Si d'autres paliers
 * sont ajoutés plus tard, il suffira d'étendre ce fichier — le reste du code utilise déjà
 * `Shop.maxProducts`, qui s'adaptera automatiquement.
 */
object ShopLimits {
    const val FREE_PRODUCTS = 5
    const val EXTRA_PACK_PRODUCTS = 15 // 5 + 15 = 20 produits actifs au total
    const val EXTRA_PACK_PRICE_FCFA = 5000
}

/** Nombre maximum de produits actifs autorisés pour cette boutique (gratuit + capacité achetée). */
val Shop.maxProducts: Int
    get() = ShopLimits.FREE_PRODUCTS + extraProductSlots

/**
 * Tarification de la mise en avant payante d'un produit ("Promouvoir mes produits").
 *
 * Le vendeur choisit :
 * - un nombre d'expositions (le produit sera montré à ce nombre d'ouvertures de
 *   l'application par des acheteurs, toutes boutiques confondues) — entre 50 et 1000 ;
 * - une durée de campagne en jours — entre 10 et 30 — sur laquelle ces expositions
 *   seront réparties.
 *
 * Le prix ne dépend QUE du nombre d'expositions : 20 FCFA/exposition (donc 100
 * expositions = 2 000 FCFA, exemple donné). La durée fixe seulement le rythme de
 * diffusion et la date de fin de campagne.
 */
object AdPricing {
    const val PRICE_PER_EXPOSITION_FCFA = 20
    const val MIN_EXPOSITIONS = 50
    const val MAX_EXPOSITIONS = 1000
    const val EXPOSITION_STEP = 50

    const val MIN_DAYS = 10
    const val MAX_DAYS = 30

    fun priceFor(expositions: Int): Int = expositions * PRICE_PER_EXPOSITION_FCFA

    /** Rythme de diffusion indicatif affiché au vendeur : expositions par jour, arrondi au supérieur. */
    fun expositionsPerDay(expositions: Int, days: Int): Int =
        if (days <= 0) expositions else ((expositions + days - 1) / days)
}
