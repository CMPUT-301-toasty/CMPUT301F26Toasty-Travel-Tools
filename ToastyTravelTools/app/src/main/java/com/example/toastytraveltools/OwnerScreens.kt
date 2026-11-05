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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    onNavigateToAddCampsite: () -> Unit,
    onNavigateToCreateLottery: () -> Unit,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateToCreateLottery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Create Lottery")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenCreateLottery(
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    onNavigateBack: () -> Unit
) {
    var campsiteId by remember { mutableStateOf("") }
    var registrationStart by remember { mutableStateOf("") }
    var registrationEnd by remember { mutableStateOf("") }
    var bookingStart by remember { mutableStateOf("") }
    var bookingEnd by remember { mutableStateOf("") }
    var numberOfWinners by remember { mutableStateOf("") }
    
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Lottery") },
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
                value = campsiteId,
                onValueChange = { campsiteId = it },
                label = { Text("Campsite ID *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = registrationStart,
                onValueChange = { registrationStart = it },
                label = { Text("Registration Start (yyyy-mm-dd) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = registrationEnd,
                onValueChange = { registrationEnd = it },
                label = { Text("Registration End (yyyy-mm-dd) *") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = bookingStart,
                onValueChange = { bookingStart = it },
                label = { Text("Booking Start Date (yyyy-mm-dd) *") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = bookingEnd,
                onValueChange = { bookingEnd = it },
                label = { Text("Booking End Date (yyyy-mm-dd) *") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = numberOfWinners,
                onValueChange = { numberOfWinners = it },
                label = { Text("Number of Winners *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (campsiteId.isBlank() || registrationStart.isBlank() || registrationEnd.isBlank() || bookingStart.isBlank() || bookingEnd.isBlank() || numberOfWinners.isBlank()) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }
                    
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val regStart = try { fmt.parse(registrationStart) } catch (e: Exception) { null }
                    val regEnd = try { fmt.parse(registrationEnd) } catch (e: Exception) { null }
                    val bookStart = try { fmt.parse(bookingStart) } catch (e: Exception) { null }
                    val bookEnd = try { fmt.parse(bookingEnd) } catch (e: Exception) { null }
                    
                    if (regStart == null || regEnd == null || bookStart == null || bookEnd == null) {
                        errorMessage = "Dates must be in yyyy-MM-dd format."
                        return@Button
                    }
                    
                    if (regEnd.before(regStart)) {
                        errorMessage = "Registration end cannot be before start."
                        return@Button
                    }
                    
                    if (bookStart.before(regEnd) || bookStart.equals(regEnd)) {
                        errorMessage = "Booking period must begin after the registration period ends."
                        return@Button
                    }
                    if (bookEnd.before(bookStart)) {
                        errorMessage = "Booking end cannot be before start."
                        return@Button
                    }
                    
                    val winners = numberOfWinners.toIntOrNull()
                    if (winners == null || winners <= 0) {
                        errorMessage = "Please enter a valid number of winners."
                        return@Button
                    }
                    
                    errorMessage = ""
                    isSaving = true
                    
                    val newLottery = Lottery(
                        campsiteIds = listOf(campsiteId),
                        ownerId = "owner123", // mocked for now until auth is fully piped
                        registrationStart = Timestamp(regStart),
                        registrationEnd = Timestamp(regEnd),
                        bookingStartDate = Timestamp(bookStart),
                        bookingEndDate = Timestamp(bookEnd),
                        numberOfWinners = winners
                    )
                    
                    lotteryRepository.addLottery(
                        lottery = newLottery,
                        onSuccess = { onNavigateBack() },
                        onFailure = { e -> 
                            isSaving = false
                            errorMessage = "Failed: ${e.message}"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Creating..." else "Create Lottery")
            }
        }
    }
}
