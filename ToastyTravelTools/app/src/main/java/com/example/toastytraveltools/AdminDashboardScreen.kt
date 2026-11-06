package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") {
                                    inclusive = false
                                }
                            }
                        }
                    ) {
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Manage your community")

            Button(
                onClick = { navController.navigate("admin_campsites") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registered Campsites")
            }

            Button(
                onClick = { navController.navigate("admin_profiles") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("User Profiles")
            }

            Button(
                onClick = { navController.navigate("admin_images") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uploaded Images")
            }
        }
    }
}