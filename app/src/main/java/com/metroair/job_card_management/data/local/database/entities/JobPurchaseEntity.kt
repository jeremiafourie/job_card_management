package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_purchases",
    foreignKeys = [
        ForeignKey(
            entity = JobCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("job_id")
    ]
)
data class JobPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "job_id") val jobId: Int,
    val vendor: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    val notes: String? = null,
    @ColumnInfo(name = "purchased_at") val purchasedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "receipt_uri") val receiptUri: String? = null,
    @ColumnInfo(name = "receipt_mime_type") val receiptMimeType: String? = null,
    @ColumnInfo(name = "receipt_captured_at") val receiptCapturedAt: Long? = null
)
