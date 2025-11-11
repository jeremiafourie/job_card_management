package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_checkouts",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("assetId"),
        Index("technicianId"),
        Index("jobId")
    ]
)
data class AssetCheckoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assetId: Int,
    val assetCode: String,
    val assetName: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long = System.currentTimeMillis(),
    val returnTime: Long? = null,
    val reason: String,
    val jobId: Int? = null,
    val jobNumber: String? = null,
    val checkoutCondition: String = "Good",
    val returnCondition: String? = null,
    val checkoutNotes: String? = null,
    val returnNotes: String? = null
)