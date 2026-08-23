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

    @Insert
    suspend fun insert(interest: Interest): Long

    @Update
    suspend fun update(interest: Interest)
}
