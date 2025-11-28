package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.metroair.job_card_management.data.local.database.entities.JobFixedAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobFixedAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckout(checkout: JobFixedAssetEntity): Long

    @Update
    suspend fun updateCheckout(checkout: JobFixedAssetEntity)

    @Query("SELECT * FROM job_fixed_assets WHERE jobId = :jobId ORDER BY checkoutTime DESC")
    fun getCheckoutsForJob(jobId: Int): Flow<List<JobFixedAssetEntity>>

    @Query("SELECT * FROM job_fixed_assets WHERE returnTime IS NULL")
    fun getActiveCheckouts(): Flow<List<JobFixedAssetEntity>>

    @Query("SELECT * FROM job_fixed_assets WHERE fixedId = :fixedId ORDER BY checkoutTime DESC")
    fun getHistoryForFixed(fixedId: Int): Flow<List<JobFixedAssetEntity>>

    @Query("SELECT * FROM job_fixed_assets WHERE id = :checkoutId")
    suspend fun getById(checkoutId: Int): JobFixedAssetEntity?
}
