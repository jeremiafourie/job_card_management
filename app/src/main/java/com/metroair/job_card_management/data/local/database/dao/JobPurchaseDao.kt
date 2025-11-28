package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.JobPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobPurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: JobPurchaseEntity): Long

    @Query("SELECT * FROM job_purchases WHERE jobId = :jobId ORDER BY purchasedAt DESC")
    fun getPurchasesForJob(jobId: Int): Flow<List<JobPurchaseEntity>>
}
