package com.metroair.job_card_management.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metroair.job_card_management.domain.model.JobResource
import com.metroair.job_card_management.domain.model.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceDialog(
    onDismiss: () -> Unit,
    onResourceAdded: (String, String, Double) -> Unit, // itemName, itemCode, quantity
    availableResources: List<Resource>
) {
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    var quantity by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Resource") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Resource selector dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedResource?.let { "${it.itemName} (${it.itemCode})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Resource *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableResources.forEach { resource ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(resource.itemName)
                                        Text(
                                            text = "${resource.itemCode} • ${resource.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedResource = resource
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity *") },
                    suffix = { Text(selectedResource?.unitOfMeasure ?: "") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedResource?.let { resource ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            if (qty > 0) {
                                onResourceAdded(
                                    resource.itemName,
                                    resource.itemCode,
                                    qty
                                )
                            }
                        }
                    }
                },
                enabled = selectedResource != null && quantity.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
