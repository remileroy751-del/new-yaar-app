package com.yaarapp.app.data

/**
 * Catégories de produits, fixes et communes à toute l'application :
 * - Utilisées comme liste déroulante lors de la publication d'un produit (le vendeur
 *   choisit une catégorie dans cette liste, il ne peut pas en saisir une autre).
 * - Affichées en haut de la page d'accueil ("Acheter") pour permettre aux acheteurs de
 *   filtrer par catégorie, même si aucun produit n'existe encore dans certaines d'entre
 *   elles.
 *
 * Pour ajouter/renommer une catégorie plus tard, il suffit de modifier cette liste —
 * elle est utilisée à la fois par l'écran d'ajout de produit et par la page d'accueil.
 */
object ProductCategories {
    val all = listOf(
        "Vêtements",
        "Chaussures",
        "Accessoires",
        "Cosmétiques & Beauté",
        "Électroménager",
        "Électronique",
        "Meubles & Déco",
        "Jouets & Jeux",
        "Alimentation & Boissons",
        "Bijoux",
        "Sport & Loisirs",
        "Bébé & Puériculture",
        "Auto & Moto",
        "Santé & Bien-être",
        "Divers"
    )
}
