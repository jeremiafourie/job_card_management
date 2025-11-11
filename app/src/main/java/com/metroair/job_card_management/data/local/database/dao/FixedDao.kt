package com.metroair.job_card_management.data.local.database.dao

import androidx.room.*
import com.metroair.job_card_management.data.local.database.entities.FixedEntity
import com.metroair.job_card_management.data.local.database.entities.FixedCheckoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedDao {

    // ========== FIXED ASSETS ==========

    @Query("SELECT * FROM fixed ORDER BY fixedType, fixedName")
    fun getAllFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed WHERE fixedType = :type ORDER BY fixedName")
    fun getFixedByType(type: String): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed WHERE isAvailable = 1 ORDER BY fixedType, fixedName")
    fun getAvailableFixed(): Flow<List<FixedEntity>>

    @Query("SELECT * FROM fixed WHERE id = :fixedId")
    suspend fun getFixedById(fixedId: Int): FixedEntity?

    @Query("SELECT * FROM fixed WHERE fixedCode = :fixedCode")
    suspend fun getFixedByCode(fixedCode: String): FixedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixed(fixed: FixedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedFixeds(fixedFixeds: List<FixedEntity>)

    @Update
    suspend fun updateFixed(fixed: FixedEntity)

    @Query("UPDATE fixed SET isAvailable = :isAvailable, currentHolder = :holder, updatedAt = :timestamp WHERE id = :fixedId")
    suspend fun updateFixedAvailability(fixedId: Int, isAvailable: Boolean, holder: String?, timestamp: Long)

    @Delete
    suspend fun deleteFixed(fixed: FixedEntity)

    // ========== FIXED CHECKOUTS ==========

    @Insert
    suspend fun insertCheckout(checkout: FixedCheckoutEntity): Long

    @Query("SELECT * FROM fixed_checkouts WHERE fixedId = :fixedId ORDER BY checkoutTime DESC")
    fun getFixedHistory(fixedId: Int): Flow<List<FixedCheckoutEntity>>

    @Query("SELECT * FROM fixed_checkouts WHERE technicianId = :technicianId AND returnTime IS NULL")
    fun getActiveCheckoutsForTechnician(technicianId: Int): Flow<List<FixedCheckoutEntity>>

    @Query("SELECT * FROM fixed_checkouts WHERE returnTime IS NULL ORDER BY checkoutTime DESC")
    fun getAllActiveCheckouts(): Flow<List<FixedCheckoutEntity>>

    @Query("SELECT * FROM fixed_checkouts WHERE jobId = :jobId ORDER BY checkoutTime DESC")
    fun getCheckoutsForJob(jobId: Int): Flow<List<FixedCheckoutEntity>>

    @Query("UPDATE fixed_checkouts SET returnTime = :returnTime, returnCondition = :condition, returnNotes = :notes WHERE id = :checkoutId")
    suspend fun returnFixed(checkoutId: Int, returnTime: Long, condition: String, notes: String?)

    @Query("SELECT * FROM fixed_checkouts WHERE id = :checkoutId")
    suspend fun getCheckoutById(checkoutId: Int): FixedCheckoutEntity?

    // Search fixed assets
    @Query("""
        SELECT * FROM fixed
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
