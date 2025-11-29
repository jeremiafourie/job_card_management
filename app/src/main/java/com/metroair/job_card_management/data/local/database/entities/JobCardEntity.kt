package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_cards",
    indices = [
        Index(value = ["job_number"], unique = true),
        Index("scheduled_date")
    ]
)
data class JobCardEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "job_number") val jobNumber: String,

    // Customer (denormalized)
    @ColumnInfo(name = "customer_name") val customerName: String,
    @ColumnInfo(name = "customer_phone") val customerPhone: String,
    @ColumnInfo(name = "customer_email") val customerEmail: String?,
    @ColumnInfo(name = "customer_address") val customerAddress: String,

    // Job details
    val title: String,
    val description: String?,
    @ColumnInfo(name = "job_type") val jobType: String,
    val priority: String = "NORMAL",
    @ColumnInfo(name = "status_history") val statusHistory: String = "[]", // JSON array of status events

    // Scheduling
    @ColumnInfo(name = "scheduled_date") val scheduledDate: String,
    @ColumnInfo(name = "scheduled_time") val scheduledTime: String?,
    @ColumnInfo(name = "estimated_duration") val estimatedDuration: Int?, // minutes

    // Location
    @ColumnInfo(name = "service_address") val serviceAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "travel_distance") val travelDistance: Double?,

    // Work/output
    @ColumnInfo(name = "work_performed") val workPerformed: String? = null,
    @ColumnInfo(name = "technician_notes") val technicianNotes: String? = null,
    @ColumnInfo(name = "issues_encountered") val issuesEncountered: String? = null,

    // Evidence
    @ColumnInfo(name = "customer_signature") val customerSignature: String? = null,
    @ColumnInfo(name = "before_photos") val beforePhotos: String? = null, // JSON array of photo objects
    @ColumnInfo(name = "after_photos") val afterPhotos: String? = null,
    @ColumnInfo(name = "other_photos") val otherPhotos: String? = null,

    // Follow-up/feedback
    @ColumnInfo(name = "requires_follow_up") val requiresFollowUp: Boolean = false,
    @ColumnInfo(name = "follow_up_notes") val followUpNotes: String? = null,
    @ColumnInfo(name = "customer_rating") val customerRating: Int? = null,
    @ColumnInfo(name = "customer_feedback") val customerFeedback: String? = null,

    // Sync
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,

    // Metadata
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
