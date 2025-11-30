package com.metroair.job_card_management.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.FixedRepository
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.Fixed
import com.metroair.job_card_management.domain.model.FixedCheckout
import com.metroair.job_card_management.domain.model.FixedType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val fixedRepository: FixedRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedFixedType = MutableStateFlow<FixedType?>(null)
    val selectedFixedType: StateFlow<FixedType?> = _selectedFixedType.asStateFlow()

    private val _viewType = MutableStateFlow(AssetViewType.INVENTORY)
    val viewType: StateFlow<AssetViewType> = _viewType.asStateFlow()

    // Inventory Assets (consumables/inventory)
    private val allAssets: StateFlow<List<Asset>> = assetRepository.getAllAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Fixed Assets (durable items)
    private val allFixed: StateFlow<List<Fixed>> = fixedRepository.getAllFixed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get unique categories from inventory assets
    val categories: StateFlow<List<String>> = allAssets
        .map { assets ->
            assets.map { it.category }.distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered inventory assets based on search and category
    val filteredAssets: StateFlow<List<Asset>> = combine(
        allAssets,
        _searchQuery,
        _selectedCategory,
        _viewType
    ) { assets, query, category, viewType ->
        if (viewType == AssetViewType.FIXED) {
            emptyList() // Don't show inventory assets when in fixed view
        } else {
            assets.filter { asset ->
                val matchesSearch = query.isBlank() ||
                    asset.itemName.contains(query, ignoreCase = true) ||
                    asset.itemCode.contains(query, ignoreCase = true) ||
                    asset.category.contains(query, ignoreCase = true)

                val matchesCategory = category == null || asset.category == category

                // Filter out tools from inventory view (they should be in fixed assets)
                val isNotTool = !asset.category.equals("Tools", ignoreCase = true)

                matchesSearch && matchesCategory && isNotTool
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered fixed assets based on search and type
    val filteredFixed: StateFlow<List<Fixed>> = combine(
        allFixed,
        _searchQuery,
        _selectedFixedType,
        _viewType
    ) { fixedAssets, query, fixedType, viewType ->
        if (viewType == AssetViewType.INVENTORY) {
            emptyList() // Don't show fixed assets when in inventory view
        } else {
            fixedAssets.filter { fixed ->
                val matchesSearch = query.isBlank() ||
                    fixed.fixedName.contains(query, ignoreCase = true) ||
                    fixed.fixedCode.contains(query, ignoreCase = true) ||
                    fixed.serialNumber?.contains(query, ignoreCase = true) == true ||
                    fixed.manufacturer?.contains(query, ignoreCase = true) == true ||
                    fixed.model?.contains(query, ignoreCase = true) == true

                val matchesType = fixedType == null || fixed.fixedType == fixedType

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

    fun selectFixedType(type: FixedType?) {
        _selectedFixedType.value = type
    }

    fun setViewType(type: AssetViewType) {
        _viewType.value = type
        // Reset filters when changing view type
        when (type) {
            AssetViewType.FIXED -> {
                _selectedCategory.value = null
            }
            AssetViewType.INVENTORY -> {
                _selectedFixedType.value = null
            }
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedFixedType.value = null
    }

    // Inventory Asset operations
    fun useAsset(assetId: Int, quantity: Double) {
        viewModelScope.launch {
            assetRepository.useAsset(assetId, quantity)
        }
    }

    // Fixed Asset operations
    suspend fun checkoutFixed(
        fixedId: Int,
        reason: String,
        jobId: Int? = null,
        condition: String = "Good",
        notes: String? = null
    ): Boolean {
        return fixedRepository.checkoutFixed(
            fixedId = fixedId,
            reason = reason,
            jobId = jobId,
            condition = condition,
            notes = notes
        )
    }

    fun returnFixed(
        checkoutId: Int,
        condition: String = "Good",
        notes: String? = null
    ) {
        viewModelScope.launch {
            fixedRepository.returnFixed(
                checkoutId = checkoutId,
                condition = condition,
                notes = notes
            )
        }
    }

    fun getFixedHistory(fixedId: Int): Flow<List<FixedCheckout>> {
        return fixedRepository.getFixedHistory(fixedId)
    }

    suspend fun searchFixed(query: String, limit: Int = 5): List<Fixed> {
        return fixedRepository.searchFixed(query, limit)
    }

    suspend fun searchAssets(query: String, limit: Int = 5): List<Asset> {
        return allAssets.value
            .filter { asset ->
                asset.itemName.contains(query, ignoreCase = true) ||
                asset.itemCode.contains(query, ignoreCase = true) ||
                asset.category.contains(query, ignoreCase = true)
            }
            .take(limit)
    }

    // Get active checkouts
    val activeCheckouts: StateFlow<List<FixedCheckout>> =
        fixedRepository.getActiveCheckouts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
