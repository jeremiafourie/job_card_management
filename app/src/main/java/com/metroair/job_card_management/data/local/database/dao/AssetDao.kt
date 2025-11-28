package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.metroair.job_card_management.data.local.database.entities.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM inventory_assets ORDER BY itemName")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM inventory_assets WHERE category = :category ORDER BY itemName")
    fun getAssetsByCategory(category: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM inventory_assets WHERE currentStock <= minimumStock")
    fun getLowStock(): Flow<List<AssetEntity>>

    @Query("UPDATE inventory_assets SET currentStock = currentStock - :quantity, updatedAt = :timestamp WHERE id = :id")
    suspend fun useAsset(id: Int, quantity: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_assets SET currentStock = currentStock + :quantity, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreAsset(id: Int, quantity: Double, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssets(assets: List<AssetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("DELETE FROM inventory_assets")
    suspend fun clearAll()
}
