package com.metroair.job_card_management.ui.resources

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroair.job_card_management.domain.model.Asset
import com.metroair.job_card_management.domain.model.AssetCheckout
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCheckoutDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, jobId: Int?, condition: String, notes: String?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var jobNumber by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Good") }
    var notes by remember { mutableStateOf("") }
    val conditions = listOf("Good", "Fair", "Poor", "Needs Maintenance")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Checkout Asset")
                Text(
                    text = asset.assetName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reason for checkout (required)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Checkout *") },
                    placeholder = { Text("e.g., Job JOB001, Maintenance, Testing") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Job Number (optional)
                OutlinedTextField(
                    value = jobNumber,
                    onValueChange = { jobNumber = it },
                    label = { Text("Job Number (optional)") },
                    placeholder = { Text("JOB001") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Condition Dropdown
                var expandedCondition by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCondition,
                    onExpandedChange = { expandedCondition = !expandedCondition }
                ) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Current Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCondition) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCondition,
                        onDismissRequest = { expandedCondition = false }
                    ) {
                        conditions.forEach { cond ->
                            DropdownMenuItem(
                                text = { Text(cond) },
                                onClick = {
                                    condition = cond
                                    expandedCondition = false
                                }
                            )
                        }
                    }
                }

                // Notes (optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Any special instructions or observations") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Asset Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Asset Information",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code: ${asset.assetCode}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (asset.serialNumber != null) {
                            Text(
                                text = "Serial: ${asset.serialNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jobId = jobNumber.toIntOrNull()
                    onConfirm(
                        reason,
                        jobId,
                        condition,
                        notes.ifEmpty { null }
                    )
                },
                enabled = reason.isNotBlank()
            ) {
                Icon(Icons.Default.Output, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checkout")
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
fun AssetDetailsDialog(
    asset: Asset,
    viewModel: ResourcesViewModel,
    onDismiss: () -> Unit
) {
    val assetHistory by viewModel.getAssetHistory(asset.id).collectAsStateWithLifecycle(emptyList())
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f),
        title = {
            Column {
                Text("Asset Details")
                AssistChip(
                    onClick = { },
                    label = {
                        Text(if (asset.isAvailable) "Available" else "In Use")
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (asset.isAvailable) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Asset Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = asset.assetName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LabeledText("Asset Code", asset.assetCode)
                        asset.serialNumber?.let { LabeledText("Serial Number", it) }
                        asset.manufacturer?.let { LabeledText("Manufacturer", it) }
                        asset.model?.let { LabeledText("Model", it) }

                        if (!asset.isAvailable && asset.currentHolder != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Currently with: ${asset.currentHolder}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        asset.notes?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline/History
                Text(
                    text = "Checkout History",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (assetHistory.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No checkout history",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(assetHistory) { checkout ->
                            AssetHistoryItem(checkout, dateFormatter)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AssetHistoryItem(
    checkout: AssetCheckout,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checkout.technicianName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = checkout.reason,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    checkout.jobId?.let {
                        Text(
                            text = "Job #$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (checkout.returnTime == null) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Active") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Out: ${dateFormatter.format(Date(checkout.checkoutTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            checkout.returnTime?.let { returnTime ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "In: ${dateFormatter.format(Date(returnTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Duration
                val duration = returnTime - checkout.checkoutTime
                val hours = duration / (1000 * 60 * 60)
                val days = hours / 24
                val durationText = if (days > 0) {
                    "$days day${if (days > 1) "s" else ""}"
                } else {
                    "$hours hour${if (hours != 1L) "s" else ""}"
                }
                Text(
                    text = "Duration: $durationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Conditions
            if (checkout.condition != checkout.returnCondition && checkout.returnCondition != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Condition: ${checkout.condition} → ${checkout.returnCondition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checkout.returnCondition == "Needs Maintenance") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            checkout.notes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LabeledText(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}