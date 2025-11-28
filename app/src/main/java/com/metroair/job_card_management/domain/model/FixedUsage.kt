package com.metroair.job_card_management.domain.model

data class FixedUsage(
    val id: Int = 0,
    val jobId: Int,
    val fixedId: Int,
    val fixedCode: String,
    val fixedName: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long,
    val returnTime: Long? = null,
    val condition: String = "Good",
    val returnCondition: String? = null,
    val notes: String? = null
)
