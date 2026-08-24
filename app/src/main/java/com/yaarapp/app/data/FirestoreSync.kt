package com.yaarapp.app.data

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.DocumentChange
import com.yaarapp.app.firebase.FirebaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Synchronisation en ligne des boutiques et produits via Firestore + Storage.
 *
 * Principe : Room reste la source de vérité LOCALE (l'app fonctionne à 100% hors-ligne,
 * comme avant). Cette classe ajoute, en tâche de fond :
 * - un "write-through" : chaque création/modification locale est aussi poussée vers
 *   Firestore (et les photos locales vers Storage) dès qu'une connexion est disponible ;
 * - une écoute temps réel Firestore qui reflète dans Room les boutiques/produits publiés
 *   par LES AUTRES téléphones, pour qu'ils apparaissent dans "Acheter" chez tout le monde.
 *
 * Les échecs réseau sont silencieusement ignorés : une modification locale n'est jamais
 * bloquée par l'absence de connexion, elle sera synchronisée à la prochaine occasion (au
 * prochain appel de synchro déclenché par une autre action, ou au prochain lancement de
 * l'app). Ce n'est pas une file d'attente persistante de tentatives — une amélioration
 * possible plus tard si des pertes de synchro sont constatées en usage réel.
 *
 * Chaque boutique/produit garde son identifiant local Room (Int, généré indépendamment
 * sur chaque téléphone) ET reçoit un [Shop.remoteId]/[Product.remoteId] (String, généré
 * par Firestore, globalement unique) une fois synchronisé. C'est ce second identifiant
 * qui permet de faire correspondre les mises à jour entre téléphones sans jamais faire
 * entrer en collision les identifiants locaux de deux appareils différents.
 */
class FirestoreSync(context: Context, private val db: YaarDatabase) {

    private val appContext = context.applicationContext
    private val shopDao = db.shopDao()
    private val productDao = db.productDao()
    private val firestore get() = FirebaseModule.firestore

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val shopsCollection get() = firestore.collection("shops")
    private val productsCollection get() = firestore.collection("products")

    // ---------- Écoute temps réel : reflète Firestore vers Room ----------

    private var listenersStarted = false

    /** À appeler une fois au démarrage de l'app (voir YaarApplication). */
    fun startRemoteSync() {
        if (listenersStarted) return
        listenersStarted = true
        syncScope.launch {
            try {
                FirebaseModule.ensureSignedIn()
            } catch (e: Exception) {
                listenersStarted = false
                return@launch // pas de réseau au lancement : on retentera au prochain appel de synchro
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
            if (change.type == DocumentChange.Type.REMOVED) continue // pas de suppression de boutique pour l'instant
            try {
                val remote = change.document.toObject(Shop::class.java).copy(remoteId = change.document.id)
                val existing = shopDao.findByRemoteId(change.document.id)
                if (existing != null) {
                    shopDao.update(remote.copy(id = existing.id))
                } else {
                    shopDao.insert(remote.copy(id = 0))
                }
            } catch (e: Exception) {
                // Document mal formé ou incompatible : on l'ignore plutôt que de crasher la synchro.
            }
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
                val remote = change.document.toObject(Product::class.java).copy(remoteId = change.document.id)
                if (existing != null) {
                    productDao.update(remote.copy(id = existing.id))
                } else {
                    productDao.insert(remote.copy(id = 0))
                }
            } catch (e: Exception) {
                // Document mal formé ou incompatible : on l'ignore plutôt que de crasher la synchro.
            }
        }
    }

    // ---------- Write-through : reflète Room vers Firestore (+ Storage pour les photos) ----------

    /** Pousse une boutique créée/modifiée localement vers Firestore. Ne bloque pas l'appelant. */
    fun syncShop(shop: Shop) {
        syncScope.launch {
            try {
                FirebaseModule.ensureSignedIn()
                val logoUrl = shop.logoUrl?.let { uploadImageIfLocal("shops", it) }
                val docRef = shop.remoteId?.let { shopsCollection.document(it) } ?: shopsCollection.document()
                val toSync = shop.copy(logoUrl = logoUrl ?: shop.logoUrl, remoteId = docRef.id)
                docRef.set(toSync).await()
                if (toSync.remoteId != shop.remoteId || toSync.logoUrl != shop.logoUrl) {
                    shopDao.update(toSync)
                }
            } catch (e: Exception) {
                // Pas de réseau, ou erreur Firestore : la boutique reste utilisable en local ;
                // la prochaine modification (ou le prochain lancement) retentera la synchro.
            }
        }
    }

    /** Pousse un produit créé/modifié localement vers Firestore. Ne bloque pas l'appelant. */
    fun syncProduct(product: Product) {
        syncScope.launch {
            try {
                FirebaseModule.ensureSignedIn()
                val imageUrl = uploadImageIfLocal("products", product.imageUrl)
                val docRef = product.remoteId?.let { productsCollection.document(it) } ?: productsCollection.document()
                val toSync = product.copy(imageUrl = imageUrl, remoteId = docRef.id)
                docRef.set(toSync).await()
                if (toSync.remoteId != product.remoteId || toSync.imageUrl != product.imageUrl) {
                    productDao.update(toSync)
                }
            } catch (e: Exception) {
                // Idem : échec silencieux, la synchro réessaiera plus tard.
            }
        }
    }

    /** Supprime définitivement un produit côté Firestore (suite à une suppression locale). */
    fun deleteProductRemote(product: Product) {
        val remoteId = product.remoteId ?: return
        syncScope.launch {
            try {
                productsCollection.document(remoteId).delete().await()
            } catch (e: Exception) {
                // Le document restera en ligne jusqu'à la prochaine tentative ; sans conséquence
                // grave puisque le produit est de toute façon retiré localement.
            }
        }
    }

    /**
     * Envoie une photo locale (chemin de fichier interne à l'app) vers Firebase Storage et
     * retourne son URL de téléchargement. Les images intégrées ("res:...") et les URL déjà
     * distantes (http/https) sont renvoyées telles quelles, sans upload.
     */
    private suspend fun uploadImageIfLocal(folder: String, imageUrl: String): String {
        if (imageUrl.startsWith("res:") || imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }
        return try {
            val file = File(imageUrl)
            if (!file.exists()) return imageUrl
            val ref = FirebaseModule.storage.reference.child("$folder/${UUID.randomUUID()}.jpg")
            ref.putFile(Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            imageUrl
        }
    }
}
