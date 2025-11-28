package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.JobInventoryUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobInventoryUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: JobInventoryUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageList(usages: List<JobInventoryUsageEntity>)

    @Query("SELECT * FROM job_inventory_usage WHERE jobId = :jobId ORDER BY recordedAt DESC")
    fun getUsageForJob(jobId: Int): Flow<List<JobInventoryUsageEntity>>

    @Query("DELETE FROM job_inventory_usage WHERE jobId = :jobId")
    suspend fun clearForJob(jobId: Int)
}
