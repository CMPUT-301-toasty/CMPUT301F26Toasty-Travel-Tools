/*
OwnerScreens.kt

Purpose: Forms for campsite owners: list a new campsite (anyone can, and becomes its owner),
edit one of your campsites, and create a lottery for one or more of them. They are opened from
the Explore screen ("List a campsite"), from Profile ("Create a lottery") and from the owner
actions on a campsite's Details page, which pass the campsite to preselect.
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone


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

/**
 * One of the owner's campsites with a checkbox, used to pick a lottery's campsites on the
 * create and edit lottery forms. Tapping anywhere on the row toggles it.
 */
@Composable
fun CampsitePickerRow(site: Campsite, checked: Boolean, onToggle: () -> Unit) {
    // each campsite is its own box, styled like the boxes on the lottery details screen
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Column {
            Text(site.name, style = MaterialTheme.typography.titleSmall)
            val guests = "Max ${site.occupancyLimit} " +
                if (site.occupancyLimit == 1) "guest" else "guests"
            Text(
                text = listOf(site.location, guests)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// The lottery forms keep dates as yyyy-MM-dd days. The date picker works in midnight-UTC millis,
// so these convert between the two in UTC, where a day is always exactly 24 hours.
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun utcDayFormat(pattern: String) =
    SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

/** A yyyy-MM-dd day as the date picker's millis, or null if it isn't a real day. */
fun lotteryDayToPickerMillis(day: String): Long? {
    val trimmed = day.trim()
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)) return null
    return try { utcDayFormat("yyyy-MM-dd").parse(trimmed)?.time } catch (e: Exception) { null }
}

/** The date picker's millis as a yyyy-MM-dd day. */
fun pickerMillisToLotteryDay(millis: Long): String = utcDayFormat("yyyy-MM-dd").format(Date(millis))

/** Today on this device, as a yyyy-MM-dd day. */
fun todayLotteryDay(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

/** The first day booking can open: the day after registration closes, or tomorrow if that isn't set. */
fun firstBookingDay(registrationEnd: String, today: String = todayLotteryDay()): String {
    val base = lotteryDayToPickerMillis(registrationEnd) ?: lotteryDayToPickerMillis(today)!!
    return pickerMillisToLotteryDay(base + DAY_MILLIS)
}

private fun displayLotteryDay(day: String): String =
    lotteryDayToPickerMillis(day)?.let { utcDayFormat("MMM d, yyyy").format(Date(it)) } ?: "Not set"

/**
 * A date window on the lottery forms: shows the chosen start and end days and opens a calendar
 * to change them, the same calendar used when reserving. Days before [firstSelectableDay] are
 * greyed out. [onChange] gets the new start and end as yyyy-MM-dd days.
 */
@Composable
fun LotteryDateRangeField(
    pickerTitle: String,
    start: String,
    end: String,
    firstSelectableDay: String?,
    onChange: (start: String, end: String) -> Unit,
    enabled: Boolean = true
) {
    var picking by remember { mutableStateOf(false) }

    listOf("Opens" to start, "Closes" to end).forEach { (label, day) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(displayLotteryDay(day), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
    OutlinedButton(
        onClick = { picking = true },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (lotteryDayToPickerMillis(start) == null) "Choose dates" else "Change dates")
    }

    if (picking) {
        LotteryDateRangeDialog(
            title = pickerTitle,
            start = start,
            end = end,
            firstSelectableDay = firstSelectableDay,
            onDismiss = { picking = false },
            onConfirm = { newStart, newEnd ->
                picking = false
                onChange(newStart, newEnd)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LotteryDateRangeDialog(
    title: String,
    start: String,
    end: String,
    firstSelectableDay: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val firstMillis = firstSelectableDay?.let { lotteryDayToPickerMillis(it) }
    val startMillis = lotteryDayToPickerMillis(start)
    // the picker rejects an end before the start, so drop a bad saved end
    val endMillis = lotteryDayToPickerMillis(end)?.takeIf { startMillis != null && it >= startMillis }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startMillis,
        initialSelectedEndDateMillis = endMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                firstMillis == null || utcTimeMillis >= firstMillis
        }
    )
    var error by remember { mutableStateOf<String?>(null) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val pickedStart = state.selectedStartDateMillis
                // a single tapped day is a one-day window
                val pickedEnd = state.selectedEndDateMillis ?: pickedStart
                if (pickedStart == null || pickedEnd == null) {
                    error = "Pick a start and end date"
                } else {
                    onConfirm(pickerMillisToLotteryDay(pickedStart), pickerMillisToLotteryDay(pickedEnd))
                }
            }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = { Text(text = title, modifier = Modifier.padding(16.dp)) }
            )
            Text(
                text = "Greyed out dates are unavailable",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
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
    
    LotteryFormScaffold(title = "Create Lottery", onNavigateBack = onNavigateBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LotterySectionTitle(if (myCampsites.size > 1) "Campsites in this lottery *" else "Campsite *")
                if (myCampsites.isEmpty()) {
                    Text(
                        text = "List a campsite before creating a lottery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                myCampsites.forEach { site ->
                    val checked = site.id in selectedCampsiteIds
                    CampsitePickerRow(site = site, checked = checked) {
                        if (checked) selectedCampsiteIds.remove(site.id) else selectedCampsiteIds.add(site.id)
                    }
                }
            }

            LotteryFormSection("Winners") {
                OutlinedTextField(
                    value = numberOfWinners,
                    onValueChange = { numberOfWinners = it },
                    label = { Text("Number of Winners *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LotteryFormSection("Registration window *") {
                LotteryDateRangeField(
                    pickerTitle = "Pick the registration window",
                    start = registrationStart,
                    end = registrationEnd,
                    // registration can't start in the past
                    firstSelectableDay = todayLotteryDay(),
                    onChange = { start, end ->
                        registrationStart = start
                        registrationEnd = end
                    }
                )
            }

            LotteryFormSection("Booking window *") {
                LotteryDateRangeField(
                    pickerTitle = "Pick the booking window",
                    start = bookingStart,
                    end = bookingEnd,
                    // booking opens after registration closes
                    firstSelectableDay = firstBookingDay(registrationEnd),
                    onChange = { start, end ->
                        bookingStart = start
                        bookingEnd = end
                    }
                )
            }

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

// The look of the lottery forms, matching the lottery details screen: a white page with
// bordered off-white boxes.

/** White page, or the normal dark background in dark theme. */
@Composable
private fun lotteryPageColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color.White
    else MaterialTheme.colorScheme.background

/** A bold section title, like "Selected campsites" on the details screen. */
@Composable
private fun LotterySectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

/** A titled box holding part of a lottery form. */
@Composable
private fun LotteryFormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LotterySectionTitle(title)
        Card(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** The white page with a Back button and title that the form sits in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LotteryFormScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val page = lotteryPageColor()
    Scaffold(
        containerColor = page,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = page)
            )
        },
        content = content
    )
}
