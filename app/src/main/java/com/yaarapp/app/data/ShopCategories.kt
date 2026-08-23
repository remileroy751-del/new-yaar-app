package com.yaarapp.app.data

/**
 * Catégories proposées par défaut à la création d'une boutique (jusqu'à 3 à choisir).
 * Pour en ajouter d'autres plus tard, il suffit de compléter cette liste (jusqu'à 10
 * conseillé pour rester lisible) — rien d'autre à changer dans l'application.
 */
object ShopCategories {
    const val MAX_SELECTABLE = 3

    val defaultCategories = listOf(
        "Vêtements",
        "Chaussures",
        "Accessoires",
        "Cosmétiques",
        "Électroménager",
        "Électronique",
        "Meubles",
        "Jouets",
        "Alimentation",
        "Divers"
    )
}
