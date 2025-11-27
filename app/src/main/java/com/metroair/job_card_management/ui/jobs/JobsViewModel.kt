package com.metroair.job_card_management.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobCardRepository: JobCardRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    val selectedStatus: StateFlow<JobStatus?> = _selectedStatus.asStateFlow()

    private val _isActiveFilter = MutableStateFlow(false)
    val isActiveFilter: StateFlow<Boolean> = _isActiveFilter.asStateFlow()

    init {
        // Read status filter from navigation arguments
        val statusParam = savedStateHandle.get<String>("status")
        if (statusParam != null && statusParam != "null") {
            when (statusParam) {
                "ACTIVE" -> _isActiveFilter.value = true
                else -> {
                    try {
                        _selectedStatus.value = JobStatus.valueOf(statusParam)
                    } catch (e: IllegalArgumentException) {
                        // Invalid status, keep as null
                    }
                }
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // My jobs (assigned to current technician)
    val myJobs: StateFlow<List<JobCard>> = jobCardRepository.getMyJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Available jobs (unassigned)
    val availableJobs: StateFlow<List<JobCard>> = jobCardRepository.getAvailableJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results
    private val searchResults: Flow<List<JobCard>> = _searchQuery
        .debounce(300) // Wait 300ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                jobCardRepository.searchJobs(query)
            }
        }

    // Filtered jobs based on selection
    val filteredJobs: StateFlow<List<JobCard>> =
        combine(
            myJobs,
            availableJobs,
            _selectedStatus,
            _searchQuery
        ) { myJobsList, availableJobsList, statusFilter, query ->
            // Combine all jobs
            val allJobs = myJobsList + availableJobsList

            // Apply filters
            var baseList = if (query.isNotBlank()) {
                // Search filter
                allJobs.filter { job ->
                    job.customerName.contains(query, ignoreCase = true) ||
                    job.title.contains(query, ignoreCase = true) ||
                    job.jobNumber.contains(query, ignoreCase = true) ||
                    (job.serviceAddress?.contains(query, ignoreCase = true) == true)
                }
            } else {
                allJobs
            }

            // Apply status filter
            if (statusFilter != null) {
                baseList = baseList.filter { it.status == statusFilter }
            }

            baseList
        }.combine(_isActiveFilter) { jobs, activeFilter ->
            if (activeFilter) {
                jobs.filter { it.status == JobStatus.BUSY || it.status == JobStatus.EN_ROUTE || it.status == JobStatus.PAUSED }
            } else {
                jobs
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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

    fun resumeJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.resumeJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot resume this job. You must first pause or complete your current busy job."
            }
        }
    }

    fun enRouteJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.enRouteJob(jobId)
            if (!success) {
                _uiMessage.value = "Cannot set job to en route. You must first pause or complete your current busy job."
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

    fun acceptJob(jobId: Int) {
        viewModelScope.launch {
            val success = jobCardRepository.acceptJob(jobId)
            if (success) {
                _uiMessage.value = "Job accepted successfully"
            } else {
                _uiMessage.value = "Failed to accept job"
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

    fun clearMessage() {
        _uiMessage.value = null
    }
}