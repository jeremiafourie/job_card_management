package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.entities.AssetCheckoutEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.AssetCheckout
import com.metroair.job_card_management.domain.model.AssetType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AssetRepository {
    fun getAllAssets(): Flow<List<Asset>>
    fun getAssetsByType(type: AssetType): Flow<List<Asset>>
    fun getAvailableAssets(): Flow<List<Asset>>
    fun getAssetHistory(assetId: Int): Flow<List<AssetCheckout>>
    suspend fun checkoutAsset(
        assetId: Int,
        reason: String,
        jobId: Int? = null,
        condition: String = "Good",
        notes: String? = null
    ): Boolean
    suspend fun returnAsset(
        checkoutId: Int,
        condition: String = "Good",
        notes: String? = null
    ): Boolean
    suspend fun searchAssets(query: String, limit: Int = 5): List<Asset>
    fun getActiveCheckoutsForCurrentTechnician(): Flow<List<AssetCheckout>>
    fun getCheckoutsForJob(jobId: Int): Flow<List<AssetCheckout>>
}

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val currentTechnicianDao: CurrentTechnicianDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AssetRepository {

    override fun getAllAssets(): Flow<List<Asset>> =
        assetDao.getAllAssets()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getAssetsByType(type: AssetType): Flow<List<Asset>> =
        assetDao.getAssetsByType(type.name)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getAvailableAssets(): Flow<List<Asset>> =
        assetDao.getAvailableAssets()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getAssetHistory(assetId: Int): Flow<List<AssetCheckout>> =
        assetDao.getAssetHistory(assetId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override suspend fun checkoutAsset(
        assetId: Int,
        reason: String,
        jobId: Int?,
        condition: String,
        notes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val technician = currentTechnicianDao.getCurrentTechnicianSync()
                ?: return@withContext false

            val asset = assetDao.getAssetById(assetId)
                ?: return@withContext false

            if (!asset.isAvailable) {
                return@withContext false
            }

            // Get job details if jobId is provided
            val jobNumber = if (jobId != null) {
                // You might want to fetch job number from JobCardDao
                "JOB${jobId}"
            } else null

            val checkout = AssetCheckoutEntity(
                assetId = assetId,
                assetCode = asset.assetCode,
                assetName = asset.assetName,
                technicianId = technician.technicianId,
                technicianName = technician.name,
                reason = reason,
                jobId = jobId,
                jobNumber = jobNumber,
                checkoutCondition = condition,
                checkoutNotes = notes
            )

            val checkoutId = assetDao.insertCheckout(checkout)

            // Update asset availability
            assetDao.updateAssetAvailability(
                assetId = assetId,
                isAvailable = false,
                holder = technician.name,
                timestamp = System.currentTimeMillis()
            )

            checkoutId > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun returnAsset(
        checkoutId: Int,
        condition: String,
        notes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val checkout = assetDao.getCheckoutById(checkoutId)
                ?: return@withContext false

            // Update checkout record
            assetDao.returnAsset(
                checkoutId = checkoutId,
                returnTime = System.currentTimeMillis(),
                condition = condition,
                notes = notes
            )

            // Update asset availability
            assetDao.updateAssetAvailability(
                assetId = checkout.assetId,
                isAvailable = true,
                holder = null,
                timestamp = System.currentTimeMillis()
            )

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchAssets(query: String, limit: Int): List<Asset> =
        withContext(ioDispatcher) {
            assetDao.searchAssets("%$query%", limit).map { it.toDomainModel() }
        }

    override fun getActiveCheckoutsForCurrentTechnician(): Flow<List<AssetCheckout>> =
        assetDao.getAllActiveCheckouts()
            .map { entities ->
                entities.mapNotNull { entity ->
                    val currentTech = currentTechnicianDao.getCurrentTechnicianSync()
                    if (currentTech != null && entity.technicianId == currentTech.technicianId) {
                        entity.toDomainModel()
                    } else null
                }
            }
            .flowOn(ioDispatcher)

    override fun getCheckoutsForJob(jobId: Int): Flow<List<AssetCheckout>> =
        assetDao.getCheckoutsForJob(jobId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    // Extension functions for mapping
    private fun com.metroair.job_card_management.data.local.database.entities.AssetEntity.toDomainModel(): Asset {
        return Asset(
            id = id,
            assetCode = assetCode,
            assetName = assetName,
            assetType = try {
                AssetType.valueOf(assetType)
            } catch (e: Exception) {
                AssetType.EQUIPMENT
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

    private fun AssetCheckoutEntity.toDomainModel(): AssetCheckout {
        return AssetCheckout(
            id = id,
            assetId = assetId,
            assetCode = assetCode,
            assetName = assetName,
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