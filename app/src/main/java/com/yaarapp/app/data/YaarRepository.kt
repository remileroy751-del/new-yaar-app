package com.yaarapp.app.data

import android.content.Context
import com.yaarapp.app.util.PasswordHasher
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class AddProductResult {
    object Success : AddProductResult()
    /** Le vendeur a atteint la limite de produits ACTIFS de sa boutique (voir [ShopLimits]). */
    data class LimitReached(val maxProducts: Int) : AddProductResult()
    data class Error(val message: String) : AddProductResult()
}

class YaarRepository(context: Context) {

    private val db = YaarDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val shopDao = db.shopDao()
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()
    private val interestDao = db.interestDao()
    private val adCampaignDao = db.adCampaignDao()

    val session = SessionManager(context)

    // ---------- Amorçage des données de démonstration ----------

    suspend fun seedIfEmpty() {
        if (userDao.count() > 0) return
        val users = SeedData.demoUsers()
        val fashionOwnerId = userDao.insert(users[0]).toInt()
        val electroOwnerId = userDao.insert(users[1]).toInt()

        val shops = SeedData.demoShops(fashionOwnerId, electroOwnerId)
        val fashionShopId = shopDao.insert(shops[0]).toInt()
        val electroShopId = shopDao.insert(shops[1]).toInt()

        productDao.insertAll(SeedData.demoProducts(fashionShopId, electroShopId))
    }

    // ---------- Authentification (locale, voir data/User.kt) ----------

    /**
     * @param whatsappNumber déjà normalisé au format "00" + indicatif + numéro local
     * (voir [com.yaarapp.app.util.PhoneFormat]).
     */
    suspend fun signUp(
        firstName: String,
        sex: Sex,
        country: Country,
        city: String,
        whatsappNumber: String,
        password: String
    ): AuthResult {
        if (firstName.isBlank() || city.isBlank() || password.length < 4) {
            return AuthResult.Error("Merci de remplir tous les champs (mot de passe : 4 caractères minimum).")
        }
        if (whatsappNumber.length < 10) {
            return AuthResult.Error("Le numéro WhatsApp saisi semble incomplet.")
        }
        if (userDao.findByWhatsapp(whatsappNumber) != null) {
            return AuthResult.Error("Un compte existe déjà avec ce numéro WhatsApp.")
        }
        val user = User(
            firstName = firstName,
            sex = sex,
            country = country,
            city = city,
            whatsappNumber = whatsappNumber,
            passwordHash = PasswordHasher.hash(password)
        )
        val id = userDao.insert(user)
        session.setCurrentUser(id.toInt())
        return AuthResult.Success(user.copy(id = id.toInt()))
    }

    suspend fun login(whatsappNumber: String, password: String): AuthResult {
        val user = userDao.findByWhatsapp(whatsappNumber)
            ?: return AuthResult.Error("Aucun compte trouvé avec ce numéro WhatsApp.")
        if (!PasswordHasher.matches(password, user.passwordHash)) {
            return AuthResult.Error("Mot de passe incorrect.")
        }
        session.setCurrentUser(user.id)
        return AuthResult.Success(user)
    }

    suspend fun logout() {
        session.clearSession()
    }

    suspend fun getUser(id: Int): User? = userDao.findById(id)

    suspend fun setNotificationsEnabled(user: User, enabled: Boolean): User {
        val updated = user.copy(notificationsEnabled = enabled)
        userDao.update(updated)
        return updated
    }

    // ---------- Boutique du vendeur connecté ----------

    fun observeMyShop(ownerId: Int): Flow<Shop?> = shopDao.observeShopForOwner(ownerId)

    /** La boutique hérite automatiquement du pays et de la ville du profil du vendeur. */
    suspend fun createShop(
        owner: User,
        name: String,
        whatsappNumber: String,
        logoUrl: String?,
        activityDescription: String,
        categories: List<String>
    ): Shop {
        val shop = Shop(
            ownerId = owner.id,
            name = name,
            whatsappNumber = whatsappNumber,
            country = owner.country,
            city = owner.city,
            logoUrl = logoUrl,
            activityDescription = activityDescription,
            categories = categories.take(ShopCategories.MAX_SELECTABLE)
        )
        val id = shopDao.insert(shop)
        return shop.copy(id = id.toInt())
    }

