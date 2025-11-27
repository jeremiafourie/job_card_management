package com.metroair.job_card_management.domain.model

data class Asset(
    val id: Int,
    val itemCode: String,
    val itemName: String,
    val category: String,
    val currentStock: Double,
    val minimumStock: Double,
    val unitOfMeasure: String
) {
    val isLowStock: Boolean
        get() = currentStock <= minimumStock
}