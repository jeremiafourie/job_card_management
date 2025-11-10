package com.metroair.job_card_management.data.repository

import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
import com.metroair.job_card_management.di.IoDispatcher
import com.metroair.job_card_management.domain.model.JobCard
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
    // My Jobs
    fun getMyJobs(): Flow<List<JobCard>>
    fun getMyCurrentActiveJob(): Flow<JobCard?>
    fun getMyTodayPendingJobs(): Flow<List<JobCard>>
    fun getMyJobsByStatus(status: JobStatus): Flow<List<JobCard>>
    fun getJobCardById(jobId: Int): Flow<JobCard?>

    // Available Jobs
    fun getAvailableJobs(): Flow<List<JobCard>>
    suspend fun claimJob(jobId: Int): Boolean
    suspend fun acceptJob(jobId: Int): Boolean

    // Job Operations
    suspend fun canStartNewJob(): Boolean
    suspend fun startJob(jobId: Int): Boolean
    suspend fun pauseJob(jobId: Int, reason: String): Boolean
    suspend fun cancelJob(jobId: Int, reason: String): Boolean
    suspend fun resumeJob(jobId: Int): Boolean
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
    suspend fun updateJobResources(jobId: Int, resourcesJson: String): Boolean

    // Stats
    suspend fun getTodayCompletedCount(): Int
    suspend fun getJobById(jobId: Int): JobCard?

    // Search
    fun searchJobs(query: String): Flow<List<JobCard>>
}

