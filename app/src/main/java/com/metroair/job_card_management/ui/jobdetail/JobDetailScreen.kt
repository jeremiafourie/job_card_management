package com.metroair.job_card_management.ui.jobdetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.metroair.job_card_management.domain.model.JobStatus
import com.metroair.job_card_management.domain.model.Purchase
import com.metroair.job_card_management.ui.components.AddJobMaterialDialog
import com.metroair.job_card_management.ui.components.JobTimelineCard
import com.metroair.job_card_management.ui.components.PhotoCaptureDialog
import com.metroair.job_card_management.ui.components.PhotoCategory
import com.metroair.job_card_management.ui.components.UnifiedJobPhotosCard
import com.metroair.job_card_management.ui.components.createImageFile

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun JobDetailScreen(
    navController: NavHostController,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val jobCard by viewModel.jobCard.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableAssets by viewModel.availableAssets.collectAsStateWithLifecycle()
    val availableFixed by viewModel.availableFixed.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val inventoryUsage by viewModel.inventoryUsage.collectAsStateWithLifecycle()

    var showPhotoDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showReceiptCapture by remember { mutableStateOf(false) }
    var receiptCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var receiptTargetPurchaseId by remember { mutableStateOf<Int?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseVendor by remember { mutableStateOf("") }
    var purchaseAmount by remember { mutableStateOf("") }
    var purchaseNotes by remember { mutableStateOf("") }
    var pendingReceiptPath by remember { mutableStateOf<String?>(null) }
    var activeReceiptUri: String? by remember { mutableStateOf(null) }
    var activePurchase: Purchase? by remember { mutableStateOf(null) }
    var showReceiptActionDialog by remember { mutableStateOf(false) }
    var showMaterialDialog by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            val stored = persistReceiptFromPicker(context, picked)
            stored?.let { storedPath ->
                receiptTargetPurchaseId?.let { targetId ->
                    viewModel.replaceReceipt(targetId, storedPath)
                    receiptTargetPurchaseId = null
                } ?: run {
                    pendingReceiptPath = storedPath
                }
            }
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) navController.popBackStack()
    }

    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearMessage()
        }
    }
    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearMessage()
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job #${jobCard?.jobNumber ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        jobCard?.let { job ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Job Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.AssistChip(
                                    onClick = {},
                                    label = { Text(job.status.name) },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(job.title, style = MaterialTheme.typography.titleLarge)
                            Text(job.jobType.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            job.description?.let { desc ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(desc, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${job.scheduledDate} ${job.scheduledTime ?: ""}".trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            job.estimatedDuration?.let { est ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Est. $est minutes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = job.latitude != null && job.longitude != null) {
                                            val uri = android.net.Uri.parse("geo:${job.latitude},${job.longitude}?q=${job.latitude},${job.longitude}(${job.serviceAddress})")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")
                                        try { context.startActivity(intent) } catch (e: android.content.ActivityNotFoundException) {
                                            intent.setPackage(null); context.startActivity(intent)
                                        }
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(job.serviceAddress ?: "", style = MaterialTheme.typography.bodyMedium)
                                        job.customerAddress?.let { addr ->
                                            Text(addr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                if (job.latitude != null && job.longitude != null) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Customer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(job.customerName)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SurfaceButton(icon = Icons.Default.Phone, label = job.customerPhone) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:${job.customerPhone}")
                                }
                                context.startActivity(intent)
                            }
                            job.customerEmail?.let { email ->
                                Spacer(modifier = Modifier.height(8.dp))
                                SurfaceButton(icon = Icons.Default.Email, label = email) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:$email")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            job.customerAddress?.let { addr ->
                                Spacer(modifier = Modifier.height(8.dp))
                                SurfaceButton(icon = Icons.Default.LocationOn, label = addr) { }
                            }
                        }
                    }
                }

                item {
                    UnifiedJobPhotosCard(
                        beforePhotos = job.beforePhotos ?: emptyList(),
                        duringPhotos = job.otherPhotos ?: emptyList(),
                        afterPhotos = job.afterPhotos ?: emptyList(),
                        onAddPhoto = {
                            val (_, uri) = createImageFile(context)
                            currentPhotoUri = uri
                            showPhotoDialog = true
                        },
                        onRemovePhoto = { uri, category -> viewModel.removePhoto(job.id, uri, category) },
                        onRetagPhoto = { uri, from, to -> viewModel.retagPhoto(job.id, uri, from, to) },
                        onUpdateNotes = { uri, category, notes -> viewModel.updatePhotoNotes(job.id, uri, category, notes) }
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Materials Used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { showMaterialDialog = true }) { Text("Add") }
                            }
                            if (inventoryUsage.isEmpty()) {
                                Text("No materials recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                inventoryUsage.forEach { usage ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(usage.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            Text("${usage.itemCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${usage.quantity} ${usage.unitOfMeasure}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Purchases", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showPurchaseDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Purchase")
                                }
                            }
                            if (purchases.isEmpty()) {
                                Text("No purchases recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                purchases.forEachIndexed { index, purchase ->
                                    PurchaseRow(
                                        purchase,
                                        onReceiptAction = { actionPurchase ->
                                            activePurchase = actionPurchase
                                            activeReceiptUri = actionPurchase.receiptUri
                                            showReceiptActionDialog = true
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

                item {
                    JobTimelineCard(job = job)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Work & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = uiState.workPerformed,
                                onValueChange = { viewModel.updateWorkPerformed(it) },
                                label = { Text("Work performed") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.technicianNotes,
                                onValueChange = { viewModel.updateTechnicianNotes(it) },
                                label = { Text("Technician notes") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.issuesEncountered,
                                onValueChange = { viewModel.updateIssuesEncountered(it) },
                                label = { Text("Issues encountered") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.travelDistance?.toString().orEmpty(),
                                onValueChange = { input ->
                                    val sanitized = input.replace(',', '.')
                                    val distance = sanitized.toDoubleOrNull()
                                    viewModel.updateTravelDistance(distance)
                                },
                                label = { Text("Travel distance (km)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = uiState.requiresFollowUp,
                                    onCheckedChange = { viewModel.updateRequiresFollowUp(it) }
                                )
                                Column {
                                    Text("Requires follow-up?")
                                    Text("Add notes if a return visit is needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (uiState.requiresFollowUp) {
                                OutlinedTextField(
                                    value = uiState.followUpNotes,
                                    onValueChange = { viewModel.updateFollowUpNotes(it) },
                                    label = { Text("Follow-up notes") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Button(onClick = { viewModel.completeJob() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Complete Job")
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo dialog
    if (showPhotoDialog && currentPhotoUri != null) {
        PhotoCaptureDialog(
            onDismiss = { showPhotoDialog = false; currentPhotoUri = null },
            onPhotoSelected = { uri, category, notes ->
                jobCard?.let { job -> viewModel.addPhoto(job.id, uri.toString(), category.name, notes) }
                showPhotoDialog = false; currentPhotoUri = null
            },
            currentPhotoUri = currentPhotoUri
        )
    }

    // Receipt capture dialog
    if (showReceiptCapture && receiptCaptureUri != null) {
        PhotoCaptureDialog(
            onDismiss = {
                showReceiptCapture = false
                receiptCaptureUri = null
            },
            onPhotoSelected = { uri, _, _ ->
                val storedUri = uri.toString()
                receiptTargetPurchaseId?.let { purchaseId ->
                    viewModel.replaceReceipt(purchaseId, storedUri)
                    receiptTargetPurchaseId = null
                } ?: run {
                    pendingReceiptPath = storedUri
                }
                showReceiptCapture = false
                receiptCaptureUri = null
            },
            currentPhotoUri = receiptCaptureUri,
            preselectedCategory = PhotoCategory.OTHER
        )
    }

    // Pause dialog
    if (showPauseDialog) {
        ReasonDialog(
            title = "Pause Job",
            message = "Provide a reason for pausing",
            onConfirm = { reason -> viewModel.pauseJob(reason); showPauseDialog = false },
            onDismiss = { showPauseDialog = false }
        )
    }

    // Add purchase dialog
    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Add Purchase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = purchaseVendor, onValueChange = { purchaseVendor = it }, label = { Text("Vendor") }, singleLine = true)
                    OutlinedTextField(
                        value = purchaseAmount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) purchaseAmount = it },
                        label = { Text("Total Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(value = purchaseNotes, onValueChange = { purchaseNotes = it }, label = { Text("Notes (optional)") }, singleLine = false, maxLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val (_, uri) = createReceiptFile(context)
                            receiptTargetPurchaseId = null
                            receiptCaptureUri = uri
                            showReceiptCapture = true
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capture")
                        }
                        OutlinedButton(onClick = {
                            receiptTargetPurchaseId = null
                            receiptPicker.launch("image/*")
                        }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                    pendingReceiptPath?.let {
                        Text("Receipt attached: ${readableReceiptName(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { pendingReceiptPath = null }) { Text("Remove") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = purchaseAmount.toDoubleOrNull()
                        if (!purchaseVendor.isBlank() && amount != null) {
                            viewModel.addPurchase(purchaseVendor.trim(), amount, purchaseNotes.ifBlank { null }, pendingReceiptPath)
                            showPurchaseDialog = false
                            purchaseVendor = ""; purchaseAmount = ""; purchaseNotes = ""; pendingReceiptPath = null
                        }
                    },
                    enabled = purchaseVendor.isNotBlank() && purchaseAmount.toDoubleOrNull() != null
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showPurchaseDialog = false }) { Text("Cancel") } }
        )
    }

    // Receipt action dialog
    if (showReceiptActionDialog && activePurchase != null) {
        AlertDialog(
            onDismissRequest = {
                showReceiptActionDialog = false
                activeReceiptUri = null
                activePurchase = null
            },
            title = { Text("Receipt options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(activePurchase?.vendor.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        activeReceiptUri?.let { "Current: ${readableReceiptName(it)}" } ?: "No receipt attached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                activeReceiptUri?.let { openReceipt(context, it) }
                            },
                            enabled = activeReceiptUri != null
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View")
                        }
                        OutlinedButton(onClick = {
                            receiptTargetPurchaseId = activePurchase?.id
                            showReceiptActionDialog = false
                            val (_, uri) = createReceiptFile(context)
                            receiptCaptureUri = uri
                            showReceiptCapture = true
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capture")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            receiptTargetPurchaseId = activePurchase?.id
                            showReceiptActionDialog = false
                            receiptPicker.launch("image/*")
                        }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        OutlinedButton(
                            onClick = {
                                activePurchase?.id?.let { viewModel.removeReceipt(it) }
                                showReceiptActionDialog = false
                                activeReceiptUri = null
                                activePurchase = null
                            },
                            enabled = activeReceiptUri != null
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showReceiptActionDialog = false
                    activeReceiptUri = null
                    activePurchase = null
                }) { Text("Close") }
            }
        )
    }

    // Material dialog
    val currentJob = jobCard
    if (showMaterialDialog && currentJob != null) {
        AddJobMaterialDialog(
            onDismiss = { showMaterialDialog = false },
            onInventoryAssetAdded = { itemName, itemCode, quantity ->
                viewModel.addInventoryUsage(currentJob.id, itemName, itemCode, quantity)
                showMaterialDialog = false
            },
            onFixedAssetAdded = { fixedId, reason ->
                viewModel.checkoutFixedToJob(currentJob.id, fixedId, reason)
                showMaterialDialog = false
            },
            availableAssets = availableAssets,
            availableFixed = availableFixed,
            jobNumber = currentJob.jobNumber
        )
    }
}

@Composable
private fun SurfaceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SingleButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
private fun RowButtons(leftLabel: String, rightLabel: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onLeft, modifier = Modifier.weight(1f)) { Text(leftLabel) }
        Button(onClick = onRight, modifier = Modifier.weight(1f)) { Text(rightLabel) }
    }
}

@Composable
fun ReasonDialog(title: String, message: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (reason.isNotBlank()) onConfirm(reason) }, enabled = reason.isNotBlank()) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PurchaseRow(purchase: Purchase, onReceiptAction: (Purchase) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(purchase.vendor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                purchase.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(purchase.purchasedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("R${"%.2f".format(purchase.totalAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (purchase.receiptUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReceiptAction(purchase) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Receipt: ${readableReceiptName(purchase.receiptUri)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            TextButton(onClick = { onReceiptAction(purchase) }) { Text("Attach receipt") }
        }
    }
}

private fun createReceiptFile(context: Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "receipts").apply { mkdirs() }
    val imageFile = File.createTempFile("RECEIPT_${timeStamp}_", ".jpg", storageDir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    return Pair(imageFile, uri)
}

private fun persistReceiptFromPicker(context: Context, sourceUri: Uri): String? {
    return try {
        val resolver = context.contentResolver
        val extension = resolver.getType(sourceUri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "receipts").apply { mkdirs() }
        val destination = File(storageDir, "RECEIPT_${timeStamp}.$extension")

        resolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination).toString()
    } catch (e: Exception) {
        Log.e("ReceiptPersist", "Unable to persist receipt from picker", e)
        null
    }
}

private fun readableReceiptName(uriString: String): String {
    val parsed = Uri.parse(uriString)
    return parsed.lastPathSegment ?: uriString
}

private fun openReceipt(context: Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "image/*")
        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Best-effort; ignore if no handler installed
    }
}
