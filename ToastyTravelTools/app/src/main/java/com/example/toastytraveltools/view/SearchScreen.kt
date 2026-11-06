package com.example.toastytraveltools.view

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.toastytraveltools.model.Campsite
import com.google.firebase.firestore.DocumentSnapshot

// helper to safely parse campsites that might have been saved with either capitalized or lowercase keys
fun DocumentSnapshot.toCampsiteSafely(): Campsite {
    val name = getString("Name") ?: getString("name") ?: ""
    val desc = getString("Description") ?: getString("description") ?: ""
    val loc = getString("Location") ?: getString("location") ?: ""
    val limit = getLong("OccupancyLimit") ?: getLong("occupancyLimit") ?: 0L
    val imagesUrl = getString("ImagesUrl") ?: getString("imagesUrl") ?: ""
    val rating = getDouble("Rating") ?: getDouble("rating") ?: 0.0
    val tags = (get("Tags") as? List<*>)?.mapNotNull { it as? String }
        ?: (get("tags") as? List<*>)?.mapNotNull { it as? String }
        ?: emptyList()
    val qrCodeUrl = getString("QrCodeUrl") ?: getString("qrCodeUrl") ?: ""
    val ownerId = getString("ownerId") ?: ""

    return Campsite(
        id = id,
        Name = name,
        Description = desc,
        Location = loc,
        OccupancyLimit = limit.toInt(),
        ImagesUrl = imagesUrl,
        Rating = rating,
        Tags = tags,
        QrCodeUrl = qrCodeUrl,
        ownerId = ownerId
    )
}

// searches for campsites by name
fun searchCampsite(
    query: String,
    onResult: (List<Campsite>) -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("campsites")
        .get()
        .addOnSuccessListener { snapshot ->
            val matches = snapshot.documents.map { it.toCampsiteSafely() }
                .filter { 
                    it.Name.contains(query, ignoreCase = true) ||
                    it.Description.contains(query, ignoreCase = true) ||
                    it.Location.contains(query, ignoreCase = true) ||
                    it.Tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            onResult(matches)
        }
        .addOnFailureListener { onError(it) }
}

// search for campsites created by the user
fun searchMyCampsites(
    ownerId: String,
    onResult: (List<Campsite>) -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("campsites")
        .whereEqualTo("ownerId", ownerId)
        .get()
        .addOnSuccessListener { snapshot ->
            onResult(snapshot.documents.map { it.toCampsiteSafely() })
        }
        .addOnFailureListener { onError(it) }
}

// search for when the user wants to search with filters
fun filter_search(
    location: String,
    date: String,
    numGuests: Int,
    tags: String,
    onResult: (List<Campsite>) -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    db.collection("campsites")
        .get()
        .addOnSuccessListener { campsiteSnap ->
            // read the raw fields so this works with the capitalized names in the console
            val campsites = campsiteSnap.documents.filter { doc ->
                val loc = doc.getString("Location") ?: doc.getString("location") ?: ""
                val limit = doc.getLong("OccupancyLimit") ?: doc.getLong("occupancyLimit") ?: 0L
                val docTags = (doc.get("Tags") as? List<*>)?.mapNotNull { it as? String } 
                    ?: (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } 
                    ?: emptyList()

                val locMatch = loc.contains(location, ignoreCase = true)
                val limitMatch = limit >= numGuests
                
                val searchTagsList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val tagsMatch = if (searchTagsList.isEmpty()) {
                    true
                } else {
                    searchTagsList.any { searchTag -> 
                        docTags.any { it.contains(searchTag, ignoreCase = true) } 
                    }
                }

                locMatch && limitMatch && tagsMatch
            }
            Log.d("FilterDebug", "location/guests matches: ${campsites.map { it.id }}")

            db.collection("lotteries")
                .get()
                .addOnSuccessListener { lotterySnap ->
                    // campsite IDs that have a lottery whose booking period includes the date
                    val lotteryCampsiteIds = lotterySnap.documents
                        .filter { doc ->
                            val bookStart = doc.getDate("BookingStartDate")
                            val bookEnd = doc.getDate("BookingEndDate")
                            bookStart != null && bookEnd != null &&
                                    fmt.format(bookStart) <= date && date <= fmt.format(bookEnd)
                        }
                        .flatMap { doc ->
                            when (val raw = doc.get("campsiteIds") ?: doc.get("campsite_id")) {
                                is List<*> -> raw.filterIsInstance<String>()
                                is String -> listOf(raw)
                                else -> emptyList()
                            }
                        }
                    Log.d("FilterDebug", "campsite IDs with a lottery on $date: $lotteryCampsiteIds")

                    val withLottery = campsites.filter { it.id in lotteryCampsiteIds }

                    db.collection("reservations")
                        .get()
                        .addOnSuccessListener { resSnap ->
                            val bookedNames = resSnap.documents
                                .filter { doc ->
                                    val start = doc.getString("startDate") ?: doc.getString("StartDate") ?: ""
                                    val end = doc.getString("endDate") ?: doc.getString("EndDate") ?: ""
                                    if (start.isBlank() || end.isBlank()) return@filter false
                                    start <= date && date <= end
                                }
                                .map { it.getString("campsiteName") ?: "" }

                            val matches = withLottery
                                .filter { 
                                    val name = it.getString("Name") ?: it.getString("name") ?: ""
                                    name !in bookedNames 
                                }
                                .map { it.toCampsiteSafely() }
                            Log.d("FilterDebug", "final matches: ${matches.size}")
                            onResult(matches)
                        }
                        .addOnFailureListener {
                            Log.e("FilterDebug", "reservations failed", it)
                            onError(it)
                        }
                }
                .addOnFailureListener {
                    Log.e("FilterDebug", "lotteries failed", it)
                    onError(it)
                }
        }
        .addOnFailureListener {
            Log.e("FilterDebug", "campsites failed", it)
            onError(it)
        }
}
@Composable
fun CampsiteRow(
    campsite: Campsite,
    onClick: ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = campsite.Name,
            fontSize = 18.sp,
        )

        Text(
            text = campsite.Location,
            fontSize = 14.sp,
        )

        if (campsite.Tags.isNotEmpty()) {
            Text(
                text = "Tags: ${campsite.Tags.joinToString(", ")}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = "Guests Limit: ${campsite.OccupancyLimit}",
            fontSize = 14.sp,
        )

        Text(
            text = "Rating: ${campsite.Rating}",
            fontSize = 14.sp,
        )
    }
}