@Singleton
class JobCardRepositoryImpl @Inject constructor(
    private val jobCardDao: JobCardDao,
    private val currentTechnicianDao: CurrentTechnicianDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobCardRepository {

    // ========== MY JOBS ==========

    override fun getMyJobs(): Flow<List<JobCard>> =
        jobCardDao.getMyJobs()
            .map { entities ->
                entities.map { entity -> entity.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getMyCurrentActiveJob(): Flow<JobCard?> =
        jobCardDao.getMyCurrentActiveJob()
            .map { entity -> entity?.toDomainModel() }
            .flowOn(ioDispatcher)

    override fun getMyTodayPendingJobs(): Flow<List<JobCard>> =
        jobCardDao.getMyTodayPendingJobs()
            .map { entities ->
                entities.map { entity -> entity.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override fun getMyJobsByStatus(status: JobStatus): Flow<List<JobCard>> =
        jobCardDao.getMyJobsByStatus(status.name)
            .map { entities ->
                entities.map { entity -> entity.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    // ========== AVAILABLE JOBS ==========

    override fun getAvailableJobs(): Flow<List<JobCard>> =
        jobCardDao.getAvailableJobs()
            .map { entities ->
                entities.map { entity -> entity.toDomainModel() }
            }
            .flowOn(ioDispatcher)

    override suspend fun claimJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        // Technician can claim jobs even with an active job (to work on later)
        val rowsUpdated = jobCardDao.claimJob(jobId, System.currentTimeMillis())
        return@withContext rowsUpdated > 0
    }

    override suspend fun acceptJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        try {
            val rowsUpdated = jobCardDao.acceptJob(jobId, System.currentTimeMillis())
            rowsUpdated > 0
        } catch (e: Exception) {
            false
        }
    }

    // ========== JOB OPERATIONS ==========

    override suspend fun canStartNewJob(): Boolean = withContext(ioDispatcher) {
        // Technician can only have ONE active job (BUSY or EN_ROUTE)
        val activeJobCount = jobCardDao.getMyActiveJobCount()
        activeJobCount == 0
    }

    override suspend fun startJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        try {
            // Check if can start new job
            val job = jobCardDao.getJobById(jobId)
            if (job == null || !job.isMyJob) return@withContext false

            val newStatus = if (job.status == "PENDING") "EN_ROUTE" else "BUSY"

            // Check if already has an active job (only when starting a new EN_ROUTE job)
            // Allow EN_ROUTE → BUSY transition even with active jobs (same job)
            if (job.status == "PENDING" && !canStartNewJob()) {
                return@withContext false
            }

            val startTime = if (newStatus == "BUSY") System.currentTimeMillis() else job.startTime

            jobCardDao.startJob(
                id = jobId,
                status = newStatus,
                timestamp = System.currentTimeMillis(),
                startTime = startTime ?: System.currentTimeMillis()
            )

            // Update technician's current active job
            if (newStatus == "BUSY") {
                currentTechnicianDao.setCurrentActiveJob(jobId)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun pauseJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        try {
            val job = jobCardDao.getJobById(jobId)
            if (job == null || !job.isMyJob || job.status != "BUSY") {
                return@withContext false
            }

            val pauseTimestamp = System.currentTimeMillis()

            // Store pause event in JSON history (duration will be calculated on resume)
            val existingHistory = try {
                org.json.JSONArray(job.pauseHistory ?: "[]")
            } catch (e: Exception) {
                org.json.JSONArray()
            }

            val pauseEvent = org.json.JSONObject().apply {
                put("timestamp", pauseTimestamp)
                put("reason", reason)
                put("duration", 0) // Will be updated on resume
            }
            existingHistory.put(pauseEvent)

            // Update job with pause status and history
            jobCardDao.pauseJob(jobId, 0, existingHistory.toString(), pauseTimestamp)

            // Clear technician's current active job
            currentTechnicianDao.setCurrentActiveJob(null)

            true
        } catch (e: Exception) {
            android.util.Log.e("JobCardRepository", "Error pausing job", e)
            false
        }
    }

    override suspend fun cancelJob(jobId: Int, reason: String): Boolean = withContext(ioDispatcher) {
        try {
            val job = jobCardDao.getJobById(jobId)
            if (job == null || !job.isMyJob || job.status == "COMPLETED" || job.status == "CANCELLED") {
                return@withContext false
            }

            // Store cancellation reason in technician notes
            val currentNotes = job.technicianNotes ?: ""
            val updatedNotes = if (currentNotes.isNotEmpty()) {
                "$currentNotes\n[CANCELLED: $reason]"
            } else {
                "[CANCELLED: $reason]"
            }

            // Update job to cancelled status with reason
            val updatedJob = job.copy(
                status = "CANCELLED",
                technicianNotes = updatedNotes
            )
            jobCardDao.updateJob(updatedJob)

            // Clear technician's current active job if this was the active one
            if (job.status == "BUSY") {
                currentTechnicianDao.setCurrentActiveJob(null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resumeJob(jobId: Int): Boolean = withContext(ioDispatcher) {
        try {
            val job = jobCardDao.getJobById(jobId)
            if (job == null || !job.isMyJob || job.status != "PAUSED") {
                return@withContext false
            }

            // Check if can resume (no other active job)
            if (!canStartNewJob()) {
                return@withContext false
            }

            val resumeTimestamp = System.currentTimeMillis()

            // Update the last pause event with the actual duration
            val pauseHistory = try {
                org.json.JSONArray(job.pauseHistory ?: "[]")
            } catch (e: Exception) {
                org.json.JSONArray()
            }

            if (pauseHistory.length() > 0) {
                val lastPauseIndex = pauseHistory.length() - 1
                val lastPause = pauseHistory.getJSONObject(lastPauseIndex)
                val pauseStartTime = lastPause.getLong("timestamp")
                val pauseDuration = resumeTimestamp - pauseStartTime

                // Update the last pause event with calculated duration
                lastPause.put("duration", pauseDuration)
                pauseHistory.put(lastPauseIndex, lastPause)

                // Calculate total paused time
                val totalPausedTime = (job.pausedTime ?: 0L) + pauseDuration

                // Update job with new pause history and total paused time
                val updatedJob = job.copy(
                    status = "BUSY",
                    pauseHistory = pauseHistory.toString(),
                    pausedTime = totalPausedTime,
                    updatedAt = resumeTimestamp
                )
                jobCardDao.updateJob(updatedJob)
            } else {
                // No pause history, just resume normally
                jobCardDao.resumeJob(jobId, resumeTimestamp)
            }

            currentTechnicianDao.setCurrentActiveJob(jobId)
            true
        } catch (e: Exception) {
            android.util.Log.e("JobCardRepository", "Error resuming job", e)
            false
        }
    }

    override suspend fun completeJob(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String?,
        resourcesUsed: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?
    ): Boolean = withContext(ioDispatcher) {
        try {
            jobCardDao.completeJob(
                id = jobId,
                endTime = System.currentTimeMillis(),
                workPerformed = workPerformed,
                notes = technicianNotes,
                resourcesUsed = resourcesUsed,
                requiresFollowUp = requiresFollowUp,
                followUpNotes = followUpNotes,
                timestamp = System.currentTimeMillis()
            )

            // Clear technician's current active job
            currentTechnicianDao.setCurrentActiveJob(null)

            // Increment completed jobs counter
            currentTechnicianDao.incrementTodayCompletedJobs()

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addCustomerSignature(jobId: Int, signature: String): Boolean =
        withContext(ioDispatcher) {
            try {
                jobCardDao.addCustomerSignature(jobId, signature, System.currentTimeMillis())
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addBeforePhotos(jobId: Int, photos: List<String>): Boolean =
        withContext(ioDispatcher) {
            try {
                val photosJson = photos.joinToString(",", "[\"", "\"]") { it }
                jobCardDao.addBeforePhotos(jobId, photosJson, System.currentTimeMillis())
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addAfterPhotos(jobId: Int, photos: List<String>): Boolean =
        withContext(ioDispatcher) {
            try {
                val photosJson = photos.joinToString(",", "[\"", "\"]") { it }
                jobCardDao.addAfterPhotos(jobId, photosJson, System.currentTimeMillis())
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addOtherPhotos(jobId: Int, photos: List<String>): Boolean =
        withContext(ioDispatcher) {
            try {
                val photosJson = photos.joinToString(",", "[\"", "\"]") { it }
                jobCardDao.addOtherPhotos(jobId, photosJson, System.currentTimeMillis())
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addPhotoToJob(jobId: Int, photoUri: String, category: String, notes: String?): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false

                // Get existing photos for the category and add new photo with notes
                val updatedPhotos = when (category.uppercase()) {
                    "BEFORE" -> {
                        val existingPhotos = parsePhotosJson(job.beforePhotos)
                        val newPhoto = org.json.JSONObject().apply {
                            put("uri", photoUri)
                            if (!notes.isNullOrEmpty()) put("notes", notes)
                        }
                        val photosArray = org.json.JSONArray(existingPhotos).apply { put(newPhoto) }
                        jobCardDao.addBeforePhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                    "AFTER" -> {
                        val existingPhotos = parsePhotosJson(job.afterPhotos)
                        val newPhoto = org.json.JSONObject().apply {
                            put("uri", photoUri)
                            if (!notes.isNullOrEmpty()) put("notes", notes)
                        }
                        val photosArray = org.json.JSONArray(existingPhotos).apply { put(newPhoto) }
                        jobCardDao.addAfterPhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                    else -> {
                        val existingPhotos = parsePhotosJson(job.otherPhotos)
                        val newPhoto = org.json.JSONObject().apply {
                            put("uri", photoUri)
                            if (!notes.isNullOrEmpty()) put("notes", notes)
                        }
                        val photosArray = org.json.JSONArray(existingPhotos).apply { put(newPhoto) }
                        jobCardDao.addOtherPhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error adding photo", e)
                false
            }
        }

    // Helper function to parse photo JSON - handles both old format (string array) and new format (object array)
    private fun parsePhotosJson(photosJson: String?): String {
        if (photosJson == null || photosJson == "null") return "[]"

        return try {
            val jsonArray = org.json.JSONArray(photosJson)
            val resultArray = org.json.JSONArray()

            for (i in 0 until jsonArray.length()) {
                when (val item = jsonArray.get(i)) {
                    is String -> {
                        // Old format: convert string to object
                        resultArray.put(org.json.JSONObject().apply {
                            put("uri", item)
                        })
                    }
                    is org.json.JSONObject -> {
                        // New format: keep as is
                        resultArray.put(item)
                    }
                }
            }
            resultArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    // Helper function to parse photos to List<PhotoWithNotes>
    private fun parsePhotosToList(photosJson: String?): List<com.metroair.job_card_management.domain.model.PhotoWithNotes>? {
        if (photosJson == null || photosJson == "null") return null

        return try {
            val jsonArray = org.json.JSONArray(photosJson)
            val photosList = mutableListOf<com.metroair.job_card_management.domain.model.PhotoWithNotes>()

            for (i in 0 until jsonArray.length()) {
                when (val item = jsonArray.get(i)) {
                    is String -> {
                        // Old format: string URI without notes
                        photosList.add(com.metroair.job_card_management.domain.model.PhotoWithNotes(uri = item))
                    }
                    is org.json.JSONObject -> {
                        // New format: object with uri and optional notes
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

    override suspend fun removePhoto(jobId: Int, photoUri: String, category: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false

                // Get existing photos for the category and remove the photo
                when (category.uppercase()) {
                    "BEFORE" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.beforePhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") != photoUri) {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addBeforePhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                    "AFTER" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.afterPhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") != photoUri) {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addAfterPhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                    else -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.otherPhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") != photoUri) {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addOtherPhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error removing photo", e)
                false
            }
        }

    override suspend fun retagPhoto(jobId: Int, photoUri: String, fromCategory: String, toCategory: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false

                // Find and remove photo from old category (preserve notes)
                var photoObject: org.json.JSONObject? = null

                when (fromCategory.uppercase()) {
                    "BEFORE" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.beforePhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                photoObject = photo
                            } else {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addBeforePhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                    "AFTER" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.afterPhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                photoObject = photo
                            } else {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addAfterPhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                    else -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.otherPhotos))
                        val updatedArray = org.json.JSONArray()
                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                photoObject = photo
                            } else {
                                updatedArray.put(photo)
                            }
                        }
                        val photosJson = if (updatedArray.length() == 0) null else updatedArray.toString()
                        jobCardDao.addOtherPhotos(jobId, photosJson, System.currentTimeMillis())
                    }
                }

                if (photoObject == null) return@withContext false

                // Add to new category with preserved notes
                val updatedJob = jobCardDao.getJobById(jobId) ?: return@withContext false
                when (toCategory.uppercase()) {
                    "BEFORE" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(updatedJob.beforePhotos))
                        photosArray.put(photoObject)
                        jobCardDao.addBeforePhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                    "AFTER" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(updatedJob.afterPhotos))
                        photosArray.put(photoObject)
                        jobCardDao.addAfterPhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                    else -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(updatedJob.otherPhotos))
                        photosArray.put(photoObject)
                        jobCardDao.addOtherPhotos(jobId, photosArray.toString(), System.currentTimeMillis())
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error retagging photo", e)
                false
            }
        }

    override suspend fun updatePhotoNotes(jobId: Int, photoUri: String, category: String, notes: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false

                // Find and update the photo with new notes
                when (category.uppercase()) {
                    "BEFORE" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.beforePhotos))
                        val updatedArray = org.json.JSONArray()

                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                // Update notes for this photo
                                photo.put("notes", notes)
                            }
                            updatedArray.put(photo)
                        }

                        jobCardDao.addBeforePhotos(jobId, updatedArray.toString(), System.currentTimeMillis())
                    }
                    "AFTER" -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.afterPhotos))
                        val updatedArray = org.json.JSONArray()

                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                photo.put("notes", notes)
                            }
                            updatedArray.put(photo)
                        }

                        jobCardDao.addAfterPhotos(jobId, updatedArray.toString(), System.currentTimeMillis())
                    }
                    else -> {
                        val photosArray = org.json.JSONArray(parsePhotosJson(job.otherPhotos))
                        val updatedArray = org.json.JSONArray()

                        for (i in 0 until photosArray.length()) {
                            val photo = photosArray.getJSONObject(i)
                            if (photo.getString("uri") == photoUri) {
                                photo.put("notes", notes)
                            }
                            updatedArray.put(photo)
                        }

                        jobCardDao.addOtherPhotos(jobId, updatedArray.toString(), System.currentTimeMillis())
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error updating photo notes", e)
                false
            }
        }

    override suspend fun updateJobResources(jobId: Int, resourcesJson: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val job = jobCardDao.getJobById(jobId) ?: return@withContext false
                val updatedJob = job.copy(
                    resourcesUsed = resourcesJson,
                    updatedAt = System.currentTimeMillis()
                )
                jobCardDao.updateJob(updatedJob)
                true
            } catch (e: Exception) {
                android.util.Log.e("JobCardRepository", "Error updating job resources", e)
                false
            }
        }

    // ========== STATS ==========

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

    // ========== MAPPING ==========

    private fun JobCardEntity.toDomainModel(): JobCard {
        return JobCard(
            id = id,
            jobNumber = jobNumber,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            title = title,
            description = description,
            jobType = try { JobType.valueOf(jobType) } catch (e: Exception) { JobType.SERVICE },
            status = try { JobStatus.valueOf(status) } catch (e: Exception) { JobStatus.PENDING },
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            serviceAddress = serviceAddress,
            latitude = latitude,
            longitude = longitude,
            estimatedDuration = estimatedDuration,
            acceptedAt = acceptedAt,
            enRouteStartTime = enRouteStartTime,
            startTime = startTime,
            endTime = endTime,
            pausedTime = pausedTime,
            pauseHistory = pauseHistory,
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
            isSynced = isSynced,
            isMyJob = isMyJob,
            acceptedByTechnician = acceptedByTechnician
        )
    }
}