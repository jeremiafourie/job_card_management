package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.metroair.job_card_management.data.local.database.entities.TechnicianEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentTechnicianDao {

    @Query("SELECT * FROM technician WHERE id = 1")
    fun getCurrentTechnician(): Flow<TechnicianEntity?>

    @Query("SELECT * FROM technician WHERE id = 1")
    suspend fun getCurrentTechnicianSync(): TechnicianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCurrentTechnician(technician: TechnicianEntity)

    @Update
    suspend fun updateCurrentTechnician(technician: TechnicianEntity)

    @Query("UPDATE technician SET last_sync_time = :timestamp WHERE id = 1")
    suspend fun updateLastSyncTime(timestamp: Long)

    @Query("DELETE FROM technician")
    suspend fun logout()

    @Query("SELECT auth_token FROM technician WHERE id = 1")
    suspend fun getAuthToken(): String?
}
