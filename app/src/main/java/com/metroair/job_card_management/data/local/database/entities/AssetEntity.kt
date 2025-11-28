package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_assets",
    indices = [
        Index(value = ["itemCode"], unique = true),
        Index("itemName"),
        Index("category")
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemCode: String,
    val itemName: String,
    val category: String,
    val currentStock: Double,
    val minimumStock: Double,
    val unitOfMeasure: String,
    val isSynced: Boolean = false,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
