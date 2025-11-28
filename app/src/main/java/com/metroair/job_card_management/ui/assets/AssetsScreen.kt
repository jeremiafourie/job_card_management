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

    var selectedFixed by remember { mutableStateOf<Fixed?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showFixedDetailsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
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

        // Category/Type Filter Chips
        when (viewType) {
            AssetViewType.FIXED -> {
                // Fixed Type Filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFixedType == null,
                            onClick = { viewModel.selectFixedType(null) },
                            label = { Text("All Types") },
                            leadingIcon = if (selectedFixedType == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFixedType == FixedType.TOOL,
                            onClick = { viewModel.selectFixedType(FixedType.TOOL) },
                            label = { Text("Tools") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFixedType == FixedType.AIR_CONDITIONER,
                            onClick = { viewModel.selectFixedType(FixedType.AIR_CONDITIONER) },
                            label = { Text("Air Conditioners") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFixedType == FixedType.LADDER,
                            onClick = { viewModel.selectFixedType(FixedType.LADDER) },
                            label = { Text("Ladders") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFixedType == FixedType.EQUIPMENT,
                            onClick = { viewModel.selectFixedType(FixedType.EQUIPMENT) },
                            label = { Text("Equipment") }
                        )
                    }
                }
            }
            AssetViewType.CURRENT -> {
                // Category Filter for Current Inventory
                if (categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("All Categories") },
                                leadingIcon = if (selectedCategory == null) {
                                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category) },
                                leadingIcon = if (selectedCategory == category) {
                                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
            else -> {
                // Show both filters for ALL view
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
                // Fixed Assets List
                if (fixedAssets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No fixed assets found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                // Current Inventory List
                if (currentAssets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No current items found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
            AssetViewType.ALL -> {
                // Combined view - show both fixed assets and current inventory
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fixedAssets.isNotEmpty()) {
                        item {
                            Text(
                                text = "Fixed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(fixedAssets.take(5)) { fixed ->
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

                    if (currentAssets.isNotEmpty()) {
                        item {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(currentAssets.take(10)) { asset ->
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

    // Fixed Checkout Dialog
    if (showCheckoutDialog && selectedFixed != null) {
        FixedCheckoutDialog(
            fixed = selectedFixed!!,
            onDismiss = {
                showCheckoutDialog = false
                selectedFixed = null
            },
            onConfirm = { reason, jobId, condition, notes ->
                viewModel.checkoutFixed(selectedFixed!!.id, reason, jobId, condition, notes)
                showCheckoutDialog = false
                selectedFixed = null
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
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Item code
            Text(
                text = "Code: ${asset.itemCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stock information and action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text("${asset.currentStock} ${asset.unitOfMeasure}")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = if (asset.isLowStock) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )

                    if (asset.isLowStock) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Low Stock") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }

                IconButton(
                    onClick = { showUseDialog = true }
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = "Use Asset",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Use Asset Dialog
    if (showUseDialog) {
        UseAssetDialog(
            asset = asset,
            onDismiss = { showUseDialog = false },
            onConfirm = { quantity ->
                onUseAsset(quantity)
                showUseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseAssetDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use ${asset.itemName}") },
        text = {
            Column {
                Text("Current Stock: ${asset.currentStock} ${asset.unitOfMeasure}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            quantity = it
                        }
                    },
                    label = { Text("Quantity to Use") },
                    suffix = { Text(asset.unitOfMeasure) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
            // Main content with padding
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
                        text = fixed.fixedName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Category label in top right
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                when(fixed.fixedType) {
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
                                when(fixed.fixedType) {
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

                // Codes and serial number
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Code: ${fixed.fixedCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fixed.serialNumber != null) {
                        Text(
                            text = "• SN: ${fixed.serialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Show checked out info if not available
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

            // Full width checkout button at the bottom (only if available)
            if (fixed.isAvailable) {
                FilledTonalButton(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Output,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Checkout")
                }
            }
        }
    }
}
