package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_fixed_assets",
    foreignKeys = [
        ForeignKey(
            entity = JobCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FixedEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("job_id"),
        Index("fixed_id")
    ]
)
data class JobFixedAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "job_id") val jobId: Int? = null,
    @ColumnInfo(name = "fixed_id") val fixedId: Int,
    @ColumnInfo(name = "fixed_code") val fixedCode: String,
    @ColumnInfo(name = "fixed_name") val fixedName: String,
    val reason: String,
    @ColumnInfo(name = "technician_id") val technicianId: Int,
    @ColumnInfo(name = "technician_name") val technicianName: String,
    @ColumnInfo(name = "checkout_time") val checkoutTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "return_time") val returnTime: Long? = null,
    val condition: String = "Good",
    @ColumnInfo(name = "return_condition") val returnCondition: String? = null,
    val notes: String? = null
)
