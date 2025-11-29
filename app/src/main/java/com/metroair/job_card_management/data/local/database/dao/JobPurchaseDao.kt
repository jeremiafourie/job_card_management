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

    @Query("SELECT * FROM job_purchases WHERE job_id = :jobId ORDER BY purchased_at DESC")
    fun getPurchasesForJob(jobId: Int): Flow<List<JobPurchaseEntity>>

    @Query(
        """
        UPDATE job_purchases 
        SET receipt_uri = :uri, 
            receipt_mime_type = :mimeType, 
            receipt_captured_at = :capturedAt 
        WHERE id = :purchaseId
        """
    )
    suspend fun updateReceipt(purchaseId: Int, uri: String?, mimeType: String?, capturedAt: Long?)

    @Query(
        """
        UPDATE job_purchases 
        SET receipt_uri = NULL, 
            receipt_mime_type = NULL, 
            receipt_captured_at = NULL 
        WHERE id = :purchaseId
        """
    )
    suspend fun clearReceipt(purchaseId: Int)
}
