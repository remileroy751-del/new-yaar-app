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

    @Insert
    suspend fun insert(shop: Shop): Long

    @Update
    suspend fun update(shop: Shop)

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun count(): Int
}
