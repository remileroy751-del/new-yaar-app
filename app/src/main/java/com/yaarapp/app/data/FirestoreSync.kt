package com.yaarapp.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.yaarapp.app.firebase.FirebaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

private const val TAG = "YaarFirestoreSync"

/** Synchronisation des comptes, boutiques et produits entre tous les téléphones. */
class FirestoreSync(context: Context, private val db: YaarDatabase) {
    private val appContext = context.applicationContext
    private val userDao = db.userDao()
    private val shopDao = db.shopDao()
    private val productDao = db.productDao()
    private val firestore get() = FirebaseModule.firestore
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val usersCollection get() = firestore.collection("users")
    private val shopsCollection get() = firestore.collection("shops")
    private val productsCollection get() = firestore.collection("products")

    private val _lastSyncEvent = MutableStateFlow<String?>(null)
    val lastSyncEvent: StateFlow<String?> = _lastSyncEvent

    fun clearLastSyncEvent() { _lastSyncEvent.value = null }
    private fun reportSuccess(message: String) { Log.i(TAG, message); _lastSyncEvent.value = "✅ $message" }
    private fun reportFailure(message: String) { Log.w(TAG, message); _lastSyncEvent.value = "❌ $message" }

    private var listenersStarted = false

    fun startRemoteSync() {
        if (listenersStarted) return
        listenersStarted = true
        syncScope.launch {
            try {
                FirebaseModule.ensureSignedIn()
                reportSuccess("Connexion Firebase OK — synchronisation temps réel démarrée.")
            } catch (e: Exception) {
                reportFailure("Connexion Firebase impossible au démarrage : ${e.message}")
                listenersStarted = false
                return@launch
            }

            shopsCollection.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncScope.launch { applyRemoteShopChanges(snapshot.documentChanges) }
            }
            productsCollection.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncScope.launch { applyRemoteProductChanges(snapshot.documentChanges) }
            }
        }
    }

    private suspend fun applyRemoteShopChanges(changes: List<DocumentChange>) {
        for (change in changes) {
            if (change.type == DocumentChange.Type.REMOVED) continue
            try {
                val remote = change.document.toObject(Shop::class.java).copy(remoteId = change.document.id)
                val existing = shopDao.findByRemoteId(change.document.id)
                val localOwnerId = remote.ownerUid?.let { userDao.findByFirebaseUid(it)?.id } ?: existing?.ownerId ?: 0
                val local = remote.copy(id = existing?.id ?: 0, ownerId = localOwnerId)
                val localId = if (existing != null) {
                    shopDao.update(local)
                    existing.id
                } else {
                    shopDao.insert(local).toInt()
                }
                productDao.rebindProductsToLocalShop(change.document.id, localId)
            } catch (_: Exception) { }
        }
    }

    private suspend fun applyRemoteProductChanges(changes: List<DocumentChange>) {
        for (change in changes) {
            try {
                val existing = productDao.findByRemoteId(change.document.id)
                if (change.type == DocumentChange.Type.REMOVED) {
                    existing?.let { productDao.delete(it) }
                    continue
                }
                val raw = change.document.toObject(Product::class.java)
                val remoteShopId = raw.shopRemoteId
                val localShopId = remoteShopId?.let { shopDao.findByRemoteId(it)?.id }
                    ?: existing?.shopId ?: 0
                val effectiveCities = raw.availableCities.ifEmpty { listOf(raw.city) }
                val remote = raw.copy(
                    id = existing?.id ?: 0,
                    remoteId = change.document.id,
                    shopId = localShopId,
                    availableCities = effectiveCities
                )
                if (existing != null) productDao.update(remote) else productDao.insert(remote)
            } catch (_: Exception) { }
        }
    }

    // ---------- Comptes ----------

    /** Écrit le profil dans users/{firebaseUid}. Aucun mot de passe n'est écrit. */
    suspend fun syncUserNow(user: User): User {
        val uid = user.firebaseUid ?: FirebaseModule.auth.currentUser?.uid
            ?: throw IllegalStateException("Compte Firebase introuvable.")
        val remote = user.copy(firebaseUid = uid)
        usersCollection.document(uid).set(
            mapOf(
                "firstName" to remote.firstName,
                "country" to remote.country.name,
                "city" to remote.city,
                "whatsappNumber" to remote.whatsappNumber,
                "notificationsEnabled" to remote.notificationsEnabled,
                "createdAt" to remote.createdAt,
                "firebaseUid" to uid
            )
        ).await()
        userDao.update(remote)
        return remote
    }

    /** Reconstitue un utilisateur local à partir de users/{uid} sur un téléphone neuf. */
    suspend fun createLocalUserFromCloud(uid: String, whatsappNumber: String): User? {
        val doc = usersCollection.document(uid).get().await()
        if (!doc.exists()) return null
        val country = doc.getString("country")?.let { runCatching { Country.valueOf(it) }.getOrNull() }
            ?: return null
        val user = User(
            firstName = doc.getString("firstName") ?: return null,
            country = country,
            city = doc.getString("city") ?: return null,
            whatsappNumber = doc.getString("whatsappNumber") ?: whatsappNumber,
            notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            firebaseUid = uid
        )
        val id = userDao.insert(user).toInt()
        return restoreAccount(user.copy(id = id))
    }

    /**
     * Récupère le compte et toutes ses boutiques/produits sur un nouveau téléphone.
     * Les identifiants Room sont recréés localement ; les remoteId/UID restent stables.
     */
    suspend fun restoreAccount(localUser: User): User {
        val uid = localUser.firebaseUid ?: FirebaseModule.auth.currentUser?.uid
            ?: return localUser

        val userDoc = usersCollection.document(uid).get().await()
        val restoredUser = if (userDoc.exists()) {
            val country = userDoc.getString("country")?.let { runCatching { Country.valueOf(it) }.getOrNull() } ?: localUser.country
            localUser.copy(
                firstName = userDoc.getString("firstName") ?: localUser.firstName,
                country = country,
                city = userDoc.getString("city") ?: localUser.city,
                whatsappNumber = userDoc.getString("whatsappNumber") ?: localUser.whatsappNumber,
                notificationsEnabled = userDoc.getBoolean("notificationsEnabled") ?: localUser.notificationsEnabled,
                createdAt = userDoc.getLong("createdAt") ?: localUser.createdAt,
                firebaseUid = uid
            )
        } else {
            localUser.copy(firebaseUid = uid)
        }
        userDao.update(restoredUser)

        val remoteShops = shopsCollection.whereEqualTo("ownerUid", uid).get().await().documents
        for (doc in remoteShops) {
            val remote = doc.toObject(Shop::class.java)?.copy(remoteId = doc.id) ?: continue
            val existing = shopDao.findByRemoteId(doc.id)
            val localShop = remote.copy(id = existing?.id ?: 0, ownerId = restoredUser.id, ownerUid = uid)
            val localId = if (existing == null) shopDao.insert(localShop).toInt() else { shopDao.update(localShop); existing.id }
            productDao.rebindProductsToLocalShop(doc.id, localId)
        }

        val remoteProducts = productsCollection.whereEqualTo("ownerUid", uid).get().await().documents
        for (doc in remoteProducts) {
            val remote = doc.toObject(Product::class.java)?.copy(remoteId = doc.id) ?: continue
            val existing = productDao.findByRemoteId(doc.id)
            val localShopId = remote.shopRemoteId?.let { shopDao.findByRemoteId(it)?.id } ?: 0
            val localProduct = remote.copy(
                id = existing?.id ?: 0,
                shopId = localShopId,
                ownerUid = uid,
                availableCities = remote.availableCities.ifEmpty { listOf(remote.city) }
            )
            if (existing == null) productDao.insert(localProduct) else productDao.update(localProduct)
        }
        reportSuccess("Compte restauré : boutique(s) et produit(s) récupérés depuis Firebase.")
        return restoredUser
    }

    /** Migration des anciens comptes anonymes vers le compte permanent avec le même UID. */
    suspend fun migrateLegacyAccount(user: User): User {
        val uid = user.firebaseUid ?: FirebaseModule.auth.currentUser?.uid
            ?: throw IllegalStateException("Aucun UID Firebase disponible pour la migration.")
        var updatedUser = user.copy(firebaseUid = uid)
        userDao.update(updatedUser)
        syncUserNow(updatedUser)

        val shops = shopDao.getAll().filter { it.ownerId == user.id }
        for (shop in shops) {
            val syncedShop = syncShopNow(shop.copy(ownerUid = uid))
            val localProducts = productDao.getAllForShop(shop.id)
            for (product in localProducts) {
                syncProductNow(product.copy(ownerUid = uid, shopRemoteId = syncedShop.remoteId))
            }
            updatedUser = updatedUser
        }
        reportSuccess("Ancien compte sécurisé et données existantes rattachées à Firebase.")
        return updatedUser
    }

    // ---------- Write-through ----------

    fun syncUser(user: User) {
        syncScope.launch {
            try { syncUserNow(user) } catch (e: Exception) { reportFailure("Échec de synchro du compte : ${e.message}") }
        }
    }

    suspend fun syncShopNow(shop: Shop): Shop {
        val uid = shop.ownerUid ?: FirebaseModule.auth.currentUser?.uid
            ?: throw IllegalStateException("Compte Firebase introuvable.")
        val logoUrl = shop.logoUrl?.let { uploadImageIfLocal("shops", it) }
        val docRef = shop.remoteId?.let { shopsCollection.document(it) } ?: shopsCollection.document()
        val toSync = shop.copy(ownerUid = uid, logoUrl = logoUrl ?: shop.logoUrl, remoteId = docRef.id)
        docRef.set(toSync).await()
        shopDao.update(toSync)
        return toSync
    }

    fun syncShop(shop: Shop) {
        syncScope.launch {
            try {
                syncShopNow(shop)
                reportSuccess("Boutique \"${shop.name}\" synchronisée.")
            } catch (e: Exception) { reportFailure("Échec de synchro pour la boutique \"${shop.name}\" : ${e.message}") }
        }
    }

    suspend fun syncProductNow(product: Product): Product {
        val uid = product.ownerUid ?: FirebaseModule.auth.currentUser?.uid
            ?: throw IllegalStateException("Compte Firebase introuvable.")
        val localShop = shopDao.getById(product.shopId)
        val shopRemoteId = product.shopRemoteId ?: localShop?.remoteId ?: localShop?.let {
            syncShopNow(it.copy(ownerUid = uid)).remoteId
        }
        val imageUrl = uploadImageIfLocal("products", product.imageUrl)
        val docRef = product.remoteId?.let { productsCollection.document(it) } ?: productsCollection.document()
        val toSync = product.copy(
            ownerUid = uid,
            shopRemoteId = shopRemoteId,
            availableCities = product.availableCities.ifEmpty { listOf(product.city) },
            imageUrl = imageUrl,
            remoteId = docRef.id
        )
        docRef.set(toSync).await()
        productDao.update(toSync)
        return toSync
    }

    fun syncProduct(product: Product) {
        syncScope.launch {
            try {
                syncProductNow(product)
                reportSuccess("Produit \"${product.name}\" synchronisé.")
            } catch (e: Exception) { reportFailure("Échec de synchro pour le produit \"${product.name}\" : ${e.message}") }
        }
    }

    fun deleteProductRemote(product: Product) {
        val remoteId = product.remoteId ?: return
        syncScope.launch {
            try { productsCollection.document(remoteId).delete().await() } catch (_: Exception) { }
        }
    }

    private suspend fun uploadImageIfLocal(folder: String, imageUrl: String): String {
        if (imageUrl.startsWith("res:") || imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) return imageUrl
        return try {
            val file = File(imageUrl)
            if (!file.exists()) return imageUrl
            val ref = FirebaseModule.storage.reference.child("$folder/${UUID.randomUUID()}.jpg")
            ref.putFile(Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { imageUrl }
    }
}
