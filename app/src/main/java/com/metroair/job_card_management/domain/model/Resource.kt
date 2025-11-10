package com.metroair.job_card_management.domain.model

data class Resource(
    val id: Int,
    val itemCode: String,
    val itemName: String,
    val category: String,
    val currentStock: Int,
    val minimumStock: Int,
    val unitOfMeasure: String
) {
    val isLowStock: Boolean
        get() = currentStock <= minimumStock
}