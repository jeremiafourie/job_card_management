package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_receipts",
    foreignKeys = [
        ForeignKey(
            entity = JobPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchase_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("purchase_id")
    ]
)
data class PurchaseReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "purchase_id") val purchaseId: Int,
    val uri: String,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "captured_at") val capturedAt: Long = System.currentTimeMillis()
)