    /**
     * Achat unique de la capacité supplémentaire de 15 produits (5 → 20 produits actifs),
     * pour [ShopLimits.EXTRA_PACK_PRICE_FCFA] FCFA. N'a pas d'effet si déjà acheté.
     */
    suspend fun purchaseExtraProductSlots(shop: Shop) {
        if (shop.extraProductSlots > 0) return
        shopDao.update(shop.copy(extraProductSlots = ShopLimits.EXTRA_PACK_PRODUCTS))
    }

    fun observeShopProducts(shopId: Int): Flow<List<Product>> = productDao.observeByShop(shopId)

    suspend fun addProduct(
        shop: Shop,
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String
    ): AddProductResult {
        if (name.isBlank() || description.isBlank() || imageUrl.isBlank() || price <= 0) {
            return AddProductResult.Error("Merci de remplir tous les champs (photo, nom, description, prix).")
        }
        val activeCount = productDao.countActiveForShop(shop.id)
        if (activeCount >= shop.maxProducts) {
            return AddProductResult.LimitReached(shop.maxProducts)
        }
        productDao.insert(
            Product(
                shopId = shop.id,
                shopName = shop.name,
                name = name,
                description = description,
                price = price,
                imageUrl = imageUrl,
                category = category.ifBlank { "Divers" },
                country = shop.country,
                city = shop.city
            )
        )
        return AddProductResult.Success
    }

    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    /** Le vendeur désactive manuellement un produit encore actif (ex : produit vendu). */
    suspend fun deactivateProduct(product: Product) {
        productDao.update(product.copy(isActive = false))
    }

    /**
     * Remet un produit désactivé en vente : réactive et réinitialise le compteur de 14 jours.
     * Vérifie que la boutique n'a pas déjà atteint sa limite de produits actifs.
     */
    suspend fun reactivateProduct(product: Product, shop: Shop): AddProductResult {
        val activeCount = productDao.countActiveForShop(shop.id)
        if (activeCount >= shop.maxProducts) {
            return AddProductResult.LimitReached(shop.maxProducts)
        }
        productDao.update(product.copy(isActive = true, activatedAt = System.currentTimeMillis()))
        return AddProductResult.Success
    }

    suspend fun productCountForShop(shopId: Int): Int = productDao.countActiveForShop(shopId)

    /**
     * À appeler chaque fois que le vendeur ouvre sa boutique : désactive automatiquement
     * tout produit actif dont les 14 jours d'exposition gratuite sont dépassés, et
     * retourne le nombre de produits concernés (pour afficher la notification).
     */
    suspend fun deactivateExpiredProducts(shopId: Int): Int {
        val cutoff = System.currentTimeMillis() - FREE_LISTING_DURATION_MS
        return productDao.deactivateExpired(shopId, cutoff)
    }

    // ---------- Campagnes publicitaires ("Promouvoir mes produits") ----------

    /**
     * Lance une campagne : [expositions] doit être compris entre [AdPricing.MIN_EXPOSITIONS]
     * et [AdPricing.MAX_EXPOSITIONS], [days] entre [AdPricing.MIN_DAYS] et [AdPricing.MAX_DAYS].
     * Le montant facturé est [AdPricing.priceFor] (déjà payé sur Kkiapay avant cet appel).
     */
    suspend fun createAdCampaign(product: Product, shop: Shop, expositions: Int, days: Int): AdCampaign {
        val now = System.currentTimeMillis()
        val campaign = AdCampaign(
            productId = product.id,
            productName = product.name,
            shopId = shop.id,
            totalExpositions = expositions,
            remainingExpositions = expositions,
            durationDays = days,
            startedAt = now,
            endsAt = now + days * 24L * 60L * 60L * 1000L,
            priceFcfa = AdPricing.priceFor(expositions)
        )
        val id = adCampaignDao.insert(campaign)
        productDao.update(product.copy(isPromoted = true))
        return campaign.copy(id = id.toInt())
    }

