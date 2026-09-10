package com.yaarapp.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.yaarapp.app.supabase.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.time.format.DateTimeParseException

private const val TAG = "YaarSupabaseSync"
private const val POLL_INTERVAL_MS = 15_000L

private fun Long.toIso(): String = Instant.ofEpochMilli(this).toString()
private fun String.toEpochMillis(): Long = try { Instant.parse(this).toEpochMilli() } catch (_: DateTimeParseException) { System.currentTimeMillis() }
private fun extractStorageObjectPath(url: String): String? {
    val marker = "/storage/v1/object/public/"
    return url.substringAfter(marker, "").substringAfter('/', "").takeIf { it.isNotBlank() }
}

/** Synchronisation Yaar-App <-> Supabase. Firebase n'est plus utilisé par cette classe. */
class SupabaseSync(context: Context, private val db: YaarDatabase) {
    private val appContext = context.applicationContext
    private val userDao = db.userDao()
    private val shopDao = db.shopDao()
    private val productDao = db.productDao()
    private val interestDao = db.interestDao()
    private val adCampaignDao = db.adCampaignDao()
    private val client get() = SupabaseModule.client
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lastSyncEvent = MutableStateFlow<String?>(null)
    val lastSyncEvent: StateFlow<String?> = _lastSyncEvent
    private var started = false

    fun clearLastSyncEvent() { _lastSyncEvent.value = null }

    /** Les détails techniques restent dans Logcat ; l'utilisateur ne voit qu'un statut court. */
    private fun success(message: String) { Log.i(TAG, message) }

    fun reportFailure(message: String) {
        Log.w(TAG, message)
        _lastSyncEvent.value = if (isOffline()) {
            "Vous êtes hors ligne"
        } else {
            "Connexion indisponible. Réessayez."
        }
    }

    private fun isOffline(): Boolean {
        val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivity.activeNetwork ?: return true
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return true
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun normalizeWhatsapp(whatsappNumber: String): String {
        var digits = whatsappNumber.filter(Char::isDigit)
        while (digits.startsWith("00")) digits = digits.removePrefix("00")
        val codes = listOf("228", "229", "226", "225", "223", "227", "221")
        return if (codes.any { digits.startsWith(it) }) "00$digits" else digits
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean =
        email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))

    fun isValidPassword(password: String): Boolean =
        password.matches(Regex("^[A-Za-z0-9]{6}$"))

    suspend fun createAccount(email: String, whatsappNumber: String, password: String, firstName: String, country: Country, city: String): User {
        val normalizedEmail = normalizeEmail(email)
        require(isValidEmail(normalizedEmail)) { "Adresse e-mail invalide." }
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        client.auth.signUpWith(Email) {
            this.email = normalizedEmail
            this.password = password
        }
        val uid = currentUid() ?: throw IllegalStateException("Supabase n'a pas créé la session du compte. Vérifiez que l'authentification Email est activée et que Confirm email est désactivé.")
        val user = User(
            firstName = firstName,
            email = normalizedEmail,
            country = country,
            city = city,
            whatsappNumber = whatsappNumber,
            firebaseUid = uid
        )
        client.from("users").insert(UserRow.from(user))
        return user
    }

    suspend fun signIn(email: String, password: String): String {
        val normalizedEmail = normalizeEmail(email)
        require(isValidEmail(normalizedEmail)) { "Adresse e-mail invalide." }
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        client.auth.signInWith(Email) {
            this.email = normalizedEmail
            this.password = password
        }
        return currentUid() ?: throw IllegalStateException("Session Supabase introuvable après connexion.")
    }

