/*
ReservationScreen.kt

Purpose: This is the screen that shows the past and present reservations of the user. Users are
able to cancel any active reservations but a confirmation screen will popup to confirm cancellation.

Issues:
 - make the active and past buttons more clear on which one is active
 */

package com.example.toastytraveltools.view

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
import androidx.compose.ui.graphics.RectangleShape
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
                // the class has an id field, so fill it in with the document's ID
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
            Log.d("ReservationDebug", "found ${reservations.size} reservations for entrant_id=$entrantId")
            onResult(reservations)
        }
        .addOnFailureListener { onError(it) }
}

fun cancelReservation(
    reservationId: String,
    onDone: () -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("reservations")
        .document(reservationId)
        .delete()
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { onError(it) }
}

@Composable
fun BottomButtons_Reservation(
    onSearchClick: () -> Unit,
    onReservationClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // filled button = the screen you're on
        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Search", maxLines = 1)
        }
        Button(
            onClick = onReservationClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Reservation", maxLines = 1)
        }
        OutlinedButton(
            onClick = { onProfileClick(deviceId) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Profile", maxLines = 1)
        }
    }
}
@Composable
fun ReservationScreen (
    onNavigateBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onClaimed: (Reservation) -> Unit = {}
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
    var claimCampsiteName by remember { mutableStateOf("") }

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
        val campsiteId = claimable.firstOrNull()?.campsiteIds?.firstOrNull()
        if (campsiteId != null) {
            loadCampsiteName(campsiteId) { claimCampsiteName = it }
        }
    }

    val lotteryToClaim = claimable.firstOrNull()

    if (showClaimDialog && lotteryToClaim != null) {
        ClaimSpotDialog(
            lottery = lotteryToClaim,
            campsiteName = claimCampsiteName,
            claiming = claiming,
            error = claimMessage,
            onDismiss = { if (!claiming) showClaimDialog = false },
            onClaim = { startMillis, endMillis, guests ->
                claiming = true
                claimMessage = null
                claimLotterySpot(
                    lotteryId = lotteryToClaim.id,
                    entrantId = deviceId,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    guests = guests,
                    onSuccess = { reservation ->
                        claiming = false
                        showClaimDialog = false

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

    // active = now is before the end time, past = the end time has passed
    val now = Timestamp.now()
    val shown = reservations
        .filter { if (active_clicked) it.endDate > now else it.endDate <= now }
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
                            onDone = {
                                reservations = reservations.filter {
                                    it.id != toCancel.id
                                }
                                selected = null
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
                title = { Text("Reservations") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            ))
        },
        bottomBar = {
            Column {
                if (claimable.isNotEmpty()) {
                    Button(
                        onClick = { showClaimDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            "Claim your spot at $claimCampsiteName"
                        )
                    }
                }
                BottomButtons_Reservation(
                    onSearchClick = onSearchClick,
                    onReservationClick = onReservationClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        if (active_clicked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text("Active")
                }

                Button(
                    onClick = { active_clicked = false },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        if (!active_clicked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
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

                                if (active_clicked) {
                                    Button(
                                        onClick = { selected = reservation },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RectangleShape
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
    claiming: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClaim: (Long, Long, Int) -> Unit
) {
    val bookingStart = lottery.bookingStartDate?.toDate()?.time?.let { startOfDay(it) }
    val bookingEnd = lottery.bookingEndDate?.toDate()?.time?.let { endOfDay(it) }

    // only the days inside the booking window can be picked
    val state = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val start = pickerDateToLocal(utcTimeMillis, false)
                val end = pickerDateToLocal(utcTimeMillis, true)
                return (bookingStart == null || start >= bookingStart) &&
                        (bookingEnd == null || end <= bookingEnd)
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
                Text("Cancel")
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