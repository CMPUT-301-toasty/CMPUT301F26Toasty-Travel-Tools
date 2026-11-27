/*
CampsiteDetailsScreen.kt

Purpose: Shows the details of the campsite that is clicked on. When the user clicks on one of the
campsites from the search results, a new screen will popup that show the rating, max guests,
description, location, and a button to join the lottery if available.

Issues:
 - no current outstanding issues
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.Image
import com.example.toastytraveltools.R
import androidx.compose.foundation.background
import com.google.firebase.firestore.FieldValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.example.toastytraveltools.controller.MessageRepository
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Message
import com.example.toastytraveltools.model.Reservation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource


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
                Log.d("ReservationDebug", "reservation: ${r}")
                if(r.reviewScore != null){
                    //Log.d("ReservationDebug", "score not null, ${r.reviewScore}")
                    scoreTotal += r.reviewScore ?: 0
                    scoreCount += 1
                }
            }
            Log.d("Debug", "${scoreTotal} & ${scoreCount}")
            FirebaseFirestore.getInstance()
                .collection("campsites")
                .document(campsite.id)
                .update("rating", scoreTotal.toDouble()/scoreCount.toDouble())
            //Log.d("ReservationDebug", "scoreTotal:, ${scoreTotal}")
            //Log.d("ReservationDebug", "scoreTotal:, ${scoreCount}")
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
    Log.d("ReviewsDebug", "Flow0")
    var campsite by remember { mutableStateOf<Campsite?>(null)}
    var lottery by remember { mutableStateOf<Lottery?>(null)}
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    var campsiteRating by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(campsiteId) {
        Log.d("ReviewsDebug", "Flow1")
        FirebaseFirestore.getInstance()
            .collection("campsites")
            .document(campsiteId)
            .addSnapshotListener { snapshot, exception ->
                if ( snapshot == null || exception != null) return@addSnapshotListener
                campsite = snapshot.toObject(Campsite::class.java)
                campsiteRating = snapshot.getDouble("rating") ?: 0.0
            }
        Log.d("ReservationDebug", "this device's id: $deviceId")
        loadReservations( //To identify if a user has been to a campsite before, so they can place a review.
            entrantId = deviceId,
            onResult = { reservations = it },
            onError = { reservations = listOf() }
        )
    }

    Log.d("ReviewsDebug", "Flow2")
    val snapshot_campsite = campsite
    if(snapshot_campsite == null) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        Log.d("ReviewsDebug", "Flow3")
        return


    }
    updateScore(snapshot_campsite) //ensure accurate rating before opening campsite
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // first we display the details
        // image, name, location, rating, max occupancy, description, address
        Row(modifier = Modifier.padding(16.dp)){
            Button(shape = MaterialTheme.shapes.small, onClick = onBack) {
                Text("Back")
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = snapshot_campsite.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        CampsiteImageCarousel(
            imageUrls = snapshot_campsite.imageUrls,
            emptyContent = {
                if (snapshot_campsite.name.trim().equals("bruh please", ignoreCase = true)) {
                    Image(
                        painter = painterResource(id = R.drawable.bruh_please),
                        contentDescription = "Campsite Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        Text(
            text = snapshot_campsite.location,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // rating and max guests side by side in a card
        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
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
                        //text = "${snapshot_campsite.rating}",
                        text = "$campsiteRating",
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
        val messageRepository = MessageRepository()
        var reviewMessage = Message()
        val now = Timestamp.now()
        var hasReview by remember { mutableStateOf<Boolean>(false)} //If user has review, display their review in the review section instead
        var reviewing by remember { mutableStateOf<Boolean>(false)}
        for (r in reservations) {
            if (r.campsiteName == snapshot_campsite.name && r.endDate < now) {
                if (r.reviewScore == null) {
                    Log.d("ReviewsDebug2", "I don't have a review")
                    if (reviewing == false) {
                        OutlinedButton(
                            shape = MaterialTheme.shapes.small,
                            onClick = { reviewing = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        {
                            Text(text = "You have visited this campsite before, how would you rate your stay?")
                        }
                    }
                    else{
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    //make review score selection more intuitive later (use stars)
                                    var sliderPosition by remember { mutableStateOf<Float>(0F) }
                                    Slider(
                                        value = sliderPosition,
                                        onValueChange = { sliderPosition = it },
                                        steps = 4,
                                        valueRange = 0f..5f
                                    )
                                    val submitReview = rememberTextFieldState() //https://developer.android.com/develop/ui/compose/text/user-input#manage-tf-state
                                    OutlinedTextField(
                                        state = submitReview,
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Leave a review (Optional)") }
                                    )
                                    OutlinedButton(
                                        shape = MaterialTheme.shapes.small,
                                        onClick =
                                            {
                                                FirebaseFirestore.getInstance()
                                                    .collection("reservations")
                                                    .document(r.id)
                                                    .update(
                                                        "reviewScore",
                                                        sliderPosition.toInt()
                                                    ) //this change's the user's score
                                                if(submitReview.toTextFieldBuffer().toString().isNotEmpty()){
                                                    reviewMessage.messageType = "review"
                                                    reviewMessage.sendTime = Timestamp.now()
                                                    reviewMessage.messageOwner = deviceId
                                                    reviewMessage.text = submitReview.toTextFieldBuffer().toString()
                                                    FirebaseFirestore.getInstance()
                                                        .collection("reservations")
                                                        .document(r.id)
                                                        .update(
                                                            "reviewMessage",
                                                            messageRepository.addMessage(reviewMessage)
                                                        ) //this adds the document id of the user's review to their reservation listing.
                                                }
                                                r.reviewScore = sliderPosition.toInt() //just for visual
                                                updateScore(snapshot_campsite) //this updates the aggregate score on the campsite
                                                reviewing = false
                                                hasReview = true
                                            },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    {
                                        Text(text = "You give this campsite a rating of: ${sliderPosition.toInt()}")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d("ReviewsDebug2", "Has review is true")
                    hasReview = true
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
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "You have already joined this lottery",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            shape = MaterialTheme.shapes.small,
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
                    shape = MaterialTheme.shapes.small,
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
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No Active Lottery",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Text(
            text = "Reviews",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Log.d("ReviewsDebug2", "Do i have a review?")
        //Your review is shown above other reviews
        var reviewScore by remember { mutableIntStateOf(0) }
        var reviewText by remember { mutableStateOf("N/A")}
        var reviewMessageId by remember { mutableStateOf("N/A")}
        var reservationId by remember { mutableStateOf("")}
        var pleaseDoOnce by remember { mutableStateOf<Boolean>(false)}
        if(hasReview) {
            Log.d("ReviewsDebug2", "Yeah I do")
            LaunchedEffect(Unit) {
                loadReservations( //refresh reviews just in case
                    entrantId = deviceId,
                    onResult = { reservations = it },
                    onError = { reservations = listOf() }
                )
                for (r in reservations) {
                    if (r.campsiteName == snapshot_campsite.name){
                        reviewScore = r.reviewScore ?: 0
                        reservationId = r.id
                        val savedReview = r.reviewMessage ?: ""
                        if (isMessageId(savedReview)) {
                            FirebaseFirestore.getInstance() //Review poster profile lookup
                                .collection("messages")
                                .document(savedReview)
                                .get()
                                .addOnSuccessListener { message ->
                                    if (message.exists()) {
                                        reviewMessageId = savedReview
                                        reviewText = message.getString("text") ?: "None"
                                    } else
                                    // older reviews saved the text itself instead of a message id
                                        reviewText = savedReview
                                }
                        } else {
                            reviewText = savedReview.ifBlank { "N/A" }
                        }
                    }
                }
            }
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                if(reviewing == false) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Review",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 5.dp)
                            )
                            Text(
                                text = reviewScore.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 5.dp)
                            )
                            if (reviewText != "N/A") {
                                Text(
                                    text = reviewText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Button(
                                shape = MaterialTheme.shapes.small,
                                onClick = { reviewing = true },
                                modifier = Modifier
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            var sliderPosition by remember { mutableStateOf<Float>(reviewScore.toFloat()) }
                            Slider(
                                value = sliderPosition,
                                onValueChange = { sliderPosition = it },
                                steps = 4,
                                valueRange = 0f..5f
                            )
                            val submitReview = rememberTextFieldState()
                            if (reviewText != "N/A" && pleaseDoOnce == false) {
                                submitReview.edit{ append(reviewText) }
                                pleaseDoOnce = true
                            }
                            OutlinedTextField(
                                state = submitReview,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Leave a review (Optional)") }
                            )
                            OutlinedButton(
                                shape = MaterialTheme.shapes.small,
                                onClick =
                                    {
                                        FirebaseFirestore.getInstance()
                                            .collection("reservations")
                                            .document(reservationId)
                                            .update(
                                                "reviewScore",
                                                sliderPosition.toInt()
                                            ) //this change's the user's score
                                        if(submitReview.toTextFieldBuffer().toString().isNotEmpty()) { //Either add review, or edit the attached review
                                            if (reviewMessageId == "N/A") {
                                                reviewMessage.messageType = "review"
                                                reviewMessage.sendTime = Timestamp.now()
                                                reviewMessage.messageOwner = deviceId
                                                reviewMessage.text =
                                                    submitReview.toTextFieldBuffer().toString()
                                                val newMessageId = messageRepository.addMessage(reviewMessage)
                                                FirebaseFirestore.getInstance()
                                                    .collection("reservations")
                                                    .document(reservationId)
                                                    .update(
                                                        "reviewMessage",
                                                        newMessageId
                                                    ) //this adds the document id of the user's review to their reservation listing.
                                                reviewMessageId = newMessageId // so editing again updates this review
                                            } else {
                                                FirebaseFirestore.getInstance()
                                                    .collection("messages")
                                                    .document(reviewMessageId)
                                                    .update(
                                                        "text",
                                                        submitReview.toTextFieldBuffer().toString()
                                                    ) //this change's the user's score
                                            }
                                            reviewText = submitReview.toTextFieldBuffer().toString()
                                        } else { //Delete review if it is presnet (set its text to N/A).
                                            if (reviewMessageId != "N/A") {
                                                FirebaseFirestore.getInstance()
                                                    .collection("messages")
                                                    .document(reviewMessageId)
                                                    .update(
                                                        "text",
                                                        "N/A"
                                                    )
                                            }
                                            reviewText = "N/A"
                                        }
                                        reviewScore = sliderPosition.toInt() //just for visual
                                        updateScore(snapshot_campsite) //this updates the aggregate score on the campsite
                                        reviewing = false
                                        pleaseDoOnce = false
                                    },
                                modifier = Modifier.fillMaxWidth()
                            )
                            {
                                Text(text = "You give this campsite a rating of: ${sliderPosition.toInt()}")
                            }
                        }
                    }
                }
            }
        }
        displayReviews(snapshot_campsite,deviceId)
    }
}

fun loadCampsiteReviews(
    entrantId: String,
    campsite: Campsite,
    onResult: (List<Reservation>) -> Unit,
    onError: (Exception) -> Unit
) {
    Log.d("ReviewsDebug", "Searching for reservations...")
    Log.d("ReviewsDebug", "${campsite.name}")
    var reservations: List<Reservation>
    FirebaseFirestore.getInstance()
        .collection("reservations")
        .whereEqualTo("campsiteName", campsite.name)
        .get()
        .addOnSuccessListener { snapshot ->
            reservations = snapshot.documents.mapNotNull { doc ->
                // the class has an id field, so fill it in with the document's ID
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
            Log.d("ReviewsDebug", "found ${reservations.size} reservations for entrant_id=$entrantId")
            onResult(reservations)
        }
        .addOnFailureListener { onError(it) }
}

@Composable
fun displayReviews (
    campsite: Campsite,
    entrantId: String
){
    //var reservations: List<Reservation> = listOf()
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    LaunchedEffect(campsite) {
        Log.d("ReviewsDebug", "this device's id: $entrantId")
        loadCampsiteReviews(
            entrantId = entrantId,
            campsite = campsite,
            onResult = { reservations = it },
            onError = { reservations = listOf() }
        )
        Log.d("ReviewsDebug", "${reservations}")
        Log.d("ReviewsDebug", "Lookup Complete")
    }
    // reviews from everyone else, a score on its own still counts as a review
    val otherReviews = reservations.filter {
        it.entrant_id != entrantId && (it.reviewScore != null || !it.reviewMessage.isNullOrBlank())
    }
    if(otherReviews.isEmpty()) {
        Text(
            text = "No reviews yet",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        Log.d("ReviewsDebug", "Lookup Aborted")
        return
    }
    Log.d("ReviewsDebug", "${reservations}")
    for (r in otherReviews) {
        Log.d("ReviewsDebug", "reservation: ${r}")
        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    reviewRow(r.reviewMessage, r.reviewScore ?: 0, r.entrant_id, r.entrantName)
                }
            }
        }
    }
}





@Composable
fun reviewRow(reviewID: String?, rScore: Int, rOwner: String, rName: String = "") {
    //Place reviewer's pfp to the left of review
    //var reviewerName: String = "N/A"
    var reviewerName by remember { mutableStateOf("N/A") }
    var reviewText by remember { mutableStateOf("N/A") }
    LaunchedEffect(rOwner) {
        Log.d("ReviewsDebug", "review owner: $rOwner")
        if (rOwner.isBlank()) {
            reviewerName = rName.ifBlank { "Anonymous" }
            return@LaunchedEffect
        }
        FirebaseFirestore.getInstance() //Review poster profile lookup
            .collection("entrants")
            .document(rOwner)
            .get()
            .addOnSuccessListener { profile ->
                if (profile.exists()) {
                    reviewerName = profile.getString("name") ?: "None"
                } else
                // profile is gone, use the name saved on the reservation
                    reviewerName = rName.ifBlank { "Anonymous" }
            }
    }

    LaunchedEffect(reviewID) {
        Log.d("ReviewsDebug", "review id: $reviewID")
        val saved = reviewID ?: ""
        if (!isMessageId(saved)) {
            // no written review, or an older review that saved the text itself
            reviewText = saved
            return@LaunchedEffect
        }
        FirebaseFirestore.getInstance() //Review poster profile lookup
            .collection("messages")
            .document(saved)
            .get()
            .addOnSuccessListener { message ->
                if (message.exists()) {
                    reviewText = message.getString("text") ?: "None"
                } else
                // older reviews saved the text itself instead of a message id
                    reviewText = saved
            }
    }

    if (reviewerName == "N/A"){
        return
    }

    Text(
        text = reviewerName,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
    )

    Text(
        text = "$rScore/5", //replace this with stars if we use stars
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
    )

    // score-only reviews and deleted review text have nothing to show here
    if (reviewText.isNotBlank() && reviewText != "N/A") {
        Text(
            text = reviewText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 5.dp)
        )
    }
}

// true if the saved review looks like a "messages" document id
// older reviews saved the text itself, and Firestore crashes on ids with a "/" in them
fun isMessageId(saved: String): Boolean = saved.isNotBlank() && !saved.contains("/")