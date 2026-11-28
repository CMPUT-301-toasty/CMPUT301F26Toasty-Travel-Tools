/*
EditLotteryScreen.kt

Purpose: Lets a lottery's owner change it: which of their campsites are in it, the number of
winners, the registration window, the booking window, and an optional cap on the waitlist size.
Opened from the "Edit lottery" button on the lottery details screen.

Rules:
 - the same date rules as creating a lottery (yyyy-MM-dd, registration ends after it starts,
   booking begins after registration ends, booking ends after it starts)
 - at least one campsite, and only the owner's own campsites; after the draw a campsite can't be
   removed while a winner has a booking there from this lottery
 - the waitlist cap can't be set below the number of people already on the waitlist
 - once a lottery has been drawn, the winners, registration window and waitlist cap are locked
   because the draw already used them; the booking window can still change, but must still cover
   any reservations winners have already made

Dates are picked on a calendar (LotteryDateRangeField in OwnerScreens.kt). Days that can't be
picked are greyed out: registration can't start in the past, and booking can't open until the
day after registration closes.

Issues:
 - the calendar can't grey out days that would leave a winner's booking outside the booking
   window, so that is only checked when saving
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the owner typed into the edit form. */
data class LotteryEditInput(
    val campsiteIds: List<String>,
    val numberOfWinners: String,
    val registrationStart: String,
    val registrationEnd: String,
    val bookingStart: String,
    val bookingEnd: String,
    val limitWaitlist: Boolean,
    val waitlistCapacity: String
)

/** The result of checking an edit: either the Firestore fields to save, or what's wrong. */
sealed class LotteryEditResult {
    data class Valid(val updates: Map<String, Any?>) : LotteryEditResult()
    data class Invalid(val message: String) : LotteryEditResult()
}

private const val DATE_PATTERN = "yyyy-MM-dd"

private fun dateFormat() = SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).apply { isLenient = false }

/** Parses a yyyy-MM-dd date, or null if it isn't a real date in that format. */
fun parseLotteryDate(text: String): Date? {
    val trimmed = text.trim()
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)) return null
    return try { dateFormat().parse(trimmed) } catch (e: Exception) { null }
}

/** Formats a timestamp for the edit form's date fields. */
fun formatLotteryInputDate(timestamp: Timestamp?): String =
    timestamp?.let { dateFormat().format(it.toDate()) } ?: ""

/** Fills the edit form with the lottery's current values. */
fun lotteryEditInputFrom(lottery: Lottery) = LotteryEditInput(
    campsiteIds = lottery.campsiteIds.distinct(),
    numberOfWinners = lottery.numberOfWinners.takeIf { it > 0 }?.toString() ?: "",
    registrationStart = formatLotteryInputDate(lottery.registrationStart),
    registrationEnd = formatLotteryInputDate(lottery.registrationEnd),
    bookingStart = formatLotteryInputDate(lottery.bookingStartDate),
    bookingEnd = formatLotteryInputDate(lottery.bookingEndDate),
    limitWaitlist = lottery.waitlistCapacity != null,
    waitlistCapacity = lottery.waitlistCapacity?.toString() ?: ""
)

/** True once the draw has happened, which locks the winners, registration window and cap. */
fun isLotteryDrawn(lottery: Lottery): Boolean = lottery.status == "drawn"

/** Reservations winners made through this lottery that still count. */
private fun activeLotteryReservations(lottery: Lottery) =
    lottery.reservations.filter { it.entrant_id !in lottery.cancelledWinners && it.status == "active" }

/**
 * Checks the owner's edit against [lottery] as it is now and works out the fields to save.
 * [ownedCampsites] are the owner's campsites; only these can be in the lottery, so a
 * campsite that was deleted drops out when the lottery is saved.
 */
