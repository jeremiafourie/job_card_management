package com.metroair.job_card_management.ui.jobdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.FixedRepository
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.data.repository.PurchaseRepository
import com.metroair.job_card_management.domain.model.FixedCheckout
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.domain.model.InventoryUsage
import com.metroair.job_card_management.domain.model.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository,
    private val assetRepository: AssetRepository,
    private val fixedRepository: FixedRepository,
    private val purchaseRepository: PurchaseRepository,
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableFixed = fixedRepository.getAvailableFixed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixedCheckouts = fixedRepository.getCheckoutsForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = purchaseRepository.getPurchasesForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryUsage: StateFlow<List<InventoryUsage>> = assetRepository.getUsageForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            jobCard.filterNotNull().collect { job ->
                _uiState.update { state ->
                    state.copy(
                        workPerformed = job.workPerformed ?: "",
                        issuesEncountered = job.issuesEncountered ?: "",
                        technicianNotes = job.technicianNotes ?: "",
                        requiresFollowUp = job.requiresFollowUp,
                        followUpNotes = job.followUpNotes ?: "",
                        customerSignature = job.customerSignature
                    )
                }
            }
        }
    }

    fun updateWorkPerformed(value: String) { _uiState.update { it.copy(workPerformed = value) }; scheduleAutoSave() }
    fun updateIssuesEncountered(value: String) { _uiState.update { it.copy(issuesEncountered = value) }; scheduleAutoSave() }
    fun updateTechnicianNotes(value: String) { _uiState.update { it.copy(technicianNotes = value) }; scheduleAutoSave() }
    fun updateRequiresFollowUp(value: Boolean) { _uiState.update { it.copy(requiresFollowUp = value) }; scheduleAutoSave() }
    fun updateFollowUpNotes(value: String) { _uiState.update { it.copy(followUpNotes = value) }; scheduleAutoSave() }
    fun setCustomerSignature(signature: String) = _uiState.update { it.copy(customerSignature = signature) }

    fun startJob() { viewModelScope.launch { jobCardRepository.startJob(jobId) } }
    fun pauseJob(reason: String) { viewModelScope.launch { jobCardRepository.pauseJob(jobId, reason) } }
    fun resumeJob() { viewModelScope.launch { jobCardRepository.resumeJob(jobId) } }
    fun enRouteJob() { viewModelScope.launch { jobCardRepository.enRouteJob(jobId) } }
    fun cancelJob(reason: String) { viewModelScope.launch { jobCardRepository.cancelJob(jobId, reason) } }

    fun completeJob() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.workPerformed.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please describe the work performed") }
                return@launch
            }
            val success = jobCardRepository.completeJob(
                jobId = jobId,
                workPerformed = state.workPerformed,
                technicianNotes = state.technicianNotes.ifBlank { null },
                requiresFollowUp = state.requiresFollowUp,
                followUpNotes = state.followUpNotes
            )
            if (success) {
                _uiState.update { it.copy(successMessage = "Job completed successfully", isCompleted = true) }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to complete job") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(errorMessage = null, successMessage = null) } }

    fun addPurchase(vendor: String, total: Double, notes: String?, receiptUri: String?) {
        viewModelScope.launch {
            val success = purchaseRepository.addPurchase(jobId, vendor, total, notes, receiptUri)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to add purchase") }
            }
        }
    }

    fun replaceReceipt(purchaseId: Int, newUri: String, mimeType: String? = null) {
        viewModelScope.launch {
            val success = purchaseRepository.replaceReceiptForPurchase(purchaseId, newUri, mimeType)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to update receipt") }
            }
        }
    }

    fun removeReceipt(purchaseId: Int) {
        viewModelScope.launch {
            val success = purchaseRepository.removeReceiptForPurchase(purchaseId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to remove receipt") }
            }
        }
    }

    fun addPhoto(jobId: Int, uri: String, category: String, notes: String?) {
        viewModelScope.launch {
            val success = jobCardRepository.addPhotoToJob(jobId, uri, category, notes)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Unable to save photo") }
            }
        }
    }

    fun addInventoryUsage(jobId: Int, itemName: String, itemCode: String, quantity: Double) {
        viewModelScope.launch {
            val asset = availableAssets.value.firstOrNull { it.itemCode == itemCode } ?: return@launch
            assetRepository.recordUsage(
                jobId = jobId,
                assetId = asset.id,
                itemCode = asset.itemCode,
                itemName = asset.itemName.ifBlank { itemName },
                quantity = quantity,
                unit = asset.unitOfMeasure
            )
        }
    }

    fun checkoutFixedToJob(jobId: Int, fixedId: Int, reason: String) {
        viewModelScope.launch {
            fixedRepository.checkoutFixed(
                fixedId = fixedId,
                reason = reason,
                jobId = jobId,
                condition = "Good",
                notes = null
            )
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            val state = _uiState.value
            jobCardRepository.updateJobDetails(
                jobId = jobId,
                workPerformed = state.workPerformed,
                technicianNotes = state.technicianNotes.ifBlank { null },
                issuesEncountered = state.issuesEncountered.ifBlank { null },
                customerSignature = state.customerSignature,
                requiresFollowUp = state.requiresFollowUp,
                followUpNotes = state.followUpNotes.ifBlank { null }
            )
        }
    }

    fun saveJobDetails() {
        val state = _uiState.value
        viewModelScope.launch {
            jobCardRepository.updateJobDetails(
                jobId = jobId,
                workPerformed = state.workPerformed,
                technicianNotes = state.technicianNotes.ifBlank { null },
                issuesEncountered = state.issuesEncountered.ifBlank { null },
                customerSignature = state.customerSignature,
                requiresFollowUp = state.requiresFollowUp,
                followUpNotes = state.followUpNotes.ifBlank { null }
            )
        }
    }

    fun removePhoto(jobId: Int, uri: String, category: String) {
        viewModelScope.launch {
            val success = jobCardRepository.removePhoto(jobId, uri, category)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Unable to remove photo") }
            }
        }
    }

    fun retagPhoto(jobId: Int, uri: String, fromCategory: String, toCategory: String) {
        viewModelScope.launch {
            val success = jobCardRepository.retagPhoto(jobId, uri, fromCategory, toCategory)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Unable to move photo") }
            }
        }
    }

    fun updatePhotoNotes(jobId: Int, uri: String, category: String, notes: String) {
        viewModelScope.launch {
            val success = jobCardRepository.updatePhotoNotes(jobId, uri, category, notes)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Unable to update photo notes") }
            }
        }
    }
}

data class JobDetailUiState(
    val workPerformed: String = "",
    val issuesEncountered: String = "",
    val technicianNotes: String = "",
    val customerSignature: String? = null,
    val requiresFollowUp: Boolean = false,
    val followUpNotes: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isCompleted: Boolean = false
)
