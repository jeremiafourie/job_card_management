package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.metroair.job_card_management.data.local.database.entities.CurrentTechnicianEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentTechnicianDao {

    @Query("SELECT * FROM current_technician WHERE id = 1")
    fun getCurrentTechnician(): Flow<CurrentTechnicianEntity?>

    @Query("SELECT * FROM current_technician WHERE id = 1")
    suspend fun getCurrentTechnicianSync(): CurrentTechnicianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCurrentTechnician(technician: CurrentTechnicianEntity)

    @Update
    suspend fun updateCurrentTechnician(technician: CurrentTechnicianEntity)

    @Query("UPDATE current_technician SET currentActiveJobId = :jobId WHERE id = 1")
    suspend fun setCurrentActiveJob(jobId: Int?)

    @Query("UPDATE current_technician SET isOnDuty = :isOnDuty WHERE id = 1")
    suspend fun setOnDutyStatus(isOnDuty: Boolean)

    @Query("UPDATE current_technician SET totalJobsCompletedToday = totalJobsCompletedToday + 1 WHERE id = 1")
    suspend fun incrementTodayCompletedJobs()

    @Query("UPDATE current_technician SET lastSyncTime = :timestamp WHERE id = 1")
    suspend fun updateLastSyncTime(timestamp: Long)

    @Query("DELETE FROM current_technician")
    suspend fun logout()

    @Query("SELECT authToken FROM current_technician WHERE id = 1")
    suspend fun getAuthToken(): String?
}