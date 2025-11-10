package com.metroair.job_card_management.domain.model

data class User(
    val id: Int = 1,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val authToken: String
)