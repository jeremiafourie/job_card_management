package com.metroair.job_card_management.data.local.database.entities

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
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("purchaseId")
    ]
)
data class PurchaseReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val purchaseId: Int,
    val uri: String,
    val mimeType: String? = null,
    val notes: String? = null,
    val capturedAt: Long = System.currentTimeMillis()
)
