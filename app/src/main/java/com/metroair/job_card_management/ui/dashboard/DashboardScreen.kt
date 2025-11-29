package com.metroair.job_card_management.ui.dashboard

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.ui.components.AddJobMaterialDialog
import com.metroair.job_card_management.ui.components.PhotoCaptureDialog
import com.metroair.job_card_management.ui.components.PhotoCategory
import com.metroair.job_card_management.ui.components.createImageFile

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val currentJob by viewModel.currentActiveJob.collectAsStateWithLifecycle()
    val pausedJobs by viewModel.pausedJobs.collectAsStateWithLifecycle()
    val awaitingJobs by viewModel.awaitingJobs.collectAsStateWithLifecycle()
    val pendingJobs by viewModel.pendingJobs.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val availableAssets by viewModel.availableAssets.collectAsStateWithLifecycle()
    val availableFixed by viewModel.availableFixed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showAssetDialog by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentJobForPhoto by remember { mutableStateOf<JobCard?>(null) }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // All stats in one row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Available",
                    value = stats.availableJobs.toString(),
                    icon = Icons.Default.Assignment,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("jobs?status=AVAILABLE") }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Awaiting",
                    value = stats.awaitingJobs.toString(),
                    icon = Icons.Default.Notifications,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { navController.navigate("jobs?status=AWAITING") }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active",
                    value = stats.activeJob.toString(),
                    icon = Icons.Default.Engineering,
                    color = if (stats.activeJob > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { navController.navigate("jobs?status=ACTIVE") }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    value = stats.pending.toString(),
                    icon = Icons.Default.Pending,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { navController.navigate("jobs?status=PENDING") }
                )
            }
        }

        // Current Active Job Card - Now below stats
        item {
            currentJob?.let { job ->
                CurrentJobCard(
                    job = job,
                    onClick = { navController.navigate("jobDetail/${job.id}") },
                    onStartClick = { viewModel.startJob(job.id) },
                    onPauseClick = { reason -> viewModel.pauseJob(job.id, reason) },
                    onResumeClick = { viewModel.resumeJob(job.id) },
                    onEnRouteClick = { viewModel.enRouteJob(job.id) },
                    onCancelClick = { reason -> viewModel.cancelJob(job.id, reason) },
                    onCompleteClick = { navController.navigate("jobDetail/${job.id}") },
                    onPhotoClick = {
                        currentJobForPhoto = job
                        val (file, uri) = createImageFile(context)
                        currentPhotoUri = uri
                        showPhotoDialog = true
                    },
                    onAssetClick = {
                        currentJobForPhoto = job
                        showAssetDialog = true
                    }
                )
            } ?: NoActiveJobCard()
        }

        // Paused Jobs Section - Jobs that are paused
        if (pausedJobs.isNotEmpty()) {
            item {
                Text(
                    text = "Paused Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(pausedJobs) { job ->
                PausedJobListItem(
                    job = job,
                    onResumeClick = { viewModel.resumeJob(job.id) },
                    onEnRouteClick = { viewModel.enRouteJob(job.id) },
                    onClick = { navController.navigate("jobDetail/${job.id}") }
                )
            }
        }

        // Awaiting Jobs Section - Jobs assigned but not yet accepted
        if (awaitingJobs.isNotEmpty()) {
            item {
                Text(
                    text = "Awaiting Acceptance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(awaitingJobs.take(3)) { job ->
                AwaitingJobListItem(
                    job = job,
                    onAcceptClick = { viewModel.acceptJob(job.id) },
                    onClick = { navController.navigate("jobDetail/${job.id}") }
                )
            }
        }

        // Pending Jobs Section - Jobs accepted but not yet started
        if (pendingJobs.isNotEmpty()) {
            item {
                Text(
                    text = "Pending Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(pendingJobs.take(3)) { job ->
                MyJobListItem(
                    job = job,
                    onStartClick = { viewModel.startJob(job.id) },
                    onClick = { navController.navigate("jobDetail/${job.id}") }
                )
            }
        }
        }
    }

    // Photo Capture Dialog
    if (showPhotoDialog && currentJobForPhoto != null) {
        PhotoCaptureDialog(
            onDismiss = {
                showPhotoDialog = false
                currentPhotoUri = null
                currentJobForPhoto = null
            },
            onPhotoSelected = { uri, category, notes ->
                viewModel.addPhotoToJob(currentJobForPhoto!!.id, uri.toString(), category.name, notes)
                showPhotoDialog = false
                currentPhotoUri = null
                currentJobForPhoto = null
            },
            currentPhotoUri = currentPhotoUri
        )
    }

    // Add Asset Dialog
    if (showAssetDialog && currentJobForPhoto != null) {
        AddJobMaterialDialog(
            onDismiss = {
                showAssetDialog = false
                currentJobForPhoto = null
            },
            onCurrentAssetAdded = { itemName, itemCode, quantity ->
                viewModel.addAssetToJob(currentJobForPhoto!!.id, itemName, itemCode, quantity)
                showAssetDialog = false
                currentJobForPhoto = null
            },
            onFixedAssetAdded = { fixedId, reason ->
                viewModel.checkoutFixedAssetToJob(currentJobForPhoto!!.id, fixedId, reason)
                showAssetDialog = false
                currentJobForPhoto = null
            },
            availableAssets = availableAssets,
            availableFixed = availableFixed,
            jobNumber = currentJobForPhoto!!.jobNumber,
            jobId = currentJobForPhoto!!.id
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentJobCard(
    job: JobCard,
    onClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: (String) -> Unit,
    onResumeClick: () -> Unit,
    onEnRouteClick: () -> Unit,
    onCancelClick: (String) -> Unit,
    onCompleteClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onAssetClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPauseDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with job ID and cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job #${job.jobNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Cancel button in top right
                if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
                    IconButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (!job.description.isNullOrEmpty()) {
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time information row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scheduled time
                if (job.scheduledDate != null || job.scheduledTime != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${job.scheduledDate ?: ""} ${job.scheduledTime ?: ""}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Estimated duration
                if (job.estimatedDuration != null && job.estimatedDuration > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${job.estimatedDuration} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = job.customerPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = job.customerEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Location card
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = job.serviceAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Show single "Arrived" button for EN_ROUTE jobs
            if (job.status == JobStatus.EN_ROUTE) {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Arrived - Begin Work")
                }
            } else {
                // Show normal action buttons for BUSY/PAUSED jobs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action buttons aligned to the left
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (job.status == JobStatus.BUSY) {
                            OutlinedIconButton(
                                onClick = { showPauseDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause"
                                )
                            }
                        }
                        if (job.status == JobStatus.PAUSED) {
                            OutlinedIconButton(
                                onClick = onEnRouteClick
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = "En Route"
                                )
                            }
                            OutlinedIconButton(
                                onClick = onResumeClick
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Resume"
                                )
                            }
                        }
                        OutlinedIconButton(
                            onClick = onAssetClick
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = "Add Asset")
                        }
                        OutlinedIconButton(
                            onClick = onPhotoClick
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Photo")
                        }
                    }

                    // Complete button on the right
                    FilledIconButton(
                        onClick = onCompleteClick
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Complete")
                    }
                }
            }
        }
    }

    // Pause Dialog
    if (showPauseDialog) {
        ReasonDialog(
            title = "Pause Job",
            message = "Please provide a reason for pausing this job:",
            onConfirm = { reason ->
                onPauseClick(reason)
                showPauseDialog = false
            },
            onDismiss = { showPauseDialog = false }
        )
    }

    // Cancel Dialog
    if (showCancelDialog) {
        ReasonDialog(
            title = "Cancel Job",
            message = "Please provide a reason for cancelling this job:",
            onConfirm = { reason ->
                onCancelClick(reason)
                showCancelDialog = false
            },
            onDismiss = { showCancelDialog = false },
            isCancel = true
        )
    }
}

