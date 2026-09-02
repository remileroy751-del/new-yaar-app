package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdCampaignDao {
    /** Toutes les campagnes actives, toutes boutiques confondues — utilisé par le moteur d'exposition. */
    @Query("SELECT * FROM ad_campaigns WHERE isActive = 1")
    suspend fun getAllActive(): List<AdCampaign>

    /** Campagnes actives d'une boutique — utilisé par l'écran "Ma Publicité". */
    @Query("SELECT * FROM ad_campaigns WHERE shopId = :shopId AND isActive = 1 ORDER BY endsAt ASC")
    fun observeActiveForShop(shopId: Int): Flow<List<AdCampaign>>

    @Insert
    suspend fun insert(campaign: AdCampaign): Long

    @Update
    suspend fun update(campaign: AdCampaign)

    @Query("DELETE FROM ad_campaigns WHERE shopId IN (SELECT id FROM shops WHERE ownerId = :ownerId)")
    suspend fun deleteAllForOwner(ownerId: Int)
}