// the three buttons pinned to the bottom of the screen
@Composable
fun BottomButtons_Search(
    onSearchClick: () -> Unit,
    onReservationClick: () -> Unit,
    onProfileClick: (String) -> Unit = {}
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
        Button(
            onClick = onSearchClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Search", maxLines = 1)
        }
        OutlinedButton(
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
fun SearchScreen(
    onNavigateBack: () -> Unit = {},
    onCampsiteClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    var searchClicked by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Campsite>()) }
    var hasSearched by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(false) }
    var filter_location by remember { mutableStateOf("") }
    var filter_date by remember { mutableStateOf("") }
    var filter_num_guests by remember { mutableStateOf("") }
    var filter_tags by remember { mutableStateOf("") }
    
    var filter_my_campsites by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Search Campsites") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons_Search(
                onSearchClick = onSearchClick,
                onReservationClick = onReservationClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            if (!searchClicked) {
                Button(onClick = { searchClicked = true }) {
                    Text("Search")
                }
            } else {
                // search bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Search for campsite") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (search.isNotBlank()) {
                                searchCampsite(
                                    query = search,
                                    onResult = {
                                        results = it
                                        hasSearched = true
                                    },
                                    onError = {
                                        results = listOf()
                                        hasSearched = true
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { filter = !filter },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (filter){
                        Text("Hide filters")
                    } else if (!filter) {
                        Text("Filter")
                    }
                }

                // filter options
                if (filter) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = filter_location,
                                onValueChange = { filter_location = it },
                                label = { Text("Location") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filter_date,
                                onValueChange = { filter_date = it },
                                label = { Text("Date (yyyy-MM-dd)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filter_num_guests,
                                onValueChange = { filter_num_guests = it },
                                label = { Text("Number of guests") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filter_tags,
                                onValueChange = { filter_tags = it },
                                label = { Text("Tags (comma separated)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = filter_my_campsites,
                                    onCheckedChange = { filter_my_campsites = it }
                                )
                                Text("Only show campsites I created")
                            }
                            
                            Button(
                                onClick = {
                                    if (filter_my_campsites) {
                                        // if checkbox is ticked, only filter on my campsites (bypassing the other filters for simplicity based on prompt)
                                        // if it's meant to intersect we can change that logic
                                        searchMyCampsites(
                                            ownerId = deviceId,
                                            onResult = { results = it; hasSearched = true },
                                            onError = { results = listOf(); hasSearched = true }
                                        )
                                        return@Button
                                    }
                                    if (filter_location.isNotBlank() &&
                                        filter_date.isNotBlank() &&
                                        filter_num_guests.isNotBlank()
                                    ) {
                                        filter_search(
                                            location = filter_location,
                                            date = filter_date,
                                            numGuests = filter_num_guests.toIntOrNull() ?: 0,
                                            tags = filter_tags,
                                            onResult = {
                                                results = it
                                                hasSearched = true
                                            },
                                            onError = {
                                                results = listOf()
                                                hasSearched = true
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Apply filters")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // results
                if (hasSearched) {
                    val errMessage = if (filter_my_campsites) "You own no campsites" else "No campsites found"
                    if (results.isEmpty()) {
                        Text(
                            text = errMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results) { campsite ->
                                CampsiteRow(
                                    campsite = campsite,
                                    onClick = { onCampsiteClick(campsite.id)}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}