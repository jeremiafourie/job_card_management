package com.metroair.job_card_management.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String,
    val area: String?,
    val notes: String? = null,
    val isSynced: Boolean = true
)