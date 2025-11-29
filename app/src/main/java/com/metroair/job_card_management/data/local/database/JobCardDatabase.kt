package com.metroair.job_card_management.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.CustomerDao
import com.metroair.job_card_management.data.local.database.dao.FixedDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.dao.JobFixedAssetDao
import com.metroair.job_card_management.data.local.database.dao.JobInventoryUsageDao
import com.metroair.job_card_management.data.local.database.dao.JobPurchaseDao
import com.metroair.job_card_management.data.local.database.dao.PurchaseReceiptDao
import com.metroair.job_card_management.data.local.database.entities.AssetEntity
import com.metroair.job_card_management.data.local.database.entities.CustomerEntity
import com.metroair.job_card_management.data.local.database.entities.FixedEntity
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
import com.metroair.job_card_management.data.local.database.entities.JobFixedAssetEntity
import com.metroair.job_card_management.data.local.database.entities.JobInventoryUsageEntity
import com.metroair.job_card_management.data.local.database.entities.JobPurchaseEntity
import com.metroair.job_card_management.data.local.database.entities.PurchaseReceiptEntity
import com.metroair.job_card_management.data.local.database.entities.TechnicianEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

@Database(
    entities = [
        JobCardEntity::class,
        CustomerEntity::class,
        AssetEntity::class,
        FixedEntity::class,
        TechnicianEntity::class,
        JobInventoryUsageEntity::class,
        JobFixedAssetEntity::class,
        JobPurchaseEntity::class,
        PurchaseReceiptEntity::class
    ],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JobCardDatabase : RoomDatabase() {

    abstract fun jobCardDao(): JobCardDao
    abstract fun customerDao(): CustomerDao
    abstract fun assetDao(): AssetDao
    abstract fun fixedDao(): FixedDao
    abstract fun currentTechnicianDao(): CurrentTechnicianDao
    abstract fun jobInventoryUsageDao(): JobInventoryUsageDao
    abstract fun jobFixedAssetDao(): JobFixedAssetDao
    abstract fun jobPurchaseDao(): JobPurchaseDao
    abstract fun purchaseReceiptDao(): PurchaseReceiptDao

    companion object {
        @Volatile
        private var INSTANCE: JobCardDatabase? = null

        fun getInstance(context: Context): JobCardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JobCardDatabase::class.java,
                    "jobcard_database"
                )
                    .fallbackToDestructiveMigration() // Clean reset/reseed approved
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateSampleData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateSampleData(database: JobCardDatabase) {
            val now = System.currentTimeMillis()
            val minute = 60_000L
            val hour = 60 * minute
            val day = 24 * hour

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val currentTechnician = TechnicianEntity(
                id = 1,
                username = "mike.wilson",
                name = "Mike Wilson",
                email = "mike.wilson@metroair.com",
                phone = "0825551234",
                authToken = "sample_auth_token_123"
            )

            val customers = listOf(
                CustomerEntity(
                    id = 1,
                    name = "John Smith",
                    phone = "0821234567",
                    email = "john@email.com",
                    address = "123 Main St, Sandton",
                    area = "Sandton",
                    notes = "Gate code: 1234",
                    isSynced = true
                ),
                CustomerEntity(
                    id = 2,
                    name = "Sarah Johnson",
                    phone = "0834567890",
                    email = "sarah.j@gmail.com",
                    address = "789 Oak Ave, Rosebank",
                    area = "Rosebank",
                    notes = "Complex B, Unit 15",
                    isSynced = true
                ),
                CustomerEntity(
                    id = 3,
                    name = "ABC Company Ltd",
                    phone = "0119876543",
                    email = "info@abc.co.za",
                    address = "456 Business Park, Midrand",
                    area = "Midrand",
                    notes = "Ask for reception",
                    isSynced = true
                )
            )

            val assets = listOf(
                AssetEntity(
                    id = 1,
                    itemCode = "GAS-R410A",
                    itemName = "R410A Refrigerant Gas",
                    category = "Consumables",
                    currentStock = 15.5,
                    minimumStock = 5.0,
                    unitOfMeasure = "kg"
                ),
                AssetEntity(
                    id = 2,
                    itemCode = "FILTER-UNI",
                    itemName = "Universal AC Filter",
                    category = "Parts",
                    currentStock = 25.0,
                    minimumStock = 10.0,
                    unitOfMeasure = "piece"
                ),
                AssetEntity(
                    id = 3,
                    itemCode = "PIPE-COPPER-15",
                    itemName = "Copper Pipe 15mm",
                    category = "Parts",
                    currentStock = 50.75,
                    minimumStock = 20.0,
                    unitOfMeasure = "meter"
                ),
                AssetEntity(
                    id = 4,
                    itemCode = "BRACKET-WALL",
                    itemName = "Wall Mounting Bracket",
                    category = "Parts",
                    currentStock = 8.0,
                    minimumStock = 5.0,
                    unitOfMeasure = "set"
                )
            )

            val fixedAssets = listOf(
                FixedEntity(
                    id = 1,
                    fixedCode = "TOOL-001",
                    fixedName = "Digital Manifold Gauge Set",
                    fixedType = "TOOL",
                    serialNumber = "MG-2024-001",
                    manufacturer = "Fieldpiece",
                    model = "SM480V",
                    isAvailable = true,
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 5))
                    )
                ),
                FixedEntity(
                    id = 2,
                    fixedCode = "LADDER-001",
                    fixedName = "Extension Ladder - 24ft",
                    fixedType = "LADDER",
                    serialNumber = "LAD-2024-001",
                    manufacturer = "Werner",
                    model = "D1224-2",
                    isAvailable = true,
                    currentHolder = null,
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 4))
                    )
                ),
                FixedEntity(
                    id = 3,
                    fixedCode = "PUMP-001",
                    fixedName = "Vacuum Pump",
                    fixedType = "PUMP",
                    serialNumber = "VP-2024-001",
                    manufacturer = "Yellow Jacket",
                    model = "93600",
                    isAvailable = true,
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 5))
                    )
                )
            )

            val jobCards = listOf(
                JobCardEntity(
                    id = 2001,
                    jobNumber = "JC-2024-001",
                    customerName = "John Smith",
                    customerPhone = "0821234567",
                    customerEmail = "john@email.com",
                    customerAddress = "123 Main St, Sandton",
                    title = "Split AC Installation - Master Bedroom",
                    description = "Install new 12000 BTU inverter with outdoor mounting and condensate management.",
                    jobType = "INSTALLATION",
                    priority = "HIGH",
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 3)),
                        StatusEvent("AWAITING", now - (day * 2)),
                        StatusEvent("PENDING", now - (day - hour)),
                        StatusEvent("EN_ROUTE", now - (90 * minute)),
                        StatusEvent("BUSY", now - (70 * minute)),
                        StatusEvent("PAUSED", now - (40 * minute), reason = "Parts check"),
                        StatusEvent("BUSY", now - (30 * minute))
                    ),
                    scheduledDate = today.toString(),
                    scheduledTime = "09:00",
                    estimatedDuration = 180,
                    serviceAddress = "123 Main St, Sandton",
                    latitude = -26.1076,
                    longitude = 28.0567,
                    travelDistance = 14.2,
                    workPerformed = null,
                    beforePhotos = photoList(
                        PhotoItem("file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/Pictures/job2001_before_01.jpg", "Indoor mounting position"),
                        PhotoItem("file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/Pictures/job2001_before_02.jpg", "Outdoor wall anchor check")
                    ),
                    otherPhotos = photoList(
                        PhotoItem("file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/Pictures/job2001_during_01.jpg", "Condenser base install")
                    ),
                    isSynced = false
                ),
                JobCardEntity(
                    id = 2002,
                    jobNumber = "JC-2024-002",
                    customerName = "Sarah Johnson",
                    customerPhone = "0834567890",
                    customerEmail = "sarah.j@gmail.com",
                    customerAddress = "789 Oak Ave, Rosebank",
                    title = "Bedroom AC Not Cooling",
                    description = "Unit cycling and not reaching setpoint, suspected low gas or blocked filter.",
                    jobType = "REPAIR",
                    priority = "URGENT",
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 2)),
                        StatusEvent("AWAITING", now - (day / 2))
                    ),
                    scheduledDate = today.toString(),
                    scheduledTime = "14:00",
                    estimatedDuration = 90,
                    serviceAddress = "789 Oak Ave, Rosebank, Complex B, Unit 15",
                    latitude = -26.1450,
                    longitude = 28.0398,
                    travelDistance = 9.5,
                    isSynced = false
                ),
                JobCardEntity(
                    id = 2003,
                    jobNumber = "JC-2024-003",
                    customerName = "ABC Company Ltd",
                    customerPhone = "0119876543",
                    customerEmail = "info@abc.co.za",
                    customerAddress = "456 Business Park, Midrand",
                    title = "Office AC Service - Server Room",
                    description = "Quarterly preventive maintenance for server room cooling.",
                    jobType = "SERVICE",
                    priority = "NORMAL",
                    statusHistory = statusHistory(
                        StatusEvent("AVAILABLE", now - (day * 7)),
                        StatusEvent("PENDING", now - (day * 6)),
                        StatusEvent("EN_ROUTE", now - (day * 6) + (2 * hour)),
                        StatusEvent("BUSY", now - (day * 6) + (3 * hour)),
                        StatusEvent("COMPLETED", now - (day * 6) + (6 * hour)),
                        StatusEvent("SIGNED", now - (day * 6) + (7 * hour), signedBy = "Facility Manager")
                    ),
                    scheduledDate = yesterday.toString(),
                    scheduledTime = "08:00",
                    estimatedDuration = 240,
                    serviceAddress = "456 Business Park, Midrand",
                    latitude = -25.9894,
                    longitude = 28.1286,
                    travelDistance = 22.3,
                    workPerformed = "Cleaned coils, replaced filters, tested airflow and condensate pump.",
                    technicianNotes = "Server room kept online during service. No downtime.",
                    afterPhotos = photoList(
                        PhotoItem("file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/Pictures/job2003_after_01.jpg", "Filters replaced"),
                        PhotoItem("file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/Pictures/job2003_after_02.jpg", "Condensate line cleared")
                    ),
                    customerRating = 5,
                    customerFeedback = "Thanks for the fast service",
                    isSynced = true
                )
            )

            val inventoryUsage = listOf(
                JobInventoryUsageEntity(
                    jobId = 2001,
                    inventoryId = 1,
                    itemCode = "GAS-R410A",
                    itemName = "R410A Refrigerant Gas",
                    quantity = 2.5,
                    unitOfMeasure = "kg",
                    recordedAt = now - (20 * minute)
                ),
                JobInventoryUsageEntity(
                    jobId = 2001,
                    inventoryId = 4,
                    itemCode = "BRACKET-WALL",
                    itemName = "Wall Mounting Bracket",
                    quantity = 1.0,
                    unitOfMeasure = "set",
                    recordedAt = now - (25 * minute)
                ),
                JobInventoryUsageEntity(
                    jobId = 2003,
                    inventoryId = 2,
                    itemCode = "FILTER-UNI",
                    itemName = "Universal AC Filter",
                    quantity = 2.0,
                    unitOfMeasure = "piece",
                    recordedAt = now - (day * 6)
                )
            )

            val fixedUsage = listOf(
                JobFixedAssetEntity(
                    jobId = 2001,
                    fixedId = 1,
                    fixedCode = "TOOL-001",
                    fixedName = "Digital Manifold Gauge Set",
                    reason = "Commissioning pressures",
                    technicianId = currentTechnician.id,
                    technicianName = currentTechnician.name,
                    checkoutTime = now - (80 * minute),
                    returnTime = null,
                    condition = "Good"
                ),
                JobFixedAssetEntity(
                    jobId = 2001,
                    fixedId = 2,
                    fixedCode = "LADDER-001",
                    fixedName = "Extension Ladder - 24ft",
                    reason = "Outdoor unit mounting",
                    technicianId = currentTechnician.id,
                    technicianName = currentTechnician.name,
                    checkoutTime = now - (90 * minute),
                    returnTime = null,
                    condition = "Good"
                )
            )

            database.currentTechnicianDao().setCurrentTechnician(currentTechnician)
            database.customerDao().insertAllCustomers(customers)
            database.assetDao().insertAllAssets(assets)
            database.fixedDao().insertFixedFixeds(fixedAssets)
            database.jobCardDao().insertJobs(jobCards)
            database.jobInventoryUsageDao().insertUsageList(inventoryUsage)
            fixedUsage.forEach { database.jobFixedAssetDao().insertCheckout(it) }

            val purchaseId1 = database.jobPurchaseDao().insertPurchase(
                JobPurchaseEntity(
                    jobId = 2001,
                    vendor = "HVAC Parts Depot",
                    totalAmount = 2450.0,
                    notes = "Install kit, mounting brackets"
                )
            ).toInt()

            val purchaseId2 = database.jobPurchaseDao().insertPurchase(
                JobPurchaseEntity(
                    jobId = 2003,
                    vendor = "Metro HVAC Supply",
                    totalAmount = 780.0,
                    notes = "Filters and drain cleaner"
                )
            ).toInt()

            database.purchaseReceiptDao().insertReceipt(
                PurchaseReceiptEntity(
                    purchaseId = purchaseId1,
                    uri = sampleReceiptUri("jc-2024-001_install"),
                    mimeType = "image/jpeg",
                    capturedAt = now - (25 * minute)
                )
            )

            database.purchaseReceiptDao().insertReceipt(
                PurchaseReceiptEntity(
                    purchaseId = purchaseId2,
                    uri = sampleReceiptUri("jc-2024-003_service"),
                    mimeType = "image/jpeg",
                    capturedAt = now - (day * 6)
                )
            )
        }

        private fun statusHistory(vararg events: StatusEvent): String {
            val array = JSONArray()
            events.forEach { event ->
                array.put(
                    JSONObject().apply {
                        put("status", event.status)
                        put("timestamp", event.timestamp)
                        event.reason?.let { put("reason", it) }
                        event.signedBy?.let { put("signed_by", it) }
                        event.holder?.let { put("holder", it) }
                    }
                )
            }
            return array.toString()
        }

        private fun photoList(vararg photos: PhotoItem): String {
            val array = JSONArray()
            photos.forEach { photo ->
                array.put(
                    JSONObject().apply {
                        put("uri", photo.uri)
                        photo.notes?.let { put("notes", it) }
                    }
                )
            }
            return array.toString()
        }

        private fun sampleReceiptUri(name: String) =
            "file:///storage/emulated/0/Android/data/com.metroair.job_card_management/files/receipts/${name}.jpg"

        private data class StatusEvent(
            val status: String,
            val timestamp: Long,
            val reason: String? = null,
            val signedBy: String? = null,
            val holder: String? = null
        )

        private data class PhotoItem(
            val uri: String,
            val notes: String? = null
        )
    }
}
