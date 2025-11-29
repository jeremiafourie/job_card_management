package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
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
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface JobCardRepository {
    fun getJobs(): Flow<List<JobCard>>
    fun getCurrentActiveJob(): Flow<JobCard?>
    fun getTodayPendingJobs(): Flow<List<JobCard>>
    fun getJobsByStatus(status: JobStatus): Flow<List<JobCard>>
    fun getJobCardById(jobId: Int): Flow<JobCard?>
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
    fun searchJobs(query: String): Flow<List<JobCard>>
}

@Singleton
class JobCardRepositoryImpl @Inject constructor(
    private val jobCardDao: JobCardDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobCardRepository {

    override fun getJobs(): Flow<List<JobCard>> =
        jobCardDao.getJobs()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getCurrentActiveJob(): Flow<JobCard?> =
        jobCardDao.getJobs()
            .map { list ->
                list.firstOrNull { entity ->
                    when (deriveStatus(entity.statusHistory)) {
                        JobStatus.BUSY, JobStatus.EN_ROUTE, JobStatus.PAUSED -> true
                        else -> false
                    }
                }?.toDomainModel()
            }
            .flowOn(ioDispatcher)

    override fun getTodayPendingJobs(): Flow<List<JobCard>> =
        jobCardDao.getTodayPendingJobs()
            .map { entities ->
                entities
                    .map { it.toDomainModel() }
                    .filter { it.status != JobStatus.COMPLETED && it.status != JobStatus.CANCELLED }
            }
            .flowOn(ioDispatcher)

    override fun getJobsByStatus(status: JobStatus): Flow<List<JobCard>> =
        jobCardDao.getJobs()
            .map { entities ->
                entities.map { it.toDomainModel() }.filter { it.status == status }
            }
            .flowOn(ioDispatcher)

    override fun getJobCardById(jobId: Int): Flow<JobCard?> =
        jobCardDao.getJobCardByIdFlow(jobId)
            .map { it?.toDomainModel() }
            .flowOn(ioDispatcher)

    override suspend fun canStartNewJob(): Boolean = withContext(ioDispatcher) {
        val jobs = jobCardDao.getJobsOnce()
        jobs.none {
            when (deriveStatus(it.statusHistory)) {
                JobStatus.BUSY, JobStatus.EN_ROUTE -> true
                else -> false
            }
        }
    }

    override suspend fun startJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val currentStatus = deriveStatus(job.statusHistory)
        if (currentStatus == JobStatus.PENDING && !canStartNewJob()) return@withContext false
        val newStatus = if (currentStatus == JobStatus.PENDING || currentStatus == JobStatus.AWAITING || currentStatus == JobStatus.AVAILABLE) {
            JobStatus.EN_ROUTE
        } else {
            JobStatus.BUSY
        }
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                statusHistory = appendStatusEvent(job.statusHistory, newStatus.name),
                updatedAt = now
            )
        )
        true
    }

    override suspend fun pauseJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        if (deriveStatus(job.statusHistory) != JobStatus.BUSY) return@withContext false
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                statusHistory = appendStatusEvent(job.statusHistory, JobStatus.PAUSED.name, reason),
                updatedAt = now
            )
        )
        true
    }

    override suspend fun cancelJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                statusHistory = appendStatusEvent(job.statusHistory, JobStatus.CANCELLED.name, reason),
                updatedAt = now
            )
        )
        true
    }

    override suspend fun resumeJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        if (deriveStatus(job.statusHistory) != JobStatus.PAUSED) return@withContext false
        if (!canStartNewJob()) return@withContext false
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                statusHistory = appendStatusEvent(job.statusHistory, JobStatus.BUSY.name),
                updatedAt = now
            )
        )
        true
    }

    override suspend fun enRouteJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val current = deriveStatus(job.statusHistory)
        if (current != JobStatus.PAUSED && current != JobStatus.PENDING && current != JobStatus.AWAITING) return@withContext false
        if (!canStartNewJob()) return@withContext false
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                statusHistory = appendStatusEvent(job.statusHistory, JobStatus.EN_ROUTE.name),
                updatedAt = now
            )
        )
        true
    }

    override suspend fun completeJob(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean = withContext(ioDispatcher) {
        val job = jobCardDao.getJobById(jobId) ?: return@withContext false
        val now = System.currentTimeMillis()
        jobCardDao.updateJob(
            job.copy(
                workPerformed = workPerformed,
                technicianNotes = technicianNotes,
                requiresFollowUp = requiresFollowUp,
                followUpNotes = followUpNotes,
                statusHistory = appendStatusEvent(job.statusHistory, JobStatus.COMPLETED.name),
                updatedAt = now
            )
        )
        true
    }

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
            val job = jobCardDao.getJobById(jobId) ?: return@withContext false
            jobCardDao.updateJob(
                job.copy(
                    workPerformed = workPerformed,
                    technicianNotes = technicianNotes,
                    issuesEncountered = issuesEncountered,
                    customerSignature = customerSignature,
                    requiresFollowUp = requiresFollowUp,
                    followUpNotes = followUpNotes,
                    updatedAt = System.currentTimeMillis()
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addCustomerSignature(jobId: Int, signature: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                jobCardDao.updateJob(
                    job.copy(
                        customerSignature = signature,
                        statusHistory = appendStatusEvent(job.statusHistory, JobStatus.SIGNED.name),
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
                val photosJson = JSONArray().apply {
                    photos.forEach { uri ->
                        put(JSONObject().apply { put("uri", uri) })
                    }
                }.toString()
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

    override fun searchJobs(query: String): Flow<List<JobCard>> =
        jobCardDao.searchJobs("%$query%")
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    private fun appendStatusEvent(existing: String?, newStatus: String, reason: String? = null): String {
        val array = try { JSONArray(existing ?: "[]") } catch (_: Exception) { JSONArray() }
        val obj = JSONObject().apply {
            put("status", newStatus)
            put("timestamp", System.currentTimeMillis())
            reason?.let { put("reason", it) }
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
        val array = try { JSONArray(existing ?: "[]") } catch (_: Exception) { JSONArray() }
        val obj = JSONObject().apply {
            put("uri", uri)
            if (!notes.isNullOrEmpty()) put("notes", notes)
        }
        array.put(obj)
        return array.toString()
    }

    private fun removePhotoFromJson(existing: String?, uri: String): String? {
        val array = try { JSONArray(existing ?: "[]") } catch (_: Exception) { JSONArray() }
        val filtered = JSONArray()
        for (i in 0 until array.length()) {
            val photo = array.getJSONObject(i)
            if (photo.optString("uri") != uri) filtered.put(photo)
        }
        return if (filtered.length() == 0) null else filtered.toString()
    }

    private fun retagPhotoJson(job: JobCardEntity, uri: String, from: String, to: String): Pair<String?, String?> {
        val fromArray = try { JSONArray(selectPhotoJson(job, from) ?: "[]") } catch (_: Exception) { JSONArray() }
        val toArray = try { JSONArray(selectPhotoJson(job, to) ?: "[]") } catch (_: Exception) { JSONArray() }
        var moved: JSONObject? = null
        val newFrom = JSONArray()
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
        val array = try { JSONArray(existing ?: "[]") } catch (_: Exception) { JSONArray() }
        val updated = JSONArray()
        for (i in 0 until array.length()) {
            val photo = array.getJSONObject(i)
            if (photo.optString("uri") == uri) {
                photo.put("notes", notes)
            }
            updated.put(photo)
        }
        return updated.toString()
    }

    private fun deriveStatus(statusHistory: String?): JobStatus {
        if (statusHistory.isNullOrBlank()) return JobStatus.PENDING
        return try {
            val arr = JSONArray(statusHistory)
            if (arr.length() == 0) return JobStatus.PENDING
            val last = arr.getJSONObject(arr.length() - 1)
            JobStatus.valueOf(last.getString("status"))
        } catch (_: Exception) {
            JobStatus.PENDING
        }
    }

    private fun JobCardEntity.toDomainModel(): JobCard {
        return JobCard(
            id = id,
            jobNumber = jobNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            customerAddress = customerAddress,
            title = title,
            description = description,
            jobType = try { JobType.valueOf(jobType) } catch (_: Exception) { JobType.SERVICE },
            status = deriveStatus(statusHistory),
            priority = try { JobPriority.valueOf(priority) } catch (_: Exception) { JobPriority.NORMAL },
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            serviceAddress = serviceAddress,
            latitude = latitude,
            longitude = longitude,
            estimatedDuration = estimatedDuration,
            travelDistance = travelDistance,
            statusHistory = statusHistory,
            workPerformed = workPerformed,
            technicianNotes = technicianNotes,
            issuesEncountered = issuesEncountered,
            customerSignature = customerSignature,
            beforePhotos = parsePhotosToList(beforePhotos),
            afterPhotos = parsePhotosToList(afterPhotos),
            otherPhotos = parsePhotosToList(otherPhotos),
            requiresFollowUp = requiresFollowUp,
            followUpNotes = followUpNotes,
            customerRating = customerRating,
            customerFeedback = customerFeedback,
            isSynced = isSynced,
            lastSyncedAt = lastSyncedAt
        )
    }

    private fun parsePhotosToList(photosJson: String?): List<com.metroair.job_card_management.domain.model.PhotoWithNotes>? {
        if (photosJson.isNullOrBlank() || photosJson == "null") return null
        return try {
            val jsonArray = JSONArray(photosJson)
            val photosList = mutableListOf<com.metroair.job_card_management.domain.model.PhotoWithNotes>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val uri = item.getString("uri")
                val notes = item.optString("notes", null)?.takeIf { it.isNotEmpty() }
                photosList.add(com.metroair.job_card_management.domain.model.PhotoWithNotes(uri = uri, notes = notes))
            }
            photosList.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }
}
