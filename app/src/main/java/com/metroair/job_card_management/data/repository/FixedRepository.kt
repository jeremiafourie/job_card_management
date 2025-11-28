package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.FixedDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.JobFixedAssetDao
import com.metroair.job_card_management.data.local.database.entities.JobFixedAssetEntity
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
        private val jobFixedAssetDao: JobFixedAssetDao,
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
        jobFixedAssetDao.getHistoryForFixed(fixedId)
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

            val statusHistoryJson = appendStatusEvent(fixed.statusHistory, status = "CHECKED_OUT", reason = reason, actor = technician.name)

            val checkout = JobFixedAssetEntity(
                fixedId = fixedId,
                fixedCode = fixed.fixedCode,
                fixedName = fixed.fixedName,
                reason = reason,
                technicianId = technician.id,
                technicianName = technician.name,
                jobId = jobId,
                condition = condition,
                notes = notes
            )

            val checkoutId = jobFixedAssetDao.insertCheckout(checkout)

            // Update fixed availability
            fixedDao.updateFixedAvailability(
                fixedId = fixedId,
                isAvailable = false,
                holder = technician.name,
                statusHistory = statusHistoryJson,
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
            val active = jobFixedAssetDao.getById(checkoutId) ?: return@withContext false

            val updated = active.copy(
                returnTime = System.currentTimeMillis(),
                returnCondition = condition,
                notes = notes ?: active.notes
            )
            jobFixedAssetDao.updateCheckout(updated)

            // Update fixed availability
            fixedDao.updateFixedAvailability(
                fixedId = active.fixedId,
                isAvailable = true,
                holder = null,
                statusHistory = appendStatusEvent(
                    fixedDao.getFixedById(active.fixedId)?.statusHistory ?: "[]",
                    status = "AVAILABLE",
                    reason = "Returned",
                    actor = currentTechnicianDao.getCurrentTechnicianSync()?.name
                ),
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
        jobFixedAssetDao.getActiveCheckouts()
            .map { entities ->
                val technician = currentTechnicianDao.getCurrentTechnicianSync()
                if (technician == null) {
                    emptyList()
                } else {
                    entities.filter { checkout -> checkout.technicianId == technician.id }
                        .map { it.toDomainModel() }
                }
            }
            .flowOn(ioDispatcher)

    override fun getCheckoutsForJob(jobId: Int): Flow<List<FixedCheckout>> =
        jobFixedAssetDao.getCheckoutsForJob(jobId)
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
            notes = notes,
            statusHistory = statusHistory
        )
    }

    private fun JobFixedAssetEntity.toDomainModel(): FixedCheckout {
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
            condition = condition,
            returnCondition = returnCondition,
            notes = notes
        )
    }

    private fun appendStatusEvent(existing: String?, status: String, reason: String? = null, actor: String? = null): String {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val obj = org.json.JSONObject().apply {
            put("status", status)
            put("timestamp", System.currentTimeMillis())
            reason?.let { put("reason", it) }
            actor?.let { put("actor", it) }
        }
        array.put(obj)
        return array.toString()
    }
}
