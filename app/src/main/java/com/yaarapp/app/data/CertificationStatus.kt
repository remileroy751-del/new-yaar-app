package com.yaarapp.app.data

enum class CertificationStatus(val label: String) {
    NONE("Non certifiée"),
    /** Paiement effectué, pièce d'identité envoyée : en attente d'étude par l'équipe Yaar-App. */
    PENDING("Certification en cours d'étude"),
    CERTIFIED("Boutique certifiée")
}
