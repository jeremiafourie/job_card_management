package com.metroair.job_card_management.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.FixedDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.CustomerDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.dao.ToolCheckoutDao
import com.metroair.job_card_management.data.local.database.entities.AssetEntity
import com.metroair.job_card_management.data.local.database.entities.FixedEntity
import com.metroair.job_card_management.data.local.database.entities.FixedCheckoutEntity
import com.metroair.job_card_management.data.local.database.entities.CurrentTechnicianEntity
import com.metroair.job_card_management.data.local.database.entities.CustomerEntity
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
import com.metroair.job_card_management.data.local.database.entities.ToolCheckoutEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        JobCardEntity::class,
        CustomerEntity::class,
        AssetEntity::class,
        CurrentTechnicianEntity::class,
        ToolCheckoutEntity::class,
        FixedEntity::class,
        FixedCheckoutEntity::class
    ],
    version = 11, // Added Asset and Fixed management entities
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JobCardDatabase : RoomDatabase() {

    abstract fun jobCardDao(): JobCardDao
    abstract fun customerDao(): CustomerDao
    abstract fun assetDao(): AssetDao
    abstract fun fixedDao(): FixedDao
    abstract fun currentTechnicianDao(): CurrentTechnicianDao
    abstract fun toolCheckoutDao(): ToolCheckoutDao

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
                    .fallbackToDestructiveMigration() // This will recreate the database if migration fails
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate with sample data
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
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
            val today = LocalDate.now().toString()
            val tomorrow = LocalDate.now().plusDays(1).toString()
            val dayAfter = LocalDate.now().plusDays(2).toString()
            val yesterday = LocalDate.now().minusDays(1).toString()

            // Current logged-in technician
            val currentTechnician = CurrentTechnicianEntity(
                id = 1,
                technicianId = 101,
                employeeNumber = "EMP001",
                name = "Mike Wilson",
                email = "mike.wilson@metroair.com",
                phone = "0825551234",
                specialization = "AC Installation & Repair",
                authToken = "sample_auth_token_123",
                isOnDuty = true,
                totalJobsCompletedToday = 2
            )

            // Sample customers
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
                    name = "ABC Company Ltd",
                    phone = "0119876543",
                    email = "info@abc.co.za",
                    address = "456 Business Park, Midrand",
                    area = "Midrand",
                    notes = "Ask for reception",
                    isSynced = true
                ),
                CustomerEntity(
                    id = 3,
                    name = "Sarah Johnson",
                    phone = "0834567890",
                    email = "sarah.j@gmail.com",
                    address = "789 Oak Ave, Rosebank",
                    area = "Rosebank",
                    notes = "Complex B, Unit 15",
                    isSynced = true
                ),
                CustomerEntity(
                    id = 4,
                    name = "Tech Solutions Inc",
                    phone = "0126543210",
                    email = "support@techsol.co.za",
                    address = "321 Innovation Hub, Centurion",
                    area = "Centurion",
                    notes = null,
                    isSynced = true
                ),
                CustomerEntity(
                    id = 5,
                    name = "David Miller",
                    phone = "0795551234",
                    email = null,
                    address = "555 Park Lane, Fourways",
                    area = "Fourways",
                    notes = "Beware of dogs",
                    isSynced = true
                )
            )

            // Sample job cards - Mix of my jobs and available jobs
            val jobCards = listOf(
                // My current active job (BUSY status - only ONE allowed)
                JobCardEntity(
                    id = 1001,
                    jobNumber = "JOB001",
                    customerId = 1,
                    customerName = "John Smith",
                    customerPhone = "0821234567",
                    customerEmail = "john@email.com",
                    customerAddress = "123 Main St, Sandton",
                    isMyJob = true,
                    title = "AC Installation - Master Bedroom",
                    description = "Install new 12000 BTU split unit",
                    jobType = "INSTALLATION",
                    status = "BUSY",
                    scheduledDate = today,
                    scheduledTime = "09:00",
                    estimatedDuration = 180,
                    serviceAddress = "123 Main St, Sandton",
                    latitude = -26.1076,
                    longitude = 28.0567,
                    startTime = System.currentTimeMillis() - 3600000, // Started 1 hour ago
                    isSynced = false
                ),
                // My assigned job for later today
                JobCardEntity(
                    id = 1002,
                    jobNumber = "JOB002",
                    customerId = 3,
                    customerName = "Sarah Johnson",
                    customerPhone = "0834567890",
                    customerEmail = "sarah.j@gmail.com",
                    customerAddress = "789 Oak Ave, Rosebank",
                    isMyJob = true,
                    title = "AC Not Cooling - Urgent",
                    description = "Unit not cooling, possible gas leak",
                    jobType = "REPAIR",
                    status = "PENDING",
                    scheduledDate = today,
                    scheduledTime = "14:00",
                    estimatedDuration = 90,
                    serviceAddress = "789 Oak Ave, Rosebank, Complex B, Unit 15",
                    latitude = -26.1450,
                    longitude = 28.0398,
                    isSynced = false
                ),
                // My job for tomorrow
                JobCardEntity(
                    id = 1003,
                    jobNumber = "JOB003",
                    customerId = 2,
                    customerName = "ABC Company Ltd",
                    customerPhone = "0119876543",
                    customerEmail = "info@abc.co.za",
                    customerAddress = "456 Business Park, Midrand",
                    isMyJob = true,
                    title = "Office AC Service",
                    description = "Annual service for 5 units",
                    jobType = "SERVICE",
                    status = "PENDING",
                    scheduledDate = tomorrow,
                    scheduledTime = "08:30",
                    estimatedDuration = 240,
                    serviceAddress = "456 Business Park, Midrand",
                    latitude = -25.9894,
                    longitude = 28.1286,
                    isSynced = false
                ),
                // My completed job from yesterday
                JobCardEntity(
                    id = 1004,
                    jobNumber = "JOB004",
                    customerId = 5,
                    customerName = "David Miller",
                    customerPhone = "0795551234",
                    customerEmail = null,
                    customerAddress = "555 Park Lane, Fourways",
                    isMyJob = true,
                    title = "Gas Refill",
                    description = "Top up refrigerant",
                    jobType = "REPAIR",
                    status = "COMPLETED",
                    scheduledDate = yesterday,
                    scheduledTime = "15:00",
                    estimatedDuration = 60,
                    serviceAddress = "555 Park Lane, Fourways",
                    latitude = -26.0171,
                    longitude = 28.0073,
                    startTime = System.currentTimeMillis() - 86400000,
                    endTime = System.currentTimeMillis() - 83000000,
                    workPerformed = "Refilled 2kg R410A gas, tested cooling performance, all units working within normal parameters",
                    technicianNotes = "Customer advised to schedule regular maintenance",
                    resourcesUsed = "[{\"id\":1,\"name\":\"R410A Refrigerant Gas\",\"code\":\"GAS-R410A\",\"quantity\":2,\"unit\":\"kg\"}]",
                    isSynced = true
                ),
                // Available job (unassigned) - technician can claim
                JobCardEntity(
                    id = 1005,
                    jobNumber = "JOB005",
                    customerId = 4,
                    customerName = "Tech Solutions Inc",
                    customerPhone = "0126543210",
                    customerEmail = "support@techsol.co.za",
                    customerAddress = "321 Innovation Hub, Centurion",
                    isMyJob = false,
                    title = "Preventive Maintenance",
                    description = "Quarterly maintenance check for server room AC units",
                    jobType = "SERVICE",
                    status = "AVAILABLE",
                    scheduledDate = tomorrow,
                    scheduledTime = "10:00",
                    estimatedDuration = 120,
                    serviceAddress = "321 Innovation Hub, Centurion",
                    latitude = -25.8607,
                    longitude = 28.1886,
                    isSynced = false
                ),
                // Available urgent job
                JobCardEntity(
                    id = 1006,
                    jobNumber = "JOB006",
                    customerId = 1,
                    customerName = "John Smith",
                    customerPhone = "0821234567",
                    customerEmail = "john@email.com",
                    customerAddress = "123 Main St, Sandton",
                    isMyJob = false,
                    title = "Emergency - No Cooling",
                    description = "Complete AC failure in server room",
                    jobType = "REPAIR",
                    status = "AVAILABLE",
                    scheduledDate = today,
                    scheduledTime = "ASAP",
                    estimatedDuration = 120,
                    serviceAddress = "123 Main St, Sandton",
                    latitude = -26.1076,
                    longitude = 28.0567,
                    isSynced = false
                ),
                // My completed job from today
                JobCardEntity(
                    id = 1007,
                    jobNumber = "JOB007",
                    customerId = 2,
                    customerName = "ABC Company Ltd",
                    customerPhone = "0119876543",
                    customerEmail = "info@abc.co.za",
                    customerAddress = "456 Business Park, Midrand",
                    isMyJob = true,
                    title = "Replace Faulty Compressor",
                    description = "Compressor making noise and not cooling",
                    jobType = "REPAIR",
                    status = "COMPLETED",
                    scheduledDate = today,
                    scheduledTime = "07:00",
                    estimatedDuration = 180,
                    serviceAddress = "456 Business Park, Midrand",
                    latitude = -25.9894,
                    longitude = 28.1286,
                    startTime = System.currentTimeMillis() - 21600000,
                    endTime = System.currentTimeMillis() - 14400000,
                    workPerformed = "Replaced faulty compressor unit, recharged system with R410A, tested all functions",
                    technicianNotes = "Old compressor had burnt windings. Warranty claim possible.",
                    customerSignature = "base64_signature_here",
                    beforePhotos = "[\"photo1.jpg\",\"photo2.jpg\"]",
                    afterPhotos = "[\"photo3.jpg\",\"photo4.jpg\"]",
                    resourcesUsed = "[{\"id\":5,\"name\":\"Capacitor 35uF\",\"code\":\"CAPACITOR-35UF\",\"quantity\":1,\"unit\":\"piece\"},{\"id\":1,\"name\":\"R410A Refrigerant Gas\",\"code\":\"GAS-R410A\",\"quantity\":3,\"unit\":\"kg\"}]",
                    isSynced = true
                )
            )

            // Sample assets/current inventory
            val assets = listOf(
                AssetEntity(
                    id = 1,
                    itemCode = "GAS-R410A",
                    itemName = "R410A Refrigerant Gas",
                    category = "Consumables",
                    currentStock = 15,
                    minimumStock = 5,
                    unitOfMeasure = "kg"
                ),
                AssetEntity(
                    id = 2,
                    itemCode = "FILTER-UNI",
                    itemName = "Universal AC Filter",
                    category = "Parts",
                    currentStock = 25,
                    minimumStock = 10,
                    unitOfMeasure = "piece"
                ),
                AssetEntity(
                    id = 3,
                    itemCode = "PIPE-COPPER-15",
                    itemName = "Copper Pipe 15mm",
                    category = "Parts",
                    currentStock = 50,
                    minimumStock = 20,
                    unitOfMeasure = "meter"
                ),
                AssetEntity(
                    id = 4,
                    itemCode = "BRACKET-WALL",
                    itemName = "Wall Mounting Bracket",
                    category = "Parts",
                    currentStock = 8,
                    minimumStock = 5,
                    unitOfMeasure = "set"
                ),
                AssetEntity(
                    id = 5,
                    itemCode = "CAPACITOR-35UF",
                    itemName = "Capacitor 35uF",
                    category = "Parts",
                    currentStock = 12,
                    minimumStock = 5,
                    unitOfMeasure = "piece"
                ),
                AssetEntity(
                    id = 6,
                    itemCode = "TOOL-GAUGE",
                    itemName = "Manifold Gauge Set",
                    category = "Tools",
                    currentStock = 3,
                    minimumStock = 2,
                    unitOfMeasure = "set"
                ),
                AssetEntity(
                    id = 7,
                    itemCode = "CLEANER-COIL",
                    itemName = "Coil Cleaning Solution",
                    category = "Consumables",
                    currentStock = 10,
                    minimumStock = 3,
                    unitOfMeasure = "bottle"
                ),
                AssetEntity(
                    id = 8,
                    itemCode = "TAPE-INSUL",
                    itemName = "Insulation Tape",
                    category = "Consumables",
                    currentStock = 20,
                    minimumStock = 10,
                    unitOfMeasure = "roll"
                )
            )

            // Sample fixed assets
            val fixedAssets = listOf(
                FixedEntity(
                    fixedCode = "TOOL-001",
                    fixedName = "Digital Manifold Gauge Set",
                    fixedType = "TOOL",
                    serialNumber = "MG-2024-001",
                    manufacturer = "Fieldpiece",
                    model = "SM480V",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "TOOL-002",
                    fixedName = "Recovery Machine",
                    fixedType = "TOOL",
                    serialNumber = "RM-2024-002",
                    manufacturer = "Inficon",
                    model = "G5Twin",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "AC-001",
                    fixedName = "Portable AC Unit - 12000 BTU",
                    fixedType = "AIR_CONDITIONER",
                    serialNumber = "PAC-2024-001",
                    manufacturer = "LG",
                    model = "LP1217GSR",
                    isAvailable = true,
                    notes = "For temporary customer use"
                ),
                FixedEntity(
                    fixedCode = "AC-002",
                    fixedName = "Portable AC Unit - 18000 BTU",
                    fixedType = "AIR_CONDITIONER",
                    serialNumber = "PAC-2024-002",
                    manufacturer = "Samsung",
                    model = "AX3000",
                    isAvailable = false,
                    currentHolder = "John Technician"
                ),
                FixedEntity(
                    fixedCode = "LADDER-001",
                    fixedName = "Extension Ladder - 24ft",
                    fixedType = "LADDER",
                    serialNumber = "LAD-2024-001",
                    manufacturer = "Werner",
                    model = "D1224-2",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "LADDER-002",
                    fixedName = "Step Ladder - 8ft",
                    fixedType = "LADDER",
                    serialNumber = "LAD-2024-002",
                    manufacturer = "Werner",
                    model = "FS108",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "TOOL-003",
                    fixedName = "Vacuum Pump",
                    fixedType = "PUMP",
                    serialNumber = "VP-2024-001",
                    manufacturer = "Yellow Jacket",
                    model = "93600",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "METER-001",
                    fixedName = "Digital Multimeter",
                    fixedType = "METER",
                    serialNumber = "DM-2024-001",
                    manufacturer = "Fluke",
                    model = "87V",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "TOOL-004",
                    fixedName = "Refrigerant Leak Detector",
                    fixedType = "TOOL",
                    serialNumber = "LD-2024-001",
                    manufacturer = "Inficon",
                    model = "D-TEK 3",
                    isAvailable = true
                ),
                FixedEntity(
                    fixedCode = "EQUIP-001",
                    fixedName = "Refrigerant Recovery Tank",
                    fixedType = "EQUIPMENT",
                    serialNumber = "RT-2024-001",
                    manufacturer = "Mastercool",
                    model = "62010",
                    isAvailable = true,
                    notes = "30lb capacity"
                )
            )

            // Insert all sample data
            database.currentTechnicianDao().setCurrentTechnician(currentTechnician)
            database.customerDao().insertAllCustomers(customers)
            database.jobCardDao().insertJobs(jobCards)
            database.assetDao().insertAllAssets(assets)
            database.fixedDao().insertFixedFixeds(fixedAssets)

            // Set current active job ID for the technician
            database.currentTechnicianDao().setCurrentActiveJob(1001)
        }
    }
}