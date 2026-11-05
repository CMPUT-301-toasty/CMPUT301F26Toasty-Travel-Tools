package com.example.toastytraveltools

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
import com.google.firebase.firestore.FieldValue

// this screen appears when the user clicks on the specific row
// we display further details about the campsite
// join button shown if applicable
@Composable
fun CampsiteDetailsScreen(campsiteId: String,
                          onError: (Exception) -> Unit) {
    var campsite by remember { mutableStateOf<Campsite?>(null)}
    var lottery by remember { mutableStateOf<Lottery?>(null)}
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    LaunchedEffect(campsiteId) {
        FirebaseFirestore.getInstance()
            .collection("campsites")
            .document(campsiteId)
            .get()
            .addOnSuccessListener { snapshot ->
                campsite = snapshot.toObject(Campsite::class.java)
            }
            .addOnFailureListener { onError(it) }
    }

    val snapshot_campsite = campsite
    if(snapshot_campsite == null) {
        Text(text = "Loading...")
        return
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // first we display the details
        // image, name, location, rating, max occupancy, description, address
        Text(text = snapshot_campsite.Name)
        Spacer(Modifier.height(8.dp))
        Text(text = snapshot_campsite.Location)
        Spacer(Modifier.height(8.dp))
        Text(text = "Rating: ${snapshot_campsite.Rating}")
        Spacer(Modifier.height(8.dp))
        Text(text = "Max Guests: ${snapshot_campsite.OccupancyLimit}")
        Spacer(Modifier.height(8.dp))
        Text(text = snapshot_campsite.Description)


        // search for the campsite_id to see if it exists inside of lotteries
        // if it exists, also check if today's date is within the registration start/end
        // if the requirements are met, that is when we display the join button

        LaunchedEffect(snapshot_campsite.id) {
            FirebaseFirestore.getInstance()
                .collection("lotteries")
                .whereArrayContains("campsite_id", snapshot_campsite.id)
                .whereEqualTo("status", "OPEN")
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
            if (snapshot_lottery.waitList.contains(deviceId)) {
                Text(text = "You have joined this lottery")
            } else {
                Button(
                    onClick = {FirebaseFirestore.getInstance()
                        .collection("lotteries")
                        .document(snapshot_lottery.id)
                        .update("waitlist", FieldValue.arrayUnion(deviceId))
                        .addOnSuccessListener {
                            lottery = snapshot_lottery.copy(waitList = snapshot_lottery.waitList + deviceId)
                        }
                        .addOnFailureListener { onError(it) }
                    }
                )
                {
                    Text(text = "Join")
                }
            }
        }
        else {
            Text(text = "No Active Lottery")
        }

}

}