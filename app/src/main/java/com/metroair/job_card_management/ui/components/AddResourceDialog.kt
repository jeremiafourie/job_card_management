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
import com.metroair.job_card_management.domain.model.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceDialog(
    onDismiss: () -> Unit,
    onResourceAdded: (String, String, Double) -> Unit, // itemName, itemCode, quantity
    availableResources: List<Resource>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    var quantity by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Filter resources based on search with debouncing
    var filteredResources by remember { mutableStateOf(emptyList<Resource>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.isNotBlank()) {
            searchJob = coroutineScope.launch {
                delay(300) // Debounce
                filteredResources = availableResources
                    .filter { resource ->
                        resource.itemName.contains(searchQuery, ignoreCase = true) ||
                        resource.itemCode.contains(searchQuery, ignoreCase = true) ||
                        resource.category.contains(searchQuery, ignoreCase = true)
                    }
                    .take(5)
                showDropdown = filteredResources.isNotEmpty()
            }
        } else {
            filteredResources = emptyList()
            showDropdown = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Resource") },
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
                        value = if (selectedResource != null) {
                            "${selectedResource!!.itemName} (${selectedResource!!.itemCode})"
                        } else {
                            searchQuery
                        },
                        onValueChange = { newValue ->
                            if (selectedResource != null) {
                                // If a resource was selected, clear it when typing
                                selectedResource = null
                            }
                            searchQuery = newValue
                        },
                        label = { Text("Search Resource *") },
                        placeholder = { Text("Type to search resources...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || selectedResource != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedResource = null
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && searchQuery.isNotBlank() && selectedResource == null) {
                                    showDropdown = true
                                }
                            },
                        singleLine = true
                    )

                    // Dropdown with search results
                    if (showDropdown && filteredResources.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn {
                                items(filteredResources) { resource ->
                                    ResourceSearchItem(
                                        resource = resource,
                                        onClick = {
                                            selectedResource = resource
                                            searchQuery = ""
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected resource info
                if (selectedResource != null) {
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
                                text = "Selected Resource",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = selectedResource!!.itemName,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Code: ${selectedResource!!.itemCode}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• ${selectedResource!!.category}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "Stock: ${selectedResource!!.currentStock} ${selectedResource!!.unitOfMeasure}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedResource!!.isLowStock) {
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
                        Text(selectedResource?.unitOfMeasure ?: "")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedResource != null,
                    supportingText = {
                        if (selectedResource != null) {
                            val qty = quantity.toDoubleOrNull() ?: 0.0
                            val stock = selectedResource!!.currentStock
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
                if (selectedResource != null) {
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
                    selectedResource?.let { resource ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            if (qty > 0 && qty <= resource.currentStock) {
                                onResourceAdded(
                                    resource.itemName,
                                    resource.itemCode,
                                    qty
                                )
                            }
                        }
                    }
                },
                enabled = selectedResource != null &&
                         quantity.toDoubleOrNull()?.let { it > 0 && it <= (selectedResource?.currentStock ?: 0) } == true
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Resource")
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
private fun ResourceSearchItem(
    resource: Resource,
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
                    text = resource.itemName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = resource.itemCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${resource.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${resource.currentStock} ${resource.unitOfMeasure}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (resource.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (resource.isLowStock) {
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