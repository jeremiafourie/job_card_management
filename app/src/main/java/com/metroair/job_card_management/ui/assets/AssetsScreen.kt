package com.metroair.job_card_management.ui.assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.Fixed
import com.metroair.job_card_management.domain.model.FixedType
import kotlinx.coroutines.launch

enum class AssetViewType {
    ALL, CURRENT, FIXED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = hiltViewModel()
) {
    val currentAssets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val fixedAssets by viewModel.filteredFixed.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val selectedFixedType by viewModel.selectedFixedType.collectAsStateWithLifecycle()
    val lowStockAssets = currentAssets.filter { it.isLowStock }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedFixed by remember { mutableStateOf<Fixed?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showFixedDetailsDialog by remember { mutableStateOf(false) }
    var isCheckoutLoading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        when (viewType) {
                            AssetViewType.FIXED -> "Search fixed assets..."
                            AssetViewType.CURRENT -> "Search current items..."
                            else -> "Search all..."
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // View Type Filters (Current, Fixed, All)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewType == AssetViewType.ALL,
                        onClick = { viewModel.setViewType(AssetViewType.ALL) },
                        label = { Text("All") },
                        leadingIcon = if (viewType == AssetViewType.ALL) {
                            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = viewType == AssetViewType.CURRENT,
                        onClick = { viewModel.setViewType(AssetViewType.CURRENT) },
                        label = { Text("Current") },
                        leadingIcon = if (viewType == AssetViewType.CURRENT) {
                            { Icon(Icons.Default.Inventory2, contentDescription = null, Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = viewType == AssetViewType.FIXED,
                        onClick = { viewModel.setViewType(AssetViewType.FIXED) },
                        label = { Text("Fixed") },
                        leadingIcon = if (viewType == AssetViewType.FIXED) {
                            { Icon(Icons.Default.Build, contentDescription = null, Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            // Category Filter (only for current inventory)
            if (viewType != AssetViewType.FIXED) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            // Fixed Type Filter (only for fixed view)
            if (viewType == AssetViewType.FIXED) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FixedType.values().forEach { type ->
                        item {
                            FilterChip(
                                selected = selectedFixedType == type,
                                onClick = { viewModel.selectFixedType(type) },
                                label = { Text(type.name.replace('_', ' ')) }
                            )
                        }
                    }
                }
            }

            // Low Stock Warning (only for current inventory)
            if (viewType != AssetViewType.FIXED && lowStockAssets.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Low Stock Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${lowStockAssets.size} items below minimum stock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Main Content
            when (viewType) {
                AssetViewType.FIXED -> {
                    if (fixedAssets.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Build,
                            title = "No fixed assets found"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fixedAssets) { fixed ->
                                FixedCard(
                                    fixed = fixed,
                                    onCheckout = {
                                        selectedFixed = fixed
                                        showCheckoutDialog = true
                                    },
                                    onViewDetails = {
                                        selectedFixed = fixed
                                        showFixedDetailsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                AssetViewType.CURRENT -> {
                    if (currentAssets.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Inventory,
                            title = "No current items found"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedCategory != null || searchQuery.isNotEmpty()) {
                                items(currentAssets) { asset ->
                                    AssetCard(
                                        asset = asset,
                                        onUseAsset = { quantity -> viewModel.useAsset(asset.id, quantity) }
                                    )
                                }
                            } else {
                                val groupedAssets = currentAssets.groupBy { it.category }
                                groupedAssets.forEach { (category, categoryAssets) ->
                                    item {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(categoryAssets) { asset ->
                                        AssetCard(
                                            asset = asset,
                                            onUseAsset = { quantity -> viewModel.useAsset(asset.id, quantity) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentAssets) { asset ->
                            AssetCard(
                                asset = asset,
                                onUseAsset = { quantity -> viewModel.useAsset(asset.id, quantity) }
                            )
                        }
                        items(fixedAssets) { fixed ->
                            FixedCard(
                                fixed = fixed,
                                onCheckout = {
                                    selectedFixed = fixed
                                    showCheckoutDialog = true
                                },
                                onViewDetails = {
                                    selectedFixed = fixed
                                    showFixedDetailsDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Fixed Checkout Dialog
    if (showCheckoutDialog && selectedFixed != null) {
        FixedCheckoutDialog(
            fixed = selectedFixed!!,
            isLoading = isCheckoutLoading,
            onDismiss = {
                showCheckoutDialog = false
                selectedFixed = null
                isCheckoutLoading = false
            },
            onConfirm = { reason, jobId, condition, notes ->
                coroutineScope.launch {
                    isCheckoutLoading = true
                    val success = viewModel.checkoutFixed(selectedFixed!!.id, reason, jobId, condition, notes)
                    snackbarHostState.showSnackbar(
                        if (success) "Checked out ${selectedFixed!!.fixedName}" else "Checkout failed. Asset may already be in use."
                    )
                    showCheckoutDialog = false
                    selectedFixed = null
                    isCheckoutLoading = false
                }
            }
        )
    }

    // Fixed Details Dialog
    if (showFixedDetailsDialog && selectedFixed != null) {
        FixedDetailsDialog(
            fixed = selectedFixed!!,
            viewModel = viewModel,
            onDismiss = {
                showFixedDetailsDialog = false
                selectedFixed = null
            }
        )
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCard(
    asset: Asset,
    onUseAsset: (Double) -> Unit
) {
    var showUseDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showUseDialog = true }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top row with name and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = asset.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Category label in top right
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            asset.category,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            // Stock and minimum
            Text(
                text = "Stock: ${asset.currentStock} ${asset.unitOfMeasure}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Minimum: ${asset.minimumStock} ${asset.unitOfMeasure}",
                style = MaterialTheme.typography.bodySmall,
                color = if (asset.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Low stock badge
            if (asset.isLowStock) {
                AssistChip(
                    onClick = { },
                    label = { Text("Low Stock") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showUseDialog) {
        AssetUseDialog(
            asset = asset,
            onDismiss = { showUseDialog = false },
            onConfirm = {
                onUseAsset(it)
                showUseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetUseDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use Asset") },
        text = {
            Column {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity to use") },
                    placeholder = { Text("Available: ${asset.currentStock}") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    if (qty > 0 && qty <= asset.currentStock) {
                        onConfirm(qty)
                    }
                },
                enabled = quantity.isNotEmpty() &&
                         (quantity.toDoubleOrNull() ?: 0.0) > 0 &&
                         (quantity.toDoubleOrNull() ?: 0.0) <= asset.currentStock
            ) {
                Text("Use")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedCard(
    fixed: Fixed,
    onCheckout: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = fixed.fixedName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                when (fixed.fixedType) {
                                    FixedType.AIR_CONDITIONER -> "Air Conditioner"
                                    FixedType.TOOL -> "Tool"
                                    FixedType.LADDER -> "Ladder"
                                    FixedType.EQUIPMENT -> "Equipment"
                                    FixedType.VEHICLE -> "Vehicle"
                                    FixedType.METER -> "Meter"
                                    FixedType.PUMP -> "Pump"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        leadingIcon = {
                            Icon(
                                when (fixed.fixedType) {
                                    FixedType.AIR_CONDITIONER -> Icons.Default.AcUnit
                                    FixedType.TOOL -> Icons.Default.Build
                                    FixedType.LADDER -> Icons.Default.Stairs
                                    else -> Icons.Default.Hardware
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Code: ${fixed.fixedCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    fixed.serialNumber?.let {
                        Text(
                            text = "• SN: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!fixed.isAvailable && fixed.currentHolder != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Checked out by: ${fixed.currentHolder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = { },
                            label = { Text("In Use") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            if (fixed.isAvailable) {
                FilledTonalButton(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Checkout")
                }
            }
        }
    }
}
