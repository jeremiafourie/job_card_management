package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Asset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AssetRepository {
    fun getAllAssets(): Flow<List<Asset>>
    fun getAssetsByCategory(category: String): Flow<List<Asset>>
    fun getLowStock(): Flow<List<Asset>>
    fun getUsageForJob(jobId: Int): Flow<List<com.metroair.job_card_management.domain.model.InventoryUsage>>
    suspend fun useAsset(assetId: Int, quantity: Double)
    suspend fun restoreAsset(assetId: Int, quantity: Double)
    suspend fun recordUsage(
        jobId: Int,
        assetId: Int,
        itemCode: String,
        itemName: String,
        quantity: Double,
        unit: String
    )
}

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val jobInventoryUsageDao: com.metroair.job_card_management.data.local.database.dao.JobInventoryUsageDao,
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

    override fun getLowStock(): Flow<List<Asset>> =
        assetDao.getLowStock()
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

    override fun getUsageForJob(jobId: Int): Flow<List<com.metroair.job_card_management.domain.model.InventoryUsage>> =
        jobInventoryUsageDao.getUsageForJob(jobId)
            .map { list ->
                list.map { usage ->
                    com.metroair.job_card_management.domain.model.InventoryUsage(
                        id = usage.id,
                        jobId = usage.jobId,
                        inventoryId = usage.inventoryId,
                        itemCode = usage.itemCode,
                        itemName = usage.itemName,
                        quantity = usage.quantity,
                        unitOfMeasure = usage.unitOfMeasure,
                        recordedAt = usage.recordedAt
                    )
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun useAsset(assetId: Int, quantity: Double) {
        withContext(ioDispatcher) { assetDao.useAsset(assetId, quantity, System.currentTimeMillis()) }
    }

    override suspend fun restoreAsset(assetId: Int, quantity: Double) {
        withContext(ioDispatcher) { assetDao.restoreAsset(assetId, quantity, System.currentTimeMillis()) }
    }

    override suspend fun recordUsage(
        jobId: Int,
        assetId: Int,
        itemCode: String,
        itemName: String,
        quantity: Double,
        unit: String
    ) {
        withContext(ioDispatcher) {
            jobInventoryUsageDao.insertUsage(
                com.metroair.job_card_management.data.local.database.entities.JobInventoryUsageEntity(
                    jobId = jobId,
                    inventoryId = assetId,
                    itemCode = itemCode,
                    itemName = itemName,
                    quantity = quantity,
                    unitOfMeasure = unit
                )
            )
            assetDao.useAsset(assetId, quantity, System.currentTimeMillis())
        }
    }
}
