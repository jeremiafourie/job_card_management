package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fixed_assets",
    indices = [
        Index(value = ["fixed_code"], unique = true),
        Index("fixed_name"),
        Index("fixed_type")
    ]
)
data class FixedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "fixed_code") val fixedCode: String,
    @ColumnInfo(name = "fixed_name") val fixedName: String,
    @ColumnInfo(name = "fixed_type") val fixedType: String,
    @ColumnInfo(name = "serial_number") val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    @ColumnInfo(name = "status_history") val statusHistory: String = "[]",
    @ColumnInfo(name = "is_available") val isAvailable: Boolean = true,
    @ColumnInfo(name = "current_holder") val currentHolder: String? = null,
    @ColumnInfo(name = "last_maintenance_date") val lastMaintenanceDate: Long? = null,
    @ColumnInfo(name = "next_maintenance_date") val nextMaintenanceDate: Long? = null,
    val notes: String? = null,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
