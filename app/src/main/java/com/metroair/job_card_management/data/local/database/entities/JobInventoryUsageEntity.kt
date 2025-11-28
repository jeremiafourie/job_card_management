package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_inventory_usage",
    foreignKeys = [
        ForeignKey(
            entity = JobCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId"),
        Index("inventoryId")
    ]
)
data class JobInventoryUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val inventoryId: Int,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val recordedAt: Long = System.currentTimeMillis()
)