fun validateLotteryEdit(
    lottery: Lottery,
    ownedCampsites: List<Campsite>,
    input: LotteryEditInput
): LotteryEditResult {
    val drawn = isLotteryDrawn(lottery)
    val updates = mutableMapOf<String, Any?>()

    // keep the order the owner picked them in, and only their own campsites
    val chosenIds = input.campsiteIds.distinct().filter { id -> ownedCampsites.any { it.id == id } }
    if (chosenIds.isEmpty()) {
        return LotteryEditResult.Invalid("Select at least one campsite.")
    }
    // reservations only store the campsite name, so match removed campsites by name
    val removedNames = ownedCampsites
        .filter { it.id in lottery.campsiteIds && it.id !in chosenIds }
        .map { it.name }
    val bookedRemoved = activeLotteryReservations(lottery)
        .map { it.campsiteName }
        .filter { it in removedNames }
        .distinct()
    if (bookedRemoved.isNotEmpty()) {
        return LotteryEditResult.Invalid(
            "Winners have already booked ${bookedRemoved.joinToString(", ")}, so it can't be " +
                "removed from this lottery."
        )
    }
    updates["campsiteIds"] = chosenIds

    val bookStart = parseLotteryDate(input.bookingStart)
    val bookEnd = parseLotteryDate(input.bookingEnd)
    if (bookStart == null || bookEnd == null) {
        return LotteryEditResult.Invalid("Booking dates must be in yyyy-MM-dd format.")
    }

    val regEnd: Date
    if (drawn) {
        // the registration window is locked, check the booking window against the saved one
        regEnd = lottery.registrationEnd?.toDate()
            ?: return LotteryEditResult.Invalid("This lottery has no registration end date.")
    } else {
        val winners = input.numberOfWinners.trim().toIntOrNull()
        if (winners == null || winners <= 0) {
            return LotteryEditResult.Invalid("Please enter a valid number of winners.")
        }
        val regStart = parseLotteryDate(input.registrationStart)
        val parsedRegEnd = parseLotteryDate(input.registrationEnd)
        if (regStart == null || parsedRegEnd == null) {
            return LotteryEditResult.Invalid("Registration dates must be in yyyy-MM-dd format.")
        }
        if (parsedRegEnd.before(regStart)) {
            return LotteryEditResult.Invalid("Registration end cannot be before start.")
        }
        regEnd = parsedRegEnd

        val capacity: Int? = if (input.limitWaitlist) {
            val cap = input.waitlistCapacity.trim().toIntOrNull()
            if (cap == null || cap <= 0) {
                return LotteryEditResult.Invalid("Please enter a valid waitlist size.")
            }
            val onWaitlist = lottery.waitlist.distinct().size
            if (cap < onWaitlist) {
                return LotteryEditResult.Invalid(
                    "$onWaitlist ${if (onWaitlist == 1) "person is" else "people are"} already on the " +
                        "waitlist, so the limit can't be lower than that."
                )
            }
            cap
        } else null

        updates["numberOfWinners"] = winners
        updates["registrationStart"] = Timestamp(regStart)
        updates["registrationEnd"] = Timestamp(regEnd)
        updates["waitlistCapacity"] = capacity
    }

    if (!bookStart.after(regEnd)) {
        return LotteryEditResult.Invalid("Booking period must begin after the registration period ends.")
    }
    if (bookEnd.before(bookStart)) {
        return LotteryEditResult.Invalid("Booking end cannot be before start.")
    }

    // reservations winners already made must still fit in the booking window
    val fmt = dateFormat()
    val newStartDay = fmt.format(bookStart)
    val newEndDay = fmt.format(bookEnd)
    val outside = activeLotteryReservations(lottery)
        .any { fmt.format(it.startDate.toDate()) < newStartDay || fmt.format(it.endDate.toDate()) > newEndDay }
    if (outside) {
        return LotteryEditResult.Invalid(
            "Some winners have already booked dates outside this booking window. " +
                "Keep the window wide enough to cover their reservations."
        )
    }

    updates["bookingStartDate"] = Timestamp(bookStart)
    updates["bookingEndDate"] = Timestamp(bookEnd)
    return LotteryEditResult.Valid(updates)
}

