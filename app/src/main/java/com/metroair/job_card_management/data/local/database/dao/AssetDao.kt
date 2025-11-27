package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM current ORDER BY itemName")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM current WHERE category = :category")
    fun getAssetsByCategory(category: String): Flow<List<AssetEntity>>

    @Query("UPDATE current SET currentStock = currentStock - :quantity WHERE id = :id")
    suspend fun useAsset(id: Int, quantity: Double)

    @Query("UPDATE current SET currentStock = currentStock + :quantity WHERE id = :id")
    suspend fun restoreAsset(id: Int, quantity: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssets(assets: List<AssetEntity>)
}
