package com.metroair.job_card_management.domain.model

data class Customer(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String,
    val area: String?,
    val notes: String? = null
)