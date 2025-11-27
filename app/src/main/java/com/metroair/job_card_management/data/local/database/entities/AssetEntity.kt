package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current")
data class AssetEntity(
    @PrimaryKey val id: Int,
    val itemCode: String,
    val itemName: String,
    val category: String,
    val currentStock: Double,
    val minimumStock: Double,
    val unitOfMeasure: String
)