    fun observeActiveAdCampaignsForShop(shopId: Int): Flow<List<AdCampaign>> =
        adCampaignDao.observeActiveForShop(shopId)

    /**
     * Moteur d'exposition des campagnes publicitaires : à appeler une fois à chaque
     * ouverture de l'application. Chaque campagne encore active "consomme" une exposition ;
     * dès qu'elle atteint 0 exposition restante ou dépasse sa date de fin, elle se termine
     * et le produit associé redevient normal (non sponsorisé).
     */
    suspend fun recordAppOpenExposure() {
        val now = System.currentTimeMillis()
        val activeCampaigns = adCampaignDao.getAllActive()
        for (campaign in activeCampaigns) {
            val newRemaining = (campaign.remainingExpositions - 1).coerceAtLeast(0)
            val stillRunning = newRemaining > 0 && now < campaign.endsAt
            adCampaignDao.update(campaign.copy(remainingExpositions = newRemaining, isActive = stillRunning))
            if (!stillRunning) {
                productDao.getById(campaign.productId)?.let { product ->
                    productDao.update(product.copy(isPromoted = false))
                }
            }
        }
    }

    // ---------- Certification de boutique ----------

    suspend fun requestShopCertification(shop: Shop, idCardFrontUrl: String, idCardBackUrl: String) {
        shopDao.update(
            shop.copy(
                certificationStatus = CertificationStatus.PENDING,
                idCardFrontUrl = idCardFrontUrl,
                idCardBackUrl = idCardBackUrl,
                certificationRequestedAt = System.currentTimeMillis()
            )
        )
    }

    // ---------- Marketplace ("Acheter") ----------

    fun observeMarketplaceProducts(): Flow<List<Product>> = productDao.observeAllActive()

    fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    suspend fun getProduct(id: Int): Product? = productDao.getById(id)

    suspend fun getShop(id: Int): Shop? = shopDao.getById(id)

    // ---------- Notifications "Je suis intéressé" ----------

    suspend fun expressInterest(product: Product, shop: Shop, buyer: User) {
        interestDao.insert(
            Interest(
                productId = product.id,
                productName = product.name,
                productImageUrl = product.imageUrl,
                shopId = shop.id,
                shopOwnerId = shop.ownerId,
                buyerId = buyer.id,
                buyerFirstName = buyer.firstName,
                buyerWhatsappNumber = buyer.whatsappNumber
            )
        )
    }

    fun observeInterestsForOwner(ownerId: Int): Flow<List<Interest>> = interestDao.observeForOwner(ownerId)

    fun observeUnreadInterestCount(ownerId: Int): Flow<Int> = interestDao.observeUnreadCount(ownerId)

    suspend fun markInterestRead(interest: Interest) {
        if (!interest.isRead) interestDao.update(interest.copy(isRead = true))
    }

    suspend fun setInterestStatus(interest: Interest, status: InterestStatus) {
        interestDao.update(interest.copy(status = status, isRead = true))
    }

    // ---------- Panier (par utilisateur connecté) ----------

    fun observeCart(userId: Int): Flow<List<CartItem>> = cartDao.observeCart(userId)

    suspend fun addToCart(userId: Int, product: Product, shop: Shop, quantity: Int = 1) {
        val existing = cartDao.getItem(userId, product.id)
        if (existing != null) {
            cartDao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cartDao.upsert(
                CartItem(
                    userId = userId,
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = shop.id,
                    shopName = shop.name,
                    shopWhatsappNumber = shop.whatsappNumber,
                    quantity = quantity
                )
            )
        }
    }

    suspend fun updateCartQuantity(item: CartItem, quantity: Int) {
        if (quantity <= 0) cartDao.delete(item) else cartDao.update(item.copy(quantity = quantity))
    }

    suspend fun removeFromCart(item: CartItem) = cartDao.delete(item)

    suspend fun clearCart(userId: Int) = cartDao.clear(userId)
}
