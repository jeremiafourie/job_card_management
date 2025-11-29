package com.metroair.job_card_management.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
                Spacer(modifier = Modifier.height(8.dp))
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
                        .height(32.dp)
                        .padding(vertical = 2.dp)
                ) {
                    HorizontalDivider(
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
    // Prefer statusHistory (normalized) for the timeline; fall back to legacy fields.
    val statusEvents = parseStatusHistory(job.statusHistory)
    if (statusEvents.isNotEmpty()) {
        return statusEvents.map { event ->
            when (event.status) {
                JobStatus.AVAILABLE -> TimelineEvent(
                    title = "Job Available",
                    timestamp = event.timestamp,
                    icon = Icons.Default.CheckCircle,
                    color = Color.Gray
                )
                JobStatus.AWAITING, JobStatus.PENDING -> TimelineEvent(
                    title = if (event.status == JobStatus.AWAITING) "Awaiting Acceptance" else "Scheduled",
                    timestamp = event.timestamp,
                    icon = Icons.Default.Schedule,
                    color = Color(0xFF1976D2)
                )
                JobStatus.EN_ROUTE -> TimelineEvent(
                    title = "En Route",
                    timestamp = event.timestamp,
                    icon = Icons.Default.DirectionsCar,
                    color = Color(0xFF9C27B0)
                )
                JobStatus.BUSY -> TimelineEvent(
                    title = "Work In Progress",
                    timestamp = event.timestamp,
                    icon = Icons.Default.PlayArrow,
                    color = Color(0xFF2196F3)
                )
                JobStatus.PAUSED -> TimelineEvent(
                    title = "Paused",
                    timestamp = event.timestamp,
                    icon = Icons.Default.Pause,
                    color = Color(0xFFFF9800),
                    details = event.reason
                )
                JobStatus.COMPLETED -> TimelineEvent(
                    title = "Completed",
                    timestamp = event.timestamp,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
                JobStatus.SIGNED -> TimelineEvent(
                    title = "Signed Off",
                    timestamp = event.timestamp,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF2E7D32),
                    details = event.signedBy?.let { "Signed by $it" }
                )
                JobStatus.CANCELLED -> TimelineEvent(
                    title = "Cancelled",
                    timestamp = event.timestamp,
                    icon = Icons.Default.Cancel,
                    color = Color(0xFFF44336),
                    details = event.reason
                )
            }
        }.filterNotNull().sortedBy { it.timestamp }
    }

    return emptyList()
}

private data class StatusHistoryEvent(
    val status: JobStatus,
    val timestamp: Long,
    val reason: String? = null,
    val signedBy: String? = null
)

private fun parseStatusHistory(historyJson: String?): List<StatusHistoryEvent> {
    if (historyJson.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(historyJson)
        (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.getJSONObject(idx)
            val status = try { JobStatus.valueOf(obj.getString("status")) } catch (_: Exception) { null } ?: return@mapNotNull null
            val ts = obj.optLong("timestamp", 0L)
            val reason = if (obj.has("reason")) obj.optString("reason").takeIf { it.isNotBlank() } else null
            val signedBy = if (obj.has("signed_by")) obj.optString("signed_by").takeIf { it.isNotBlank() } else null
            StatusHistoryEvent(status, ts, reason, signedBy)
        }
    } catch (e: Exception) {
        emptyList()
    }
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
