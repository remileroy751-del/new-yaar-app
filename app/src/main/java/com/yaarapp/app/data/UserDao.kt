package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE whatsappNumber = :whatsappNumber LIMIT 1")
    suspend fun findByWhatsapp(whatsappNumber: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): User?

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun findByFirebaseUid(firebaseUid: String): User?

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
