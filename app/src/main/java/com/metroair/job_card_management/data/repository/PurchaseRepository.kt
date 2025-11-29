package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.JobPurchaseDao
import com.metroair.job_card_management.data.local.database.entities.JobPurchaseEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Purchase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface PurchaseRepository {
    fun getPurchasesForJob(jobId: Int): Flow<List<Purchase>>
    suspend fun addPurchase(
        jobId: Int,
        vendor: String,
        totalAmount: Double,
        notes: String?,
        receiptUri: String?
    ): Boolean
    suspend fun replaceReceiptForPurchase(purchaseId: Int, receiptUri: String, mimeType: String? = null): Boolean
    suspend fun removeReceiptForPurchase(purchaseId: Int): Boolean
}

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val jobPurchaseDao: JobPurchaseDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PurchaseRepository {

    override fun getPurchasesForJob(jobId: Int): Flow<List<Purchase>> =
        jobPurchaseDao.getPurchasesForJob(jobId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addPurchase(
        jobId: Int,
        vendor: String,
        totalAmount: Double,
        notes: String?,
        receiptUri: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val now = System.currentTimeMillis()
            val purchaseId = jobPurchaseDao.insertPurchase(
                JobPurchaseEntity(
                    jobId = jobId,
                    vendor = vendor,
                    totalAmount = totalAmount,
                    notes = notes,
                    receiptUri = receiptUri,
                    receiptCapturedAt = receiptUri?.let { now }
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun replaceReceiptForPurchase(purchaseId: Int, receiptUri: String, mimeType: String?): Boolean =
        withContext(ioDispatcher) {
            try {
                jobPurchaseDao.updateReceipt(
                    purchaseId = purchaseId,
                    uri = receiptUri,
                    mimeType = mimeType,
                    capturedAt = System.currentTimeMillis()
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun removeReceiptForPurchase(purchaseId: Int): Boolean =
        withContext(ioDispatcher) {
            try {
                jobPurchaseDao.clearReceipt(purchaseId)
                true
            } catch (e: Exception) {
                false
            }
        }

    private fun JobPurchaseEntity.toDomain(): Purchase =
        Purchase(
            id = id,
            jobId = jobId,
            vendor = vendor,
            totalAmount = totalAmount,
            notes = notes,
            purchasedAt = purchasedAt,
            receiptUri = receiptUri,
            receiptMimeType = receiptMimeType,
            receiptCapturedAt = receiptCapturedAt
        )
}
