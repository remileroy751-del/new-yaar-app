package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests WHERE shopOwnerId = :ownerId ORDER BY createdAt DESC")
    fun observeForOwner(ownerId: Int): Flow<List<Interest>>

    @Query("SELECT COUNT(*) FROM interests WHERE shopOwnerId = :ownerId AND isRead = 0")
    fun observeUnreadCount(ownerId: Int): Flow<Int>

    @Query("SELECT * FROM interests WHERE productId = :productId AND buyerId = :buyerId AND ABS(createdAt - :createdAt) < 10000 LIMIT 1")
    suspend fun findLatest(productId: Int, buyerId: Int, createdAt: Long): Interest?

    @Insert
    suspend fun insert(interest: Interest): Long

    @Update
    suspend fun update(interest: Interest)

    @Query("DELETE FROM interests WHERE shopOwnerId = :ownerId OR buyerId = :ownerId")
    suspend fun deleteAllForUser(ownerId: Int)
}
