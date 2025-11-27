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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onAssetAdded: (String, String, Double) -> Unit, // itemName, itemCode, quantity
    availableAssets: List<Asset>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAsset by remember { mutableStateOf<Asset?>(null) }
    var quantity by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Filter assets based on search with debouncing
    var filteredAssets by remember { mutableStateOf(emptyList<Asset>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.isNotBlank()) {
            searchJob = coroutineScope.launch {
                delay(300) // Debounce
                filteredAssets = availableAssets
                    .filter { asset ->
                        asset.itemName.contains(searchQuery, ignoreCase = true) ||
                        asset.itemCode.contains(searchQuery, ignoreCase = true) ||
                        asset.category.contains(searchQuery, ignoreCase = true)
                    }
                    .take(5)
                showDropdown = filteredAssets.isNotEmpty()
            }
        } else {
            filteredAssets = emptyList()
            showDropdown = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Asset") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field with dropdown
                Column {
                    OutlinedTextField(
                        value = if (selectedAsset != null) {
                            "${selectedAsset!!.itemName} (${selectedAsset!!.itemCode})"
                        } else {
                            searchQuery
                        },
                        onValueChange = { newValue ->
                            if (selectedAsset != null) {
                                // If an asset was selected, clear it when typing
                                selectedAsset = null
                            }
                            searchQuery = newValue
                        },
                        label = { Text("Search Asset *") },
                        placeholder = { Text("Type to search assets...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || selectedAsset != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedAsset = null
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && searchQuery.isNotBlank() && selectedAsset == null) {
                                    showDropdown = true
                                }
                            },
                        singleLine = true
                    )

                    // Dropdown with search results
                    if (showDropdown && filteredAssets.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn {
                                items(filteredAssets) { asset ->
                                    AssetSearchItem(
                                        asset = asset,
                                        onClick = {
                                            selectedAsset = asset
                                            searchQuery = ""
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected asset info
                if (selectedAsset != null) {
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
                                text = "Selected Asset",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = selectedAsset!!.itemName,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Code: ${selectedAsset!!.itemCode}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• ${selectedAsset!!.category}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "Stock: ${selectedAsset!!.currentStock} ${selectedAsset!!.unitOfMeasure}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedAsset!!.isLowStock) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }

                // Quantity input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        // Allow decimal numbers
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            quantity = newValue
                        }
                    },
                    label = { Text("Quantity *") },
                    placeholder = { Text("Enter quantity") },
                    suffix = {
                        Text(selectedAsset?.unitOfMeasure ?: "")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAsset != null,
                    supportingText = {
                        if (selectedAsset != null) {
                            val qty = quantity.toDoubleOrNull() ?: 0.0
                            val stock = selectedAsset!!.currentStock
                            if (qty > stock) {
                                Text(
                                    text = "Quantity exceeds available stock",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )

                // Quick quantity buttons
                if (selectedAsset != null) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedAsset?.let { asset ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            if (qty > 0 && qty <= asset.currentStock) {
                                onAssetAdded(
                                    asset.itemName,
                                    asset.itemCode,
                                    qty
                                )
                            }
                        }
                    }
                },
                enabled = selectedAsset != null &&
                         quantity.toDoubleOrNull()?.let { it > 0 && it <= (selectedAsset?.currentStock ?: 0.0) } == true
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Asset")
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
private fun AssetSearchItem(
    asset: Asset,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.itemName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = asset.itemCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${asset.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${asset.currentStock} ${asset.unitOfMeasure}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (asset.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (asset.isLowStock) {
                    Text(
                        text = "Low Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}