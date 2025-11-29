package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.PurchaseReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: PurchaseReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<PurchaseReceiptEntity>)

    @Query("SELECT * FROM purchase_receipts WHERE purchase_id = :purchaseId ORDER BY captured_at DESC")
    fun getReceiptsForPurchase(purchaseId: Int): Flow<List<PurchaseReceiptEntity>>

    @Query("SELECT * FROM purchase_receipts WHERE purchase_id = :purchaseId ORDER BY captured_at DESC")
    suspend fun getReceiptsForPurchaseSync(purchaseId: Int): List<PurchaseReceiptEntity>

    @Query("DELETE FROM purchase_receipts WHERE purchase_id = :purchaseId")
    suspend fun clearForPurchase(purchaseId: Int)

    @Query("UPDATE purchase_receipts SET uri = COALESCE(:uri, uri), mime_type = COALESCE(:mimeType, mime_type) WHERE id = :receiptId")
    suspend fun updateReceipt(receiptId: Int, uri: String?, mimeType: String?)
}
