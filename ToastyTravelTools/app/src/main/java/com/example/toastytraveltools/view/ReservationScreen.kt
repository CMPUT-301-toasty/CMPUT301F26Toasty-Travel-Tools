/*
ReservationScreen.kt

Purpose: This is the screen that shows the past and present reservations of the user. Users are
able to cancel any active reservations but a confirmation screen will popup to confirm cancellation.
Lottery winners can claim their spot here. If the lottery has more than one campsite they pick
one campsite first, then the dates, and are only shown dates that are still available.
Winners can also decline their spot if they changed their mind or none of the dates work.
After cancelling they are taken to the cancellation screen, and get a notification if turned on.

Issues:
 - make the active and past buttons more clear on which one is active
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import com.example.toastytraveltools.view.components.MainTab
import com.example.toastytraveltools.view.components.ToastyMainBottomBar

import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// imports added for the reservation list
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.text.input.KeyboardType
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Campsite
import java.util.Calendar
import java.util.TimeZone

// loads the reservations that belong to one entrant document, using the Reservation class
fun loadReservations(
    entrantId: String,
    onResult: (List<Reservation>) -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("reservations")
        .whereEqualTo("entrant_id", entrantId)
        .get()
        .addOnSuccessListener { snapshot ->
            val reservations = snapshot.documents.mapNotNull { doc ->
                val status = doc.getString("status") ?: doc.getString("Status") ?: "active"
                doc.toObject(Reservation::class.java)?.copy(id = doc.id, status = status)
            }
            Log.d("ReservationDebug", "found ${reservations.size} reservations for entrant_id=$entrantId")
            onResult(reservations)
        }
        .addOnFailureListener { onError(it) }
}

// updates the reservation status in Firestore (e.g. "canceled"),
// and if it came from a lottery also removes it from that lottery's reservations list.
// The winner is added to cancelledWinners so the win is used up
fun cancelReservation(
    reservationId: String,
    cancelledStatus: String = "canceled",
    onDone: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val reservationRef = db.collection("reservations").document(reservationId)

    db.runTransaction { tx ->
        // reads first, writes after
        val reservationDoc = tx.get(reservationRef)
        val lotteryId = reservationDoc.getString("lotteryId") ?: ""
        val entrantId = reservationDoc.getString("entrantId")
            ?: reservationDoc.getString("entrant_id")
            ?: ""

        val lotteryRef = if (lotteryId.isNotBlank()) db.collection("lotteries").document(lotteryId) else null
        val lottery = lotteryRef?.let { tx.get(it).toObject(Lottery::class.java) }

        if (lotteryRef != null && lottery != null) {
            val updates = mutableMapOf<String, Any>(
                "reservations" to lottery.reservations.filter { it.id != reservationId }
            )
            if (entrantId.isNotBlank()) {
                updates["cancelledWinners"] = FieldValue.arrayUnion(entrantId)
            }
            tx.update(lotteryRef, updates)
        }

        tx.update(reservationRef, "status", cancelledStatus)
        null
    }
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { onError(it) }
}

@Composable
fun ReservationScreen (
    onNavigateBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onClaimed: (Reservation) -> Unit = {},
    onCancelled: (Reservation) -> Unit = {},
    onMessageClick: (String) -> Unit = {}
) {
    var active_clicked by remember { mutableStateOf(true) }

    // the id of this device, the same way the details screen gets it
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    var selected by remember { mutableStateOf<Reservation?>(null) }

    var claimable by remember { mutableStateOf(listOf<Lottery>()) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var claiming by remember { mutableStateOf(false) }
    var claimMessage by remember { mutableStateOf<String?>(null) }

    // every campsite in the lottery the user won, and the one they picked
    var lotteryCampsites by remember { mutableStateOf(listOf<Campsite>()) }
    var chosenCampsite by remember { mutableStateOf<Campsite?>(null) }

    // dates already reserved at each campsite, the key is the campsite id
    var bookedDates by remember { mutableStateOf(mapOf<String, List<Pair<Long, Long>>>()) }
    var datesLoaded by remember { mutableStateOf(false) }
    var datesError by remember { mutableStateOf<String?>(null) }

    // declining the reservation
    var showDeclineConfirm by remember { mutableStateOf(false) }
    // the exact lottery being declined, saved when the decline button is pressed
    var lotteryToDecline by remember { mutableStateOf<Lottery?>(null) }
    var declinePlaceName by remember { mutableStateOf("") }
    var declining by remember { mutableStateOf(false) }
    var declineError by remember { mutableStateOf<String?>(null) }
    var declinedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deviceId) {
        Log.d("ReservationDebug", "this device's id: $deviceId")
        loadReservations(
            entrantId = deviceId,
            onResult = { reservations = it },
            onError = { reservations = listOf() }
        )
    }
    LaunchedEffect(deviceId) {
        loadClaimableLotteries(
            entrantId = deviceId,
            onResult = { claimable = it },
            onError = { claimable = listOf() }
        )
    }
    LaunchedEffect(claimable) {
        // claimable only has lotteries this user won, so only winners load the campsites and dates
        val wonLottery = claimable.firstOrNull()
        if (wonLottery != null) {
            datesLoaded = false
            datesError = null
            loadLotteryCampsites(
                lottery = wonLottery,
                onResult = { campsites ->
                    lotteryCampsites = campsites
                    loadBookedDates(
                        campsites = campsites,
                        onResult = {
                            bookedDates = it
                            datesLoaded = true
                        },
                        onError = {
                            datesError = "Could not load the available dates, please try again later"
                            datesLoaded = true
                        }
                    )
                },
                onError = {
                    datesError = "Could not load the campsites, please try again later"
                    datesLoaded = true
                }
            )
        }
    }

    val lotteryToClaim = claimable.firstOrNull()
    val pickedCampsite = chosenCampsite

    // opens the "are you sure" dialog for this lottery
    fun askToDecline(lottery: Lottery) {
        lotteryToDecline = lottery
        declinePlaceName = if (lotteryCampsites.size == 1) lotteryCampsites.first().name else "this lottery"
        declineError = null
        showClaimDialog = false
        chosenCampsite = null
        showDeclineConfirm = true
    }

    if (showClaimDialog && lotteryToClaim != null && datesError != null) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text("Something went wrong") },
            text = { Text(datesError ?: "") },
            confirmButton = {
                TextButton(onClick = { showClaimDialog = false }) {
                    Text("Close")
                }
            }
        )
    } else if (showClaimDialog && lotteryToClaim != null &&
        lotteryCampsites.none { hasAvailableDates(lotteryToClaim, bookedDates[it.id] ?: listOf()) }
    ) {
        // every date at every campsite is already booked or has passed
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text("No dates available") },
            text = {
                Text(
                    "Sorry, there are no available dates left at " +
                            (if (lotteryCampsites.size == 1) lotteryCampsites.first().name else "any of the campsites in this lottery") +
                            ". All of the dates have already been reserved or are no longer available."
                )
            },
            confirmButton = {
                TextButton(onClick = { showClaimDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                // none of the dates work, so let them turn the reservation down here too
                TextButton(onClick = { askToDecline(lotteryToClaim) }) {
                    Text("Decline reservation", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    } else if (showClaimDialog && lotteryToClaim != null && pickedCampsite == null) {
        // first step: pick which campsite to reserve
        ChooseCampsiteDialog(
            lottery = lotteryToClaim,
            campsites = lotteryCampsites,
            bookedDates = bookedDates,
            onDismiss = { showClaimDialog = false },
            onChoose = {
                chosenCampsite = it
                claimMessage = null
            },
            onDecline = { askToDecline(lotteryToClaim) }
        )
    } else if (showClaimDialog && lotteryToClaim != null && pickedCampsite != null) {
        // second step: pick the dates at that campsite
        ClaimSpotDialog(
            lottery = lotteryToClaim,
            campsiteName = pickedCampsite.name,
            bookedDates = bookedDates[pickedCampsite.id] ?: listOf(),
            claiming = claiming,
            error = claimMessage,
            onDismiss = {
                if (!claiming) {
                    claimMessage = null
                    // go back to the campsite list
                    chosenCampsite = null
                }
            },
            onClaim = { startMillis, endMillis, guests ->
                claiming = true
                claimMessage = null
                claimLotterySpot(
                    lotteryId = lotteryToClaim.id,
                    entrantId = deviceId,
                    campsiteId = pickedCampsite.id,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    guests = guests,
                    onSuccess = { reservation ->
                        claiming = false
                        showClaimDialog = false
                        chosenCampsite = null
                        // the lottery win has been used, so the claim button goes away
                        claimable = claimable.filter { it.id != lotteryToClaim.id }

                        ReservationNotifier.send(context, reservation)
                        onClaimed(reservation)
                    },
                    onError = {
                        claiming = false
                        claimMessage = it.localizedMessage ?: "Could not claim your spot"
                    }
                )
            }
        )
    }

    // ask before declining, it can't be undone
    val declining_lottery = lotteryToDecline
    if (showDeclineConfirm && declining_lottery != null) {
        val placeName = declinePlaceName
        AlertDialog(
            onDismissRequest = {
                if (!declining) {
                    showDeclineConfirm = false
                    lotteryToDecline = null
                }
            },
            title = { Text("Decline reservation?") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to decline your spot at $placeName? " +
                                "You will not be able to reserve for this lottery again. This can't be undone."
                    )
                    declineError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !declining,
                    onClick = {
                        declining = true
                        declineError = null
                        declineLotterySpot(
                            lotteryId = declining_lottery.id,
                            entrantId = deviceId,
                            onSuccess = {
                                declining = false
                                showDeclineConfirm = false
                                lotteryToDecline = null
                                // only remove the lottery that was declined, other wins stay
                                claimable = claimable.filter { it.id != declining_lottery.id }
                                declinedMessage = "You have declined your spot at $placeName. " +
                                        "You can no longer reserve for this lottery."
                            },
                            onError = {
                                declining = false
                                declineError = it.localizedMessage ?: "Could not decline the reservation"
                            }
                        )
                    }
                ) {
                    Text(
                        if (declining) "Declining..." else "Yes, decline",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !declining,
                    onClick = {
                        showDeclineConfirm = false
                        lotteryToDecline = null
                    }
                ) {
                    Text("Keep my spot")
                }
            }
        )
    }

    // let them know the decline went through
    val declinedText = declinedMessage
    if (declinedText != null) {
        AlertDialog(
            onDismissRequest = { declinedMessage = null },
            title = { Text("Reservation declined") },
            text = { Text(declinedText) },
            confirmButton = {
                TextButton(onClick = { declinedMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    val now = Timestamp.now()
    val shown = reservations
        .filter {
            if (active_clicked) {
                it.status == "active" && it.endDate > now
            } else {
                it.status != "active" || it.endDate <= now
            }
        }
        .sortedBy { it.startDate }
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    val toCancel = selected

    if (toCancel != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Cancel reservation?") },
            text = {
                Text(
                    "Are you sure you want to cancel your stay at ${toCancel.campsiteName} from " +
                            "${fmt.format(toCancel.startDate.toDate())} to " +
                            "${fmt.format(toCancel.endDate.toDate())}? This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelReservation(
                            reservationId = toCancel.id,
                            cancelledStatus = "canceled",
                            onDone = {
                                reservations = reservations.map {
                                    if (it.id == toCancel.id) it.copy(status = "canceled") else it
                                }
                                selected = null
                                // send a notification if they are turned on, then show the cancellation screen
                                NotificationPreferences.ifEnabled(deviceId) {
                                    ReservationNotifier.sendCancelled(context, toCancel)
                                }
                                onCancelled(toCancel)
                            },
                            onError = { selected = null }
                        )
                    }
                ) {
                    Text("Yes, cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) {
                    Text("Keep it")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            (TopAppBar(
                title = { Text("Reservations") }
            ))
        },
        bottomBar = {
            Column {
                if (claimable.isNotEmpty()) {
                    Button(
                        onClick = {
                            // always start on the campsite list, it has the decline button
                            chosenCampsite = null
                            showClaimDialog = true
                        },
                        // wait for the booked dates so taken days are never shown as open
                        enabled = datesLoaded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            if (!datesLoaded) "Loading available dates..."
                            else if (lotteryCampsites.size == 1) "Claim your spot at ${lotteryCampsites.first().name}"
                            else "Claim your spot - choose from ${lotteryCampsites.size} campsites"
                        )
                    }
                }
                ToastyMainBottomBar(
                    selected = MainTab.Reservations,
                    onExploreClick = onSearchClick,
                    onReservationsClick = onReservationClick,
                    onProfileClick = { onProfileClick(deviceId) }
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Active and Past buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { active_clicked = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active_clicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (active_clicked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Active")
                }

                Button(
                    onClick = { active_clicked = false },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!active_clicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (!active_clicked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Past")
                }
            }

            // Reservation list
            if (shown.isEmpty()) {
                Text(
                    text = if (active_clicked) "No active reservations" else "No past reservations",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown) { reservation ->
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = reservation.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${fmt.format(reservation.startDate.toDate())} to ${
                                        fmt.format(
                                            reservation.endDate.toDate()
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = { onMessageClick(reservation.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Message owner")
                                }
                                if (active_clicked) {
                                    Button(
                                        onClick = { selected = reservation },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text("Cancel Reservation")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

// lets a lottery winner pick one of the campsites in the lottery
// campsites with no dates left are shown but can't be picked
@Composable
fun ChooseCampsiteDialog(
    lottery: Lottery,
    campsites: List<Campsite>,
    bookedDates: Map<String, List<Pair<Long, Long>>>,
    onDismiss: () -> Unit,
    onChoose: (Campsite) -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a campsite") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        text = if (campsites.size == 1) "You won a lottery for this campsite. Choose your dates, or decline if you changed your mind."
                        else "You won a lottery with ${campsites.size} campsites. Pick the one you want to reserve, you can only reserve one.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(campsites) { campsite ->
                    val available = hasAvailableDates(lottery, bookedDates[campsite.id] ?: listOf())
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = campsite.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (campsite.location.isNotBlank()) {
                                Text(text = campsite.location, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = "Max guests: ${campsite.occupancyLimit}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (available) {
                                Button(
                                    onClick = { onChoose(campsite) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Choose dates")
                                }
                            } else {
                                Text(
                                    text = "No dates available",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // changed their mind or none of the campsites work for them
            TextButton(onClick = onDecline) {
                Text("Decline reservation", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun pickerDateToLocal(utcMillis: Long, endOfDay: Boolean): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = utcMillis

    val local = Calendar.getInstance()
    local.clear()
    local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    return if (endOfDay) endOfDay(local.timeInMillis) else startOfDay(local.timeInMillis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimSpotDialog(
    lottery: Lottery,
    campsiteName: String,
    bookedDates: List<Pair<Long, Long>>,
    claiming: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClaim: (Long, Long, Int) -> Unit
) {
    val bookingStart = lottery.bookingStartDate?.toDate()?.time?.let { startOfDay(it) }
    val bookingEnd = lottery.bookingEndDate?.toDate()?.time?.let { endOfDay(it) }
    val today = startOfDay(System.currentTimeMillis())

    // only days inside the booking window, not in the past and not already booked can be picked
    val state = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val start = pickerDateToLocal(utcTimeMillis, false)
                val end = pickerDateToLocal(utcTimeMillis, true)
                return (bookingStart == null || start >= bookingStart) &&
                        (bookingEnd == null || end <= bookingEnd) &&
                        start >= today &&
                        !overlapsBookedDates(start, end, bookedDates)
            }
        }
    )

    var guests by remember { mutableStateOf("1") }
    var inputError by remember { mutableStateOf<String?>(null) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = !claiming,
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis ?: start
                    val guestCount = guests.toIntOrNull()

                    when {
                        start == null || end == null ->
                            inputError = "Pick your start and end dates"
                        guestCount == null || guestCount < 1 ->
                            inputError = "Enter the number of guests"
                        // the picker blocks booked days, but a range could still go over one
                        overlapsBookedDates(
                            pickerDateToLocal(start, false),
                            pickerDateToLocal(end, true),
                            bookedDates
                        ) ->
                            inputError = "Your dates include days that are already booked"
                        else -> {
                            inputError = null
                            onClaim(
                                pickerDateToLocal(start, false),
                                pickerDateToLocal(end, true),
                                guestCount
                            )
                        }
                    }
                }
            ) {
                Text(if (claiming) "Claiming..." else "Claim")
            }
        },
        dismissButton = {
            TextButton(enabled = !claiming, onClick = onDismiss) {
                Text("Back")
            }
        }
    ) {
        Column {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = {
                    Text(
                        text = "Pick your dates at $campsiteName",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )

            Text(
                text = "Greyed out dates are already reserved or unavailable",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            OutlinedTextField(
                value = guests,
                onValueChange = { guests = it; inputError = null },
                label = { Text("Guests") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            (inputError ?: error)?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}