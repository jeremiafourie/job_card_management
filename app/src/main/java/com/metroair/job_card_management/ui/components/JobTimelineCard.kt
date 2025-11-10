package com.metroair.job_card_management.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class TimelineEvent(
    val title: String,
    val timestamp: Long,
    val icon: ImageVector,
    val color: Color,
    val duration: String? = null,
    val details: String? = null
)

@Composable
fun JobTimelineCard(
    job: JobCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Job Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            val events = buildTimelineEvents(job)

            if (events.isEmpty()) {
                Text(
                    text = "No timeline data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                events.forEachIndexed { index, event ->
                    TimelineItem(
                        event = event,
                        isLast = index == events.lastIndex
                    )
                    if (index < events.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Time spent on job
                if (job.status == JobStatus.BUSY || job.status == JobStatus.PAUSED || job.status == JobStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Time on Job",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = calculateActiveWorkTime(job),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if ((job.pausedTime ?: 0) > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Time Paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(job.pausedTime ?: 0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    event: TimelineEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = event.color,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = event.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Divider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        // Event details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            event.duration?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            event.details?.let { details ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

private fun buildTimelineEvents(job: JobCard): List<TimelineEvent> {
    val events = mutableListOf<TimelineEvent>()
    val now = System.currentTimeMillis()

    // Job accepted
    job.acceptedAt?.let { acceptedAt ->
        events.add(
            TimelineEvent(
                title = "Job Accepted",
                timestamp = acceptedAt,
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
        )
    }

    // En Route
    job.enRouteStartTime?.let { enRouteTime ->
        val duration = job.startTime?.let {
            formatDuration(it - enRouteTime)
        }
        events.add(
            TimelineEvent(
                title = "En Route to Location",
                timestamp = enRouteTime,
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFF9C27B0),
                duration = duration?.let { "Travel time: $it" }
            )
        )
    }

    // Job started (BUSY)
    job.startTime?.let { startTime ->
        if (job.status != JobStatus.PENDING) {
            events.add(
                TimelineEvent(
                    title = "Work Started",
                    timestamp = startTime,
                    icon = Icons.Default.PlayArrow,
                    color = Color(0xFF2196F3),
                    duration = if (job.status == JobStatus.COMPLETED) {
                        "Duration: ${calculateActiveWorkTime(job)}"
                    } else null
                )
            )
        }
    }

    // Pause and Resume events from history
    val pauseEvents = parsePauseHistory(job.pauseHistory)
    pauseEvents.forEach { (timestamp, reason, duration) ->
        // Add pause event
        events.add(
            TimelineEvent(
                title = "Job Paused",
                timestamp = timestamp,
                icon = Icons.Default.Pause,
                color = Color(0xFFFF9800),
                details = reason,
                duration = duration?.let { "Paused for: ${formatDuration(it)}" }
            )
        )

        // Add resume event if pause has completed (duration > 0)
        duration?.let { pauseDuration ->
            if (pauseDuration > 0) {
                events.add(
                    TimelineEvent(
                        title = "Work Resumed",
                        timestamp = timestamp + pauseDuration,
                        icon = Icons.Default.PlayArrow,
                        color = Color(0xFF2196F3)
                    )
                )
            }
        }
    }

    // Job completed
    if (job.status == JobStatus.COMPLETED) {
        job.endTime?.let { endTime ->
            events.add(
                TimelineEvent(
                    title = "Job Completed",
                    timestamp = endTime,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
            )
        }
    }

    // Job cancelled
    if (job.status == JobStatus.CANCELLED) {
        events.add(
            TimelineEvent(
                title = "Job Cancelled",
                timestamp = now,
                icon = Icons.Default.Cancel,
                color = Color(0xFFF44336),
                details = extractCancellationReason(job.technicianNotes)
            )
        )
    }

    return events.sortedBy { it.timestamp }
}

private fun parsePauseHistory(pauseHistory: String?): List<Triple<Long, String, Long?>> {
    if (pauseHistory == null) return emptyList()

    return try {
        val jsonArray = org.json.JSONArray(pauseHistory)
        (0 until jsonArray.length()).map { index ->
            val pauseEvent = jsonArray.getJSONObject(index)
            Triple(
                pauseEvent.getLong("timestamp"),
                pauseEvent.getString("reason"),
                pauseEvent.optLong("duration", 0L).takeIf { it > 0 }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun extractCancellationReason(technicianNotes: String?): String? {
    if (technicianNotes == null) return null

    val cancelPattern = """\[CANCELLED: (.+?)\]""".toRegex()
    return cancelPattern.find(technicianNotes)?.groupValues?.get(1)
}

private fun calculateActiveWorkTime(job: JobCard): String {
    val startTime = job.startTime ?: return "0h 0m"
    val endTime = job.endTime ?: System.currentTimeMillis()

    val totalDuration = endTime - startTime
    val pausedTime = job.pausedTime ?: 0L
    val activeDuration = (totalDuration - pausedTime).coerceAtLeast(0L)

    return formatDuration(activeDuration)
}

private fun formatDuration(durationMs: Long): String {
    val hours = durationMs / (1000 * 60 * 60)
    val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
