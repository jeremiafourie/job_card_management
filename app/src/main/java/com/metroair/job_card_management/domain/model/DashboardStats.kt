package com.metroair.job_card_management.domain.model

data class DashboardStats(
    val availableJobs: Int = 0,
    val awaitingJobs: Int = 0,
    val activeJob: Int = 0,  // Includes both BUSY and PAUSED jobs
    val pending: Int = 0
)