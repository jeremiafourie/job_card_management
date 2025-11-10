package com.metroair.job_card_management.domain.model

data class JobResource(
    val resourceId: Int,
    val itemName: String,
    val itemCode: String,
    val quantity: Double,
    val unit: String
)
