package com.metroair.job_card_management.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technician")
data class TechnicianEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val name: String,
    val email: String,
    val phone: String,
    @ColumnInfo(name = "auth_token") val authToken: String,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
