package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_assets",
    indices = [
        Index(value = ["item_code"], unique = true),
        Index("item_name"),
        Index("category")
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "item_code") val itemCode: String,
    @ColumnInfo(name = "item_name") val itemName: String,
    val category: String,
    @ColumnInfo(name = "current_stock") val currentStock: Double,
    @ColumnInfo(name = "minimum_stock") val minimumStock: Double,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
