package com.metroair.job_card_management.domain.model

data class Asset(
    val id: Int,
    val assetCode: String, // Unique identifier for each asset
    val assetName: String,
    val assetType: AssetType,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val isAvailable: Boolean = true,
    val currentHolder: String? = null, // Technician name who has it
    val lastMaintenanceDate: Long? = null,
    val nextMaintenanceDate: Long? = null,
    val notes: String? = null
)

enum class AssetType {
    TOOL,           // Hand tools, power tools
    AIR_CONDITIONER, // AC units
    LADDER,         // Ladders
    EQUIPMENT,      // Other equipment
    VEHICLE,        // Service vehicles
    METER,          // Testing meters
    PUMP            // Vacuum pumps, etc.
}

data class AssetCheckout(
    val id: Int = 0,
    val assetId: Int,
    val assetCode: String,
    val assetName: String,
    val technicianId: Int,
    val technicianName: String,
    val checkoutTime: Long,
    val returnTime: Long? = null,
    val reason: String, // Why the asset is being checked out
    val jobId: Int? = null, // Associated job if applicable
    val condition: String = "Good", // Condition at checkout
    val returnCondition: String? = null,
    val notes: String? = null
)