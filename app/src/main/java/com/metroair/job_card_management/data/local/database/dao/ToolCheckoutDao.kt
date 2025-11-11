package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.ToolCheckoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolCheckoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun checkoutTool(checkout: ToolCheckoutEntity)

    @Query("UPDATE tool_checkouts SET isReturned = 1, returnedAt = :returnedAt WHERE id = :checkoutId")
    suspend fun returnTool(checkoutId: Int, returnedAt: Long)

    @Query("SELECT * FROM tool_checkouts WHERE technicianId = :technicianId AND isReturned = 0 ORDER BY checkedOutAt DESC")
    fun getActiveCheckoutsForTechnician(technicianId: Int): Flow<List<ToolCheckoutEntity>>

    @Query("SELECT * FROM tool_checkouts WHERE technicianId = :technicianId ORDER BY checkedOutAt DESC")
    fun getAllCheckoutsForTechnician(technicianId: Int): Flow<List<ToolCheckoutEntity>>

    @Query("SELECT * FROM tool_checkouts WHERE resourceId = :resourceId AND isReturned = 0")
    suspend fun getActiveCheckoutsForAsset(resourceId: Int): List<ToolCheckoutEntity>
}
