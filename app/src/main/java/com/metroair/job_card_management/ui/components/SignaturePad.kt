package com.metroair.job_card_management.ui.components

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.ByteArrayOutputStream

@Composable
fun SignatureDialog(
    onDismiss: () -> Unit,
    onSignatureCaptured: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Signature",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SignaturePad(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onSignatureCaptured = onSignatureCaptured,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var paths by remember { mutableStateOf(listOf<SignaturePath>()) }
    var currentPath by remember { mutableStateOf(SignaturePath()) }
    var isDrawing by remember { mutableStateOf(false) }
    var signerName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Signer Name Input
        OutlinedTextField(
            value = signerName,
            onValueChange = { signerName = it },
            label = { Text("Signer's Name") },
            placeholder = { Text("Enter name of person signing") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clipToBounds()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val newPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                                currentPath = SignaturePath(
                                    path = newPath,
                                    points = mutableListOf(offset)
                                )
                                isDrawing = true
                            },
                            onDragEnd = {
                                if (currentPath.points.isNotEmpty()) {
                                    // Build the complete path before adding
                                    val finalPath = androidx.compose.ui.graphics.Path()
                                    currentPath.points.forEachIndexed { index, point ->
                                        if (index == 0) {
                                            finalPath.moveTo(point.x, point.y)
                                        } else {
                                            finalPath.lineTo(point.x, point.y)
                                        }
                                    }
                                    paths = paths + SignaturePath(path = finalPath, points = currentPath.points)
                                }
                                currentPath = SignaturePath()
                                isDrawing = false
                            },
                            onDrag = { change, _ ->
                                val newPoint = change.position
                                currentPath = currentPath.copy(
                                    points = currentPath.points + newPoint
                                )
                                currentPath.path.lineTo(newPoint.x, newPoint.y)
                            }
                        )
                    }
            ) {
                // Draw all completed paths
                paths.forEach { signaturePath ->
                    drawPath(
                        path = signaturePath.path,
                        color = Color.Black,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw current path being drawn
                if (isDrawing && currentPath.points.size > 1) {
                    val path = androidx.compose.ui.graphics.Path()
                    currentPath.points.forEachIndexed { index, point ->
                        if (index == 0) {
                            path.moveTo(point.x, point.y)
                        } else {
                            path.lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            if (paths.isEmpty() && !isDrawing) {
                Text(
                    text = "Sign here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clear button with text
            OutlinedButton(
                onClick = { paths = emptyList() },
                enabled = paths.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear")
            }

            // Save button with text
            Button(
                onClick = {
                    if (paths.isNotEmpty() && signerName.isNotBlank()) {
                        // Convert signature to base64 with signer name
                        val signatureBase64 = convertPathsToBase64WithName(paths, signerName.trim())
                        onSignatureCaptured(signatureBase64)
                        onDismiss()
                    }
                },
                enabled = paths.isNotEmpty() && signerName.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

data class SignaturePath(
    val path: androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path(),
    val points: List<Offset> = emptyList()
)

// Simple base64 encoding of the path points for storage
private fun convertPathsToBase64(paths: List<SignaturePath>): String {
    // For simplicity, we'll just store the points as a string
    // In production, you might want to create an actual bitmap
    val pointsString = paths.flatMap { it.points }
        .joinToString(";") { "${it.x},${it.y}" }
    return Base64.encodeToString(pointsString.toByteArray(), Base64.NO_WRAP)
}

// Base64 encoding with signer name
private fun convertPathsToBase64WithName(paths: List<SignaturePath>, name: String): String {
    // Store format: NAME|POINTS
    val pointsString = paths.flatMap { it.points }
        .joinToString(";") { "${it.x},${it.y}" }
    val dataWithName = "$name|$pointsString"
    return Base64.encodeToString(dataWithName.toByteArray(), Base64.NO_WRAP)
}

// Display a captured signature
@Composable
fun SignatureDisplay(
    signatureBase64: String,
    modifier: Modifier = Modifier
) {
    val (signerName, points) = remember(signatureBase64) {
        try {
            val decoded = String(Base64.decode(signatureBase64, Base64.NO_WRAP))

            // Check if it contains name (new format: NAME|POINTS)
            val parts = decoded.split("|", limit = 2)
            val (name, pointsData) = if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                // Old format without name
                "" to decoded
            }

            val parsedPoints = pointsData.split(";").mapNotNull { pointStr ->
                val coords = pointStr.split(",")
                if (coords.size == 2) {
                    Offset(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f)
                } else null
            }

            name to parsedPoints
        } catch (e: Exception) {
            "" to emptyList()
        }
    }

    if (points.isNotEmpty()) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Customer Signature",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (signerName.isNotEmpty()) {
                        Text(
                            text = "Signed by: $signerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val scaleX = size.width / (points.maxOfOrNull { it.x } ?: 1f)
                        val scaleY = size.height / (points.maxOfOrNull { it.y } ?: 1f)
                        val scale = minOf(scaleX, scaleY) * 0.9f // 90% to add padding

                        val paths = mutableListOf<androidx.compose.ui.graphics.Path>()
                        var currentPath = androidx.compose.ui.graphics.Path()
                        var lastPoint: Offset? = null

                        points.forEach { point ->
                            val scaledPoint = Offset(point.x * scale, point.y * scale)

                            if (lastPoint == null) {
                                currentPath.moveTo(scaledPoint.x, scaledPoint.y)
                            } else {
                                val distance = (scaledPoint - lastPoint!!).getDistance()
                                if (distance > 50f) { // Start new path if points are far apart
                                    paths.add(currentPath)
                                    currentPath = androidx.compose.ui.graphics.Path()
                                    currentPath.moveTo(scaledPoint.x, scaledPoint.y)
                                } else {
                                    currentPath.lineTo(scaledPoint.x, scaledPoint.y)
                                }
                            }
                            lastPoint = scaledPoint
                        }
                        paths.add(currentPath)

                        paths.forEach { path ->
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}