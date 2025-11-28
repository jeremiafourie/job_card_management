package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the single technician profile for the device.
 */
@Entity(tableName = "technician")
data class TechnicianEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val name: String,
    val email: String,
    val phone: String,
    val authToken: String,
    val lastSyncTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
