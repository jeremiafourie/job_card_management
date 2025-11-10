package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_cards",
    indices = [
        Index("status"),
        Index("scheduledDate"),
        Index("isMyJob"),
        Index(value = ["isMyJob", "status"])
    ]
)
data class JobCardEntity(
    @PrimaryKey val id: Int, // Server-side job ID
    val jobNumber: String,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String? = null,
    val customerAddress: String,
    val isMyJob: Boolean, // true = assigned to me, false = unassigned/available
    val acceptedByTechnician: Boolean = false, // true if technician has accepted the assigned job
    val title: String,
    val description: String?,
    val jobType: String, // "INSTALLATION", "REPAIR", "SERVICE", "INSPECTION"
    val status: String, // "ASSIGNED", "EN_ROUTE", "BUSY", "PAUSED", "COMPLETED", "CANCELLED"
    val scheduledDate: String?,
    val scheduledTime: String?,
    val estimatedDuration: Int? = null, // Duration in minutes
    val serviceAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val navigationUri: String? = null, // For launching navigation apps
    val acceptedAt: Long? = null, // When technician accepted the job
    val enRouteStartTime: Long? = null, // When EN_ROUTE status started
    val startTime: Long? = null, // When technician started the actual work (BUSY)
    val endTime: Long? = null, // When technician completed the job
    val pausedTime: Long? = null, // Total time paused in milliseconds
    val pauseHistory: String? = null, // JSON array of pause events [{timestamp, reason, duration}]
    val workPerformed: String? = null,
    val technicianNotes: String? = null,
    val issuesEncountered: String? = null,
    val customerSignature: String? = null, // Base64 encoded signature
    val beforePhotos: String? = null, // JSON array of photo paths
    val afterPhotos: String? = null, // JSON array of photo paths
    val otherPhotos: String? = null, // JSON array of photo paths for other/during work photos
    val resourcesUsed: String? = null, // JSON string of resources used
    val requiresFollowUp: Boolean = false,
    val followUpNotes: String? = null,
    val isSynced: Boolean = false,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)