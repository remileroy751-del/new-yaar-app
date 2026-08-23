package com.yaarapp.app.data

import com.yaarapp.app.util.PasswordHasher

/**
 * Données de démonstration utilisées uniquement au tout premier lancement de
 * l'application (base de données vide). Deux boutiques dans deux villes/pays
 * différents, pour que le tri "produits de ma ville en priorité" (Marketplace
 * et Recherche) soit visible dès le premier essai.
 *
 * Comptes de test (voir README) :
 *   Boutique "Chic & Style" (Lomé, Togo)     → WhatsApp 0022890000001 / mot de passe test1234
 *   Boutique "Yaar Électro" (Cotonou, Bénin) → WhatsApp 0022990000002 / mot de passe test1234
 */
object SeedData {

    fun demoUsers(): List<User> = listOf(
        User(
            firstName = "Ama",
            sex = Sex.F,
            country = Country.TOGO,
            city = "Lomé",
            whatsappNumber = "0022890000001",
            passwordHash = PasswordHasher.hash("test1234")
        ),
        User(
            firstName = "Kossi",
            sex = Sex.M,
            country = Country.BENIN,
            city = "Cotonou",
            whatsappNumber = "0022990000002",
            passwordHash = PasswordHasher.hash("test1234")
        )
    )

    fun demoShops(fashionOwnerId: Int, electroOwnerId: Int): List<Shop> = listOf(
        Shop(
            ownerId = fashionOwnerId,
            name = "Chic & Style",
            whatsappNumber = "0022890000001",
            country = Country.TOGO,
            city = "Lomé",
            activityDescription = "Vente de vêtements, chaussures et accessoires pour homme et femme.",
            categories = listOf("Vêtements", "Chaussures", "Accessoires")
        ),
        Shop(
            ownerId = electroOwnerId,
            name = "Yaar Électro",
            whatsappNumber = "0022990000002",
            country = Country.BENIN,
            city = "Cotonou",
            activityDescription = "Électroménager et petit électronique pour la maison.",
            categories = listOf("Électroménager", "Électronique")
        )
    )

    fun demoProducts(fashionShopId: Int, electroShopId: Int): List<Product> = listOf(
        Product(
            shopId = fashionShopId,
            shopName = "Chic & Style",
            name = "Sac à main élégant",
            description = "Sac à main tendance, simili-cuir de qualité, idéal pour toutes les occasions.",
            price = 17000.0,
            imageUrl = "res:product_sac",
            category = "Mode",
            country = Country.TOGO,
            city = "Lomé"
        ),
        Product(
            shopId = fashionShopId,
            shopName = "Chic & Style",
            name = "Veste homme élégante",
            description = "Veste croisée pour homme, coupe soignée, parfaite pour les grandes occasions.",
            price = 15000.0,
            imageUrl = "res:product_veste",
            category = "Mode",
            country = Country.TOGO,
            city = "Lomé"
        ),
        Product(
            shopId = fashionShopId,
            shopName = "Chic & Style",
            name = "Chaussures femme talon",
            description = "Sandales à talon fin, brides croisées, coloris doré.",
            price = 9000.0,
            imageUrl = "res:product_chaussure",
            category = "Mode",
            country = Country.TOGO,
            city = "Lomé"
        ),
        Product(
            shopId = electroShopId,
            shopName = "Yaar Électro",
            name = "Tondeuse professionnelle",
            description = "Kit de tonte complet avec tondeuse, finisseur et rasoir, accessoires inclus.",
            price = 15000.0,
            imageUrl = "res:product_tondeuse",
            category = "Électronique",
            country = Country.BENIN,
            city = "Cotonou"
        ),
        Product(
            shopId = electroShopId,
            shopName = "Yaar Électro",
            name = "Mixeur multifonction",
            description = "Blender robuste avec bol en verre, broyeur à glace, plusieurs vitesses.",
            price = 15000.0,
            imageUrl = "res:product_moulinex",
            category = "Électronique",
            country = Country.BENIN,
            city = "Cotonou"
        ),
        Product(
            shopId = electroShopId,
            shopName = "Yaar Électro",
            name = "Mini frigo 2 portes",
            description = "Réfrigérateur compact 2 portes, idéal chambre ou petit espace.",
            price = 25000.0,
            imageUrl = "res:product_frigo",
            category = "Électronique",
            country = Country.BENIN,
            city = "Cotonou"
        )
    )
}
