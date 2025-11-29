package com.metroair.job_card_management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.metroair.job_card_management.ui.dashboard.DashboardScreen
import com.metroair.job_card_management.ui.jobdetail.JobDetailScreen
import com.metroair.job_card_management.ui.jobs.JobsScreen
import com.metroair.job_card_management.ui.assets.AssetsScreen
import com.metroair.job_card_management.ui.settings.SettingsScreen
import com.metroair.job_card_management.ui.theme.JobCardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JobCardTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom bar on main screens
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute?.startsWith("jobDetail/") != true) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("jobs") {
                JobsScreen(
                    onJobSelected = { jobId -> navController.navigate("jobDetail/$jobId") },
                    onCreateJob = { /* TODO: navigate to job creation */ }
                )
            }
            composable(
                route = "jobs?status={status}",
                arguments = listOf(
                    navArgument("status") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                JobsScreen(
                    onJobSelected = { jobId -> navController.navigate("jobDetail/$jobId") },
                    onCreateJob = { /* TODO: navigate to job creation */ }
                )
            }
            composable("assets") { AssetsScreen() }
            composable("settings") { SettingsScreen() }
            composable(
                route = "jobDetail/{jobId}",
                arguments = listOf(navArgument("jobId") { type = NavType.IntType })
            ) {
                JobDetailScreen(navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("dashboard", "Dashboard", Icons.Default.Home),
        BottomNavItem("jobs", "Jobs", Icons.Default.Work),
        BottomNavItem("assets", "Assets", Icons.Default.Build),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            // Determine if this item should be selected based on current route
            val isSelected = when {
                item.route == "jobs" -> currentRoute?.startsWith("jobs") == true
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    // Only navigate if not already on this exact route
                    if (currentRoute != item.route || item.route == "jobs") {
                        navController.navigate(item.route) {
                            popUpTo("dashboard")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)
