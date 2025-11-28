package com.metroair.job_card_management.domain.model

data class User(
    val id: Int = 1,
    val username: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "Technician",
    val authToken: String,
    val lastSyncTime: Long? = null
)
