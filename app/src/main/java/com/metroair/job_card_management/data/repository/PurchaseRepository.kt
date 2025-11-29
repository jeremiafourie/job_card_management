package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.JobPurchaseDao
import com.metroair.job_card_management.data.local.database.dao.PurchaseReceiptDao
import com.metroair.job_card_management.data.local.database.entities.JobPurchaseEntity
import com.metroair.job_card_management.data.local.database.entities.PurchaseReceiptEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.Purchase
import com.metroair.job_card_management.domain.model.PurchaseReceipt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    private val purchaseReceiptDao: PurchaseReceiptDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PurchaseRepository {

    override fun getPurchasesForJob(jobId: Int): Flow<List<Purchase>> = flow {
        jobPurchaseDao.getPurchasesForJob(jobId).collect { purchases ->
            val mapped = purchases.map { entity ->
                val receipts = purchaseReceiptDao.getReceiptsForPurchaseSync(entity.id)
                entity.toDomain(receipts)
            }
            emit(mapped)
        }
    }.flowOn(ioDispatcher)

    override suspend fun addPurchase(
        jobId: Int,
        vendor: String,
        totalAmount: Double,
        notes: String?,
        receiptUri: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            val purchaseId = jobPurchaseDao.insertPurchase(
                JobPurchaseEntity(
                    jobId = jobId,
                    vendor = vendor,
                    totalAmount = totalAmount,
                    notes = notes
                )
            )
            if (!receiptUri.isNullOrBlank()) {
                purchaseReceiptDao.clearForPurchase(purchaseId.toInt())
                purchaseReceiptDao.insertReceipt(
                    PurchaseReceiptEntity(
                        purchaseId = purchaseId.toInt(),
                        uri = receiptUri,
                        capturedAt = System.currentTimeMillis()
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun replaceReceiptForPurchase(purchaseId: Int, receiptUri: String, mimeType: String?): Boolean =
        withContext(ioDispatcher) {
            try {
                purchaseReceiptDao.clearForPurchase(purchaseId)
                purchaseReceiptDao.insertReceipt(
                    PurchaseReceiptEntity(
                        purchaseId = purchaseId,
                        uri = receiptUri,
                        mimeType = mimeType,
                        capturedAt = System.currentTimeMillis()
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun removeReceiptForPurchase(purchaseId: Int): Boolean =
        withContext(ioDispatcher) {
            try {
                purchaseReceiptDao.clearForPurchase(purchaseId)
                true
            } catch (e: Exception) {
                false
            }
        }

    private fun JobPurchaseEntity.toDomain(receipts: List<PurchaseReceiptEntity>): Purchase {
        return Purchase(
            id = id,
            jobId = jobId,
            vendor = vendor,
            totalAmount = totalAmount,
            notes = notes,
            purchasedAt = purchasedAt,
            receipts = receipts.map { it.toDomain() }
        )
    }

    private fun PurchaseReceiptEntity.toDomain(): PurchaseReceipt =
        PurchaseReceipt(
            id = id,
            purchaseId = purchaseId,
            uri = uri,
            mimeType = mimeType,
            capturedAt = capturedAt
        )
}
