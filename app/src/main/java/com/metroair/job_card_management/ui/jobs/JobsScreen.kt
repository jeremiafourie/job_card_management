package com.metroair.job_card_management.ui.jobs

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    navController: NavHostController,
    viewModel: JobsViewModel = hiltViewModel()
) {
    val jobs by viewModel.filteredJobs.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val isAwaitingFilter by viewModel.isAwaitingFilter.collectAsStateWithLifecycle()
    val isActiveFilter by viewModel.isActiveFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCancelDialog by remember { mutableStateOf(false) }
    var jobToCancel by remember { mutableStateOf<JobCard?>(null) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var jobToPause by remember { mutableStateOf<JobCard?>(null) }

    // Show snackbar for UI messages
    LaunchedEffect(uiMessage) {
        uiMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        // Search Bar - Using OutlinedTextField instead of SearchBar to avoid background issues
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search jobs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        // Filters - Status
        if (searchQuery.isEmpty()) {
            Column {
                // Status filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null && !isAwaitingFilter && !isActiveFilter,
                            onClick = { viewModel.clearFilters() },
                            label = { Text("All Status") },
                            leadingIcon = if (selectedStatus == null && !isAwaitingFilter && !isActiveFilter) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = isAwaitingFilter,
                            onClick = { viewModel.filterByAwaiting() },
                            label = { Text("Awaiting") },
                            leadingIcon = if (isAwaitingFilter) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = isActiveFilter,
                            onClick = { viewModel.filterByActive() },
                            label = { Text("Active") },
                            leadingIcon = if (isActiveFilter) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    items(JobStatus.values().toList()) { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { viewModel.filterByStatus(status) },
                            label = {
                                Text(when(status) {
                                    JobStatus.AVAILABLE -> "Available"
                                    JobStatus.PENDING -> "Pending"
                                    JobStatus.EN_ROUTE -> "En Route"
                                    JobStatus.BUSY -> "Busy"
                                    JobStatus.PAUSED -> "Paused"
                                    JobStatus.COMPLETED -> "Completed"
                                    JobStatus.CANCELLED -> "Cancelled"
                                })
                            },
                            leadingIcon = if (selectedStatus == status) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // Jobs List
        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.WorkOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No jobs found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedStatus != null) {
                        Text(
                            text = "Try changing the filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onClick = {
                            navController.navigate("jobDetail/${job.id}")
                        },
                        onStatusChange = {
                            // Handle status change based on action
                            when (it) {
                                "start" -> viewModel.startJob(job.id)
                                "pause" -> {
                                    jobToPause = job
                                    showPauseDialog = true
                                }
                                "resume" -> viewModel.resumeJob(job.id)
                                "claim" -> viewModel.claimJob(job.id)
                                else -> {}
                            }
                        },
                        onCancelClick = {
                            jobToCancel = job
                            showCancelDialog = true
                        }
                    )
                }
            }
        }
        }
    }

    // Cancel Job Dialog
    if (showCancelDialog && jobToCancel != null) {
        ReasonDialog(
            title = "Cancel Job",
            message = "Please provide a reason for cancelling this job:",
            onConfirm = { reason ->
                viewModel.cancelJob(jobToCancel!!.id, reason)
                showCancelDialog = false
                jobToCancel = null
            },
            onDismiss = {
                showCancelDialog = false
                jobToCancel = null
            }
        )
    }

    // Pause Job Dialog
    if (showPauseDialog && jobToPause != null) {
        ReasonDialog(
            title = "Pause Job",
            message = "Please provide a reason for pausing this job:",
            onConfirm = { reason ->
                viewModel.pauseJob(jobToPause!!.id, reason)
                showPauseDialog = false
                jobToPause = null
            },
            onDismiss = {
                showPauseDialog = false
                jobToPause = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    job: JobCard,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onCancelClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Job #${job.jobNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = when(job.status) {
                                JobStatus.AVAILABLE -> "Available"
                                JobStatus.PENDING -> "Pending"
                                JobStatus.EN_ROUTE -> "En Route"
                                JobStatus.BUSY -> "Busy"
                                JobStatus.PAUSED -> "Paused"
                                JobStatus.COMPLETED -> "Completed"
                                JobStatus.CANCELLED -> "Cancelled"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when(job.status) {
                            JobStatus.AVAILABLE -> MaterialTheme.colorScheme.secondaryContainer
                            JobStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                            JobStatus.EN_ROUTE -> MaterialTheme.colorScheme.tertiaryContainer
                            JobStatus.BUSY -> MaterialTheme.colorScheme.primary
                            JobStatus.PAUSED -> MaterialTheme.colorScheme.errorContainer
                            JobStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                            JobStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        },
                        labelColor = when(job.status) {
                            JobStatus.BUSY -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = job.title,
                style = MaterialTheme.typography.bodyLarge
            )

            // Customer Name
            if (!job.customerName.isEmpty()) {
                Text(
                    text = job.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${job.scheduledDate ?: "Not scheduled"} ${job.scheduledTime ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phone number card
            if (!job.customerPhone.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${job.customerPhone}")
                            }
                            context.startActivity(intent)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.customerPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Email card
            if (!job.customerEmail.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:${job.customerEmail}")
                            }
                            context.startActivity(intent)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.customerEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!job.serviceAddress.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = job.latitude != null && job.longitude != null
                        ) {
                            if (job.latitude != null && job.longitude != null) {
                                val uri = android.net.Uri.parse("geo:${job.latitude},${job.longitude}?q=${job.latitude},${job.longitude}(${job.serviceAddress})")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")

                                // Try to open with Google Maps, fallback to any map app
                                try {
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    intent.setPackage(null)
                                    context.startActivity(intent)
                                }
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.serviceAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick action buttons based on status
            when (job.status) {
                JobStatus.AVAILABLE -> {
                    Button(
                        onClick = { onStatusChange("claim") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Claim Job")
                    }
                }
                JobStatus.PENDING -> {
                    Button(
                        onClick = { onStatusChange("start") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start - En Route")
                    }
                }
                JobStatus.EN_ROUTE -> {
                    Button(
                        onClick = { onStatusChange("start") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Construction, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Begin Work")
                    }
                }
                JobStatus.BUSY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onStatusChange("pause") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        OutlinedButton(
                            onClick = { onClick() }, // Navigate to details
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Details")
                        }
                    }
                }
                JobStatus.PAUSED -> {
                    Button(
                        onClick = { onStatusChange("resume") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume Work")
                    }
                }
                else -> {}
            }

            // Cancel button for all jobs that aren't completed or cancelled
            if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Job")
                }
            }

            // Show signature icon if job is completed and has signature
            if (job.status == JobStatus.COMPLETED && job.customerSignature != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = "Signed",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Customer Signed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonDialog(
    title: String,
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (reason.isNotBlank()) {
                        onConfirm(reason)
                    }
                },
                enabled = reason.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}