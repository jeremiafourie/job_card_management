package com.metroair.job_card_management.domain.model

enum class JobStatus {
    AVAILABLE,     // Job created but not assigned to any technician
    AWAITING,      // Job assigned to technician but not yet accepted
    PENDING,       // Job accepted by technician but not started
    EN_ROUTE,      // Technician is on the way to job site
    BUSY,          // Technician is actively working on the job (only ONE per technician)
    PAUSED,        // Job temporarily paused (break, waiting for parts, etc.)
    COMPLETED,     // Job successfully completed
    SIGNED,        // Customer signed off and job is locked
    CANCELLED      // Job cancelled
}

enum class JobType {
    INSTALLATION,
    REPAIR,
    SERVICE,
    INSPECTION
}
