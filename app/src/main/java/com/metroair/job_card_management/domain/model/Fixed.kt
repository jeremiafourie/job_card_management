package com.metroair.job_card_management.domain.model

data class Fixed(
    val id: Int,
    val fixedCode: String, // Unique identifier for each fixed asset
    val fixedName: String,
    val fixedType: FixedType,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val isAvailable: Boolean = true,
    val currentHolder: String? = null, // Technician name who has it
    val lastMaintenanceDate: Long? = null,
    val nextMaintenanceDate: Long? = null,
    val notes: String? = null
)

enum class FixedType {
    TOOL,           // Hand tools, power tools
    AIR_CONDITIONER, // AC units
    LADDER,         // Ladders
    EQUIPMENT,      // Other equipment
    VEHICLE,        // Service vehicles
    METER,          // Testing meters
    PUMP            // Vacuum pumps, etc.
}

data class FixedCheckout(
    val id: Int = 0,
    val fixedId: Int,
    val fixedCode: String,
    val fixedName: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long,
    val returnTime: Long? = null,
    val reason: String, // Why the fixed asset is being checked out
    val jobId: Int? = null, // Associated job if applicable
    val condition: String = "Good", // Condition at checkout
    val returnCondition: String? = null,
    val notes: String? = null
)