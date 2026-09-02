package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    fun observeShopForOwner(ownerId: Int): Flow<Shop?>

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getShopForOwner(ownerId: Int): Shop?

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Shop?

    @Query("SELECT * FROM shops WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Shop?

    @Query("SELECT * FROM shops WHERE ownerUid = :ownerUid LIMIT 1")
    suspend fun getShopForOwnerUid(ownerUid: String): Shop?

    @Query("SELECT * FROM shops WHERE ownerUid = :ownerUid")
    suspend fun getAllForOwnerUid(ownerUid: String): List<Shop>

    @Query("SELECT * FROM shops")
    suspend fun getAll(): List<Shop>

    @Insert
    suspend fun insert(shop: Shop): Long

    @Update
    suspend fun update(shop: Shop)

    @Query("DELETE FROM shops WHERE ownerId = :ownerId")
    suspend fun deleteAllForOwner(ownerId: Int)

    @Query("SELECT id FROM shops WHERE ownerId = :ownerId")
    suspend fun idsForOwner(ownerId: Int): List<Int>

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun count(): Int
}
