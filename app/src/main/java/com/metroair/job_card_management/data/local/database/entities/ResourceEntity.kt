package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey val id: Int,
    val itemCode: String,
    val itemName: String,
    val category: String,
    val currentStock: Int,
    val minimumStock: Int,
    val unitOfMeasure: String
)