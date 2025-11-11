package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    indices = [Index(value = ["assetCode"], unique = true)]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assetCode: String, // Unique asset identifier
    val assetName: String,
    val assetType: String, // TOOL, AIR_CONDITIONER, LADDER, etc.
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val isAvailable: Boolean = true,
    val currentHolder: String? = null,
    val lastMaintenanceDate: Long? = null,
    val nextMaintenanceDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)