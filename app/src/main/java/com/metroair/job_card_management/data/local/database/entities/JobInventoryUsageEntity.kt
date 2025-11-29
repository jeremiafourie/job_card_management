package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
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
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("job_id"),
        Index("inventory_id")
    ]
)
data class JobInventoryUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "job_id") val jobId: Int,
    @ColumnInfo(name = "inventory_id") val inventoryId: Int,
    @ColumnInfo(name = "item_code") val itemCode: String,
    @ColumnInfo(name = "item_name") val itemName: String,
    val quantity: Double,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long = System.currentTimeMillis()
)
