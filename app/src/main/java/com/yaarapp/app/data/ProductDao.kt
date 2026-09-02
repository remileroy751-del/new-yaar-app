package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /**
     * Flux marketplace : uniquement les produits ACTIFS de toutes les boutiques.
     * Les produits actuellement "sponsorisés" (campagne publicitaire active, voir
     * [AdCampaign]) sortent en premier, puis les plus récents en premier.
     */
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY isPromoted DESC, createdAt DESC")
    fun observeAllActive(): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products WHERE isActive = 1 ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    /** Tous les produits d'une boutique (actifs ET désactivés), pour l'écran "Ma boutique". */
    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY isActive DESC, createdAt DESC")
    fun observeByShop(shopId: Int): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Int): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Int): Product?

    @Query("SELECT * FROM products WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Product?

    @Query("UPDATE products SET shopId = :shopId WHERE shopRemoteId = :shopRemoteId")
    suspend fun rebindProductsToLocalShop(shopRemoteId: String, shopId: Int)

    @Query("SELECT * FROM products WHERE ownerUid = :ownerUid")
    suspend fun getAllForOwnerUid(ownerUid: String): List<Product>

    /** Nombre de produits actuellement EXPOSÉS (actifs) — c'est ce qui compte pour la limite du forfait. */
    @Query("SELECT COUNT(*) FROM products WHERE shopId = :shopId AND isActive = 1")
    suspend fun countActiveForShop(shopId: Int): Int

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("DELETE FROM products WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForOwnerUid(ownerUid: String)

    @Query("DELETE FROM products WHERE shopId IN (SELECT id FROM shops WHERE ownerId = :ownerId)")
    suspend fun deleteAllForOwnerId(ownerId: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    /** Produits encore actifs mais dont les 14 jours d'exposition gratuite sont dépassés. */
    @Query("SELECT * FROM products WHERE shopId = :shopId AND isActive = 1 AND activatedAt <= :cutoff")
    suspend fun getExpiredActiveForShop(shopId: Int, cutoff: Long): List<Product>

    /** Désactive en masse les produits expirés d'une boutique. Retourne le nombre de lignes touchées. */
    @Query("UPDATE products SET isActive = 0 WHERE shopId = :shopId AND isActive = 1 AND activatedAt <= :cutoff")
    suspend fun deactivateExpired(shopId: Int, cutoff: Long): Int
}