@Composable
fun NoActiveJobCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No Active Job",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Start a job from the Jobs tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwaitingJobListItem(
    job: JobCard,
    onAcceptClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
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
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${job.customerName} • Job #${job.jobNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!job.scheduledTime.isNullOrEmpty()) {
                        Text(
                            text = "Scheduled: ${job.scheduledDate ?: "Today"} ${job.scheduledTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PausedJobListItem(
    job: JobCard,
    onResumeClick: () -> Unit,
    onEnRouteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        AssistChip(
                            onClick = { },
                            label = { Text("PAUSED", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    Text(
                        text = "${job.customerName} • Job #${job.jobNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!job.serviceAddress.isNullOrEmpty()) {
                        Text(
                            text = job.serviceAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onEnRouteClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = "En Route",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("En Route", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onResumeClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Resume", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobListItem(
    job: JobCard,
    onStartClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${job.customerName} • Job #${job.jobNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!job.scheduledTime.isNullOrEmpty()) {
                        Text(
                            text = "Scheduled: ${job.scheduledTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (job.status == JobStatus.PENDING) {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableJobListItem(
    job: JobCard,
    onClaimClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
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
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${job.customerName} • ${job.serviceAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scheduled: ${job.scheduledDate ?: "ASAP"} ${job.scheduledTime ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(
                    onClick = onClaimClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Claim")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListItem(
    job: JobCard,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Job #${job.jobNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            if (!job.serviceAddress.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = job.serviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onDismiss: () -> Unit,
    isCancel: Boolean = false
) {
    val reasonOptions = if (isCancel) {
        listOf(
            "Customer Cancelled",
            "Incorrect Job Details",
            "Duplicate Job",
            "Unable to Contact Customer",
            "Safety Concerns",
            "Personal Emergency",
            "Other"
        )
    } else {
        listOf(
            "Lunch Break",
            "End of Shift",
            "Awaiting Parts/Materials",
            "Awaiting Customer Approval",
            "Equipment Issue",
            "Other"
        )
    }

    var selectedOption by remember { mutableStateOf(reasonOptions.first()) }
    var customReason by remember { mutableStateOf("") }

    val pauseReasons = reasonOptions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))

                // Radio button options
                pauseReasons.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Show text field only when "Other" is selected
                if (selectedOption == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Specify reason") },
                        placeholder = { Text("Enter custom reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalReason = if (selectedOption == "Other") {
                        customReason
                    } else {
                        selectedOption
                    }
                    if (finalReason.isNotBlank()) {
                        onConfirm(finalReason)
                    }
                },
                enabled = if (selectedOption == "Other") customReason.isNotBlank() else true
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
