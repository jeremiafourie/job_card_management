package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.dao.JobInventoryUsageDao
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
import com.metroair.job_card_management.data.local.database.entities.JobInventoryUsageEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobPriority
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.domain.model.JobType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface JobCardRepository {
    fun getMyJobs(): Flow<List<JobCard>>
    fun getMyCurrentActiveJob(): Flow<JobCard?>
    fun getMyTodayPendingJobs(): Flow<List<JobCard>>
    fun getMyJobsByStatus(status: JobStatus): Flow<List<JobCard>>
    fun getJobCardById(jobId: Int): Flow<JobCard?>
    fun getAvailableJobs(): Flow<List<JobCard>>
    suspend fun claimJob(jobId: Int): Boolean
    suspend fun acceptJob(jobId: Int): Boolean
    suspend fun canStartNewJob(): Boolean
    suspend fun startJob(jobId: Int): Boolean
    suspend fun pauseJob(jobId: Int, reason: String): Boolean
    suspend fun cancelJob(jobId: Int, reason: String): Boolean
    suspend fun resumeJob(jobId: Int): Boolean
    suspend fun enRouteJob(jobId: Int): Boolean
    suspend fun completeJob(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        resourcesUsed: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean
    suspend fun updateJobDetails(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        issuesEncountered: String?,
        customerSignature: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean
    suspend fun addCustomerSignature(jobId: Int, signature: String): Boolean
    suspend fun addBeforePhotos(jobId: Int, photos: List<String>): Boolean
    suspend fun addAfterPhotos(jobId: Int, photos: List<String>): Boolean
    suspend fun addOtherPhotos(jobId: Int, photos: List<String>): Boolean
    suspend fun addPhotoToJob(jobId: Int, photoUri: String, category: String, notes: String? = null): Boolean
    suspend fun removePhoto(jobId: Int, photoUri: String, category: String): Boolean
    suspend fun retagPhoto(jobId: Int, photoUri: String, fromCategory: String, toCategory: String): Boolean
    suspend fun updatePhotoNotes(jobId: Int, photoUri: String, category: String, notes: String): Boolean
    suspend fun updateJobAssets(jobId: Int, resourcesJson: String): Boolean
    suspend fun getTodayCompletedCount(): Int
    suspend fun getJobById(jobId: Int): JobCard?
    fun searchJobs(query: String): Flow<List<JobCard>>
}

@Singleton
class JobCardRepositoryImpl @Inject constructor(
    private val jobCardDao: JobCardDao,
    private val assetDao: AssetDao,
    private val jobInventoryUsageDao: JobInventoryUsageDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobCardRepository {

    override fun getMyJobs(): Flow<List<JobCard>> =
        jobCardDao.getMyJobs()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getMyCurrentActiveJob(): Flow<JobCard?> =
        jobCardDao.getMyCurrentActiveJob()
            .map { it?.toDomainModel() }
            .flowOn(ioDispatcher)

    override fun getMyTodayPendingJobs(): Flow<List<JobCard>> =
        jobCardDao.getMyTodayPendingJobs()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getMyJobsByStatus(status: JobStatus): Flow<List<JobCard>> =
        jobCardDao.getMyJobsByStatus(status.name)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getAvailableJobs(): Flow<List<JobCard>> =
        jobCardDao.getAvailableJobs()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override suspend fun claimJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val rowsUpdated = jobCardDao.claimJob(jobId, System.currentTimeMillis())
        rowsUpdated > 0
    }

    override suspend fun acceptJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        try {
            val rowsUpdated = jobCardDao.acceptJob(jobId, System.currentTimeMillis())
            rowsUpdated > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun canStartNewJob(): Boolean = withContext(ioDispatcher) {
        val activeJobCount = jobCardDao.getMyActiveJobCount()
        activeJobCount == 0
    }

    override suspend fun startJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val newStatus = if (job.status == "PENDING") "EN_ROUTE" else "BUSY"
        if (job.status == "PENDING" && !canStartNewJob()) return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = newStatus,
            startTime = job.startTime ?: now,
            enRouteStartTime = if (newStatus == "EN_ROUTE") now else job.enRouteStartTime,
            statusHistory = appendStatusEvent(job.statusHistory, newStatus),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        true
    }

    override suspend fun pauseJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        if (job.status != "BUSY") return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = "PAUSED",
            pauseHistory = appendPause(job.pauseHistory, reason, now),
            statusHistory = appendStatusEvent(job.statusHistory, "PAUSED", reason),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        true
    }

    override suspend fun cancelJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = "CANCELLED",
            cancellationReason = reason,
            cancelledAt = now,
            statusHistory = appendStatusEvent(job.statusHistory, "CANCELLED"),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        true
    }

    override suspend fun resumeJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        if (job.status != "PAUSED") return@withContext false
        if (!canStartNewJob()) return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = "BUSY",
            statusHistory = appendStatusEvent(job.statusHistory, "BUSY"),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        true
    }

    override suspend fun enRouteJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        if (job.status != "PAUSED") return@withContext false
        if (!canStartNewJob()) return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = "EN_ROUTE",
            statusHistory = appendStatusEvent(job.statusHistory, "EN_ROUTE"),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        true
    }

    override suspend fun completeJob(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        resourcesUsed: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val now = System.currentTimeMillis()
        val updated = job.copy(
            status = "COMPLETED",
            endTime = now,
            workPerformed = workPerformed,
            technicianNotes = technicianNotes,
            resourcesUsed = resourcesUsed,
            requiresFollowUp = requiresFollowUp,
            followUpNotes = followUpNotes,
            statusHistory = appendStatusEvent(job.statusHistory, "COMPLETED"),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)

        // Normalize resource usage into job_inventory_usage table and decrement stock
        if (!resourcesUsed.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(resourcesUsed)
                val usageEntities = mutableListOf<JobInventoryUsageEntity>()
                for (i in 0 until jsonArray.length()) {
                    val resource = jsonArray.getJSONObject(i)
                    val resourceId = resource.getInt("id")
                    val quantity = resource.getDouble("quantity")
                    val name = resource.optString("name")
                    val code = resource.optString("code")
                    val unit = resource.optString("unit")
                    usageEntities.add(
                        JobInventoryUsageEntity(
                            jobId = jobId,
                            inventoryId = resourceId,
                            itemCode = code,
                            itemName = name,
                            quantity = quantity,
                            unitOfMeasure = unit
                        )
                    )
                    assetDao.useAsset(resourceId, quantity, now)
                }
                jobInventoryUsageDao.insertUsageList(usageEntities)
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error normalizing inventory usage", e)
            }
        }
        true
    }

    override suspend fun addCustomerSignature(jobId: Int, signature: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                jobCardDao.updateJob(
                    job.copy(
                        customerSignature = signature,
                        status = "SIGNED",
                        statusHistory = appendStatusEvent(job.statusHistory, "SIGNED"),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addBeforePhotos(jobId: Int, photos: List<String>): Boolean =
        addPhotos(jobId, photos, category = "BEFORE")

    override suspend fun addAfterPhotos(jobId: Int, photos: List<String>): Boolean =
        addPhotos(jobId, photos, category = "AFTER")

    override suspend fun addOtherPhotos(jobId: Int, photos: List<String>): Boolean =
        addPhotos(jobId, photos, category = "OTHER")

    private suspend fun addPhotos(jobId: Int, photos: List<String>, category: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val photosJson = photos.joinToString(",", "[\"", "\"]") { it }
                val updated = when (category.uppercase()) {
                    "BEFORE" -> job.copy(beforePhotos = photosJson, updatedAt = System.currentTimeMillis())
                    "AFTER" -> job.copy(afterPhotos = photosJson, updatedAt = System.currentTimeMillis())
                    else -> job.copy(otherPhotos = photosJson, updatedAt = System.currentTimeMillis())
                }
                jobCardDao.updateJob(updated)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addPhotoToJob(jobId: Int, photoUri: String, category: String, notes: String?): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val updatedJson = addPhotoToJson(selectPhotoJson(job, category), photoUri, notes)
                val updated = when (category.uppercase()) {
                    "BEFORE" -> job.copy(beforePhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    "AFTER" -> job.copy(afterPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    else -> job.copy(otherPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                }
                jobCardDao.updateJob(updated)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun removePhoto(jobId: Int, photoUri: String, category: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val updatedJson = removePhotoFromJson(selectPhotoJson(job, category), photoUri)
                val updated = when (category.uppercase()) {
                    "BEFORE" -> job.copy(beforePhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    "AFTER" -> job.copy(afterPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    else -> job.copy(otherPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                }
                jobCardDao.updateJob(updated)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun retagPhoto(jobId: Int, photoUri: String, fromCategory: String, toCategory: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val (fromJson, toJson) = retagPhotoJson(job, photoUri, fromCategory, toCategory)
                var updatedJob = job
                when (fromCategory.uppercase()) {
                    "BEFORE" -> updatedJob = updatedJob.copy(beforePhotos = fromJson)
                    "AFTER" -> updatedJob = updatedJob.copy(afterPhotos = fromJson)
                    else -> updatedJob = updatedJob.copy(otherPhotos = fromJson)
                }
                when (toCategory.uppercase()) {
                    "BEFORE" -> updatedJob = updatedJob.copy(beforePhotos = toJson)
                    "AFTER" -> updatedJob = updatedJob.copy(afterPhotos = toJson)
                    else -> updatedJob = updatedJob.copy(otherPhotos = toJson)
                }
                jobCardDao.updateJob(updatedJob.copy(updatedAt = System.currentTimeMillis()))
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun updatePhotoNotes(jobId: Int, photoUri: String, category: String, notes: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val updatedJson = updatePhotoNotesJson(selectPhotoJson(job, category), photoUri, notes)
                val updated = when (category.uppercase()) {
                    "BEFORE" -> job.copy(beforePhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    "AFTER" -> job.copy(afterPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                    else -> job.copy(otherPhotos = updatedJson, updatedAt = System.currentTimeMillis())
                }
                jobCardDao.updateJob(updated)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun updateJobAssets(jobId: Int, resourcesJson: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                jobCardDao.updateJob(job.copy(resourcesUsed = resourcesJson, updatedAt = System.currentTimeMillis()))
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun getTodayCompletedCount(): Int = withContext(ioDispatcher) {
        jobCardDao.getTodayCompletedCount()
    }

    override suspend fun getJobById(jobId: Int): JobCard? = withContext(ioDispatcher) {
        jobCardDao.getJobById(jobId)?.toDomainModel()
    }

    override fun getJobCardById(jobId: Int): Flow<JobCard?> =
        jobCardDao.getJobCardByIdFlow(jobId)
            .map { it?.toDomainModel() }
            .flowOn(ioDispatcher)

    override suspend fun updateJobDetails(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        issuesEncountered: String?,
        customerSignature: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            jobCardDao.updateJobDetails(
                jobId = jobId,
                workPerformed = workPerformed,
                technicianNotes = technicianNotes,
                issuesEncountered = issuesEncountered,
                customerSignature = customerSignature,
                requiresFollowUp = requiresFollowUp,
                followUpNotes = followUpNotes,
                updatedAt = System.currentTimeMillis()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun searchJobs(query: String): Flow<List<JobCard>> =
        jobCardDao.searchJobs("%$query%")
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    private fun appendStatusEvent(existing: String?, newStatus: String, reason: String? = null): String {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val obj = org.json.JSONObject().apply {
            put("status", newStatus)
            put("timestamp", System.currentTimeMillis())
            reason?.let { put("reason", it) }
        }
        array.put(obj)
        return array.toString()
    }

    private fun appendPause(existing: String?, reason: String, timestamp: Long): String {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val obj = org.json.JSONObject().apply {
            put("timestamp", timestamp)
            put("reason", reason)
        }
        array.put(obj)
        return array.toString()
    }

    private fun selectPhotoJson(job: JobCardEntity, category: String): String? =
        when (category.uppercase()) {
            "BEFORE" -> job.beforePhotos
            "AFTER" -> job.afterPhotos
            else -> job.otherPhotos
        }

    private fun addPhotoToJson(existing: String?, uri: String, notes: String?): String {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val obj = org.json.JSONObject().apply {
            put("uri", uri)
            if (!notes.isNullOrEmpty()) put("notes", notes)
        }
        array.put(obj)
        return array.toString()
    }

    private fun removePhotoFromJson(existing: String?, uri: String): String? {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val filtered = org.json.JSONArray()
        for (i in 0 until array.length()) {
            val photo = array.getJSONObject(i)
            if (photo.optString("uri") != uri) filtered.put(photo)
        }
        return if (filtered.length() == 0) null else filtered.toString()
    }

    private fun retagPhotoJson(job: JobCardEntity, uri: String, from: String, to: String): Pair<String?, String?> {
        val fromArray = try { org.json.JSONArray(selectPhotoJson(job, from) ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val toArray = try { org.json.JSONArray(selectPhotoJson(job, to) ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        var moved: org.json.JSONObject? = null
        val newFrom = org.json.JSONArray()
        for (i in 0 until fromArray.length()) {
            val photo = fromArray.getJSONObject(i)
            if (photo.optString("uri") == uri) {
                moved = photo
            } else {
                newFrom.put(photo)
            }
        }
        moved?.let { toArray.put(it) }
        return Pair(if (newFrom.length() == 0) null else newFrom.toString(), toArray.toString())
    }

    private fun updatePhotoNotesJson(existing: String?, uri: String, notes: String): String {
        val array = try { org.json.JSONArray(existing ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val updated = org.json.JSONArray()
        for (i in 0 until array.length()) {
            val photo = array.getJSONObject(i)
            if (photo.optString("uri") == uri) {
                photo.put("notes", notes)
            }
            updated.put(photo)
        }
        return updated.toString()
    }

    private fun JobCardEntity.toDomainModel(): JobCard {
        return JobCard(
            id = id,
            jobNumber = jobNumber,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            customerAddress = customerAddress,
            title = title,
            description = description,
            jobType = try { JobType.valueOf(jobType) } catch (_: Exception) { JobType.SERVICE },
            status = try { JobStatus.valueOf(status) } catch (_: Exception) { JobStatus.PENDING },
            priority = try { JobPriority.valueOf(priority) } catch (_: Exception) { JobPriority.NORMAL },
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            serviceAddress = serviceAddress,
            latitude = latitude,
            longitude = longitude,
            estimatedDuration = estimatedDuration,
            travelDistance = travelDistance,
            acceptedAt = acceptedAt,
            enRouteStartTime = enRouteStartTime,
            startTime = startTime,
            endTime = endTime,
            pausedTime = pausedTime,
            pauseHistory = pauseHistory,
            cancelledAt = cancelledAt,
            cancellationReason = cancellationReason,
            statusHistory = statusHistory,
            workPerformed = workPerformed,
            technicianNotes = technicianNotes,
            issuesEncountered = issuesEncountered,
            customerSignature = customerSignature,
            beforePhotos = parsePhotosToList(beforePhotos),
            afterPhotos = parsePhotosToList(afterPhotos),
            otherPhotos = parsePhotosToList(otherPhotos),
            resourcesUsed = resourcesUsed,
            requiresFollowUp = requiresFollowUp,
            followUpNotes = followUpNotes,
            customerRating = customerRating,
            customerFeedback = customerFeedback,
            isSynced = isSynced,
            lastSyncedAt = lastSyncedAt,
            isMyJob = isMyJob
        )
    }

    private fun parsePhotosToList(photosJson: String?): List<com.metroair.job_card_management.domain.model.PhotoWithNotes>? {
        if (photosJson.isNullOrBlank() || photosJson == "null") return null
        return try {
            val jsonArray = org.json.JSONArray(photosJson)
            val photosList = mutableListOf<com.metroair.job_card_management.domain.model.PhotoWithNotes>()
            for (i in 0 until jsonArray.length()) {
                when (val item = jsonArray.get(i)) {
                    is String -> photosList.add(com.metroair.job_card_management.domain.model.PhotoWithNotes(uri = item))
                    is org.json.JSONObject -> {
                        val uri = item.getString("uri")
                        val notes = item.optString("notes", null)?.takeIf { it.isNotEmpty() }
                        photosList.add(com.metroair.job_card_management.domain.model.PhotoWithNotes(uri = uri, notes = notes))
                    }
                }
            }
            photosList.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }
}
