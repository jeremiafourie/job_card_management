package com.metroair.job_card_management.domain.model

data class Purchase(
    val id: Int = 0,
    val jobId: Int,
    val vendor: String,
    val totalAmount: Double,
    val notes: String? = null,
    val purchasedAt: Long,
    val receiptUri: String? = null,
    val receiptMimeType: String? = null,
    val receiptCapturedAt: Long? = null
)
