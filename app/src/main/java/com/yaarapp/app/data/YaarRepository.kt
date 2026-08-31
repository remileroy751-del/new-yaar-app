package com.yaarapp.app.data

import android.content.Context
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

    /** Synchronisation Firestore/Storage — voir FirestoreSync.kt pour le détail du fonctionnement. */
    private val firestoreSync = FirestoreSync(context, db)

    val session = SessionManager(context)

    /** À appeler une fois au démarrage de l'app (voir YaarApplication.onCreate). */
    fun startRemoteSync() = firestoreSync.startRemoteSync()

    /** Dernier évènement de synchronisation Firebase, en clair (pour affichage direct dans l'app). */
    val lastSyncEvent get() = firestoreSync.lastSyncEvent

    fun clearLastSyncEvent() = firestoreSync.clearLastSyncEvent()

    // ---------- Authentification Firebase ----------

    suspend fun signUp(
        firstName: String,
        country: Country,
        city: String,
        whatsappNumber: String,
        password: String
    ): AuthResult {
        if (firstName.isBlank() || city.isBlank()) return AuthResult.Error("Merci de renseigner votre nom complet.")
        if (whatsappNumber.length < 10) return AuthResult.Error("Le numéro WhatsApp saisi semble incomplet.")
        if (!com.yaarapp.app.firebase.FirebaseModule.isValidPassword(password)) {
            return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        }
        if (userDao.findByWhatsapp(whatsappNumber) != null) {
            return AuthResult.Error("Un compte existe déjà avec ce numéro WhatsApp. Connectez-vous avec votre mot de passe.")
        }

        return try {
            val uid = com.yaarapp.app.firebase.FirebaseModule.createEmailPasswordAccount(whatsappNumber, password)
            val user = User(
                firstName = firstName, country = country, city = city,
                whatsappNumber = whatsappNumber, firebaseUid = uid
            )
            val id = userDao.insert(user).toInt()
            val created = user.copy(id = id)
            session.setCurrentUser(id)
            firestoreSync.syncUser(created)
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    suspend fun secureLegacyAccount(user: User, password: String): AuthResult {
        if (!com.yaarapp.app.firebase.FirebaseModule.isValidPassword(password)) {
            return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        }
        return try {
            val uid = com.yaarapp.app.firebase.FirebaseModule.linkAnonymousAccount(user.whatsappNumber, password)
            val upgraded = user.copy(firebaseUid = uid)
            userDao.update(upgraded)
            firestoreSync.migrateLegacyAccount(upgraded)
            AuthResult.Success(upgraded)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    suspend fun login(whatsappNumber: String, password: String): AuthResult {
        if (!com.yaarapp.app.firebase.FirebaseModule.isValidPassword(password)) {
            return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        }
        return try {
            val uid = com.yaarapp.app.firebase.FirebaseModule.signInWithWhatsappPassword(whatsappNumber, password)
            var user = userDao.findByWhatsapp(whatsappNumber)
            if (user == null) {
                user = firestoreSync.createLocalUserFromCloud(uid, whatsappNumber)
            } else if (user.firebaseUid != uid) {
                user = user.copy(firebaseUid = uid).also { userDao.update(it) }
            }
            if (user == null) return AuthResult.Error("Compte introuvable dans les données Yaar-App.")

            user = firestoreSync.restoreAccount(user)
            session.setCurrentUser(user.id)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    private fun authErrorMessage(e: Exception): String {
        val authCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty().lowercase()
        val message = e.message.orEmpty().lowercase()
        return when {
            authCode.contains("invalid-credential") || authCode.contains("wrong-password") || authCode.contains("invalid-login-credentials") -> "Numéro WhatsApp ou mot de passe incorrect."
            authCode.contains("user-not-found") -> "Aucun compte trouvé avec ce numéro WhatsApp."
            authCode.contains("email-already-in-use") || message.contains("credential is already associated") -> "Ce numéro WhatsApp est déjà associé à un autre compte Firebase."
            authCode.contains("requires-recent-login") -> "Reconnectez-vous puis réessayez."
            else -> e.message ?: "Une erreur Firebase est survenue. Vérifiez votre connexion Internet."
        }
    }

    suspend fun logout() {
        session.clearSession()
        runCatching { com.yaarapp.app.firebase.FirebaseModule.signOut() }
    }

    suspend fun getUser(id: Int): User? = userDao.findById(id)

    suspend fun restoreAccount(user: User): User = firestoreSync.restoreAccount(user)

    suspend fun setNotificationsEnabled(user: User, enabled: Boolean): User {
        val updated = user.copy(notificationsEnabled = enabled)
        userDao.update(updated)
        firestoreSync.syncUser(updated)
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
            ownerUid = owner.firebaseUid,
            name = name,
            whatsappNumber = whatsappNumber,
            country = owner.country,
            city = owner.city,
            logoUrl = logoUrl,
            activityDescription = activityDescription,
            categories = categories.take(ShopCategories.MAX_SELECTABLE)
        )
        val id = shopDao.insert(shop)
        val created = shop.copy(id = id.toInt())
        return try {
            firestoreSync.syncShopNow(created)
        } catch (_: Exception) {
            // La boutique reste disponible localement si le réseau est indisponible.
            created
        }
    }

    /**
     * Achat unique de la capacité supplémentaire de 15 produits (5 → 20 produits actifs),
     * pour [ShopLimits.EXTRA_PACK_PRICE_FCFA] FCFA. N'a pas d'effet si déjà acheté.
     */
    suspend fun purchaseExtraProductSlots(shop: Shop) {
        if (shop.extraProductSlots > 0) return
        val updated = shop.copy(extraProductSlots = ShopLimits.EXTRA_PACK_PRODUCTS)
        shopDao.update(updated)
        firestoreSync.syncShop(updated)
    }

    fun observeShopProducts(shopId: Int): Flow<List<Product>> = productDao.observeByShop(shopId)

    suspend fun addProduct(
        shop: Shop,
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String,
        availableCities: List<String>
    ): AddProductResult {
        if (name.isBlank() || description.isBlank() || imageUrl.isBlank() || price <= 0) {
            return AddProductResult.Error("Merci de remplir tous les champs (photo, nom, description, prix).")
        }
        val activeCount = productDao.countActiveForShop(shop.id)
        if (activeCount >= shop.maxProducts) {
            return AddProductResult.LimitReached(shop.maxProducts)
        }
        val id = productDao.insert(
            Product(
                shopId = shop.id,
                shopName = shop.name,
                name = name,
                description = description,
                price = price,
                imageUrl = imageUrl,
                category = category.ifBlank { "Divers" },
                country = shop.country,
                city = shop.city,
                availableCities = (listOf(shop.city) + availableCities).distinct().take(6),
                ownerUid = shop.ownerUid,
                shopRemoteId = shop.remoteId
            )
        )
        productDao.getById(id.toInt())?.let { firestoreSync.syncProduct(it) }
        return AddProductResult.Success
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        firestoreSync.deleteProductRemote(product)
    }

    /** Le vendeur désactive manuellement un produit encore actif (ex : produit vendu). */
    suspend fun deactivateProduct(product: Product) {
        val updated = product.copy(isActive = false)
        productDao.update(updated)
        firestoreSync.syncProduct(updated)
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
        val updated = product.copy(isActive = true, activatedAt = System.currentTimeMillis())
        productDao.update(updated)
        firestoreSync.syncProduct(updated)
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
        val expiring = productDao.getExpiredActiveForShop(shopId, cutoff)
        val count = productDao.deactivateExpired(shopId, cutoff)
        expiring.forEach { firestoreSync.syncProduct(it.copy(isActive = false)) }
        return count
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
        val promoted = product.copy(isPromoted = true)
        productDao.update(promoted)
        firestoreSync.syncProduct(promoted)
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
                    val updated = product.copy(isPromoted = false)
                    productDao.update(updated)
                    firestoreSync.syncProduct(updated)
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
