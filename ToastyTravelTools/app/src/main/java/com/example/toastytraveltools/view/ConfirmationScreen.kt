/*
ConfirmationScreen.kt

Purpose:

Issues:
 -
 */

package com.example.toastytraveltools.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatDate(timestamp: Timestamp): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timestamp.toDate())

@Composable
fun ConfirmationScreen(reservation: Reservation, onHome: () -> Unit = {}) {
    val context = LocalContext.current

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(reservationToCsv(reservation).toByteArray())
            }
        }
    }

    Column(Modifier.padding(64.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reservation Confirmed!", style = MaterialTheme.typography.headlineMedium)
        Text("Confirmation #: ${reservation.id}")
        Text("Campsite: ${reservation.campsiteName}")
        Text("Location: ${reservation.location}")
        Text("Dates: ${formatDate(reservation.startDate)} to ${formatDate(reservation.endDate)}")
        Text("Guests: ${reservation.guests}")
        Button(onClick = { saveCsv.launch("reservation_${reservation.id}.csv") }) {
            Text("Download CSV")
        }
        Button(onClick = onHome) {
            Text("Home")
        }
    }
}

fun reservationToCsv(r: Reservation) = buildString {
    appendLine("id,campsite,location,entrant,start,end,guests")
    appendLine(
        listOf(r.id, r.campsiteName, r.location, r.entrantName, r.startDate, r.endDate, r.guests)
            .joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }
    )
}