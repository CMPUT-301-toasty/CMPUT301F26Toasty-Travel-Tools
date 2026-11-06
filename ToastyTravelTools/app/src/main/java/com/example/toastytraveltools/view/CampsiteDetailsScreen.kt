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
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery


// this screen appears when the user clicks on the specific row
// we display further details about the campsite
// join button shown if applicable
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
                text = snapshot_campsite.Name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = snapshot_campsite.Location,
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
                        text = "${snapshot_campsite.Rating}",
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
                        text = "${snapshot_campsite.OccupancyLimit}",
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
            text = snapshot_campsite.Description,
            style = MaterialTheme.typography.bodyMedium
        )


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