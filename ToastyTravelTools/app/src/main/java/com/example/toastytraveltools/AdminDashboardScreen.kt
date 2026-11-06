package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AdminDashboardScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Manage your community")

        Button(
            onClick = {
                navController.navigate("admin_campsites")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registered Campsites")
        }

        Button(
            onClick = {
                navController.navigate("admin_profiles")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("User Profiles")
        }

        Button(
            onClick = {
                navController.navigate("admin_images")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Uploaded Images")
        }
    }
}