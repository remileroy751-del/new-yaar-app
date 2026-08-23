package com.yaarapp.app.data

/**
 * Certification d'une boutique ("Certifié ma boutique") : le vendeur envoie la photo
 * recto/verso de sa pièce d'identité et paie l'étude du dossier. Le prix est amené à
 * augmenter avec la croissance de la communauté Yaar-App — pensez à mettre à jour
 * [PRICE_FCFA] le moment venu (ou à le piloter depuis Firebase Remote Config une fois
 * le backend branché, pour changer le prix sans republier l'application).
 */
object CertificationConfig {
    const val PRICE_FCFA = 10000
    const val GROWTH_NOTICE =
        "Le montant de la certification augmentera au fur et à mesure que la communauté Yaar-App va croître."
}
