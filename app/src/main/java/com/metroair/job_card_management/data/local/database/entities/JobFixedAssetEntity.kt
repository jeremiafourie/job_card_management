package com.metroair.job_card_management.data.local.database.entities

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
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FixedEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId"),
        Index("fixedId")
    ]
)
data class JobFixedAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int?,
    val fixedId: Int,
    val fixedCode: String,
    val fixedName: String,
    val reason: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long = System.currentTimeMillis(),
    val returnTime: Long? = null,
    val condition: String = "Good",
    val returnCondition: String? = null,
    val notes: String? = null
)
