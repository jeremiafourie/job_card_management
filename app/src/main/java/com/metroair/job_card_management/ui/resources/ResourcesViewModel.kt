package com.metroair.job_card_management.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.ResourceRepository
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.AssetCheckout
import com.metroair.job_card_management.domain.model.AssetType
import com.metroair.job_card_management.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedAssetType = MutableStateFlow<AssetType?>(null)
    val selectedAssetType: StateFlow<AssetType?> = _selectedAssetType.asStateFlow()

    private val _viewType = MutableStateFlow(ResourceViewType.ALL)
    val viewType: StateFlow<ResourceViewType> = _viewType.asStateFlow()

    // Resources/Inventory
    private val allResources: StateFlow<List<Resource>> = resourceRepository.getAllResources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Assets
    private val allAssets: StateFlow<List<Asset>> = assetRepository.getAllAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get unique categories from resources
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
        _selectedCategory,
        _viewType
    ) { resources, query, category, viewType ->
        if (viewType == ResourceViewType.ASSETS) {
            emptyList() // Don't show resources when in assets view
        } else {
            resources.filter { resource ->
                val matchesSearch = query.isBlank() ||
                    resource.itemName.contains(query, ignoreCase = true) ||
                    resource.itemCode.contains(query, ignoreCase = true) ||
                    resource.category.contains(query, ignoreCase = true)

                val matchesCategory = category == null || resource.category == category

                // Filter out tools from inventory view (they should be in assets)
                val isNotTool = !resource.category.equals("Tools", ignoreCase = true)

                matchesSearch && matchesCategory && isNotTool
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered assets based on search and type
    val filteredAssets: StateFlow<List<Asset>> = combine(
        allAssets,
        _searchQuery,
        _selectedAssetType,
        _viewType
    ) { assets, query, assetType, viewType ->
        if (viewType == ResourceViewType.INVENTORY) {
            emptyList() // Don't show assets when in inventory view
        } else {
            assets.filter { asset ->
                val matchesSearch = query.isBlank() ||
                    asset.assetName.contains(query, ignoreCase = true) ||
                    asset.assetCode.contains(query, ignoreCase = true) ||
                    asset.serialNumber?.contains(query, ignoreCase = true) == true ||
                    asset.manufacturer?.contains(query, ignoreCase = true) == true ||
                    asset.model?.contains(query, ignoreCase = true) == true

                val matchesType = assetType == null || asset.assetType == assetType

                matchesSearch && matchesType
            }
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

    fun selectAssetType(type: AssetType?) {
        _selectedAssetType.value = type
    }

    fun setViewType(type: ResourceViewType) {
        _viewType.value = type
        // Reset filters when changing view type
        when (type) {
            ResourceViewType.ASSETS -> {
                _selectedCategory.value = null
            }
            ResourceViewType.INVENTORY -> {
                _selectedAssetType.value = null
            }
            else -> {
                _selectedCategory.value = null
                _selectedAssetType.value = null
            }
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedAssetType.value = null
    }

    // Resource operations
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

    // Asset operations
    fun checkoutAsset(
        assetId: Int,
        reason: String,
        jobId: Int? = null,
        condition: String = "Good",
        notes: String? = null
    ) {
        viewModelScope.launch {
            assetRepository.checkoutAsset(
                assetId = assetId,
                reason = reason,
                jobId = jobId,
                condition = condition,
                notes = notes
            )
        }
    }

    fun returnAsset(
        checkoutId: Int,
        condition: String = "Good",
        notes: String? = null
    ) {
        viewModelScope.launch {
            assetRepository.returnAsset(
                checkoutId = checkoutId,
                condition = condition,
                notes = notes
            )
        }
    }

    fun getAssetHistory(assetId: Int): Flow<List<AssetCheckout>> {
        return assetRepository.getAssetHistory(assetId)
    }

    suspend fun searchAssets(query: String, limit: Int = 5): List<Asset> {
        return assetRepository.searchAssets(query, limit)
    }

    suspend fun searchResources(query: String, limit: Int = 5): List<Resource> {
        return allResources.value
            .filter { resource ->
                resource.itemName.contains(query, ignoreCase = true) ||
                resource.itemCode.contains(query, ignoreCase = true) ||
                resource.category.contains(query, ignoreCase = true)
            }
            .take(limit)
    }

    // Get active checkouts for current technician
    val activeCheckouts: StateFlow<List<AssetCheckout>> =
        assetRepository.getActiveCheckoutsForCurrentTechnician()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}