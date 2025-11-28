package com.metroair.job_card_management.data.local.database.dao

import androidx.room.*
import com.metroair.job_card_management.data.local.database.entities.FixedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedDao {

    // ========== FIXED ASSETS ==========

    @Query("SELECT * FROM fixed_assets ORDER BY fixedType, fixedName")
    fun getAllFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE fixedType = :type ORDER BY fixedName")
    fun getFixedByType(type: String): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE isAvailable = 1 ORDER BY fixedType, fixedName")
    fun getAvailableFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE id = :fixedId")
    suspend fun getFixedById(fixedId: Int): FixedEntity?

    @Query("SELECT * FROM fixed_assets WHERE fixedCode = :fixedCode")
    suspend fun getFixedByCode(fixedCode: String): FixedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixed(fixed: FixedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedFixeds(fixedFixeds: List<FixedEntity>)

    @Update
    suspend fun updateFixed(fixed: FixedEntity)

    @Query("UPDATE fixed_assets SET isAvailable = :isAvailable, currentHolder = :holder, statusHistory = :statusHistory, updatedAt = :timestamp WHERE id = :fixedId")
    suspend fun updateFixedAvailability(fixedId: Int, isAvailable: Boolean, holder: String?, statusHistory: String, timestamp: Long)

    @Delete
    suspend fun deleteFixed(fixed: FixedEntity)

    // Search fixed assets
    @Query("""
        SELECT * FROM fixed_assets
        WHERE fixedName LIKE :query
        OR fixedCode LIKE :query
        OR serialNumber LIKE :query
        OR manufacturer LIKE :query
        OR model LIKE :query
        ORDER BY fixedName
        LIMIT :limit
    """)
    suspend fun searchFixed(query: String, limit: Int = 5): List<FixedEntity>
}
