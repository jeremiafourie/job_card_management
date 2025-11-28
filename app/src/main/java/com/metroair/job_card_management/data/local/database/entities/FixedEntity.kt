package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fixed_assets",
    indices = [Index(value = ["fixedCode"], unique = true), Index("fixedName"), Index("fixedType")]
)
data class FixedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fixedCode: String, // Unique fixed asset identifier
    val fixedName: String,
    val fixedType: String, // TOOL, AIR_CONDITIONER, LADDER, etc.
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val statusHistory: String = "[]",
    val isAvailable: Boolean = true,
    val currentHolder: String? = null,
    val lastMaintenanceDate: Long? = null,
    val nextMaintenanceDate: Long? = null,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
