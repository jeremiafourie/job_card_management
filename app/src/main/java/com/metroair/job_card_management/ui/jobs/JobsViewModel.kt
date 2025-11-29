package com.metroair.job_card_management.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class JobsViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    val selectedStatus: StateFlow<JobStatus?> = _selectedStatus.asStateFlow()

    private val _isActiveFilter = MutableStateFlow(false)
    val isActiveFilter: StateFlow<Boolean> = _isActiveFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // All jobs (single-tech app)
    val jobs: StateFlow<List<JobCard>> = jobCardRepository.getJobs()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                jobCardRepository.searchJobs(query)
            }
        }

    val filteredJobs: StateFlow<List<JobCard>> =
        combine(
            jobs,
            searchResults,
            _selectedStatus,
            _isActiveFilter
        ) { allJobs, search, statusFilter, activeFilter ->
            var base = if (_searchQuery.value.isNotBlank()) search else allJobs

            statusFilter?.let { status ->
                base = base.filter { it.status == status }
            }

            if (activeFilter) {
                base = base.filter { it.status == JobStatus.BUSY || it.status == JobStatus.EN_ROUTE || it.status == JobStatus.PAUSED }
            }
            base
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun filterByStatus(status: JobStatus?) {
        _selectedStatus.value = status
        _isActiveFilter.value = false
    }

    fun filterByActive() {
        _isActiveFilter.value = true
        _selectedStatus.value = null
    }

    fun clearFilters() {
        _selectedStatus.value = null
        _isActiveFilter.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun startJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.startJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot start this job. You must first pause or complete your current active job."
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

    fun resumeJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.resumeJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot resume this job. You must first pause or complete your current active job."
            }
        }
    }

    fun enRouteJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.enRouteJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot set job to en route. You must first pause or complete your current active job."
            }
        }
    }

    fun cancelJob(jobId: Int, reason: String) {
        viewModelScope.launch {
            val success = jobCardRepository.cancelJob(jobId, reason)
            _uiMessage.value = if (success) "Job cancelled" else "Failed to cancel job"
        }
    }

    suspend fun createJob(
        jobNumber: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        title: String,
        description: String?,
        jobType: com.metroair.job_card_management.domain.model.JobType,
        scheduledDate: String,
        scheduledTime: String?
    ): Int? = jobCardRepository.createJob(
        jobNumber = jobNumber,
        customerName = customerName,
        customerPhone = customerPhone,
        customerAddress = customerAddress,
        title = title,
        description = description,
        jobType = jobType,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime
    )
}
