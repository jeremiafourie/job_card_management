package com.metroair.job_card_management.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.ResourceRepository
import com.metroair.job_card_management.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val allResources: StateFlow<List<Resource>> = resourceRepository.getAllResources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get unique categories from all resources
    val categories: StateFlow<List<String>> = allResources
        .map { resources ->
            resources.map { it.category }.distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered resources based on search and category
    val filteredResources: StateFlow<List<Resource>> = combine(
        allResources,
        _searchQuery,
        _selectedCategory
    ) { resources, query, category ->
        resources.filter { resource ->
            val matchesSearch = query.isBlank() ||
                resource.itemName.contains(query, ignoreCase = true) ||
                resource.itemCode.contains(query, ignoreCase = true) ||
                resource.category.contains(query, ignoreCase = true)

            val matchesCategory = category == null || resource.category == category

            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
    }

    fun useResource(resourceId: Int, quantity: Int) {
        viewModelScope.launch {
            resourceRepository.useResource(resourceId, quantity)
        }
    }

    fun checkoutTool(resourceId: Int, itemName: String, itemCode: String) {
        viewModelScope.launch {
            resourceRepository.checkoutTool(resourceId, itemName, itemCode)
        }
    }
}