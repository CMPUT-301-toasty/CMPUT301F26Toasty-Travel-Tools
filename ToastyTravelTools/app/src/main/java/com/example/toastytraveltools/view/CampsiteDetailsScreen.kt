/*
CampsiteDetailsScreen.kt

Purpose: Shows the details of the campsite that is clicked on. When the user clicks on one of the
campsites from the search results, a new screen will popup that show the rating, max guests,
description, location, and a button to join the lottery if available.

Issues:
 - no current outstanding issues
 */

package com.example.toastytraveltools.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FieldValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Reservation


// this screen appears when the user clicks on the specific row
// we display further details about the campsite
// join button shown if applicable
fun updateScore(campsite: Campsite){
    var scoreTotal: Int = 0
    var scoreCount: Int = 0
    FirebaseFirestore.getInstance()
        .collection("reservations")
        .whereEqualTo("campsiteName", campsite.name)
        .get()
        .addOnSuccessListener { snapshot ->
            val reservations = snapshot.documents.mapNotNull { doc ->
                // the class has an id field, so fill it in with the document's ID
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
            Log.d("ReservationDebug", "found ${reservations.size} reservations for campsiteName=$campsite.Name")
            for(r in reservations){
                if(r.reviewScore != null){
                    scoreTotal += r.reviewScore ?: 0
                    scoreCount += 1
                }
            }
            Log.d("Debug", "${scoreTotal} & ${scoreCount}")
            FirebaseFirestore.getInstance()
                .collection("campsites")
                .document(campsite.id)
                .update("Rating", scoreTotal.toDouble()/scoreCount.toDouble())
            campsite.rating = scoreTotal.toDouble()/scoreCount.toDouble()
        }
        .addOnFailureListener {  }
}


@Composable
fun CampsiteDetailsScreen(
    campsiteId: String,
    onBack: () -> Unit,
    onError: (Exception) -> Unit
) {
    var campsite by remember { mutableStateOf<Campsite?>(null)}
    var lottery by remember { mutableStateOf<Lottery?>(null)}
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }

    LaunchedEffect(campsiteId) {
        FirebaseFirestore.getInstance()
            .collection("campsites")
            .document(campsiteId)
            .get()
            .addOnSuccessListener { snapshot ->
                campsite = snapshot.toObject(Campsite::class.java)
            }
            .addOnFailureListener { onError(it) }
        Log.d("ReservationDebug", "this device's id: $deviceId")
        loadReservations( //To identify if a user has been to a campsite before, so they can place a review.
            entrantId = deviceId,
            onResult = { reservations = it },
            onError = { reservations = listOf() }
        )
    }

    val snapshot_campsite = campsite
    if(snapshot_campsite == null) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // first we display the details
        // image, name, location, rating, max occupancy, description, address
        Row(modifier = Modifier.padding(16.dp)){
            Button(onClick = onBack) {
                Text("Back")
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = snapshot_campsite.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = snapshot_campsite.location,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // rating and max guests side by side in a card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${snapshot_campsite.rating}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Max Guests",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${snapshot_campsite.occupancyLimit}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = snapshot_campsite.description,
            style = MaterialTheme.typography.bodyMedium
        )

        //Present option to review only if campsite is in past reservations
        val now = Timestamp.now()
        var reviewing by remember { mutableStateOf<Boolean>(false)}
        for (r in reservations){
            if (r.campsiteName == snapshot_campsite.name && r.endDate < now){
                if(reviewing == false) {
                    if (r.reviewScore == null) {
                        OutlinedButton(
                            onClick = { reviewing = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        {
                            Text(text = "You have visited this campsite before, how would you rate your stay?")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { reviewing = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        {
                            Text(text = "You gave this campsite rating of ${r.reviewScore}. Would you like to edit your review?")
                        }
                    }
                }
                if(reviewing == true){
                    //make review score selection more intuitive later (use stars)
                    var sliderPosition by remember { mutableStateOf<Float>(0F) }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        steps = 4,
                        valueRange = 0f..5f
                    )
                    OutlinedButton(
                        onClick =
                            {
                                reviewing = false
                                FirebaseFirestore.getInstance()
                                    .collection("reservations")
                                    .document(r.id)
                                    .update("reviewScore", sliderPosition.toInt())
                                r.reviewScore = sliderPosition.toInt() //just for visual
                                updateScore(snapshot_campsite)
                            },
                        modifier = Modifier.fillMaxWidth()
                    )
                    {
                        Text(text = "You give this campsite a rating of: ${sliderPosition.toInt()}")
                    }
                }
            }
        }


        // search for the campsite_id to see if it exists inside of lotteries
        // if it exists, also check if today's date is within the registration start/end
        // if the requirements are met, that is when we display the join button

        LaunchedEffect(snapshot_campsite.id) {
            FirebaseFirestore.getInstance()
                .collection("lotteries")
                .whereArrayContains("campsiteIds", snapshot_campsite.id)
                .get()
                .addOnSuccessListener { snapshot ->
                    val now = Timestamp.now()
                    var activeLottery: Lottery? = null
                    for (l in snapshot.toObjects(Lottery::class.java)) {
                        val start = l.registrationStart
                        val end = l.registrationEnd
                        if (start == null || end == null) {
                            continue
                        }
                        if (now < start || now > end) {
                            continue
                        }
                        activeLottery = l
                        break
                    }
                    lottery = activeLottery
                }
                .addOnFailureListener { onError(it) }

        }
        val snapshot_lottery = lottery
        if(snapshot_lottery != null) {
            if (snapshot_lottery.waitlist.contains(deviceId)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "You have already joined this lottery",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                FirebaseFirestore.getInstance()
                                    .collection("lotteries")
                                    .document(snapshot_lottery.id)
                                    .update("waitlist", FieldValue.arrayRemove(deviceId))
                                    .addOnSuccessListener {
                                        // update the screen so the join button comes back
                                        lottery =
                                            snapshot_lottery.copy(waitlist = snapshot_lottery.waitlist - deviceId)
                                    }
                                    .addOnFailureListener { onError(it) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        {
                            Text(text = "Leave")
                        }
                    }
                }
            }
            else {
                Button(
                    onClick = {FirebaseFirestore.getInstance()
                        .collection("lotteries")
                        .document(snapshot_lottery.id)
                        .update("waitlist", FieldValue.arrayUnion(deviceId))
                        .addOnSuccessListener {
                            lottery = snapshot_lottery.copy(waitlist = snapshot_lottery.waitlist + deviceId)
                        }
                        .addOnFailureListener { onError(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    Text(text = "Join")
                }
            }
        }
        else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No Active Lottery",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

    }

}