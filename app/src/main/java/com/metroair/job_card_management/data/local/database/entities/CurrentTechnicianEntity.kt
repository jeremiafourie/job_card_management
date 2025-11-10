package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the currently logged-in technician
 * Only ONE record should exist in this table at any time
 */
@Entity(tableName = "current_technician")
data class CurrentTechnicianEntity(
    @PrimaryKey val id: Int = 1, // Always 1, single record
    val technicianId: Int, // Server-side technician ID
    val employeeNumber: String,
    val name: String,
    val email: String,
    val phone: String,
    val specialization: String? = null, // e.g., "AC Installation", "Refrigeration"
    val profilePhotoUrl: String? = null,
    val authToken: String,
    val refreshToken: String? = null,
    val lastSyncTime: Long? = null,
    val isOnDuty: Boolean = true,
    val currentActiveJobId: Int? = null, // ID of the job currently being worked on (BUSY status)
    val totalJobsCompletedToday: Int = 0,
    val loginTime: Long = System.currentTimeMillis()
)