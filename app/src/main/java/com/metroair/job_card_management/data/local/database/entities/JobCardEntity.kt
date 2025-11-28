package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_cards",
    indices = [
        Index("status"),
        Index("scheduledDate"),
        Index("jobNumber", unique = true),
        Index("priority"),
        Index(value = ["status", "scheduledDate"])
    ]
)
data class JobCardEntity(
    @PrimaryKey val id: Int, // Server-side job ID
    val jobNumber: String,

    // Customer Info (denormalized for offline use)
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String? = null,
    val customerAddress: String,
    val isMyJob: Boolean = true,

    // Job Details
    val title: String,
    val description: String?,
    val jobType: String, // "INSTALLATION", "REPAIR", "SERVICE", "INSPECTION"
    val priority: String = "NORMAL",

    // Status / workflow
    val status: String, // current status for quick filtering
    val statusHistory: String = "[]", // JSON array of status events

    // Scheduling
    val scheduledDate: String?,
    val scheduledTime: String?,
    val estimatedDuration: Int? = null, // Duration in minutes

    // Location
    val serviceAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val travelDistance: Double? = null,
    val navigationUri: String? = null, // For launching navigation apps

    // Time Tracking
    val acceptedAt: Long? = null, // When technician accepted the job
    val enRouteStartTime: Long? = null, // When EN_ROUTE status started
    val startTime: Long? = null, // When technician started the actual work (BUSY)
    val endTime: Long? = null, // When technician completed the job
    val pausedTime: Long? = null, // Total time paused in milliseconds
    val pauseHistory: String? = null, // JSON array of pause events [{timestamp, reason, duration}]
    val cancelledAt: Long? = null, // When job was cancelled
    val cancellationReason: String? = null, // Reason for cancellation

    // Work / Notes
    val workPerformed: String? = null,
    val technicianNotes: String? = null,
    val issuesEncountered: String? = null,

    // Evidence
    val customerSignature: String? = null, // Base64 encoded signature
    val beforePhotos: String? = null, // JSON array of photo objects
    val afterPhotos: String? = null, // JSON array of photo objects
    val otherPhotos: String? = null, // JSON array of photo objects

    // Resources / follow-up
    val resourcesUsed: String? = null, // Kept for compatibility; normalized usage stored separately
    val requiresFollowUp: Boolean = false,
    val followUpNotes: String? = null,

    // Feedback
    val customerRating: Int? = null,
    val customerFeedback: String? = null,

    // Sync tracking
    val isSynced: Boolean = false,
    val lastSyncedAt: Long? = null,

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
