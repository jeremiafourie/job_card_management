package com.metroair.job_card_management.ui.jobdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.FixedRepository
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobResource
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.domain.model.FixedCheckout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository,
    private val assetRepository: AssetRepository,
    private val fixedRepository: FixedRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = checkNotNull(savedStateHandle.get<Int>("jobId"))

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    val jobCard: StateFlow<JobCard?> = jobCardRepository.getJobCardById(jobId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val availableAssets = assetRepository.getAllAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableFixed = fixedRepository.getAvailableFixed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fixedCheckouts = fixedRepository.getCheckoutsForJob(jobId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            jobCard.filterNotNull().collect { job ->
                _uiState.update { state ->
                    state.copy(
                        workPerformed = job.workPerformed ?: "",
                        issuesEncountered = job.issuesEncountered ?: "",
                        requiresFollowUp = job.requiresFollowUp,
                        followUpNotes = job.followUpNotes ?: "",
                        customerSignature = job.customerSignature,
                        resources = parseResourcesJson(job.resourcesUsed)
                    )
                }
            }
        }
    }

    fun updateWorkPerformed(value: String) {
        _uiState.update { it.copy(workPerformed = value) }
    }

    fun updateIssuesEncountered(value: String) {
        _uiState.update { it.copy(issuesEncountered = value) }
    }

    fun updateRequiresFollowUp(value: Boolean) {
        _uiState.update { it.copy(requiresFollowUp = value) }
    }

    fun updateFollowUpNotes(value: String) {
        _uiState.update { it.copy(followUpNotes = value) }
    }

    fun setCustomerSignature(signature: String) {
        _uiState.update { it.copy(customerSignature = signature) }
    }

    fun acceptJob() {
        viewModelScope.launch {
            val success = jobCardRepository.acceptJob(jobId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to accept job") }
            } else {
                _uiState.update { it.copy(successMessage = "Job accepted successfully") }
            }
        }
    }

    fun startJob() {
        viewModelScope.launch {
            val success = jobCardRepository.startJob(jobId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Cannot start job. You may already have an active job.") }
            }
        }
    }

    fun pauseJob(reason: String) {
        viewModelScope.launch {
            val success = jobCardRepository.pauseJob(jobId, reason)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to pause job") }
            }
        }
    }

    fun resumeJob() {
        viewModelScope.launch {
            val success = jobCardRepository.resumeJob(jobId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Cannot resume job. You may already have an active job.") }
            }
        }
    }

    fun saveJobDetails() {
        viewModelScope.launch {
            val state = _uiState.value
            jobCardRepository.updateJobDetails(
                jobId = jobId,
                workPerformed = state.workPerformed,
                technicianNotes = null,
                issuesEncountered = state.issuesEncountered,
                customerSignature = state.customerSignature,
                requiresFollowUp = state.requiresFollowUp,
                followUpNotes = state.followUpNotes
            )
            _uiState.update { it.copy(successMessage = "Job details saved successfully") }
        }
    }

    fun completeJob() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validate required fields
            if (state.workPerformed.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please describe the work performed") }
                return@launch
            }

            if (state.customerSignature == null) {
                _uiState.update { it.copy(errorMessage = "Customer signature is required") }
                return@launch
            }

            val success = jobCardRepository.completeJob(
                jobId = jobId,
                workPerformed = state.workPerformed,
                technicianNotes = null,
                resourcesUsed = if (state.resources.isNotEmpty()) resourcesToJson(state.resources) else null,
                requiresFollowUp = state.requiresFollowUp,
                followUpNotes = state.followUpNotes
            )

            if (success) {
                // Return all fixed assets checked out for this job
                returnAllFixedAssets()
                _uiState.update { it.copy(successMessage = "Job completed successfully", isCompleted = true) }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to complete job") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // Resource management
    fun addResource(itemName: String, itemCode: String, quantity: Double) {
        _uiState.update { state ->
            val currentResources = state.resources.toMutableList()
            // Check if resource already exists, if so update quantity
            val existingIndex = currentResources.indexOfFirst { it.itemCode == itemCode }
            if (existingIndex >= 0) {
                val existing = currentResources[existingIndex]
                currentResources[existingIndex] = existing.copy(
                    quantity = existing.quantity + quantity
                )
            } else {
                // Find unit from available resources
                viewModelScope.launch {
                    assetRepository.getAllAssets().first().find { it.itemCode == itemCode }?.let { resource ->
                        currentResources.add(
                            JobResource(
                                resourceId = resource.id,
                                itemName = itemName,
                                itemCode = itemCode,
                                quantity = quantity,
                                unit = resource.unitOfMeasure
                            )
                        )
                        _uiState.update { it.copy(resources = currentResources) }
                    }
                }
                return@update state
            }
            state.copy(resources = currentResources)
        }
    }

    fun removeResource(itemCode: String) {
        _uiState.update { state ->
            state.copy(resources = state.resources.filter { it.itemCode != itemCode })
        }
    }

    // Fixed asset management
    fun checkoutFixedAsset(fixedId: Int, reason: String) {
        viewModelScope.launch {
            val jobNumber = jobCard.value?.jobNumber ?: "JOB$jobId"
            val success = fixedRepository.checkoutFixed(
                fixedId = fixedId,
                reason = reason,
                jobId = jobId,
                condition = "Good",
                notes = "Checked out for $jobNumber"
            )

            if (success) {
                _uiState.update { it.copy(successMessage = "Fixed asset checked out successfully") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to checkout fixed asset") }
            }
        }
    }

    private suspend fun returnAllFixedAssets() {
        // Return all fixed assets checked out for this job
        fixedCheckouts.value.forEach { checkout ->
            if (checkout.returnTime == null) {
                fixedRepository.returnFixed(
                    checkoutId = checkout.id,
                    condition = "Good",
                    notes = "Automatically returned on job completion"
                )
            }
        }
    }

    // Photo management
    fun addPhoto(jobId: Int, photoUri: android.net.Uri, category: com.metroair.job_card_management.ui.components.PhotoCategory, notes: String?) {
        viewModelScope.launch {
            val success = jobCardRepository.addPhotoToJob(jobId, photoUri.toString(), category.name, notes)
            if (success) {
                _uiState.update { it.copy(successMessage = "Photo added successfully") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to add photo") }
            }
        }
    }

    fun removePhoto(photoUri: String, category: String) {
        viewModelScope.launch {
            val success = jobCardRepository.removePhoto(jobId, photoUri, category)
            if (success) {
                _uiState.update { it.copy(successMessage = "Photo removed successfully") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to remove photo") }
            }
        }
    }

    fun retagPhoto(photoUri: String, fromCategory: String, toCategory: String) {
        viewModelScope.launch {
            val success = jobCardRepository.retagPhoto(jobId, photoUri, fromCategory, toCategory)
            if (success) {
                _uiState.update { it.copy(successMessage = "Photo category updated successfully") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to update photo category") }
            }
        }
    }

    fun updatePhotoNotes(photoUri: String, category: String, notes: String) {
        viewModelScope.launch {
            jobCardRepository.updatePhotoNotes(jobId, photoUri, category, notes)
        }
    }

    private fun parseResourcesJson(jsonString: String?): List<JobResource> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                JobResource(
                    resourceId = jsonObject.optInt("id", 0),
                    itemName = jsonObject.optString("name", ""),
                    itemCode = jsonObject.optString("code", ""),
                    quantity = jsonObject.optDouble("quantity", 0.0),
                    unit = jsonObject.optString("unit", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun resourcesToJson(resources: List<JobResource>): String {
        val jsonArray = JSONArray()
        resources.forEach { resource ->
            val jsonObject = JSONObject().apply {
                put("id", resource.resourceId)
                put("name", resource.itemName)
                put("code", resource.itemCode)
                put("quantity", resource.quantity)
                put("unit", resource.unit)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}

data class JobDetailUiState(
    val workPerformed: String = "",
    val issuesEncountered: String = "",
    val customerSignature: String? = null,
    val requiresFollowUp: Boolean = false,
    val followUpNotes: String = "",
    val resources: List<JobResource> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isCompleted: Boolean = false
)