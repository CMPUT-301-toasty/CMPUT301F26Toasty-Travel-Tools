package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    onNavigateToAddCampsite: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Owner Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onNavigateToAddCampsite,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Add New Campsite")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenAddCampsite(
    repository: CampsiteRepository,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Campsite") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Campsite Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Map location) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Amenities) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = occupancy,
                onValueChange = { occupancy = it },
                label = { Text("Maximum Occupancy *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Image URL (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated, Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (name.isBlank() || location.isBlank() || description.isBlank() || occupancy.isBlank()) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }
                    val occupancyInt = occupancy.toIntOrNull()
                    if (occupancyInt == null || occupancyInt <= 0) {
                        errorMessage = "Please enter a valid occupancy number."
                        return@Button
                    }

                    errorMessage = ""
                    isSaving = true

                    val tagList = if (tags.isNotBlank()) {
                        tags.split(",").map { it.trim() }
                    } else {
                        emptyList()
                    }
                    
                    val generatedQrUrl = "qr://campsite/${System.currentTimeMillis()}"

                    val newCampsite = Campsite(
                        Name = name,
                        Location = location,
                        Description = description,
                        OccupancyLimit = occupancyInt,
                        ImagesUrl = if (imageUrl.isBlank()) "default_image_url" else imageUrl,
                        Tags = tagList,
                        QrCodeUrl = generatedQrUrl
                    )

                    repository.addCampsite(
                        campsite = newCampsite,
                        onSuccess = {
                            isSaving = false
                            onNavigateBack()
                        },
                        onFailure = { e ->
                            isSaving = false
                            errorMessage = "Failed to save: ${e.message}"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save Campsite")
            }
        }
    }
}
