package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.FixedDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.entities.FixedCheckoutEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Fixed
import com.metroair.job_card_management.domain.model.FixedCheckout
import com.metroair.job_card_management.domain.model.FixedType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FixedRepository {
    fun getAllFixed(): Flow<List<Fixed>>
    fun getFixedByType(type: FixedType): Flow<List<Fixed>>
    fun getAvailableFixed(): Flow<List<Fixed>>
    fun getFixedHistory(fixedId: Int): Flow<List<FixedCheckout>>
    suspend fun checkoutFixed(
        fixedId: Int,
        reason: String,
        jobId: Int? = null,
        condition: String = "Good",
        notes: String? = null
    ): Boolean
    suspend fun returnFixed(
        checkoutId: Int,
        condition: String = "Good",
        notes: String? = null
    ): Boolean
    suspend fun searchFixed(query: String, limit: Int = 5): List<Fixed>
    fun getActiveCheckoutsForCurrentTechnician(): Flow<List<FixedCheckout>>
    fun getCheckoutsForJob(jobId: Int): Flow<List<FixedCheckout>>
}

@Singleton
class FixedRepositoryImpl @Inject constructor(
    private val fixedDao: FixedDao,
    private val currentTechnicianDao: CurrentTechnicianDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FixedRepository {

    override fun getAllFixed(): Flow<List<Fixed>> =
        fixedDao.getAllFixed()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getFixedByType(type: FixedType): Flow<List<Fixed>> =
        fixedDao.getFixedByType(type.name)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getAvailableFixed(): Flow<List<Fixed>> =
        fixedDao.getAvailableFixed()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getFixedHistory(fixedId: Int): Flow<List<FixedCheckout>> =
        fixedDao.getFixedHistory(fixedId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override suspend fun checkoutFixed(
        fixedId: Int,
        reason: String,
        jobId: Int?,
        condition: String,
        notes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val technician = currentTechnicianDao.getCurrentTechnicianSync()
                ?: return@withContext false

            val fixed = fixedDao.getFixedById(fixedId)
                ?: return@withContext false

            if (!fixed.isAvailable) {
                return@withContext false
            }

            // Get job details if jobId is provided
            val jobNumber = if (jobId != null) {
                // You might want to fetch job number from JobCardDao
                "JOB${jobId}"
            } else null

            val checkout = FixedCheckoutEntity(
                fixedId = fixedId,
                fixedCode = fixed.fixedCode,
                fixedName = fixed.fixedName,
                technicianId = technician.technicianId,
                technicianName = technician.name,
                reason = reason,
                jobId = jobId,
                jobNumber = jobNumber,
                checkoutCondition = condition,
                checkoutNotes = notes
            )

            val checkoutId = fixedDao.insertCheckout(checkout)

            // Update fixed availability
            fixedDao.updateFixedAvailability(
                fixedId = fixedId,
                isAvailable = false,
                holder = technician.name,
                timestamp = System.currentTimeMillis()
            )

            checkoutId > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun returnFixed(
        checkoutId: Int,
        condition: String,
        notes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val checkout = fixedDao.getCheckoutById(checkoutId)
                ?: return@withContext false

            // Update checkout record
            fixedDao.returnFixed(
                checkoutId = checkoutId,
                returnTime = System.currentTimeMillis(),
                condition = condition,
                notes = notes
            )

            // Update fixed availability
            fixedDao.updateFixedAvailability(
                fixedId = checkout.fixedId,
                isAvailable = true,
                holder = null,
                timestamp = System.currentTimeMillis()
            )

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchFixed(query: String, limit: Int): List<Fixed> =
        withContext(ioDispatcher) {
            fixedDao.searchFixed("%$query%", limit).map { it.toDomainModel() }
        }

    override fun getActiveCheckoutsForCurrentTechnician(): Flow<List<FixedCheckout>> =
        fixedDao.getAllActiveCheckouts()
            .map { entities ->
                entities.mapNotNull { entity ->
                    val currentTech = currentTechnicianDao.getCurrentTechnicianSync()
                    if (currentTech != null && entity.technicianId == currentTech.technicianId) {
                        entity.toDomainModel()
                    } else null
                }
            }
            .flowOn(ioDispatcher)

    override fun getCheckoutsForJob(jobId: Int): Flow<List<FixedCheckout>> =
        fixedDao.getCheckoutsForJob(jobId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    // Extension functions for mapping
    private fun com.metroair.job_card_management.data.local.database.entities.FixedEntity.toDomainModel(): Fixed {
        return Fixed(
            id = id,
            fixedCode = fixedCode,
            fixedName = fixedName,
            fixedType = try {
                FixedType.valueOf(fixedType)
            } catch (e: Exception) {
                FixedType.EQUIPMENT
            },
            serialNumber = serialNumber,
            manufacturer = manufacturer,
            model = model,
            isAvailable = isAvailable,
            currentHolder = currentHolder,
            lastMaintenanceDate = lastMaintenanceDate,
            nextMaintenanceDate = nextMaintenanceDate,
            notes = notes
        )
    }

    private fun FixedCheckoutEntity.toDomainModel(): FixedCheckout {
        return FixedCheckout(
            id = id,
            fixedId = fixedId,
            fixedCode = fixedCode,
            fixedName = fixedName,
            technicianId = technicianId,
            technicianName = technicianName,
            checkoutTime = checkoutTime,
            returnTime = returnTime,
            reason = reason,
            jobId = jobId,
            condition = checkoutCondition,
            returnCondition = returnCondition,
            notes = checkoutNotes ?: returnNotes
        )
    }
}
