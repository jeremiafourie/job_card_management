package com.metroair.job_card_management.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.data.repository.ResourceRepository
import com.metroair.job_card_management.domain.model.DashboardStats
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository,
    private val currentTechnicianDao: CurrentTechnicianDao,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // My jobs (all jobs assigned to me)
    val myJobs: StateFlow<List<JobCard>> = jobCardRepository.getMyJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active job (BUSY, EN_ROUTE, or PAUSED jobs - prioritize BUSY first, then EN_ROUTE)
    val currentActiveJob: StateFlow<JobCard?> = myJobs
        .map { jobs ->
            jobs.firstOrNull { it.status == JobStatus.BUSY }
                ?: jobs.firstOrNull { it.status == JobStatus.EN_ROUTE }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Paused jobs
    val pausedJobs: StateFlow<List<JobCard>> = myJobs
        .map { jobs -> jobs.filter { it.status == JobStatus.PAUSED } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Awaiting jobs - assigned to technician but not yet accepted
    val awaitingJobs: StateFlow<List<JobCard>> = myJobs
        .map { jobs -> jobs.filter { it.isMyJob && !it.acceptedByTechnician && it.status == JobStatus.PENDING } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pending jobs - accepted by technician but not yet started
    val pendingJobs: StateFlow<List<JobCard>> = myJobs
        .map { jobs -> jobs.filter { it.isMyJob && it.acceptedByTechnician && it.status == JobStatus.PENDING } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Available jobs (unassigned jobs that can be claimed)
    val availableJobs: StateFlow<List<JobCard>> = jobCardRepository.getAvailableJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Available resources for adding to jobs
    val availableResources = resourceRepository.getAllResources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dashboard stats - focus on available jobs, awaiting jobs, active jobs, and pending jobs
    val stats: StateFlow<DashboardStats> = combine(
        myJobs,
        currentActiveJob,
        pausedJobs,
        availableJobs,
        awaitingJobs
    ) { myJobsList, activeJob, paused, available, awaiting ->
        DashboardStats(
            availableJobs = available.size,
            awaitingJobs = awaiting.size,
            activeJob = myJobsList.count { it.status == JobStatus.BUSY || it.status == JobStatus.EN_ROUTE || it.status == JobStatus.PAUSED },
            pending = myJobsList.count { it.status == JobStatus.PENDING && it.acceptedByTechnician }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    init {
        // Load today's completed count
        viewModelScope.launch {
            val completedCount = jobCardRepository.getTodayCompletedCount()
            // Update stats with completed count
        }
    }

    fun startJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.startJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot start this job. You must first pause or complete your current busy job."
            }
        }
    }

    fun pauseJob(jobId: Int, reason: String) {
        viewModelScope.launch {
            val success = jobCardRepository.pauseJob(jobId, reason)
            if (!success) {
                _uiMessage.value = "Failed to pause job"
            }
        }
    }

    fun cancelJob(jobId: Int, reason: String) {
        viewModelScope.launch {
            val success = jobCardRepository.cancelJob(jobId, reason)
            if (success) {
                _uiMessage.value = "Job cancelled"
            } else {
                _uiMessage.value = "Failed to cancel job"
            }
        }
    }

    fun resumeJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.resumeJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot resume this job. You must first pause or complete your current busy job."
            }
        }
    }

    fun claimJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.claimJob(jobId)
            if (success) {
                _uiMessage.value = "Job claimed successfully"
            } else {
                _uiMessage.value = "Failed to claim job"
            }
        }
    }

    fun completeJob(
        jobId: Int,
        workPerformed: String,
        technicianNotes: String? = null,
        resourcesUsed: String? = null,
        requiresFollowUp: Boolean = false,
        followUpNotes: String? = null
    ) {
        viewModelScope.launch {
            val success = jobCardRepository.completeJob(
                jobId = jobId,
                workPerformed = workPerformed,
                technicianNotes = technicianNotes,
                resourcesUsed = resourcesUsed,
                requiresFollowUp = requiresFollowUp,
                followUpNotes = followUpNotes
            )
            if (success) {
                _uiMessage.value = "Job completed successfully"
            } else {
                _uiMessage.value = "Failed to complete job"
            }
        }
    }

    fun acceptJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.acceptJob(jobId)
            if (success) {
                _uiMessage.value = "Job accepted"
            } else {
                _uiMessage.value = "Failed to accept job"
            }
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun addPhotoToJob(jobId: Int, photoUri: android.net.Uri, category: com.metroair.job_card_management.ui.components.PhotoCategory, notes: String?) {
        viewModelScope.launch {
            val success = jobCardRepository.addPhotoToJob(jobId, photoUri.toString(), category.name, notes)
            if (success) {
                _uiMessage.value = "Photo added successfully"
            } else {
                _uiMessage.value = "Failed to add photo"
            }
        }
    }

    fun addResourceToJob(jobId: Int, itemName: String, itemCode: String, quantity: Double) {
        viewModelScope.launch {
            try {
                val job = jobCardRepository.getJobById(jobId)
                val currentResourcesJson = job?.resourcesUsed ?: "[]"

                // Find the resource details from available resources to get id and unit
                val availableResource = availableResources.value.find { it.itemCode == itemCode }
                if (availableResource == null) {
                    _uiMessage.value = "Resource not found"
                    return@launch
                }

                // Parse existing resources
                val resourcesArray = org.json.JSONArray(currentResourcesJson)

                // Check if resource already exists
                var found = false
                for (i in 0 until resourcesArray.length()) {
                    val resource = resourcesArray.getJSONObject(i)
                    if (resource.getString("code") == itemCode) {
                        // Update quantity
                        val existingQty = resource.getDouble("quantity")
                        resource.put("quantity", existingQty + quantity)
                        found = true
                        break
                    }
                }

                // Add new resource if not found
                if (!found) {
                    val newResource = org.json.JSONObject()
                    newResource.put("id", availableResource.id)
                    newResource.put("name", itemName)
                    newResource.put("code", itemCode)
                    newResource.put("quantity", quantity)
                    newResource.put("unit", availableResource.unitOfMeasure)
                    resourcesArray.put(newResource)
                }

                // Update in repository
                val success = jobCardRepository.updateJobResources(jobId, resourcesArray.toString())
                if (success) {
                    _uiMessage.value = "Resource added successfully"
                } else {
                    _uiMessage.value = "Failed to add resource"
                }
            } catch (e: Exception) {
                _uiMessage.value = "Error adding resource: ${e.message}"
            }
        }
    }
}