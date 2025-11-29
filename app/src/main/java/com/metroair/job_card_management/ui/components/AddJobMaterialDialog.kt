package com.metroair.job_card_management.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.Fixed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class JobMaterial {
    data class CurrentAsset(val asset: Asset) : JobMaterial()
    data class FixedAsset(val fixed: Fixed) : JobMaterial()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobMaterialDialog(
    onDismiss: () -> Unit,
    onCurrentAssetAdded: (String, String, Double) -> Unit, // itemName, itemCode, quantity
    onFixedAssetAdded: (Int, String) -> Unit, // fixedId, reason (checkout reason)
    availableAssets: List<Asset>,
    availableFixed: List<Fixed>,
    jobNumber: String = ""
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Current, 1 = Fixed
    var searchQuery by remember { mutableStateOf("") }
    var selectedMaterial by remember { mutableStateOf<JobMaterial?>(null) }
    var quantity by remember { mutableStateOf("") }
    var checkoutReason by remember { mutableStateOf("Required for $jobNumber") }
    var showDropdown by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Filter materials based on search with debouncing
    var filteredMaterials by remember { mutableStateOf(emptyList<JobMaterial>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery, selectedTab) {
        searchJob?.cancel()
        if (searchQuery.isNotBlank()) {
            searchJob = coroutineScope.launch {
                delay(300) // Debounce
                filteredMaterials = if (selectedTab == 0) {
                    // Search Current assets
                    availableAssets
                        .filter { asset ->
                            asset.itemName.contains(searchQuery, ignoreCase = true) ||
                            asset.itemCode.contains(searchQuery, ignoreCase = true) ||
                            asset.category.contains(searchQuery, ignoreCase = true)
                        }
                        .take(5)
                        .map { JobMaterial.CurrentAsset(it) }
                } else {
                    // Search Fixed assets
                    availableFixed
                        .filter { fixed ->
                            fixed.isAvailable && (
                                fixed.fixedName.contains(searchQuery, ignoreCase = true) ||
                                fixed.fixedCode.contains(searchQuery, ignoreCase = true) ||
                                fixed.fixedType.name.contains(searchQuery, ignoreCase = true) ||
                                (fixed.serialNumber?.contains(searchQuery, ignoreCase = true) ?: false)
                            )
                        }
                        .take(5)
                        .map { JobMaterial.FixedAsset(it) }
                }
                showDropdown = filteredMaterials.isNotEmpty()
            }
        } else {
            filteredMaterials = emptyList()
            showDropdown = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Job Material") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab selector for Current vs Fixed
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedMaterial = null
                            searchQuery = ""
                        },
                        text = { Text("Current Stock") },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedMaterial = null
                            searchQuery = ""
                        },
                        text = { Text("Fixed Assets") },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) }
                    )
                }

                // Search field with dropdown
                Column {
                    OutlinedTextField(
                        value = when (val material = selectedMaterial) {
                            is JobMaterial.CurrentAsset -> "${material.asset.itemName} (${material.asset.itemCode})"
                            is JobMaterial.FixedAsset -> "${material.fixed.fixedName} (${material.fixed.fixedCode})"
                            null -> searchQuery
                        },
                        onValueChange = { newValue ->
                            if (selectedMaterial != null) {
                                selectedMaterial = null
                            }
                            searchQuery = newValue
                        },
                        label = { Text("Search") },
                        placeholder = { Text("Search...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || selectedMaterial != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedMaterial = null
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && searchQuery.isNotBlank() && selectedMaterial == null) {
                                    showDropdown = true
                                }
                            },
                        singleLine = true
                    )

                    // Dropdown with search results
                    if (showDropdown && filteredMaterials.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn {
                                items(filteredMaterials) { material ->
                                    MaterialSearchItem(
                                        material = material,
                                        onClick = {
                                            selectedMaterial = material
                                            searchQuery = ""
                                            showDropdown = false
                                            // Auto-fill checkout reason for fixed assets
                                            if (material is JobMaterial.FixedAsset) {
                                                checkoutReason = "Required for $jobNumber"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected material info
                when (val material = selectedMaterial) {
                    is JobMaterial.CurrentAsset -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Selected Current Asset",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = material.asset.itemName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Code: ${material.asset.itemCode}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• ${material.asset.category}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "Stock: ${material.asset.currentStock} ${material.asset.unitOfMeasure}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (material.asset.isLowStock) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        // Quantity input for Current assets
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    quantity = newValue
                                }
                            },
                            label = { Text("Quantity *") },
                            placeholder = { Text("Enter quantity") },
                            suffix = {
                                Text(material.asset.unitOfMeasure)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                val qty = quantity.toDoubleOrNull() ?: 0.0
                                val stock = material.asset.currentStock
                                if (qty > stock) {
                                    Text(
                                        text = "Quantity exceeds available stock",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        // Quick quantity buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1", "5", "10", "25").forEach { quickQty ->
                                FilterChip(
                                    selected = quantity == quickQty,
                                    onClick = { quantity = quickQty },
                                    label = { Text(quickQty) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    is JobMaterial.FixedAsset -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Selected Fixed Asset",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = material.fixed.fixedName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Code: ${material.fixed.fixedCode}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• ${material.fixed.fixedType}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                material.fixed.serialNumber?.let {
                                    Text(
                                        text = "Serial: $it",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Available for checkout",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Checkout reason for Fixed assets
                        OutlinedTextField(
                            value = checkoutReason,
                            onValueChange = { checkoutReason = it },
                            label = { Text("Checkout Reason") },
                            placeholder = { Text("Why is this needed?") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text("This asset will be checked out to this job")
                            }
                        )

                        // Info about automatic return
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Asset will be automatically returned when job is completed",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    null -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (val material = selectedMaterial) {
                        is JobMaterial.CurrentAsset -> {
                            quantity.toDoubleOrNull()?.let { qty ->
                                if (qty > 0 && qty <= material.asset.currentStock) {
                                    onCurrentAssetAdded(
                                        material.asset.itemName,
                                        material.asset.itemCode,
                                        qty
                                    )
                                }
                            }
                        }
                        is JobMaterial.FixedAsset -> {
                            onFixedAssetAdded(
                                material.fixed.id,
                                checkoutReason.ifBlank { "Required for $jobNumber" }
                            )
                        }
                        null -> {}
                    }
                },
                enabled = when (val material = selectedMaterial) {
                    is JobMaterial.CurrentAsset ->
                        quantity.toDoubleOrNull()?.let { it > 0 && it <= material.asset.currentStock } == true
                    is JobMaterial.FixedAsset -> true
                    null -> false
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MaterialSearchItem(
    material: JobMaterial,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (material) {
                is JobMaterial.CurrentAsset -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = material.asset.itemName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = material.asset.itemCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${material.asset.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${material.asset.currentStock} ${material.asset.unitOfMeasure}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (material.asset.isLowStock) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (material.asset.isLowStock) {
                            Text(
                                text = "Low Stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                is JobMaterial.FixedAsset -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = material.fixed.fixedName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = material.fixed.fixedCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${material.fixed.fixedType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        material.fixed.serialNumber?.let {
                            Text(
                                text = "SN: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Available",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
