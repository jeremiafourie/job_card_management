package com.metroair.job_card_management.domain.model

data class StatusEvent(
    val status: JobStatus,
    val timestamp: Long,
    val reason: String? = null,
    val signedBy: String? = null
)
