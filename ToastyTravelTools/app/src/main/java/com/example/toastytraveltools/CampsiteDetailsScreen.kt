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

// this screen appears when the user clicks on the specific row
// we display further details about the campsite
// join button shown if applicable
@Composable
fun CampsiteDetailsScreen(campsite: Campsite,
                          onError: (Exception) -> Unit) {
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // first we display the details
        // image, name, location, rating, max occupancy, description, address
        Text(text = campsite.Name)
        Spacer(Modifier.height(8.dp))
        Text(text = campsite.Location)
        Spacer(Modifier.height(8.dp))
        Text(text = "Rating: ${campsite.Rating}")
        Spacer(Modifier.height(8.dp))
        Text(text = "Max Guests: ${campsite.OccupancyLimit}")
        Spacer(Modifier.height(8.dp))
        Text(text = campsite.Description)


        // search for the campsite_id to see if it exists inside of lotteries
        // if it exists, also check if today's date is within the registration start/end
        // if the requirements are met, that is when we display the join button

        LaunchedEffect(campsite.id) {
            FirebaseFirestore.getInstance()
                .collection("lotteries")
                .whereArrayContains("campsiteIds", campsite.id)
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

        if(lottery != null) {
            Button(
                onClick = {}
            )
            {
                Text(text = "Join")
            }
        }
        else {
            Text(text = "No Active Lottery")
    }
}

}