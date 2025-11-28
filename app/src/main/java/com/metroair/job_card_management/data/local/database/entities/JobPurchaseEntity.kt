package com.metroair.job_card_management.data.local.database.entities

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
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId")
    ]
)
data class JobPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val vendor: String,
    val totalAmount: Double,
    val notes: String? = null,
    val purchasedAt: Long = System.currentTimeMillis()
)
