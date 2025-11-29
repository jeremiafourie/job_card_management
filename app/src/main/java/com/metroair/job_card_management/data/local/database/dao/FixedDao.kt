package com.metroair.job_card_management.data.local.database.dao

import androidx.room.*
import com.metroair.job_card_management.data.local.database.entities.FixedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedDao {

    @Query("SELECT * FROM fixed_assets ORDER BY fixed_type, fixed_name")
    fun getAllFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE fixed_type = :type ORDER BY fixed_name")
    fun getFixedByType(type: String): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE is_available = 1 ORDER BY fixed_type, fixed_name")
    fun getAvailableFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed_assets WHERE id = :fixedId")
    suspend fun getFixedById(fixedId: Int): FixedEntity?

    @Query("SELECT * FROM fixed_assets WHERE fixed_code = :fixedCode")
    suspend fun getFixedByCode(fixedCode: String): FixedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixed(fixed: FixedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedFixeds(fixedFixeds: List<FixedEntity>)

    @Update
    suspend fun updateFixed(fixed: FixedEntity)

    @Query("UPDATE fixed_assets SET is_available = :isAvailable, current_holder = :holder, status_history = :statusHistory, updated_at = :timestamp WHERE id = :fixedId")
    suspend fun updateFixedAvailability(fixedId: Int, isAvailable: Boolean, holder: String?, statusHistory: String, timestamp: Long)

    @Delete
    suspend fun deleteFixed(fixed: FixedEntity)

    @Query("""
        SELECT * FROM fixed_assets
        WHERE fixed_name LIKE :query
        OR fixed_code LIKE :query
        OR serial_number LIKE :query
        OR manufacturer LIKE :query
        OR model LIKE :query
        ORDER BY fixed_name
        LIMIT :limit
    """)
    suspend fun searchFixed(query: String, limit: Int = 5): List<FixedEntity>
}