@Composable
fun EditLotteryScreen(
    lotteryId: String,
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    // reading the snapshot list here recomposes when the repository finishes loading
    val myCampsites = ownedCampsites(campsiteRepository.campsites, deviceId)

    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var loaded by remember { mutableStateOf(false) }
    // the form is filled once from the first load, later updates don't overwrite what's typed
    var input by remember { mutableStateOf<LotteryEditInput?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(lotteryId) {
        val registration = lotteryRepository.listenToLottery(
            lotteryId = lotteryId,
            onResult = {
                lottery = it
                loaded = true
                if (input == null && it != null) input = lotteryEditInputFrom(it)
            },
            onFailure = {
                errorMessage = "Could not load this lottery: ${it.message}"
                loaded = true
            }
        )
        onDispose { registration.remove() }
    }

    LotteryFormScaffold(title = "Edit Lottery", onNavigateBack = onNavigateBack) { innerPadding ->
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

            val current = lottery
            val form = input
            if (current == null || form == null) {
                Text(if (loaded) "This lottery no longer exists." else "Loading...")
                return@Column
            }
            if (!isCampsiteOwner(current.ownerId, deviceId)) {
                Text("Only the lottery's owner can edit it.")
                return@Column
            }
            val drawn = isLotteryDrawn(current)

            if (drawn) {
                Text(
                    text = "This lottery has been drawn, so the number of winners, registration " +
                        "window and waitlist limit can no longer change. You can still change the " +
                        "booking window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LotterySectionTitle(if (myCampsites.size > 1) "Campsites in this lottery *" else "Campsite *")
                if (myCampsites.isEmpty()) {
                    Text(
                        text = if (campsiteRepository.isLoading) "Loading campsites..." else "You have no campsites listed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                myCampsites.forEach { site ->
                    val checked = site.id in form.campsiteIds
                    CampsitePickerRow(site = site, checked = checked) {
                        input = form.copy(
                            campsiteIds = if (checked) form.campsiteIds - site.id else form.campsiteIds + site.id
                        )
                    }
                }
            }

            LotteryFormSection("Winners") {
                OutlinedTextField(
                    value = form.numberOfWinners,
                    onValueChange = { input = form.copy(numberOfWinners = it) },
                    label = { Text("Number of Winners *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !drawn,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LotteryFormSection("Registration window *") {
                // a registration that already started keeps its start day pickable
                val savedStart = formatLotteryInputDate(current.registrationStart)
                LotteryDateRangeField(
                    pickerTitle = "Pick the registration window",
                    start = form.registrationStart,
                    end = form.registrationEnd,
                    firstSelectableDay = listOf(todayLotteryDay(), savedStart).filter { it.isNotBlank() }.min(),
                    onChange = { start, end -> input = form.copy(registrationStart = start, registrationEnd = end) },
                    enabled = !drawn
                )
            }

            LotteryFormSection("Booking window *") {
                LotteryDateRangeField(
                    pickerTitle = "Pick the booking window",
                    start = form.bookingStart,
                    end = form.bookingEnd,
                    // booking opens after registration closes
                    firstSelectableDay = firstBookingDay(form.registrationEnd),
                    onChange = { start, end -> input = form.copy(bookingStart = start, bookingEnd = end) }
                )
            }

            LotteryFormSection("Waitlist") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Limit waitlist size", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${current.waitlist.distinct().size} on the waitlist now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = form.limitWaitlist,
                        onCheckedChange = { input = form.copy(limitWaitlist = it) },
                        enabled = !drawn
                    )
                }
                if (form.limitWaitlist) {
                    OutlinedTextField(
                        value = form.waitlistCapacity,
                        onValueChange = { input = form.copy(waitlistCapacity = it) },
                        label = { Text("Maximum waitlist size *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !drawn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                shape = MaterialTheme.shapes.small,
                // wait for the campsites, or saving would think none are selected
                enabled = !isSaving && !campsiteRepository.isLoading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when (val result = validateLotteryEdit(current, myCampsites, form)) {
                        is LotteryEditResult.Invalid -> errorMessage = result.message
                        is LotteryEditResult.Valid -> {
                            errorMessage = ""
                            isSaving = true
                            lotteryRepository.updateLottery(
                                lotteryId = current.id.ifBlank { lotteryId },
                                updates = result.updates,
                                onSuccess = { onNavigateBack() },
                                onFailure = { e ->
                                    isSaving = false
                                    errorMessage = "Failed: ${e.message}"
                                }
                            )
                        }
                    }
                }
            ) {
                Text(if (isSaving) "Saving..." else "Save changes")
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
