package com.metroair.job_card_management.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var showSyncDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sync Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Sync Settings") },
                        supportingContent = {
                            Text("Last synced: Never")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Sync, contentDescription = null)
                        },
                        trailingContent = {
                            Button(
                                onClick = { showSyncDialog = true }
                            ) {
                                Text("Sync Now")
                            }
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Auto Sync") },
                        supportingContent = { Text("Automatically sync data when connected") },
                        leadingContent = {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                        },
                        trailingContent = {
                            var checked by remember { mutableStateOf(true) }
                            Switch(
                                checked = checked,
                                onCheckedChange = { checked = it }
                            )
                        }
                    )
                }
            }
        }

        // User Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("User Profile") },
                        supportingContent = {
                            Column {
                                Text("Mike Wilson")
                                Text("mike.wilson@metroair.com")
                                Text("Technician")
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                }
            }
        }

        // App Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Notifications") },
                        supportingContent = { Text("Enable job notifications") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        trailingContent = {
                            var enabled by remember { mutableStateOf(true) }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { enabled = it }
                            )
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        supportingContent = { Text("Use system default") },
                        leadingContent = {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                        },
                        trailingContent = {
                            var darkMode by remember { mutableStateOf(false) }
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { darkMode = it }
                            )
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Clear Cache") },
                        supportingContent = { Text("Clear temporary data") },
                        leadingContent = {
                            Icon(Icons.Default.CleaningServices, contentDescription = null)
                        },
                        modifier = Modifier.clickable { /* TODO: Clear cache */ }
                    )
                }
            }
        }

        // About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("About") },
                        supportingContent = {
                            Column {
                                Text("Job Card Management")
                                Text("Version 1.0.0")
                                Text("© 2025 MetroAir Services")
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Help & Support") },
                        supportingContent = { Text("Get help with the app") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                        },
                        modifier = Modifier.clickable { /* TODO: Open help */ }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Terms & Conditions") },
                        supportingContent = { Text("View terms of service") },
                        leadingContent = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.clickable { /* TODO: Open terms */ }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Privacy Policy") },
                        supportingContent = { Text("View privacy policy") },
                        leadingContent = {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null)
                        },
                        modifier = Modifier.clickable { /* TODO: Open privacy policy */ }
                    )
                }
            }
        }

        // Logout Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Sign Out",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    },
                    supportingContent = {
                        Text(
                            "Sign out of your account",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    },
                    modifier = Modifier.clickable { /* TODO: Sign out */ }
                )
            }
        }
    }

    // Sync Dialog
    if (showSyncDialog) {
        SyncDialog(
            onDismiss = { showSyncDialog = false }
        )
    }
}

@Composable
fun SyncDialog(
    onDismiss: () -> Unit
) {
    var isSyncing by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = { Text("Syncing Data") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSyncing) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Synchronizing data with server...")
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sync completed successfully!")
                }
            }
        },
        confirmButton = {
            if (!isSyncing) {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )

    // Simulate sync
    LaunchedEffect(Unit) {
        delay(2000)
        isSyncing = false
    }
}
