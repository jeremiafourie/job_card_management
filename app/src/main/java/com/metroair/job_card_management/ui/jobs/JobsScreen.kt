package com.metroair.job_card_management.ui.jobs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metroair.job_card_management.domain.model.JobCard
import com.metroair.job_card_management.domain.model.JobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    viewModel: JobsViewModel = hiltViewModel(),
    onJobSelected: (Int) -> Unit,
    onCreateJob: () -> Unit = {}
) {
    val jobs by viewModel.filteredJobs.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val isActiveFilter by viewModel.isActiveFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jobs") },
                actions = {
                    IconButton(onClick = onCreateJob) { Icon(Icons.Default.Add, contentDescription = "New Job") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                FilterChip(
                    selected = isActiveFilter,
                    onClick = { viewModel.filterByActive() },
                    label = { Text("Active") },
                    leadingIcon = if (isActiveFilter) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                )
                JobStatus.values().forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.filterByStatus(status) },
                        label = { Text(status.name) },
                        leadingIcon = if (selectedStatus == status) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No jobs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(jobs) { job -> JobCardItem(job, onClick = { onJobSelected(job.id) }) }
                }
            }
        }
    }
}

@Composable
private fun JobCardItem(job: JobCard, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Job #${job.jobNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(job.title, style = MaterialTheme.typography.bodyLarge)
                    Text(job.customerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    job.serviceAddress?.let { addr -> Text(addr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                AssistChip(onClick = {}, label = { Text(job.status.name) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                job.scheduledDate?.let { date -> Text("Date: $date", style = MaterialTheme.typography.bodySmall) }
                job.scheduledTime?.let { time -> Text("Time: $time", style = MaterialTheme.typography.bodySmall) }
                Text(job.priority.name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
