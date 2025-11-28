package com.metroair.job_card_management.domain.model

data class InventoryUsage(
    val id: Int = 0,
    val jobId: Int,
    val inventoryId: Int,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val recordedAt: Long
)
