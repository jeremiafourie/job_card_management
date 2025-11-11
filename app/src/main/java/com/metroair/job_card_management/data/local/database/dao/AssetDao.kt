package com.metroair.job_card_management.data.local.database.dao

import androidx.room.*
import com.metroair.job_card_management.data.local.database.entities.AssetEntity
import com.metroair.job_card_management.data.local.database.entities.AssetCheckoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    // ========== ASSETS ==========

    @Query("SELECT * FROM assets ORDER BY assetType, assetName")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE assetType = :type ORDER BY assetName")
    fun getAssetsByType(type: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isAvailable = 1 ORDER BY assetType, assetName")
    fun getAvailableAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :assetId")
    suspend fun getAssetById(assetId: Int): AssetEntity?

    @Query("SELECT * FROM assets WHERE assetCode = :assetCode")
    suspend fun getAssetByCode(assetCode: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("UPDATE assets SET isAvailable = :isAvailable, currentHolder = :holder, updatedAt = :timestamp WHERE id = :assetId")
    suspend fun updateAssetAvailability(assetId: Int, isAvailable: Boolean, holder: String?, timestamp: Long)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    // ========== ASSET CHECKOUTS ==========

    @Insert
    suspend fun insertCheckout(checkout: AssetCheckoutEntity): Long

    @Query("SELECT * FROM asset_checkouts WHERE assetId = :assetId ORDER BY checkoutTime DESC")
    fun getAssetHistory(assetId: Int): Flow<List<AssetCheckoutEntity>>

    @Query("SELECT * FROM asset_checkouts WHERE technicianId = :technicianId AND returnTime IS NULL")
    fun getActiveCheckoutsForTechnician(technicianId: Int): Flow<List<AssetCheckoutEntity>>

    @Query("SELECT * FROM asset_checkouts WHERE returnTime IS NULL ORDER BY checkoutTime DESC")
    fun getAllActiveCheckouts(): Flow<List<AssetCheckoutEntity>>

    @Query("SELECT * FROM asset_checkouts WHERE jobId = :jobId ORDER BY checkoutTime DESC")
    fun getCheckoutsForJob(jobId: Int): Flow<List<AssetCheckoutEntity>>

    @Query("UPDATE asset_checkouts SET returnTime = :returnTime, returnCondition = :condition, returnNotes = :notes WHERE id = :checkoutId")
    suspend fun returnAsset(checkoutId: Int, returnTime: Long, condition: String, notes: String?)

    @Query("SELECT * FROM asset_checkouts WHERE id = :checkoutId")
    suspend fun getCheckoutById(checkoutId: Int): AssetCheckoutEntity?

    // Search assets
    @Query("""
        SELECT * FROM assets
        WHERE assetName LIKE :query
        OR assetCode LIKE :query
        OR serialNumber LIKE :query
        OR manufacturer LIKE :query
        OR model LIKE :query
        ORDER BY assetName
        LIMIT :limit
    """)
    suspend fun searchAssets(query: String, limit: Int = 5): List<AssetEntity>
}