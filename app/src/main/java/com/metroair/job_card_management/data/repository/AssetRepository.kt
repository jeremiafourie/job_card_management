package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.ToolCheckoutDao
import com.metroair.job_card_management.data.local.database.entities.ToolCheckoutEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Asset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AssetRepository {
    fun getAllAssets(): Flow<List<Asset>>
    fun getAssetsByCategory(category: String): Flow<List<Asset>>
    suspend fun useAsset(assetId: Int, quantity: Int)
    suspend fun checkoutTool(toolId: Int, itemName: String, itemCode: String): Boolean
    suspend fun returnTool(checkoutId: Int): Boolean
    fun getActiveToolCheckouts(): Flow<List<ToolCheckoutEntity>>
}

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val toolCheckoutDao: ToolCheckoutDao,
    private val currentTechnicianDao: CurrentTechnicianDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AssetRepository {

    override fun getAllAssets(): Flow<List<Asset>> =
        assetDao.getAllAssets()
            .map { entities ->
                entities.map { entity ->
                    Asset(
                        id = entity.id,
                        itemCode = entity.itemCode,
                        itemName = entity.itemName,
                        category = entity.category,
                        currentStock = entity.currentStock,
                        minimumStock = entity.minimumStock,
                        unitOfMeasure = entity.unitOfMeasure
                    )
                }
            }
            .flowOn(ioDispatcher)

    override fun getAssetsByCategory(category: String): Flow<List<Asset>> =
        assetDao.getAssetsByCategory(category)
            .map { entities ->
                entities.map { entity ->
                    Asset(
                        id = entity.id,
                        itemCode = entity.itemCode,
                        itemName = entity.itemName,
                        category = entity.category,
                        currentStock = entity.currentStock,
                        minimumStock = entity.minimumStock,
                        unitOfMeasure = entity.unitOfMeasure
                    )
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun useAsset(assetId: Int, quantity: Int) {
        withContext(ioDispatcher) {
            assetDao.useAsset(assetId, quantity)
        }
    }

    override suspend fun checkoutTool(toolId: Int, itemName: String, itemCode: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val technician = currentTechnicianDao.getCurrentTechnicianSync() ?: return@withContext false
                val checkout = ToolCheckoutEntity(
                    technicianId = technician.technicianId, // This is the server-side technician ID
                    resourceId = toolId,
                    itemName = itemName,
                    itemCode = itemCode
                )
                toolCheckoutDao.checkoutTool(checkout)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun returnTool(checkoutId: Int): Boolean =
        withContext(ioDispatcher) {
            try {
                toolCheckoutDao.returnTool(checkoutId, System.currentTimeMillis())
                true
            } catch (e: Exception) {
                false
            }
        }

    override fun getActiveToolCheckouts(): Flow<List<ToolCheckoutEntity>> =
        flow {
            val technician = currentTechnicianDao.getCurrentTechnicianSync()
            if (technician != null) {
                toolCheckoutDao.getActiveCheckoutsForTechnician(technician.technicianId).collect {
                    emit(it)
                }
            } else {
                emit(emptyList())
            }
        }.flowOn(ioDispatcher)
}
