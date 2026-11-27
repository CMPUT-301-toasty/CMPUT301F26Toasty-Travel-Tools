/*
AdminDashboardScreen.kt

Purpose: Entry point for admin tools. Reached only from the "Admin tools" row on Profile,
which is shown only when entrants/{deviceId}.role == "admin". The route itself is also
wrapped in AdminGate in ToastyTravelToolsApp, so non-admins are redirected to Explore.
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.toastytraveltools.view.components.ToastyNavRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin tools") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Manage your community",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToastyNavRow(title = "Registered Campsites", onClick = { navController.navigate("admin_campsites") })
            ToastyNavRow(title = "User Profiles", onClick = { navController.navigate("admin_profiles") })
            ToastyNavRow(title = "Uploaded Images", onClick = { navController.navigate("admin_images") })
            ToastyNavRow(title = "User Reviews", onClick = { navController.navigate("admin_reviews") })
            ToastyNavRow(
                title = "Notification Logs",
                onClick = { navController.navigate("admin_notification_logs") }
            )
        }
    }
}
