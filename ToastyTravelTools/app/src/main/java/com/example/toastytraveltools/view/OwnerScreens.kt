/*
OwnerScreens.kt

Purpose: Forms for campsite owners: list a new campsite (anyone can, and becomes its owner),
edit one of your campsites, and create a lottery for one or more of them. They are opened from
the Explore screen ("List a campsite"), from Profile ("Create a lottery") and from the owner
actions on a campsite's Details page, which pass the campsite to preselect.
 */

package com.example.toastytraveltools.view

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.net.Uri
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenAddCampsite(
    repository: CampsiteRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    val pickedImages = remember { mutableStateListOf<Uri>() }
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            CampsiteImagePicker(
                existingUrls = emptyList(),
                newImages = pickedImages,
                onAddImages = { uris -> pickedImages.addAll(uris.filterNot { it in pickedImages }) },
                onRemoveExisting = {},
                onRemoveNew = { pickedImages.remove(it) },
                enabled = !isSaving
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated, Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                shape = MaterialTheme.shapes.small,
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
                        name = name,
                        location = location,
                        description = description,
                        occupancyLimit = occupancyInt,
                        tags = tagList,
                        qrCodeUrl = generatedQrUrl,
                        ownerId = deviceId
                    )

                    repository.addCampsite(
                        campsite = newCampsite,
                        imageUris = pickedImages.toList(),
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
                Text(
                    when {
                        !isSaving -> "Save Campsite"
                        pickedImages.isNotEmpty() -> "Uploading photos..."
                        else -> "Saving..."
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenCreateLottery(
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    onNavigateBack: () -> Unit,
    initialCampsiteId: String? = null
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    
    // Get all campsites and filter by this owner
    // reading the snapshot list here recomposes when the repository finishes loading
    val myCampsites = ownedCampsites(campsiteRepository.campsites, deviceId)

    // ids of the campsites in this lottery; a lottery can cover several of the owner's campsites
    val selectedCampsiteIds = remember {
        mutableStateListOf<String>().apply {
            initialCampsiteId?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
    // an owner with a single campsite has nothing to choose, so select it once it has loaded
    LaunchedEffect(myCampsites.size) {
        if (myCampsites.size == 1 && selectedCampsiteIds.isEmpty()) {
            selectedCampsiteIds.add(myCampsites.first().id)
        }
    }
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Text(
                text = if (myCampsites.size > 1) "Campsites in this lottery *" else "Campsite *",
                style = MaterialTheme.typography.labelLarge
            )
            if (myCampsites.isEmpty()) {
                Text(
                    text = "List a campsite before creating a lottery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            myCampsites.forEach { site ->
                val checked = site.id in selectedCampsiteIds
                val toggle = {
                    if (checked) selectedCampsiteIds.remove(site.id) else selectedCampsiteIds.add(site.id)
                }
                // each campsite sits in its own small white card; text colours are fixed dark so
                // they stay readable on white in dark theme too
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, MaterialTheme.shapes.small)
                        .clickable { toggle() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { toggle() },
                        colors = CheckboxDefaults.colors(uncheckedColor = Color.DarkGray)
                    )
                    Column {
                        Text(site.name, color = Color.Black)
                        val guests = "Max ${site.occupancyLimit} " +
                            if (site.occupancyLimit == 1) "guest" else "guests"
                        Text(
                            text = listOf(site.location, guests)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

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
                shape = MaterialTheme.shapes.small,
                onClick = {
                    if (registrationStart.isBlank() || registrationEnd.isBlank() || bookingStart.isBlank() || bookingEnd.isBlank() || numberOfWinners.isBlank()) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }
                    // only the owner's own campsites can go in the lottery
                    val chosenIds = selectedCampsiteIds.filter { id -> myCampsites.any { it.id == id } }
                    if (chosenIds.isEmpty()) {
                        errorMessage = "Select at least one campsite."
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
                        campsiteIds = chosenIds,
                        ownerId = deviceId,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenEditCampsite(
    repository: CampsiteRepository,
    onNavigateBack: () -> Unit,
    initialCampsiteId: String? = null
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    
    // Get all campsites and filter by this owner
    // reading the snapshot list here recomposes when the repository finishes loading
    val myCampsites = ownedCampsites(repository.campsites, deviceId)

    var selectedCampsiteId by remember { mutableStateOf(initialCampsiteId.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    // photos already in Storage that the owner is keeping / has removed, plus new picks
    val keptImages = remember { mutableStateListOf<String>() }
    val removedImages = remember { mutableStateListOf<String>() }
    val newImages = remember { mutableStateListOf<Uri>() }
    var tags by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // when selection changes, populate the fields. The repository loads asynchronously, so a
    // preselected campsite may only appear in myCampsites later; fill the fields once it does.
    var populatedFor by remember { mutableStateOf<String?>(null) }
    val selectedSite = myCampsites.find { it.id == selectedCampsiteId }
    LaunchedEffect(selectedCampsiteId, selectedSite != null) {
        val site = selectedSite
        if (site != null && populatedFor != site.id) {
            populatedFor = site.id
            name = site.name
            location = site.location
            description = site.description
            occupancy = site.occupancyLimit.toString()
            keptImages.clear()
            keptImages.addAll(site.imageUrls)
            removedImages.clear()
            newImages.clear()
            tags = site.tags.joinToString(", ")
            errorMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Campsite") },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = if (selectedCampsiteId.isNotBlank()) { 
                        myCampsites.find { it.id == selectedCampsiteId }?.name ?: selectedCampsiteId
                    } else { "" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Campsite to Edit *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    myCampsites.forEach { site ->
                        DropdownMenuItem(
                            text = { Text(site.name) },
                            onClick = {
                                selectedCampsiteId = site.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedCampsiteId.isNotBlank()) {
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

                CampsiteImagePicker(
                    existingUrls = keptImages,
                    newImages = newImages,
                    onAddImages = { uris -> newImages.addAll(uris.filterNot { it in newImages }) },
                    onRemoveExisting = { url ->
                        keptImages.remove(url)
                        removedImages.add(url)
                    },
                    onRemoveNew = { newImages.remove(it) },
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated, Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    shape = MaterialTheme.shapes.small,
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

                        val existingSite = myCampsites.find { it.id == selectedCampsiteId }!!
                        val updatedCampsite = existingSite.copy(
                            name = name,
                            location = location,
                            description = description,
                            occupancyLimit = occupancyInt,
                            imageUrls = keptImages.toList(),
                            tags = tagList
                        )

                        repository.updateCampsite(
                            campsite = updatedCampsite,
                            newImageUris = newImages.toList(),
                            removedImageUrls = removedImages.toList(),
                            onSuccess = { onNavigateBack() },
                            onFailure = { e -> 
                                isSaving = false
                                errorMessage = "Failed to update: ${e.message}"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text(
                        when {
                            !isSaving -> "Save Changes"
                            newImages.isNotEmpty() -> "Uploading photos..."
                            else -> "Saving..."
                        }
                    )
                }
            }
        }
    }
}
