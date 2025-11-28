package com.metroair.job_card_management.ui.jobdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.domain.model.Purchase
import com.metroair.job_card_management.domain.model.PurchaseReceipt
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.metroair.job_card_management.ui.components.AddJobMaterialDialog
import com.metroair.job_card_management.ui.components.JobTimelineCard
import com.metroair.job_card_management.ui.components.PhotoCaptureDialog
import com.metroair.job_card_management.ui.components.PhotoCategory
import com.metroair.job_card_management.ui.components.UnifiedJobPhotosCard
import com.metroair.job_card_management.ui.components.SignatureDialog
import com.metroair.job_card_management.ui.components.SignatureDisplay
import com.metroair.job_card_management.ui.components.createImageFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    navController: NavHostController,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val jobCard by viewModel.jobCard.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableAssets by viewModel.availableAssets.collectAsStateWithLifecycle()
    val availableFixed by viewModel.availableFixed.collectAsStateWithLifecycle()
    val fixedCheckouts by viewModel.fixedCheckouts.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showResourceDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var purchaseVendor by remember { mutableStateOf("") }
    var purchaseAmount by remember { mutableStateOf("") }
    var purchaseNotes by remember { mutableStateOf("") }
    var receiptUris by remember { mutableStateOf(listOf<String>()) }
    var capturedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var showReceiptCapture by remember { mutableStateOf(false) }
    var editingReceipt: PurchaseReceipt? by remember { mutableStateOf(null) }
    var showReceiptEditDialog by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            receiptUris = receiptUris + it.toString()
        }
    }

    // Handle navigation after completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            navController.popBackStack()
        }
    }

    // Show messages
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show snackbar
            viewModel.clearMessage()
        }
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show snackbar
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job #${jobCard?.jobNumber ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        jobCard?.let { job ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Job Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Header with Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Job Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Priority badge
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(job.priority.name) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Flag,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = when (job.priority) {
                                                com.metroair.job_card_management.domain.model.JobPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
                                                com.metroair.job_card_management.domain.model.JobPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                    )

                                    // Status
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(job.status.name.replace("_", " ")) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = when (job.status) {
                                            JobStatus.BUSY -> AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                            JobStatus.COMPLETED, JobStatus.SIGNED -> AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                            JobStatus.CANCELLED -> AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                            else -> AssistChipDefaults.assistChipColors()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Job Title and Type
                            Text(
                                text = job.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = job.jobType.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!job.description.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = job.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Scheduled Date/Time
                            if (job.scheduledDate != null || job.scheduledTime != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${job.scheduledDate ?: "Not scheduled"} ${job.scheduledTime ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Estimated Duration
                            if (job.estimatedDuration != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Est. ${job.estimatedDuration} minutes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Service Location
                            if (!job.serviceAddress.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Service Location",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = job.serviceAddress,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (job.latitude != null && job.longitude != null) {
                                            Icon(
                                                Icons.Default.Navigation,
                                                contentDescription = "Navigate",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            job.travelDistance?.let { distance ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Travel: ${"%.1f".format(distance)} km",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Customer Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Customer Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(job.customerName)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Phone number card
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

                            if (!job.customerEmail.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Email card
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
                            }

                            job.customerAddress?.let { addr ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = addr,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Unified Job Photos Card
                item {
                    UnifiedJobPhotosCard(
                        beforePhotos = job.beforePhotos ?: emptyList(),
                        duringPhotos = job.otherPhotos ?: emptyList(),
                        afterPhotos = job.afterPhotos ?: emptyList(),
                        onAddPhoto = {
                            val (file, uri) = createImageFile(context)
                            currentPhotoUri = uri
                            showPhotoDialog = true
                        },
                        onRemovePhoto = { photoUri, category ->
                            viewModel.removePhoto(photoUri, category)
                        },
                        onRetagPhoto = { photoUri, fromCategory, toCategory ->
                            viewModel.retagPhoto(photoUri, fromCategory, toCategory)
                        },
                        onUpdateNotes = { photoUri, category, notes ->
                            viewModel.updatePhotoNotes(photoUri, category, notes)
                        }
                    )
                }

                // Purchases Section
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Purchases",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showPurchaseDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Purchase")
                                }
                            }

                            if (purchases.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No purchases recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                        purchases.forEachIndexed { index, purchase ->
                            PurchaseRow(
                                purchase = purchase,
                                onReceiptClick = { receipt ->
                                    editingReceipt = receipt
                                    showReceiptEditDialog = true
                                }
                            )
                            if (index != purchases.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
                }

                // Resources Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Job Materials",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (job.status == JobStatus.BUSY || job.status == JobStatus.PAUSED) {
                                    IconButton(onClick = { showResourceDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Resource")
                                    }
                                }
                            }

                            if (uiState.resources.isEmpty() && fixedCheckouts.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No materials recorded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.resources.forEach { resource ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resource.itemName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = resource.itemCode,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${resource.quantity} ${resource.unit}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (job.status == JobStatus.BUSY || job.status == JobStatus.PAUSED) {
                                                IconButton(
                                                    onClick = { viewModel.removeResource(resource.itemCode) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (resource != uiState.resources.last()) {
                                        HorizontalDivider()
                                    }
                                }

                                // Display Fixed Assets (checked out tools/equipment)
                                if (fixedCheckouts.isNotEmpty() && uiState.resources.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                fixedCheckouts.forEach { checkout ->
                                    if (checkout.returnTime == null) { // Only show active checkouts
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Build,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = checkout.fixedName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Text(
                                                    text = "Code: ${checkout.fixedCode}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Checked out",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (checkout != fixedCheckouts.filter { it.returnTime == null }.last()) {
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Work Details (Editable when BUSY)
                if (job.status == JobStatus.BUSY || job.status == JobStatus.PAUSED || job.status == JobStatus.COMPLETED) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Work Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = uiState.workPerformed,
                                    onValueChange = viewModel::updateWorkPerformed,
                                    label = { Text("Work Performed *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    enabled = job.status != JobStatus.COMPLETED
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = uiState.issuesEncountered,
                                    onValueChange = viewModel::updateIssuesEncountered,
                                    label = { Text("Issues Encountered") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    enabled = job.status != JobStatus.COMPLETED
                                )
                            }
                        }
                    }

                    // Follow-up Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = uiState.requiresFollowUp,
                                        onCheckedChange = viewModel::updateRequiresFollowUp,
                                        enabled = job.status != JobStatus.COMPLETED
                                    )
                                    Text("Requires Follow-up")
                                }

                                if (uiState.requiresFollowUp) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.followUpNotes,
                                        onValueChange = viewModel::updateFollowUpNotes,
                                        label = { Text("Follow-up Notes") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        enabled = job.status != JobStatus.COMPLETED
                                    )
                                }
                            }
                        }
                    }

                    // Customer Signature Section
                    // Show signature for BUSY or COMPLETED status
                    if (job.status == JobStatus.BUSY || job.status == JobStatus.COMPLETED) {
                        val signature = uiState.customerSignature ?: job.customerSignature

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Customer Signature",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (signature != null) {
                                        // Display the actual signature
                                        SignatureDisplay(
                                            signatureBase64 = signature,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Show change button only for BUSY status
                                        if (job.status == JobStatus.BUSY) {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedButton(
                                                onClick = { showSignatureDialog = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Change Signature")
                                            }
                                        }
                                    } else if (job.status == JobStatus.BUSY) {
                                        Button(
                                            onClick = { showSignatureDialog = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Capture Signature *")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Timeline Card - Show for jobs that have been started or cancelled
                if (job.status != JobStatus.PENDING && job.status != JobStatus.AWAITING && job.status != JobStatus.AVAILABLE) {
                    item {
                        JobTimelineCard(job = job)
                    }
                }

                // Action Buttons
                item {
                    when (job.status) {
                        JobStatus.AWAITING -> {
                            Button(
                                onClick = { viewModel.acceptJob() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept Job")
                            }
                        }
                        JobStatus.PENDING -> {
                            Button(
                                onClick = { viewModel.startJob() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Job")
                            }
                        }
                        JobStatus.EN_ROUTE -> {
                            Button(
                                onClick = { viewModel.startJob() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Arrived - Begin Work")
                            }
                        }
                        JobStatus.BUSY -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPauseDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }

                                Button(
                                    onClick = { viewModel.completeJob() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Complete Job")
                                }
                            }
                        }
                        JobStatus.PAUSED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.enRouteJob() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En Route")
                                }
                                Button(
                                    onClick = { viewModel.resumeJob() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }
                        }
                        else -> { /* No actions for other statuses */ }
                    }
                }
            }
        }
    }

    // Signature Dialog
    if (showSignatureDialog) {
        SignatureDialog(
            onDismiss = { showSignatureDialog = false },
            onSignatureCaptured = { signature ->
                viewModel.setCustomerSignature(signature)
                showSignatureDialog = false
            }
        )
    }

    // Add Resource Dialog
    jobCard?.let { job ->
        if (showResourceDialog) {
            AddJobMaterialDialog(
                onDismiss = { showResourceDialog = false },
                onCurrentAssetAdded = { itemName, itemCode, quantity ->
                    viewModel.addResource(itemName, itemCode, quantity)
                    showResourceDialog = false
                },
                onFixedAssetAdded = { fixedId, reason ->
                    viewModel.checkoutFixedAsset(fixedId, reason)
                    showResourceDialog = false
                },
                availableAssets = availableAssets,
                availableFixed = availableFixed,
                jobNumber = job.jobNumber,
                jobId = job.id
            )
        }
    }

    // Add Purchase Dialog
    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Add Purchase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchaseVendor,
                        onValueChange = { purchaseVendor = it },
                        label = { Text("Vendor") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = purchaseAmount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) purchaseAmount = it
                        },
                        label = { Text("Total Amount") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )
                        OutlinedTextField(
                            value = purchaseNotes,
                            onValueChange = { purchaseNotes = it },
                            label = { Text("Notes (optional)") },
                            singleLine = false,
                            maxLines = 3
                        )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val (file, uri) = createImageFile(context)
                                currentPhotoUri = uri
                                capturedReceiptUri = uri
                                showReceiptCapture = true
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capture")
                        }
                        OutlinedButton(
                            onClick = { receiptPicker.launch("image/*") }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                    if (receiptUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected Receipts", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        receiptUris.forEach { uri ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    receiptUris = receiptUris.filter { it != uri }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    if (receiptUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected Receipt", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        receiptUris.forEach { uri ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    receiptUris = receiptUris.filter { it != uri }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = purchaseAmount.toDoubleOrNull()
                        if (!purchaseVendor.isBlank() && amount != null) {
                            viewModel.addPurchase(
                                purchaseVendor.trim(),
                                amount,
                                purchaseNotes.ifBlank { null },
                                receiptUris
                            )
                            showPurchaseDialog = false
                            purchaseVendor = ""
                            purchaseAmount = ""
                            purchaseNotes = ""
                            receiptUris = emptyList()
                        }
                    },
                    enabled = purchaseVendor.isNotBlank() && (purchaseAmount.toDoubleOrNull() != null)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Photo Capture Dialog
    if (showPhotoDialog && currentPhotoUri != null) {
        PhotoCaptureDialog(
            onDismiss = {
                showPhotoDialog = false
                currentPhotoUri = null
            },
            onPhotoSelected = { uri, category, notes ->
                jobCard?.let { job ->
                    viewModel.addPhoto(job.id, uri, category, notes)
                }
                showPhotoDialog = false
                currentPhotoUri = null
            },
            currentPhotoUri = currentPhotoUri
        )
    }

    // Receipt Capture Dialog (reuse photo capture)
    if (showReceiptCapture && capturedReceiptUri != null) {
        PhotoCaptureDialog(
            onDismiss = {
                showReceiptCapture = false
                capturedReceiptUri = null
                currentPhotoUri = null
            },
            onPhotoSelected = { uri, _, _ ->
                // For simplicity, keep a single receipt URI per purchase
                receiptUris = listOf(uri.toString())
                showReceiptCapture = false
                capturedReceiptUri = null
                currentPhotoUri = null
            },
            currentPhotoUri = capturedReceiptUri,
            preselectedCategory = PhotoCategory.OTHER
        )
    }

    // Pause Dialog
    if (showPauseDialog) {
        ReasonDialog(
            title = "Pause Job",
            message = "Please provide a reason for pausing this job:",
            onConfirm = { reason ->
                viewModel.pauseJob(reason)
                showPauseDialog = false
            },
            onDismiss = { showPauseDialog = false }
        )
    }
}

@Composable
private fun PurchaseRow(purchase: Purchase, onReceiptClick: (PurchaseReceipt) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purchase.vendor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!purchase.notes.isNullOrBlank()) {
                    Text(
                        text = purchase.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(purchase.purchasedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "R${"%.2f".format(purchase.totalAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (purchase.receipts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            purchase.receipts.forEach { receipt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReceiptClick(receipt) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Receipt: ${receipt.uri}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!receipt.notes.isNullOrBlank()) {
                            Text(
                                text = receipt.notes ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonDialog(
    title: String,
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("Lunch Break") }
    var customReason by remember { mutableStateOf("") }

    val pauseReasons = listOf(
        "Lunch Break",
        "End of Shift",
        "Awaiting Parts/Materials",
        "Awaiting Customer Approval",
        "Equipment Issue",
        "Other"
    )

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
