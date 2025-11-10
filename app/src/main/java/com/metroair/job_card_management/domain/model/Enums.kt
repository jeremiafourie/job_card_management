package com.metroair.job_card_management.domain.model

enum class JobStatus {
    AVAILABLE,     // Job created but not assigned to any technician
    PENDING,       // Job assigned to technician but not started
    EN_ROUTE,      // Technician is on the way to job site
    BUSY,          // Technician is actively working on the job (only ONE per technician)
    PAUSED,        // Job temporarily paused (break, waiting for parts, etc.)
    COMPLETED,     // Job successfully completed
    CANCELLED      // Job cancelled
}

enum class JobType {
    INSTALLATION,
    REPAIR,
    SERVICE,
    INSPECTION
}