    suspend fun verifyPassword(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = normalizeEmail(email)
            this.password = password
        }
    }

    suspend fun signOut() {
        runCatching { client.auth.signOut() }
    }


    suspend fun deleteAccountData(uid: String) {
        // Supprimer d'abord les fichiers via Storage API : Supabase recommande de passer
        // par Storage API plutôt que de supprimer directement storage.objects en SQL.
        runCatching {
            val products = client.from("products").select { filter { eq("owner_uid", uid) } }.decodeList<ProductRow>()
            products.mapNotNull { it.imageStoragePath }.chunked(50).forEach { paths ->
                if (paths.isNotEmpty()) client.storage.from("products").delete(*paths.toTypedArray())
            }
            val shops = client.from("shops").select { filter { eq("owner_uid", uid) } }.decodeList<ShopRow>()
            val logoPaths = shops.mapNotNull { it.logoStoragePath }
            if (logoPaths.isNotEmpty()) client.storage.from("shops").delete(*logoPaths.toTypedArray())
            val idPaths = shops.flatMap { listOfNotNull(it.idCardFrontStoragePath, it.idCardBackStoragePath) }
            if (idPaths.isNotEmpty()) client.storage.from("id_cards").delete(*idPaths.toTypedArray())
        }

        // Les suppressions des tables métier sont protégées par les RLS de l'utilisateur courant.
        client.from("interests").delete { filter { eq("buyer_id", uid) } }
        client.from("interests").delete { filter { eq("shop_owner_id", uid) } }
        client.from("ad_campaigns").delete { filter { eq("owner_uid", uid) } }
        val conversations = client.from("conversations").select().decodeList<ConversationRow>()
            .filter { uid in it.participants }
        conversations.forEach { client.from("conversations").delete { filter { eq("id", it.id) } } }
        client.from("products").delete { filter { eq("owner_uid", uid) } }
        client.from("shops").delete { filter { eq("owner_uid", uid) } }
        client.from("users").delete { filter { eq("id", uid) } }
    }

    suspend fun deleteCurrentAccount() {
        currentUid() ?: throw IllegalStateException("Compte Supabase introuvable.")
        // Le RPC SECURITY DEFINER supprime auth.users après les données publiques.
        client.postgrest.rpc("delete_my_account")
        runCatching { client.auth.signOut() }
    }

    suspend fun awaitAuthInitialization() {
        runCatching {
            client.auth.sessionStatus.first { status -> status !is SessionStatus.Initializing }
        }
    }

    suspend fun currentUid(): String? = runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()

    suspend fun currentEmail(): String? = runCatching { client.auth.currentUserOrNull()?.email }.getOrNull()

    fun startRemoteSync() {
        if (started) return
        started = true
        syncScope.launch {
            try {
                refreshPublicData()
                _lastSyncEvent.value = "Connexion réussie"
            } catch (e: Exception) {
                reportFailure(e.message ?: "erreur réseau")
            }
            while (true) {
                delay(POLL_INTERVAL_MS)
                runCatching { refreshPublicData() }
            }
        }
    }

    suspend fun refreshPublicData() {
        val shops = client.from("shops").select().decodeList<ShopRow>()
        for (row in shops) applyShop(row)
        val products = client.from("products").select().decodeList<ProductRow>()
        for (row in products) applyProduct(row)
        currentUid()?.let { refreshPrivateData(it) }
    }

    private suspend fun refreshPrivateData(uid: String) {
        client.from("interests").select {
            filter { eq("shop_owner_id", uid) }
        }.decodeList<InterestRow>().forEach { applyInterest(it) }
        refreshAdCampaigns(uid)
    }

    private suspend fun applyShop(row: ShopRow) {
        val existing = shopDao.findByRemoteId(row.id)
        val owner = row.ownerUid?.let { userDao.findByFirebaseUid(it) }
        val local = row.toDomain(existing?.id ?: 0, owner?.id ?: existing?.ownerId ?: 0)
        if (existing == null) shopDao.insert(local) else shopDao.update(local)
    }

    private suspend fun applyProduct(row: ProductRow) {
        val existing = productDao.findByRemoteId(row.id)
        val localShopId = shopDao.findByRemoteId(row.shopId)?.id ?: existing?.shopId ?: 0
        val local = row.toDomain(existing?.id ?: 0, localShopId)
        if (existing == null) productDao.insert(local) else productDao.update(local)
    }

    private suspend fun applyInterest(row: InterestRow) {
        // Les notifications sont principalement locales dans l'UI actuelle. On évite de
        // dupliquer les lignes en les identifiant par produit + acheteur + date approximative.
        val buyer = userDao.findByFirebaseUid(row.buyerId) ?: return
        val owner = userDao.findByFirebaseUid(row.shopOwnerId) ?: return
        val localProduct = productDao.findByRemoteId(row.productId)
        val localShop = shopDao.findByRemoteId(row.shopId)
        if (localProduct == null || localShop == null) return
        val existing = interestDao.findLatest(localProduct.id, buyer.id, row.createdAt.toEpochMillis())
        val interest = Interest(
            id = existing?.id ?: 0,
            productId = localProduct.id,
            productName = row.productName,
            productImageUrl = row.productImageUrl,
            shopId = localShop.id,
            shopOwnerId = owner.id,
            buyerId = buyer.id,
            buyerFirstName = row.buyerFirstName,
            buyerWhatsappNumber = row.buyerWhatsappNumber,
            status = runCatching { InterestStatus.valueOf(row.status) }.getOrDefault(InterestStatus.PENDING),
            isRead = row.isRead,
            createdAt = row.createdAt.toEpochMillis()
        )
        if (existing == null) interestDao.insert(interest) else interestDao.update(interest)
    }

    private suspend fun refreshAdCampaigns(uid: String) {
        client.from("ad_campaigns").select {
            filter { eq("owner_uid", uid) }
        }.decodeList<AdCampaignRow>().forEach { row ->
            val product = productDao.findByRemoteId(row.productId) ?: return@forEach
            val shop = shopDao.findByRemoteId(row.shopId) ?: return@forEach
            val started = row.startedAt.toEpochMillis()
            val existing = adCampaignDao.findByProductAndStartedAt(product.id, started)
            val campaign = AdCampaign(
                id = existing?.id ?: 0,
                productId = product.id,
                productName = row.productName,
                shopId = shop.id,
                totalExpositions = row.totalExpositions,
                remainingExpositions = row.remainingExpositions,
                durationDays = row.durationDays,
                startedAt = started,
                endsAt = row.endsAt.toEpochMillis(),
                priceFcfa = row.priceFcfa,
                isActive = row.isActive
            )
            if (existing == null) adCampaignDao.insert(campaign) else adCampaignDao.update(campaign)
        }
    }

    suspend fun syncUserNow(user: User): User {
        val uid = currentUid() ?: throw IllegalStateException("Session Supabase introuvable.")
        val remote = user.copy(firebaseUid = uid)
        client.from("users").upsert(UserRow.from(remote)) { onConflict = "id" }
        userDao.update(remote)
        return remote
    }

    suspend fun createLocalUserFromCloud(uid: String, email: String): User? {
        val row = client.from("users").select {
            filter { eq("id", uid) }
        }.decodeList<UserRow>().firstOrNull() ?: return null
        val existing = userDao.findByFirebaseUid(uid) ?: userDao.findByEmail(normalizeEmail(email))
        val user = row.toDomain(existing?.id ?: 0)
        if (existing == null) userDao.insert(user) else userDao.update(user)
        return user.copy(id = existing?.id ?: userDao.findByFirebaseUid(uid)?.id ?: 0)
    }

    suspend fun restoreAccount(localUser: User): User {
        val uid = currentUid() ?: return localUser
        val cloudUser = createLocalUserFromCloud(uid, localUser.email) ?: localUser.copy(firebaseUid = uid)
        if (cloudUser.id == 0) {
            val id = userDao.insert(cloudUser).toInt()
            return cloudUser.copy(id = id)
        }
        val shops = client.from("shops").select {
            filter { eq("owner_uid", uid) }
        }.decodeList<ShopRow>()
        for (row in shops) applyShop(row)
        val products = client.from("products").select {
            filter { eq("owner_uid", uid) }
        }.decodeList<ProductRow>()
        for (row in products) applyProduct(row)
        success("Compte et données synchronisés.")
        return cloudUser
    }

    fun syncUser(user: User) { syncScope.launch { runCatching { syncUserNow(user) }.onFailure { reportFailure("Échec de synchronisation du profil : ${it.message}") } } }

    suspend fun syncShopNow(shop: Shop): Shop {
        val uid = currentUid() ?: throw IllegalStateException("Session Supabase introuvable.")
        val id = shop.remoteId ?: java.util.UUID.randomUUID().toString()
        val logo = shop.logoUrl?.let { uploadLocal("shops", it, "$uid/$id.jpg") }
        val front = shop.idCardFrontUrl?.let { uploadLocal("id_cards", it, "$uid/${id}_front.jpg") }
        val back = shop.idCardBackUrl?.let { uploadLocal("id_cards", it, "$uid/${id}_back.jpg") }
        val updated = shop.copy(
            ownerUid = uid,
            logoUrl = logo?.second ?: shop.logoUrl,
            idCardFrontUrl = null,
            idCardBackUrl = null,
            remoteId = id
        )
        val row = ShopRow(
            id = id, ownerUid = uid, name = updated.name, whatsappNumber = updated.whatsappNumber,
            country = updated.country.name, city = updated.city, logoUrl = updated.logoUrl,
            logoStoragePath = logo?.first ?: extractStorageObjectPath(updated.logoUrl.orEmpty()),
            activityDescription = updated.activityDescription, categories = updated.categories,
            extraProductSlots = updated.extraProductSlots, certificationStatus = updated.certificationStatus.name,
            idCardFrontUrl = null, idCardBackUrl = null,
            certificationRequestedAt = updated.certificationRequestedAt?.toIso(),
            certificationPaidAt = updated.certificationPaidAt?.toIso(),
            certificationExpiresAt = updated.certificationExpiresAt?.toIso(),
            createdAt = updated.createdAt.toIso(),
            idCardFrontStoragePath = front?.first, idCardBackStoragePath = back?.first
        )
        client.from("shops").upsert(row) { onConflict = "id" }
        shopDao.update(updated)
        return updated
    }

    fun syncShop(shop: Shop) { syncScope.launch { runCatching { syncShopNow(shop); success("Boutique \"${shop.name}\" synchronisée.") }.onFailure { reportFailure("Échec boutique : ${it.message}") } } }

    suspend fun syncProductNow(product: Product): Product {
        val uid = currentUid() ?: throw IllegalStateException("Session Supabase introuvable.")
        val localShop = shopDao.getById(product.shopId)
        val shopId = product.shopRemoteId ?: localShop?.remoteId ?: syncShopNow(localShop?.copy(ownerUid = uid) ?: throw IllegalStateException("Boutique introuvable.")).remoteId
        val id = product.remoteId ?: java.util.UUID.randomUUID().toString()
        val upload = uploadLocal("products", product.imageUrl, "$uid/$id.jpg")
        val updated = product.copy(
            ownerUid = uid,
            shopRemoteId = shopId,
            imageUrl = upload.second,
            remoteId = id,
            availableCities = product.availableCities.ifEmpty { listOf(product.city) }
        )
        client.from("products").upsert(ProductRow.from(updated)) { onConflict = "id" }
        // Une ligne product_images garde une référence normalisée au fichier.
        client.from("product_images").upsert(
            ProductImageRow(
                id = java.util.UUID.nameUUIDFromBytes("$id:$uid".toByteArray()).toString(),
                productId = id,
                ownerUid = uid,
                storagePath = upload.first,
                publicUrl = upload.second,
                sortOrder = 0
            )
        ) { onConflict = "id" }
        productDao.update(updated)
        return updated
    }

    fun syncProduct(product: Product) { syncScope.launch { runCatching { syncProductNow(product); success("Produit synchronisé.") }.onFailure { reportFailure("Échec produit : ${it.message}") } } }

    fun deleteProductRemote(product: Product) {
        val id = product.remoteId ?: return
        syncScope.launch {
            runCatching {
                if (product.imageUrl.startsWith("https://lhqnjzuwndgtcjeyyjwj.supabase.co/storage/")) {
                    deleteStoragePath(extractStoragePath(product.imageUrl))
                }
                client.from("products").delete { filter { eq("id", id) } }
            }
        }
    }

    suspend fun ensureConversation(product: Product, shop: Shop, buyer: User): String {
        val buyerUid = buyer.firebaseUid ?: currentUid() ?: throw IllegalStateException("Compte Supabase indisponible")
        val sellerUid = shop.ownerUid ?: product.ownerUid ?: throw IllegalStateException("Fournisseur indisponible")
        require(buyerUid != sellerUid) { "Vous ne pouvez pas démarrer une discussion avec votre propre boutique." }
        val conversationId = listOf(buyerUid, sellerUid, product.remoteId ?: product.id.toString()).joinToString("_")
        val existing = runCatching { client.from("conversations").select { filter { eq("id", conversationId) } }.decodeSingle<ConversationRow>() }.getOrNull()
        client.from("conversations").upsert(
            ConversationRow(
                id = conversationId, buyerUid = buyerUid, sellerUid = sellerUid, productId = product.remoteId,
                productName = product.name, productPrice = product.price, shopName = shop.name, participants = listOf(buyerUid, sellerUid),
                buyerName = existing?.buyerName ?: buyer.firstName, sellerName = existing?.sellerName ?: shop.name,
                buyerWhatsappNumber = existing?.buyerWhatsappNumber ?: buyer.whatsappNumber, sellerWhatsappNumber = existing?.sellerWhatsappNumber ?: shop.whatsappNumber,
                lastMessage = existing?.lastMessage ?: ""
            )
        ) { onConflict = "id" }
        return conversationId
    }

    suspend fun sendChatMessage(product: Product, shop: Shop, buyer: User, text: String) {
        val conversationId = ensureConversation(product, shop, buyer)
        client.from("messages").insert(MessageRow(conversationId = conversationId, senderUid = buyer.firebaseUid ?: currentUid() ?: error("Compte Supabase indisponible"), senderName = buyer.firstName, text = text.trim()))
        client.from("conversations").update(ConversationUpdate(lastMessage = text.trim(), updatedAt = Instant.now().toString())) { filter { eq("id", conversationId) } }
    }

    suspend fun sendChatMessageInConversation(conversationId: String, sender: User, text: String) {
        val uid = sender.firebaseUid ?: currentUid() ?: throw IllegalStateException("Compte Supabase indisponible")
        val conversation = client.from("conversations").select { filter { eq("id", conversationId) } }.decodeSingle<ConversationRow>()
        require(conversation.participants.contains(uid)) { "Vous ne participez pas à cette discussion." }
        client.from("messages").insert(MessageRow(conversationId = conversationId, senderUid = uid, senderName = sender.firstName, text = text.trim()))
        client.from("conversations").update(ConversationUpdate(lastMessage = text.trim(), updatedAt = Instant.now().toString())) { filter { eq("id", conversationId) } }
    }

    fun observeConversations(uid: String): Flow<List<ChatConversation>> = flow {
        while (true) {
            val rows = runCatching {
                val asBuyer = client.from("conversations").select { filter { eq("buyer_uid", uid) } }.decodeList<ConversationRow>()
                val asSeller = client.from("conversations").select { filter { eq("seller_uid", uid) } }.decodeList<ConversationRow>()
                (asBuyer + asSeller).distinctBy { it.id }.sortedByDescending { it.updatedAt?.toEpochMillis() ?: 0L }
            }.getOrDefault(emptyList())
            emit(rows.map { it.toDomain() })
            delay(3_000L)
        }
    }

    fun observeChatMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        while (true) {
            val rows = runCatching {
                client.from("messages").select {
                    filter { eq("conversation_id", conversationId) }
                }.decodeList<MessageRow>().sortedBy { it.createdAt.toEpochMillis() }
            }.getOrDefault(emptyList())
            emit(rows.map { ChatMessage(it.id, it.senderUid, it.senderName, it.text, it.createdAt.toEpochMillis()) })
            delay(2_000L)
        }
    }

    fun observeConversation(conversationId: String): Flow<ChatConversation?> = flow {
        while (true) {
            val row = runCatching {
                client.from("conversations").select { filter { eq("id", conversationId) } }.decodeSingle<ConversationRow>()
            }.getOrNull()
            emit(row?.toDomain())
            delay(3_000L)
        }
    }

    fun conversationId(product: Product, shop: Shop, buyer: User): String {
        val sellerUid = shop.ownerUid ?: product.ownerUid.orEmpty()
        return listOf(buyer.firebaseUid.orEmpty(), sellerUid, product.remoteId ?: product.id.toString()).joinToString("_")
    }

    suspend fun syncInterest(interest: Interest, product: Product, shop: Shop) {
        val buyerUid = userDao.findById(interest.buyerId)?.firebaseUid ?: currentUid() ?: return
        val ownerUid = shop.ownerUid ?: return
        val productRemoteId = product.remoteId ?: return
        val shopRemoteId = shop.remoteId ?: return
        val remoteId = java.util.UUID.nameUUIDFromBytes(
            "interest:$productRemoteId:$buyerUid:${interest.createdAt}".toByteArray()
        ).toString()
        client.from("interests").upsert(
            InterestRow(
                id = remoteId,
                productId = productRemoteId,
                productName = interest.productName,
                productImageUrl = interest.productImageUrl,
                shopId = shopRemoteId,
                shopOwnerId = ownerUid,
                buyerId = buyerUid,
                buyerFirstName = interest.buyerFirstName,
                buyerWhatsappNumber = interest.buyerWhatsappNumber,
                status = interest.status.name,
                isRead = interest.isRead
            )
        ) { onConflict = "id" }
    }

    suspend fun updateInterest(interest: Interest) {
        val uid = currentUid() ?: return
        val product = productDao.getById(interest.productId) ?: return
        val productRemoteId = product.remoteId ?: return
        val buyerUid = userDao.findById(interest.buyerId)?.firebaseUid ?: return
        val remoteId = java.util.UUID.nameUUIDFromBytes(
            "interest:$productRemoteId:$buyerUid:${interest.createdAt}".toByteArray()
        ).toString()
        // Le RLS autorise l'acheteur ou le propriétaire de la boutique à modifier la ligne.
        client.from("interests").update(
            mapOf("status" to interest.status.name, "is_read" to interest.isRead)
        ) {
            filter { eq("id", remoteId) }
        }
    }

    suspend fun syncAdCampaign(campaign: AdCampaign, product: Product, shop: Shop) {
        val uid = currentUid() ?: return
        client.from("ad_campaigns").upsert(
            AdCampaignRow(
                id = java.util.UUID.nameUUIDFromBytes("${shop.remoteId}:${product.remoteId}:${campaign.startedAt}".toByteArray()).toString(),
                productId = product.remoteId ?: return,
                productName = campaign.productName,
                shopId = shop.remoteId ?: return,
                ownerUid = uid,
                totalExpositions = campaign.totalExpositions,
                remainingExpositions = campaign.remainingExpositions,
                durationDays = campaign.durationDays,
                startedAt = campaign.startedAt.toIso(),
                endsAt = campaign.endsAt.toIso(),
                priceFcfa = campaign.priceFcfa,
                isActive = campaign.isActive
            )
        ) { onConflict = "id" }
    }

    private suspend fun uploadLocal(bucketName: String, imageUrl: String, storagePath: String): Pair<String, String> {
        if (imageUrl.startsWith("https://lhqnjzuwndgtcjeyyjwj.supabase.co/storage/")) {
            val full = extractStoragePath(imageUrl)
            return full.substringAfter('/', full) to imageUrl
        }
        val path = imageUrl.removePrefix("file://")
        val file = File(path)
        if (!file.exists() || !file.isFile || file.length() == 0L) {
            throw IllegalStateException("La photo est introuvable sur cet appareil.")
        }
        val finalPath = storagePath.substringBeforeLast('.') + extensionFor(file.name)
        client.storage.from(bucketName).upload(finalPath, file.readBytes()) { upsert = true }
        val publicUrl = if (bucketName == "id_cards") "" else client.storage.from(bucketName).publicUrl(finalPath)
        return finalPath to publicUrl
    }

    private fun extensionFor(name: String): String = when (name.substringAfterLast('.', "jpg").lowercase()) {
        "png" -> ".png"
        "webp" -> ".webp"
        "jpeg" -> ".jpeg"
        else -> ".jpg"
    }

    private suspend fun deleteStoragePath(path: String) {
        if (path.isBlank()) return
        val bucket = path.substringBefore('/')
        val objectPath = path.substringAfter('/', "")
        if (bucket.isNotBlank() && objectPath.isNotBlank()) {
            runCatching { client.storage.from(bucket).delete(objectPath) }
        }
    }

    private fun extractStoragePath(url: String): String {
        val marker = "/storage/v1/object/public/"
        return url.substringAfter(marker, "")
    }


    @Serializable data class UserRow(
        val id: String,
        @SerialName("first_name") val firstName: String,
        val email: String,
        val country: String,
        val city: String,
        @SerialName("whatsapp_number") val whatsappNumber: String,
        @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null
    ) {
        fun toDomain(localId: Int): User = User(localId, firstName, email, runCatching { Country.valueOf(country) }.getOrDefault(Country.TOGO), city, whatsappNumber, notificationsEnabled, createdAt?.toEpochMillis() ?: System.currentTimeMillis(), id)
        companion object { fun from(u: User) = UserRow(u.firebaseUid ?: error("UID manquant"), u.firstName, u.email, u.country.name, u.city, u.whatsappNumber, u.notificationsEnabled, u.createdAt.toIso()) }
    }

    @Serializable data class ShopRow(
        val id: String,
        @SerialName("owner_uid") val ownerUid: String,
        val name: String,
        @SerialName("whatsapp_number") val whatsappNumber: String,
        val country: String,
        val city: String,
        @SerialName("logo_url") val logoUrl: String? = null,
        @SerialName("logo_storage_path") val logoStoragePath: String? = null,
        @SerialName("activity_description") val activityDescription: String = "",
        val categories: List<String> = emptyList(),
        @SerialName("extra_product_slots") val extraProductSlots: Int = 0,
        @SerialName("certification_status") val certificationStatus: String = "NONE",
        @SerialName("id_card_front_url") val idCardFrontUrl: String? = null,
        @SerialName("id_card_back_url") val idCardBackUrl: String? = null,
        @SerialName("certification_requested_at") val certificationRequestedAt: String? = null,
        @SerialName("certification_paid_at") val certificationPaidAt: String? = null,
        @SerialName("certification_expires_at") val certificationExpiresAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("id_card_front_storage_path") val idCardFrontStoragePath: String? = null,
        @SerialName("id_card_back_storage_path") val idCardBackStoragePath: String? = null
    ) {
        fun toDomain(localId: Int, ownerId: Int) = Shop(localId, ownerId, ownerUid, name, whatsappNumber, runCatching { Country.valueOf(country) }.getOrDefault(Country.TOGO), city, logoUrl, activityDescription, categories, extraProductSlots, runCatching { CertificationStatus.valueOf(certificationStatus) }.getOrDefault(CertificationStatus.NONE), idCardFrontUrl, idCardBackUrl, certificationRequestedAt?.toEpochMillis(), certificationPaidAt?.toEpochMillis(), certificationExpiresAt?.toEpochMillis(), createdAt?.toEpochMillis() ?: System.currentTimeMillis(), id)
        companion object { fun from(s: Shop) = ShopRow(
            s.remoteId ?: error("ID boutique manquant"), s.ownerUid ?: error("UID manquant"), s.name, s.whatsappNumber, s.country.name, s.city,
            s.logoUrl, s.logoUrl?.let { extractStorageObjectPath(it) }, s.activityDescription, s.categories, s.extraProductSlots, s.certificationStatus.name,
            null, null, s.certificationRequestedAt?.toIso(), s.certificationPaidAt?.toIso(), s.certificationExpiresAt?.toIso(), s.createdAt.toIso(),
            s.idCardFrontUrl?.takeIf { !it.startsWith("http") }, s.idCardBackUrl?.takeIf { !it.startsWith("http") }
        ) }
    }

    @Serializable data class ProductRow(
        val id: String,
        @SerialName("shop_id") val shopId: String,
        @SerialName("owner_uid") val ownerUid: String,
        @SerialName("shop_name") val shopName: String,
        val name: String,
        val description: String,
        val price: Double,
        @SerialName("image_url") val imageUrl: String,
        @SerialName("image_storage_path") val imageStoragePath: String? = null,
        val category: String,
        val country: String,
        val city: String,
        @SerialName("available_cities") val availableCities: List<String> = emptyList(),
        @SerialName("is_active") val isActive: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("activated_at") val activatedAt: String? = null,
        @SerialName("is_promoted") val isPromoted: Boolean = false
    ) {
        fun toDomain(localId: Int, localShopId: Int) = Product(localId, localShopId, shopName, name, description, price, imageUrl, category, runCatching { Country.valueOf(country) }.getOrDefault(Country.TOGO), city, availableCities.ifEmpty { listOf(city) }, ownerUid, shopId, isActive, createdAt?.toEpochMillis() ?: System.currentTimeMillis(), activatedAt?.toEpochMillis() ?: (createdAt?.toEpochMillis() ?: System.currentTimeMillis()), isPromoted, id)
        companion object { fun from(p: Product) = ProductRow(p.remoteId ?: error("ID produit manquant"), p.shopRemoteId ?: error("ID boutique manquant"), p.ownerUid ?: error("UID manquant"), p.shopName, p.name, p.description, p.price, p.imageUrl, extractStorageObjectPath(p.imageUrl), p.category, p.country.name, p.city, p.availableCities.ifEmpty { listOf(p.city) }, p.isActive, p.createdAt.toIso(), p.activatedAt.toIso(), p.isPromoted) }
    }

    @Serializable data class ProductImageRow(val id: String, @SerialName("product_id") val productId: String, @SerialName("owner_uid") val ownerUid: String, @SerialName("storage_path") val storagePath: String, @SerialName("public_url") val publicUrl: String?, @SerialName("sort_order") val sortOrder: Int)
    @Serializable data class InterestRow(val id: String, @SerialName("product_id") val productId: String, @SerialName("product_name") val productName: String, @SerialName("product_image_url") val productImageUrl: String, @SerialName("shop_id") val shopId: String, @SerialName("shop_owner_id") val shopOwnerId: String, @SerialName("buyer_id") val buyerId: String, @SerialName("buyer_first_name") val buyerFirstName: String, @SerialName("buyer_whatsapp_number") val buyerWhatsappNumber: String, val status: String, @SerialName("is_read") val isRead: Boolean, @SerialName("created_at") val createdAt: String = Instant.now().toString())
    @Serializable data class AdCampaignRow(val id: String, @SerialName("product_id") val productId: String, @SerialName("product_name") val productName: String, @SerialName("shop_id") val shopId: String, @SerialName("owner_uid") val ownerUid: String, @SerialName("total_expositions") val totalExpositions: Int, @SerialName("remaining_expositions") val remainingExpositions: Int, @SerialName("duration_days") val durationDays: Int, @SerialName("started_at") val startedAt: String, @SerialName("ends_at") val endsAt: String, @SerialName("price_fcfa") val priceFcfa: Int, @SerialName("is_active") val isActive: Boolean)
    @Serializable data class ConversationRow(
        val id: String,
        @SerialName("buyer_uid") val buyerUid: String,
        @SerialName("seller_uid") val sellerUid: String,
        @SerialName("product_id") val productId: String?,
        @SerialName("product_name") val productName: String,
        @SerialName("product_price") val productPrice: Double,
        @SerialName("shop_name") val shopName: String,
        val participants: List<String>,
        @SerialName("buyer_name") val buyerName: String = "",
        @SerialName("seller_name") val sellerName: String = "",
        @SerialName("buyer_whatsapp_number") val buyerWhatsappNumber: String = "",
        @SerialName("seller_whatsapp_number") val sellerWhatsappNumber: String = "",
        @SerialName("last_message") val lastMessage: String = "",
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    ) {
        fun toDomain() = ChatConversation(id, buyerUid, sellerUid, productId, productName, productPrice, shopName, participants, buyerName, sellerName, buyerWhatsappNumber, sellerWhatsappNumber, lastMessage, updatedAt?.toEpochMillis() ?: System.currentTimeMillis())
    }
    @Serializable data class ConversationUpdate(
        @SerialName("last_message") val lastMessage: String,
        @SerialName("updated_at") val updatedAt: String
    )

    @Serializable data class MessageRow(val id: String = "", @SerialName("conversation_id") val conversationId: String, @SerialName("sender_uid") val senderUid: String, @SerialName("sender_name") val senderName: String, val text: String, @SerialName("created_at") val createdAt: String = Instant.now().toString())


}
