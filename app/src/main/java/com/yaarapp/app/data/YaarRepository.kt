package com.yaarapp.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    /** Synchronisation Supabase Database/Storage — voir SupabaseSync.kt pour le détail du fonctionnement. */
    private val supabaseSync = SupabaseSync(context, db)

    val session = SessionManager(context)
    private val credentialStore = CredentialStore(context)

    fun savedLoginEmail(): String = credentialStore.getEmail()
    fun rememberedLoginPassword(): String? = credentialStore.getRememberedPassword()
    fun saveLoginCredentials(email: String, password: String?, rememberPassword: Boolean) {
        credentialStore.saveEmail(email)
        if (rememberPassword && !password.isNullOrBlank()) credentialStore.savePassword(password)
        else credentialStore.clearPassword()
    }

    /** À appeler une fois au démarrage de l'app (voir YaarApplication.onCreate). */
    fun startRemoteSync() = supabaseSync.startRemoteSync()

    suspend fun synchronizeSession() {
        // Important : supabase-kt recharge d'abord la session persistée depuis
        // Android Storage. Ne pas lire currentUserOrNull() avant la fin de cette
        // phase, sinon un redémarrage pouvait être interprété à tort comme une
        // déconnexion et effacer la session locale.
        supabaseSync.awaitAuthInitialization()
        val uid = supabaseSync.currentUid()
        if (uid == null) {
            session.clearSession()
            return
        }

        val localId = session.currentUserId.first()
        if (localId != null) {
            getUser(localId)?.let { userDao.update(it.copy(firebaseUid = uid)) }
            return
        }

        // Restauration automatique si le fichier DataStore local a été perdu
        // alors que la session Supabase est toujours valide.
        val restored = userDao.findByFirebaseUid(uid)
            ?: supabaseSync.currentEmail()?.let { userDao.findByEmail(supabaseSync.normalizeEmail(it)) }
        if (restored != null) {
            userDao.update(restored.copy(firebaseUid = uid))
            session.setCurrentUser(restored.id)
        }
    }

    fun isValidPassword(password: String): Boolean = supabaseSync.isValidPassword(password)
    fun isValidEmail(email: String): Boolean = supabaseSync.isValidEmail(supabaseSync.normalizeEmail(email))

    /** Dernier évènement de synchronisation Supabase, en clair (pour affichage direct dans l'app). */
    val lastSyncEvent get() = supabaseSync.lastSyncEvent

    fun clearLastSyncEvent() = supabaseSync.clearLastSyncEvent()

    /** Affiche une erreur locale dans la bannière de diagnostic sans faire tomber l'application. */
    fun reportLocalSyncIssue(message: String) {
        supabaseSync.reportFailure(message)
    }

    // ---------- Authentification Supabase ----------

    suspend fun signUp(
        firstName: String,
        email: String,
        country: Country,
        city: String,
        whatsappNumber: String,
        password: String
    ): AuthResult {
        val canonicalWhatsapp = supabaseSync.normalizeWhatsapp(whatsappNumber)
        val normalizedEmail = supabaseSync.normalizeEmail(email)
        if (firstName.isBlank() || city.isBlank()) return AuthResult.Error("Merci de renseigner votre prénom et votre ville.")
        if (!supabaseSync.isValidEmail(normalizedEmail)) return AuthResult.Error("Veuillez saisir une adresse e-mail valide.")
        if (canonicalWhatsapp.length < 10) return AuthResult.Error("Le numéro WhatsApp saisi semble incomplet.")
        if (!supabaseSync.isValidPassword(password)) return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        if (userDao.findByEmail(normalizedEmail) != null) return AuthResult.Error("Un compte local existe déjà avec cette adresse e-mail. Connectez-vous avec votre mot de passe.")
        return try {
            val created = supabaseSync.createAccount(normalizedEmail, canonicalWhatsapp, password, firstName, country, city)
            val id = userDao.insert(created).toInt()
            val local = created.copy(id = id)
            session.setCurrentUser(id)
            supabaseSync.syncUserNow(local)
            AuthResult.Success(local)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    suspend fun secureLegacyAccount(user: User, password: String): AuthResult {
        // Les anciennes données Firebase ne sont pas migrées. Ce chemin sert uniquement
        // à permettre à une installation ayant encore un utilisateur local de créer son
        // nouveau compte Supabase avec le même numéro et les mêmes données de profil.
        if (!supabaseSync.isValidPassword(password)) return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        return try {
            val created = supabaseSync.createAccount(user.email, user.whatsappNumber, password, user.firstName, user.country, user.city)
            val upgraded = user.copy(firebaseUid = created.firebaseUid)
            userDao.update(upgraded)
            supabaseSync.syncUserNow(upgraded)
            AuthResult.Success(upgraded)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        val normalizedEmail = supabaseSync.normalizeEmail(email)
        if (!supabaseSync.isValidEmail(normalizedEmail)) return AuthResult.Error("Veuillez saisir une adresse e-mail valide.")
        if (!supabaseSync.isValidPassword(password)) return AuthResult.Error("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        return try {
            val uid = supabaseSync.signIn(normalizedEmail, password)
            var user = userDao.findByEmail(normalizedEmail)
            if (user == null) user = supabaseSync.createLocalUserFromCloud(uid, normalizedEmail)
            if (user == null) return AuthResult.Error("Compte introuvable dans Yaar-App. Créez d'abord votre compte.")
            user = user.copy(firebaseUid = uid).also { userDao.update(it) }
            user = supabaseSync.restoreAccount(user)
            session.setCurrentUser(user.id)
            // L'e-mail est toujours mémorisé localement. Le mot de passe reste
            // mémorisé uniquement lorsque l'utilisateur l'a explicitement demandé.
            credentialStore.saveEmail(normalizedEmail)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    private fun authErrorMessage(e: Exception): String {
        val message = e.message.orEmpty()
        val lower = message.lowercase()
        return when {
            lower.contains("invalid login credentials") || lower.contains("invalid_credentials") || lower.contains("invalid credentials") -> "Adresse e-mail ou mot de passe incorrect."
            lower.contains("user already registered") || lower.contains("already registered") -> "Cette adresse e-mail possède déjà un compte Supabase."
            lower.contains("email not confirmed") -> "La confirmation par e-mail doit être désactivée dans Supabase pour Yaar-App."
            lower.contains("network") || lower.contains("unable to resolve") || lower.contains("timeout") -> "Connexion Internet impossible. Vérifiez votre réseau puis réessayez."
            else -> message.ifBlank { "Une erreur Supabase est survenue. Vérifiez votre connexion Internet." }
        }
    }

    suspend fun logout() {
        session.clearSession()
        runCatching { supabaseSync.signOut() }
    }

    /** Supprime définitivement le compte Supabase, ses données cloud, ses fichiers et ses données locales. */
    suspend fun deleteAccount(user: User, password: String) {
        val uid = user.firebaseUid ?: throw IllegalStateException("Session Supabase introuvable.")
        if (!supabaseSync.isValidPassword(password)) throw IllegalArgumentException("Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement.")
        supabaseSync.verifyPassword(user.email, password)
        supabaseSync.deleteAccountData(uid)
        productDao.deleteAllForOwnerId(user.id)
        interestDao.deleteAllForUser(user.id)
        adCampaignDao.deleteAllForOwner(user.id)
        cartDao.clear(user.id)
        shopDao.deleteAllForOwner(user.id)
        userDao.deleteById(user.id)
        session.clearSession()
        supabaseSync.deleteCurrentAccount()
    }

    suspend fun sendChatMessage(product: Product, shop: Shop, buyer: User, text: String) =
        supabaseSync.sendChatMessage(product, shop, buyer, text)

    fun observeChatMessages(conversationId: String): Flow<List<ChatMessage>> =
        supabaseSync.observeChatMessages(conversationId)

    fun conversationId(product: Product, shop: Shop, buyer: User): String =
        supabaseSync.conversationId(product, shop, buyer)

    suspend fun getUser(id: Int): User? = userDao.findById(id)

    suspend fun restoreAccount(user: User): User = supabaseSync.restoreAccount(user)

    suspend fun setNotificationsEnabled(user: User, enabled: Boolean): User {
        val updated = user.copy(notificationsEnabled = enabled)
        userDao.update(updated)
        supabaseSync.syncUser(updated)
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
            supabaseSync.syncShopNow(created)
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
        supabaseSync.syncShop(updated)
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
        val created = productDao.getById(id.toInt()) ?: return AddProductResult.Error("Impossible de préparer le produit.")
        return try {
            // La publication n'est confirmée qu'après l'envoi de la photo et du produit
            // vers Supabase. Cela empêche qu'un chemin local inaccessible aux autres
            // téléphones soit enregistré comme image distante.
            supabaseSync.syncProductNow(created)
            AddProductResult.Success
        } catch (e: Exception) {
            AddProductResult.Error(e.message ?: "Impossible de publier le produit sur Supabase.")
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        supabaseSync.deleteProductRemote(product)
    }

    /** Le vendeur désactive manuellement un produit encore actif (ex : produit vendu). */
    suspend fun deactivateProduct(product: Product) {
        val updated = product.copy(isActive = false)
        productDao.update(updated)
        supabaseSync.syncProduct(updated)
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
        supabaseSync.syncProduct(updated)
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
        expiring.forEach { supabaseSync.syncProduct(it.copy(isActive = false)) }
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
        supabaseSync.syncProduct(promoted)
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
            val updatedCampaign = campaign.copy(remainingExpositions = newRemaining, isActive = stillRunning)
            adCampaignDao.update(updatedCampaign)
            val productForCampaign = productDao.getById(campaign.productId)
            val shopForCampaign = productForCampaign?.let { shopDao.getById(it.shopId) }
            if (productForCampaign != null && shopForCampaign != null) runCatching { supabaseSync.syncAdCampaign(updatedCampaign, productForCampaign, shopForCampaign) }
            if (!stillRunning) {
                productDao.getById(campaign.productId)?.let { product ->
                    val updated = product.copy(isPromoted = false)
                    productDao.update(updated)
                    supabaseSync.syncProduct(updated)
                }
            }
        }
    }

    // ---------- Certification de boutique ----------

    suspend fun requestShopCertification(shop: Shop, idCardFrontUrl: String, idCardBackUrl: String) {
        val paidAt = System.currentTimeMillis()
        val expiresAt = paidAt + CertificationConfig.VALIDITY_DAYS * 24L * 60L * 60L * 1000L
        val updated = shop.copy(
            certificationStatus = CertificationStatus.PENDING,
            idCardFrontUrl = idCardFrontUrl,
            idCardBackUrl = idCardBackUrl,
            certificationRequestedAt = paidAt,
            certificationPaidAt = paidAt,
            certificationExpiresAt = expiresAt
        )
        shopDao.update(updated)
        supabaseSync.syncShopNow(updated)
    }

    // ---------- Marketplace ("Acheter") ----------

    fun observeMarketplaceProducts(): Flow<List<Product>> = productDao.observeAllActive()

    fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    suspend fun getProduct(id: Int): Product? = productDao.getById(id)

    suspend fun getShop(id: Int): Shop? = shopDao.getById(id)

    // ---------- Notifications "Je suis intéressé" ----------

    suspend fun expressInterest(product: Product, shop: Shop, buyer: User) {
        val interest = Interest(
            productId = product.id, productName = product.name, productImageUrl = product.imageUrl,
            shopId = shop.id, shopOwnerId = shop.ownerId, buyerId = buyer.id,
            buyerFirstName = buyer.firstName, buyerWhatsappNumber = buyer.whatsappNumber
        )
        interestDao.insert(interest)
        runCatching { supabaseSync.syncInterest(interest, product, shop) }
    }

    fun observeInterestsForOwner(ownerId: Int): Flow<List<Interest>> = interestDao.observeForOwner(ownerId)

    fun observeUnreadInterestCount(ownerId: Int): Flow<Int> = interestDao.observeUnreadCount(ownerId)

    suspend fun markInterestRead(interest: Interest) {
        if (!interest.isRead) {
            val updated = interest.copy(isRead = true)
            interestDao.update(updated)
            runCatching { supabaseSync.updateInterest(updated) }
        }
    }

    suspend fun setInterestStatus(interest: Interest, status: InterestStatus) {
        val updated = interest.copy(status = status, isRead = true)
        interestDao.update(updated)
        runCatching { supabaseSync.updateInterest(updated) }
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
