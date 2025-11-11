package com.metroair.job_card_management.ui.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.AssetType
import com.metroair.job_card_management.domain.model.Resource

enum class ResourceViewType {
    ALL, INVENTORY, ASSETS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    viewModel: ResourcesViewModel = hiltViewModel()
) {
    val resources by viewModel.filteredResources.collectAsStateWithLifecycle()
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val selectedAssetType by viewModel.selectedAssetType.collectAsStateWithLifecycle()
    val lowStockResources = resources.filter { it.isLowStock }

    var selectedAsset by remember { mutableStateOf<Asset?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showAssetDetailsDialog by remember { mutableStateOf(false) }

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
                        ResourceViewType.ASSETS -> "Search assets..."
                        ResourceViewType.INVENTORY -> "Search inventory..."
                        else -> "Search resources..."
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

        // View Type Filters (Inventory, Assets, All)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = viewType == ResourceViewType.ALL,
                    onClick = { viewModel.setViewType(ResourceViewType.ALL) },
                    label = { Text("All") },
                    leadingIcon = if (viewType == ResourceViewType.ALL) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = viewType == ResourceViewType.INVENTORY,
                    onClick = { viewModel.setViewType(ResourceViewType.INVENTORY) },
                    label = { Text("Inventory") },
                    leadingIcon = if (viewType == ResourceViewType.INVENTORY) {
                        { Icon(Icons.Default.Inventory2, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = viewType == ResourceViewType.ASSETS,
                    onClick = { viewModel.setViewType(ResourceViewType.ASSETS) },
                    label = { Text("Assets") },
                    leadingIcon = if (viewType == ResourceViewType.ASSETS) {
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
            ResourceViewType.ASSETS -> {
                // Asset Type Filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAssetType == null,
                            onClick = { viewModel.selectAssetType(null) },
                            label = { Text("All Types") },
                            leadingIcon = if (selectedAssetType == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAssetType == AssetType.TOOL,
                            onClick = { viewModel.selectAssetType(AssetType.TOOL) },
                            label = { Text("Tools") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAssetType == AssetType.AIR_CONDITIONER,
                            onClick = { viewModel.selectAssetType(AssetType.AIR_CONDITIONER) },
                            label = { Text("Air Conditioners") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAssetType == AssetType.LADDER,
                            onClick = { viewModel.selectAssetType(AssetType.LADDER) },
                            label = { Text("Ladders") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAssetType == AssetType.EQUIPMENT,
                            onClick = { viewModel.selectAssetType(AssetType.EQUIPMENT) },
                            label = { Text("Equipment") }
                        )
                    }
                }
            }
            ResourceViewType.INVENTORY -> {
                // Category Filter for Inventory
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
                // You might want to show both asset types and resource categories here
            }
        }

        // Low Stock Warning (only for inventory)
        if (viewType != ResourceViewType.ASSETS && lowStockResources.isNotEmpty()) {
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
                            text = "${lowStockResources.size} items below minimum stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Main Content
        when (viewType) {
            ResourceViewType.ASSETS -> {
                // Assets List
                if (assets.isEmpty()) {
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
                                text = "No assets found",
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
                        items(assets) { asset ->
                            AssetItem(
                                asset = asset,
                                onCheckout = {
                                    selectedAsset = asset
                                    showCheckoutDialog = true
                                },
                                onViewDetails = {
                                    selectedAsset = asset
                                    showAssetDetailsDialog = true
                                }
                            )
                        }
                    }
                }
            }
            ResourceViewType.INVENTORY -> {
                // Inventory List
                if (resources.isEmpty()) {
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
                                text = "No inventory items found",
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
                            items(resources) { resource ->
                                ResourceItem(
                                    resource = resource,
                                    onCheckoutTool = { viewModel.checkoutTool(resource.id, resource.itemName, resource.itemCode) },
                                    onUseResource = { quantity -> viewModel.useResource(resource.id, quantity) }
                                )
                            }
                        } else {
                            val groupedResources = resources.groupBy { it.category }
                            groupedResources.forEach { (category, categoryResources) ->
                                item {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(categoryResources) { resource ->
                                    ResourceItem(
                                        resource = resource,
                                        onCheckoutTool = { viewModel.checkoutTool(resource.id, resource.itemName, resource.itemCode) },
                                        onUseResource = { quantity -> viewModel.useResource(resource.id, quantity) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ResourceViewType.ALL -> {
                // Combined view - show both assets and resources
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (assets.isNotEmpty()) {
                        item {
                            Text(
                                text = "Assets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(assets.take(5)) { asset ->
                            AssetItem(
                                asset = asset,
                                onCheckout = {
                                    selectedAsset = asset
                                    showCheckoutDialog = true
                                },
                                onViewDetails = {
                                    selectedAsset = asset
                                    showAssetDetailsDialog = true
                                }
                            )
                        }
                    }

                    if (resources.isNotEmpty()) {
                        item {
                            Text(
                                text = "Inventory",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(resources.take(10)) { resource ->
                            ResourceItem(
                                resource = resource,
                                onCheckoutTool = { viewModel.checkoutTool(resource.id, resource.itemName, resource.itemCode) },
                                onUseResource = { quantity -> viewModel.useResource(resource.id, quantity) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Asset Checkout Dialog
    if (showCheckoutDialog && selectedAsset != null) {
        AssetCheckoutDialog(
            asset = selectedAsset!!,
            onDismiss = {
                showCheckoutDialog = false
                selectedAsset = null
            },
            onConfirm = { reason, jobId, condition, notes ->
                viewModel.checkoutAsset(selectedAsset!!.id, reason, jobId, condition, notes)
                showCheckoutDialog = false
                selectedAsset = null
            }
        )
    }

    // Asset Details Dialog
    if (showAssetDetailsDialog && selectedAsset != null) {
        AssetDetailsDialog(
            asset = selectedAsset!!,
            viewModel = viewModel,
            onDismiss = {
                showAssetDetailsDialog = false
                selectedAsset = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceItem(
    resource: Resource,
    onCheckoutTool: () -> Unit,
    onUseResource: (Int) -> Unit
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
                    text = resource.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Category label in top right
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            resource.category,
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
                text = "Code: ${resource.itemCode}",
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
                            Text("${resource.currentStock} ${resource.unitOfMeasure}")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = if (resource.isLowStock) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )

                    if (resource.isLowStock) {
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
                        contentDescription = "Use Resource",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Use Resource Dialog
    if (showUseDialog) {
        UseResourceDialog(
            resource = resource,
            onDismiss = { showUseDialog = false },
            onConfirm = { quantity ->
                onUseResource(quantity)
                showUseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseResourceDialog(
    resource: Resource,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use ${resource.itemName}") },
        text = {
            Column {
                Text("Current Stock: ${resource.currentStock} ${resource.unitOfMeasure}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            quantity = it
                        }
                    },
                    label = { Text("Quantity to Use") },
                    suffix = { Text(resource.unitOfMeasure) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    if (qty > 0 && qty <= resource.currentStock) {
                        onConfirm(qty)
                    }
                },
                enabled = quantity.isNotEmpty() &&
                         (quantity.toIntOrNull() ?: 0) > 0 &&
                         (quantity.toIntOrNull() ?: 0) <= resource.currentStock
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
fun AssetItem(
    asset: Asset,
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
                        text = asset.assetName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Category label in top right
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                when(asset.assetType) {
                                    AssetType.AIR_CONDITIONER -> "Air Conditioner"
                                    AssetType.TOOL -> "Tool"
                                    AssetType.LADDER -> "Ladder"
                                    AssetType.EQUIPMENT -> "Equipment"
                                    AssetType.VEHICLE -> "Vehicle"
                                    AssetType.METER -> "Meter"
                                    AssetType.PUMP -> "Pump"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        leadingIcon = {
                            Icon(
                                when(asset.assetType) {
                                    AssetType.AIR_CONDITIONER -> Icons.Default.AcUnit
                                    AssetType.TOOL -> Icons.Default.Build
                                    AssetType.LADDER -> Icons.Default.Stairs
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
                        text = "Code: ${asset.assetCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (asset.serialNumber != null) {
                        Text(
                            text = "• SN: ${asset.serialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Show checked out info if not available
                if (!asset.isAvailable && asset.currentHolder != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Checked out by: ${asset.currentHolder}",
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
            if (asset.isAvailable) {
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