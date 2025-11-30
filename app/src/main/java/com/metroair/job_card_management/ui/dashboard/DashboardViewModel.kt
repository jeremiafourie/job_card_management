package com.metroair.job_card_management.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.FixedRepository
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.domain.model.DashboardStats
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository,
    private val assetRepository: AssetRepository,
    private val fixedRepository: FixedRepository
) : ViewModel() {

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    val jobs: StateFlow<List<JobCard>> = jobCardRepository.getJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun List<JobCard>.latestActive(): JobCard? {
        return this
            .filter { it.status == JobStatus.BUSY || it.status == JobStatus.EN_ROUTE || it.status == JobStatus.PAUSED }
            .sortedWith(compareByDescending<JobCard> {
                when (it.status) {
                    JobStatus.BUSY -> 3
                    JobStatus.EN_ROUTE -> 2
                    JobStatus.PAUSED -> 1
                    else -> 0
                }
            }.thenByDescending { it.id })
            .firstOrNull()
    }

    val currentActiveJob: StateFlow<JobCard?> = jobs
        .map { list -> list.latestActive() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pausedJobs: StateFlow<List<JobCard>> = jobs
        .map { list -> list.filter { it.status == JobStatus.PAUSED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val awaitingJobs: StateFlow<List<JobCard>> = jobs
        .map { list -> list.filter { it.status == JobStatus.AWAITING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingJobs: StateFlow<List<JobCard>> = jobs
        .map { list -> list.filter { it.status == JobStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableAssets = assetRepository.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableFixed = fixedRepository.getAvailableFixed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<DashboardStats> = jobs
        .map { list ->
            val awaiting = list.count { it.status == JobStatus.AWAITING }
            val pending = list.count { it.status == JobStatus.PENDING }
            val active = list.count { it.status == JobStatus.BUSY || it.status == JobStatus.EN_ROUTE || it.status == JobStatus.PAUSED }
            val available = list.count { it.status == JobStatus.AVAILABLE }
            DashboardStats(
                availableJobs = available,
                awaitingJobs = awaiting,
                activeJob = active,
                pending = pending
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun startJob(jobId: Int) = viewModelScope.launch { jobCardRepository.startJob(jobId) }
    fun pauseJob(jobId: Int, reason: String) = viewModelScope.launch { jobCardRepository.pauseJob(jobId, reason) }
    fun resumeJob(jobId: Int) = viewModelScope.launch { jobCardRepository.resumeJob(jobId) }
    fun enRouteJob(jobId: Int) = viewModelScope.launch { jobCardRepository.enRouteJob(jobId) }
    fun cancelJob(jobId: Int, reason: String) = viewModelScope.launch { jobCardRepository.cancelJob(jobId, reason) }
    fun acceptJob(jobId: Int) = viewModelScope.launch { jobCardRepository.startJob(jobId) }

    fun addPhotoToJob(jobId: Int, uri: String, category: String, notes: String?) = viewModelScope.launch {
        jobCardRepository.addPhotoToJob(jobId, uri, category, notes)
    }

    fun addAssetToJob(jobId: Int, itemName: String, itemCode: String, quantity: Double) = viewModelScope.launch {
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

    fun checkoutFixedAssetToJob(jobId: Int, fixedId: Int, reason: String) = viewModelScope.launch {
        fixedRepository.checkoutFixed(
            fixedId = fixedId,
            reason = reason,
            jobId = jobId,
            condition = "Good",
            notes = null
        )
    }
}
