package com.metroair.job_card_management.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metroair.job_card_management.domain.model.PhotoWithNotes

@Composable
fun UnifiedJobPhotosCard(
    beforePhotos: List<PhotoWithNotes>,
    duringPhotos: List<PhotoWithNotes>,
    afterPhotos: List<PhotoWithNotes>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String, String) -> Unit,
    onRetagPhoto: (String, String, String) -> Unit,
    onUpdateNotes: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPhoto by remember { mutableStateOf<Pair<PhotoWithNotes, String>?>(null) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and camera button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddPhoto) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Add photo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Before Photos Section
            PhotoCategorySection(
                title = "Before",
                photos = beforePhotos,
                category = "BEFORE",
                onPhotoClick = { photo -> selectedPhoto = photo to "BEFORE" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // During Photos Section
            PhotoCategorySection(
                title = "During",
                photos = duringPhotos,
                category = "OTHER",
                onPhotoClick = { photo -> selectedPhoto = photo to "OTHER" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // After Photos Section
            PhotoCategorySection(
                title = "After",
                photos = afterPhotos,
                category = "AFTER",
                onPhotoClick = { photo -> selectedPhoto = photo to "AFTER" }
            )
        }
    }

    // Full screen photo viewer
    selectedPhoto?.let { (photo, category) ->
        PhotoViewerDialog(
            photoUri = photo.uri,
            photoNotes = photo.notes,
            currentCategory = category,
            onDismiss = { selectedPhoto = null },
            onRemove = {
                onRemovePhoto(photo.uri, category)
                selectedPhoto = null
            },
            onRetag = { newCategory ->
                onRetagPhoto(photo.uri, category, newCategory)
                selectedPhoto = null
            },
            onUpdateNotes = { notes ->
                onUpdateNotes(photo.uri, category, notes)
            }
        )
    }
}

@Composable
private fun PhotoCategorySection(
    title: String,
    photos: List<PhotoWithNotes>,
    category: String,
    onPhotoClick: (PhotoWithNotes) -> Unit
) {
    Column {
        // Category title with photo count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (photos.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${photos.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (photos.isEmpty()) {
            Text(
                text = "No photos yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    PhotoThumbnail(
                        photoUri = photo.uri,
                        onClick = { onPhotoClick(photo) }
                    )
                }
            }
        }
    }
}
