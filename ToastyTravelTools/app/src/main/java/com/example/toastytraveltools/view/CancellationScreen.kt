/*
CancellationScreen.kt

Purpose: Display details about the reservation after
      an entrant cancels it. Offer a csv download
      of the cancellation as well.


Issues:
 - Fix the layout for the end product
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
import java.util.Date
import java.util.Locale

private fun formatCancelDate(timestamp: Timestamp): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timestamp.toDate())

@Composable
fun CancellationScreen(reservation: Reservation, onHome: () -> Unit = {}) {
    val context = LocalContext.current

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(cancellationToCsv(reservation).toByteArray())
            }
        }
    }

    Column(Modifier.padding(horizontal = 20.dp, vertical = 64.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reservation Cancelled", style = MaterialTheme.typography.headlineMedium)
        Text("Confirmation #: ${reservation.id}")
        Text("Campsite: ${reservation.campsiteName}")
        Text("Location: ${reservation.location}")
        Text("Dates: ${formatCancelDate(reservation.startDate)} to ${formatCancelDate(reservation.endDate)}")
        Text("Guests: ${reservation.guests}")
        Button(shape = MaterialTheme.shapes.small, onClick = { saveCsv.launch("cancellation_${reservation.id}.csv") }) {
            Text("Download CSV")
        }
        Button(shape = MaterialTheme.shapes.small, onClick = onHome) {
            Text("Home")
        }
    }
}

// same layout as reservationToCsv, plus the status and the day it was cancelled
fun cancellationToCsv(r: Reservation) = buildString {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    appendLine("id,campsite,location,entrant,start,end,guests,status,cancelled on")
    appendLine(
        listOf(
            r.id, r.campsiteName, r.location, r.entrantName,
            fmt.format(r.startDate.toDate()), fmt.format(r.endDate.toDate()), r.guests,
            "cancelled", fmt.format(Date())
        ).joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }
    )
}