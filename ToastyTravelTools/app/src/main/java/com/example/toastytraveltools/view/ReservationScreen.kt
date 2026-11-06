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
import androidx.compose.material3.Card
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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

@Composable
fun BottomButtons_Reservation(
    onSearchClick: () -> Unit,
    onReservationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
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
            onClick = onProfileClick,
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
    onSearchClick: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var active_clicked by remember { mutableStateOf(true) }

    // the id of this device, the same way the details screen gets it
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }

    LaunchedEffect(deviceId) {
        Log.d("ReservationDebug", "this device's id: $deviceId")
        loadReservations(
            entrantId = deviceId,
            onResult = { reservations = it },
            onError = { reservations = listOf() }
        )
    }

    // active = now is before the end time, past = the end time has passed
    val now = Timestamp.now()
    val shown = reservations
        .filter { if (active_clicked) it.endDate > now else it.endDate <= now }
        .sortedBy { it.startDate }
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())


    Scaffold(
        bottomBar = {
            BottomButtons_Reservation(
                onSearchClick = onSearchClick,
                onReservationClick = onReservationClick,
                onProfileClick = onProfileClick
            )
        }
    ){ innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // the list of reservations, or a message when there is nothing to show
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 130.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (shown.isEmpty()) {
                Text(
                    text = if (active_clicked) "No active reservations" else "No past reservations",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown) { reservation ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = reservation.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${fmt.format(reservation.startDate.toDate())} to ${fmt.format(reservation.endDate.toDate())}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}