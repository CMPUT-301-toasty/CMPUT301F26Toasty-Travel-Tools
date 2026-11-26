package com.example.toastytraveltools.view

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converts a Firestore date value into a Timestamp.
 */
private fun Any?.asReservationTimestamp(): Timestamp {
    return when (this) {
        is Timestamp -> this
        is Date -> Timestamp(this)
        is String -> {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(this)
                date?.let { Timestamp(it) } ?: Timestamp(0, 0)
            } catch (_: Exception) {
                Timestamp(0, 0)
            }
        }
        else -> Timestamp(0, 0)
    }
}

/**
 * Loads reservations for campsites owned by [ownerId] or [deviceId].
 */
fun loadOwnerReservations(
    ownerId: String,
    deviceId: String,
    onResult: (List<Reservation>) -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("campsites")
        .get()
        .addOnSuccessListener { campsiteSnapshot ->
            val ownerCampsites = campsiteSnapshot.documents.filter { doc ->
                val oId = doc.getString("ownerId") ?: ""
                oId == ownerId || (deviceId.isNotBlank() && oId == deviceId)
            }
            val ownerCampsiteNames = ownerCampsites.mapNotNull { doc ->
                doc.getString("Name") ?: doc.getString("name")
            }.toSet()

            db.collection("reservations")
                .get()
                .addOnSuccessListener { resSnapshot ->
                    val reservations = resSnapshot.documents.mapNotNull { doc ->
                        val entrantId = doc.getString("entrant_id") ?: doc.getString("entrantId") ?: ""
                        val campsiteName = doc.getString("campsiteName") ?: doc.getString("campsite_name") ?: ""

                        if (campsiteName in ownerCampsiteNames || entrantId == ownerId || (deviceId.isNotBlank() && entrantId == deviceId)) {
                            Reservation(
                                id = doc.id,
                                campsiteName = campsiteName,
                                location = doc.getString("location") ?: "",
                                entrantName = doc.getString("entrantName") ?: doc.getString("entrant_name") ?: "",
                                entrantId = entrantId,
                                startDate = doc.get("startDate").asReservationTimestamp(),
                                endDate = doc.get("endDate").asReservationTimestamp(),
                                guests = (doc.get("guests") as? Number)?.toInt() ?: 0,
                                reviewScore = (doc.get("reviewScore") as? Number)?.toInt(),
                                status = doc.getString("status") ?: doc.getString("Status") ?: "active"
                            )
                        } else {
                            null
                        }
                    }
                    onResult(reservations)
                }
                .addOnFailureListener { onError(it) }
        }
        .addOnFailureListener { onError(it) }
}

@Composable
fun BottomButtons_OwnerBookings(
    onOwnerScreenClick: () -> Unit,
    onLotteryClick: () -> Unit,
    onBookingClick: () -> Unit,
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
        OutlinedButton(
            onClick = onOwnerScreenClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Sites", maxLines = 1)
        }
        OutlinedButton(
            onClick = onLotteryClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Lotteries", maxLines = 1)
        }
        Button(
            onClick = onBookingClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Bookings", maxLines = 1)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreenBooking(
    onNavigateBack: () -> Unit,
    onOwnerScreenClick: () -> Unit,
    onLotteryClick: () -> Unit,
    onBookingClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    userId: String,
    onMessageClick: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("active") } // "active", "past", "canceled"
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var selected by remember { mutableStateOf<Reservation?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    val effectiveOwnerId = if (userId.isNotBlank()) userId else deviceId

    LaunchedEffect(effectiveOwnerId, deviceId) {
        isLoading = true
        loadOwnerReservations(
            ownerId = effectiveOwnerId,
            deviceId = deviceId,
            onResult = {
                reservations = it
                isLoading = false
            },
            onError = {
                reservations = emptyList()
                isLoading = false
            }
        )
    }

    val now = Timestamp.now()
    val shown = reservations
        .filter {
            when (selectedTab) {
                "active" -> it.status == "active" && it.endDate > now
                "past" -> it.status == "active" && it.endDate <= now
                "canceled" -> it.status == "canceled"
                else -> true
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
                    "Are you sure you want to cancel the reservation for ${toCancel.campsiteName} from " +
                            "${fmt.format(toCancel.startDate.toDate())} to " +
                            "${fmt.format(toCancel.endDate.toDate())}? This action cannot be undone."
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
                            },
                            onError = { selected = null }
                        )
                    }
                ) {
                    Text("Cancel Reservation", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) {
                    Text("Keep Reservation")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservations") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons_OwnerBookings(
                onOwnerScreenClick = onOwnerScreenClick,
                onLotteryClick = onLotteryClick,
                onBookingClick = onBookingClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Active, Past, and Canceled buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedTab = "active" },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == "active") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text("Active")
                }

                Button(
                    onClick = { selectedTab = "past" },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == "past") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text("Past")
                }

                Button(
                    onClick = { selectedTab = "canceled" },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == "canceled") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text("Canceled")
                }
            }

            // Reservation list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (shown.isEmpty()) {
                val emptyMessage = when (selectedTab) {
                    "active" -> "No active reservations"
                    "past" -> "No past reservations"
                    "canceled" -> "No canceled reservations"
                    else -> "No reservations"
                }
                Text(
                    text = emptyMessage,
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
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = reservation.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (reservation.entrantName.isNotBlank()) {
                                    Text(
                                        text = "Booked by: ${reservation.entrantName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (reservation.location.isNotBlank()) {
                                    Text(
                                        text = "Location: ${reservation.location}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = "${fmt.format(reservation.startDate.toDate())} to ${fmt.format(reservation.endDate.toDate())}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (reservation.guests > 0) {
                                    Text(
                                        text = "Guests: ${reservation.guests}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (reservation.status == "canceled") {
                                    Text(
                                        text = "Status: Canceled",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onMessageClick(reservation.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = RectangleShape
                                ) {
                                    Text("Message ${reservation.entrantName.ifBlank { "entrant" }}")
                                }

                                if (selectedTab == "active") {
                                    Button(
                                        onClick = { selected = reservation },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
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
