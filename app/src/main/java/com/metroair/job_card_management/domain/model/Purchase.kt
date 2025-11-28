package com.metroair.job_card_management.domain.model

data class Purchase(
    val id: Int = 0,
    val jobId: Int,
    val vendor: String,
    val totalAmount: Double,
    val notes: String? = null,
    val purchasedAt: Long,
    val receipts: List<PurchaseReceipt> = emptyList()
)

data class PurchaseReceipt(
    val id: Int = 0,
    val purchaseId: Int,
    val uri: String,
    val mimeType: String? = null,
    val notes: String? = null,
    val capturedAt: Long
)
