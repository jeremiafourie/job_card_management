package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technicians")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val employeeNumber: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "Technician",
    val specialization: String? = null, // e.g., "AC Installation", "Refrigeration"
    val isActive: Boolean = true,
    val currentJobId: Int? = null, // Currently active job
    val totalJobsCompleted: Int = 0,
    val rating: Float = 0.0f,
    val authToken: String = "sample_token_12345",
    val profilePhotoPath: String? = null,
    val lastLoginAt: Long? = null
)