package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_checkouts")
data class ToolCheckoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val technicianId: Int, // ID of the technician who checked out the tool
    val resourceId: Int, // ID of the resource/tool from resources table
    val itemName: String, // Cached name for display
    val itemCode: String, // Cached code for display
    val checkedOutAt: Long = System.currentTimeMillis(),
    val returnedAt: Long? = null, // null means still checked out
    val isReturned: Boolean = false
)
