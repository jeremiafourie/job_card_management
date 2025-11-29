package com.metroair.job_card_management.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class PhotoCategory {
    BEFORE, OTHER, AFTER
}

data class CategorizedPhoto(
    val uri: Uri,
    val category: PhotoCategory,
    val filePath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoSelected: (Uri, PhotoCategory, String) -> Unit,
    currentPhotoUri: Uri?,
    preselectedCategory: PhotoCategory? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCategoryDialog by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            if (preselectedCategory != null) {
                onPhotoSelected(currentPhotoUri, preselectedCategory, "")
            } else {
                pendingPhotoUri = currentPhotoUri
                showCategoryDialog = true
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentPhotoUri?.let { cameraLauncher.launch(it) }
        }
    }

    // Gallery launcher - copies selected photo to persistent storage
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            // Copy gallery photo to persistent storage
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                val destinationFile = File.createTempFile(
                    "JPEG_${timeStamp}_",
                    ".jpg",
                    storageDir
                )

                // Copy the content from gallery URI to our persistent file
                context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Create a persistent FileProvider URI
                val persistentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destinationFile
                )

                if (preselectedCategory != null) {
                    onPhotoSelected(persistentUri, preselectedCategory, "")
                } else {
                    pendingPhotoUri = persistentUri
                    showCategoryDialog = true
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoCapture", "Error copying gallery photo to persistent storage", e)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Choose photo source:")

                OutlinedButton(
                    onClick = {
                        // Request camera permission first
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo with Camera")
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select from Gallery")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Category and notes selection dialog
    if (showCategoryDialog && pendingPhotoUri != null) {
        if (preselectedCategory != null) {
            // Only ask for notes if category is preselected
            PhotoNotesDialog(
                category = preselectedCategory,
                onDismiss = {
                    showCategoryDialog = false
                    pendingPhotoUri = null
                },
                onNotesEntered = { notes ->
                    onPhotoSelected(pendingPhotoUri!!, preselectedCategory, notes)
                    showCategoryDialog = false
                    pendingPhotoUri = null
                    onDismiss()
                }
            )
        } else {
            // Ask for both category and notes
            PhotoCategoryDialog(
                onDismiss = {
                    showCategoryDialog = false
                    pendingPhotoUri = null
                },
                onCategorySelected = { category, notes ->
                    onPhotoSelected(pendingPhotoUri!!, category, notes)
                    showCategoryDialog = false
                    pendingPhotoUri = null
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun PhotoNotesDialog(
    category: PhotoCategory,
    onDismiss: () -> Unit,
    onNotesEntered: (String) -> Unit
) {
    var photoNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo Notes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add optional notes for this ${category.name.lowercase()} photo:")
                OutlinedTextField(
                    value = photoNotes,
                    onValueChange = { photoNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Damaged area, Before repair, etc.") },
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onNotesEntered(photoNotes) }) {
                Text("Done")
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
fun PhotoCategoryDialog(
    onDismiss: () -> Unit,
    onCategorySelected: (PhotoCategory, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PhotoCategory?>(null) }
    var photoNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedCategory == null) "Photo Category" else "Add Photo Notes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedCategory == null) {
                    Text("Select when this photo was taken:")

                    Button(
                        onClick = { selectedCategory = PhotoCategory.BEFORE },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Before Work Started")
                    }

                    Button(
                        onClick = { selectedCategory = PhotoCategory.OTHER },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Other / During Work")
                    }

                    Button(
                        onClick = { selectedCategory = PhotoCategory.AFTER },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("After Work Completed")
                    }
                } else {
                    Text("Add optional notes for this photo:")
                    OutlinedTextField(
                        value = photoNotes,
                        onValueChange = { photoNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Damaged area, Before repair, etc.") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        },
        confirmButton = {
            if (selectedCategory != null) {
                TextButton(
                    onClick = {
                        onCategorySelected(selectedCategory!!, photoNotes)
                    }
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedCategory != null) {
                    selectedCategory = null
                    photoNotes = ""
                } else {
                    onDismiss()
                }
            }) {
                Text(if (selectedCategory != null) "Back" else "Cancel")
            }
        }
    )
}

// Helper function to create image file
fun createImageFile(context: android.content.Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
    return Pair(imageFile, uri)
}
