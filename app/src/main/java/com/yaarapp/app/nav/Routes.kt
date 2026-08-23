package com.yaarapp.app.nav

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING_LOCATION = "onboarding_location" // choix pays + ville, avant inscription
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    const val MARKETPLACE = "marketplace"          // "Acheter"
    const val SEARCH = "search"                     // recherche par mot-clé + ville
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART = "cart"

    const val MY_SHOP = "my_shop"                   // "Ma boutique"
    const val CREATE_SHOP = "create_shop"
    const val ADD_PRODUCT = "add_product"
    const val PLANS = "plans"
    const val NOTIFICATIONS = "notifications"        // notifications "je suis intéressé"

    const val SELECT_PRODUCT_TO_PROMOTE = "select_product_to_promote"
    const val CONFIGURE_AD_CAMPAIGN = "configure_ad_campaign"
    const val KKIAPAY_CHECKOUT = "kkiapay_checkout"

    const val MY_ADS = "my_ads"                     // "Ma Publicité"
    const val CERTIFY_SHOP = "certify_shop"          // "Certifié ma boutique"

    const val SHOP_PUBLIC = "shop_public/{shopId}"   // vitrine publique d'une boutique

    const val PROFILE = "profile"                   // "Mon profil"

    fun productDetail(productId: Int) = "product/$productId"
    fun shopPublic(shopId: Int) = "shop_public/$shopId"
}
