package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.ResourceDao
import com.metroair.job_card_management.data.local.database.dao.ToolCheckoutDao
import com.metroair.job_card_management.data.local.database.entities.ToolCheckoutEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ResourceRepository {
    fun getAllResources(): Flow<List<Resource>>
    fun getResourcesByCategory(category: String): Flow<List<Resource>>
    suspend fun useResource(resourceId: Int, quantity: Int)
    suspend fun checkoutTool(resourceId: Int, itemName: String, itemCode: String): Boolean
    suspend fun returnTool(checkoutId: Int): Boolean
    fun getActiveToolCheckouts(): Flow<List<ToolCheckoutEntity>>
}

@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val resourceDao: ResourceDao,
    private val toolCheckoutDao: ToolCheckoutDao,
    private val currentTechnicianDao: CurrentTechnicianDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ResourceRepository {

    override fun getAllResources(): Flow<List<Resource>> =
        resourceDao.getAllResources()
            .map { entities ->
                entities.map { entity ->
                    Resource(
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

    override fun getResourcesByCategory(category: String): Flow<List<Resource>> =
        resourceDao.getResourcesByCategory(category)
            .map { entities ->
                entities.map { entity ->
                    Resource(
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

    override suspend fun useResource(resourceId: Int, quantity: Int) {
        withContext(ioDispatcher) {
            resourceDao.useResource(resourceId, quantity)
        }
    }

    override suspend fun checkoutTool(resourceId: Int, itemName: String, itemCode: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val technician = currentTechnicianDao.getCurrentTechnicianSync() ?: return@withContext false
                val checkout = ToolCheckoutEntity(
                    technicianId = technician.technicianId, // This is the server-side technician ID
                    resourceId = resourceId,
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