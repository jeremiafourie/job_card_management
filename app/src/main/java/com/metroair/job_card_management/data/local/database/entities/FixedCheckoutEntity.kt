package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fixed_checkouts",
    foreignKeys = [
        ForeignKey(
            entity = FixedEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fixedId"),
        Index("technicianId"),
        Index("jobId")
    ]
)
data class FixedCheckoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fixedId: Int,
    val fixedCode: String,
    val fixedName: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long = System.currentTimeMillis(),
    val returnTime: Long? = null,
    val reason: String,
    val jobId: Int? = null,
    val jobNumber: String? = null,
    val checkoutCondition: String = "Good",
    val returnCondition: String? = null,
    val checkoutNotes: String? = null,
    val returnNotes: String? = null
)