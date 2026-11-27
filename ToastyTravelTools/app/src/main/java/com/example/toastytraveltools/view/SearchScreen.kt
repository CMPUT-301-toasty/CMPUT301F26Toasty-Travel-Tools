/*
SearchScreen.kt

Purpose: This is the screen that allows the user to search for campsites by campsite name or
filter by location, date, number of guests, and tags.

Issues:
 - padding between the header and the search is not very eye appealing
 - make it more clear that you can click on the result to see campsite details
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import com.example.toastytraveltools.view.components.MainTab
import com.example.toastytraveltools.view.components.ToastyMainBottomBar
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.LaunchedEffect

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.toastytraveltools.model.Campsite
import com.google.firebase.firestore.DocumentSnapshot

// helper to safely parse campsites that might have been saved with either capitalized or lowercase keys
fun DocumentSnapshot.toCampsiteSafely(): Campsite {
    val name = getString("Name") ?: getString("name") ?: ""
    Log.d("CAMPSITE_DEBUG", "Raw lowercase name: ${get("name")}")
    Log.d("CAMPSITE_DEBUG", "Raw uppercase Name: ${get("Name")}")
    Log.d("CAMPSITE_DEBUG", "Document ID: $id, Name: $name")
    val desc = getString("Description") ?: getString("description") ?: ""
    val loc = getString("Location") ?: getString("location") ?: ""
    val limit = getLong("OccupancyLimit") ?: getLong("occupancyLimit") ?: 0L
    // current format: "imageUrls" list of Storage download URLs
    // legacy format: single "ImagesUrl"/"imagesUrl" string, kept only if it's a real URL
    val imageUrls = ((get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList())
        .ifEmpty {
            listOfNotNull(getString("ImagesUrl") ?: getString("imagesUrl"))
        }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
    val rating = (get("Rating") as? Number)?.toDouble()
        ?: (get("rating") as? Number)?.toDouble()
        ?: getString("Rating")?.toDoubleOrNull()
        ?: getString("rating")?.toDoubleOrNull()
        ?: 0.0
    val tags = (get("Tags") as? List<*>)?.mapNotNull { it as? String }
        ?: (get("tags") as? List<*>)?.mapNotNull { it as? String }
        ?: emptyList()
    val qrCodeUrl = getString("QrCodeUrl") ?: getString("qrCodeUrl") ?: ""
    val ownerId = getString("ownerId") ?: ""

    return Campsite(
        id = id,
        name = name,
        description = desc,
        location = loc,
        occupancyLimit = limit.toInt(),
        imageUrls = imageUrls,
        rating = rating,
        tags = tags,
        qrCodeUrl = qrCodeUrl,
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
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
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
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                CampsiteThumbnail(imageUrl = campsite.imageUrls.firstOrNull())
                if (campsite.imageUrls.size > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "+${campsite.imageUrls.size - 1}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campsite.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                Text(
                    text = campsite.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (campsite.tags.isNotEmpty()) {
                    Text(
                        text = "Tags: ${campsite.tags.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Guests Limit: ${campsite.occupancyLimit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Rating: ${campsite.rating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onCampsiteClick: (String) -> Unit,
    onAddCampsiteClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
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

    // Airbnb-style: show every campsite when Explore opens; searching narrows the list
    fun runSearch(query: String) {
        searchCampsite(
            query = query,
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
    LaunchedEffect(Unit) { runSearch("") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Toasty Travel Tools",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Explore campsites, or list your own",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCampsiteClick,
                // label the whole button; the "+" glyph is decorative
                modifier = Modifier
                    .testTag("addCampsiteAction")
                    .semantics { contentDescription = "List a campsite" },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                text = { Text("List a campsite") },
                icon = {
                    Text(
                        "+",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            )
        },
        bottomBar = {
            ToastyMainBottomBar(
                selected = MainTab.Explore,
                onExploreClick = onSearchClick,
                onReservationsClick = onReservationClick,
                onProfileClick = { onProfileClick(deviceId) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // search bar
            item {
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
                        shape = MaterialTheme.shapes.small,
                        onClick = { runSearch(search.trim()) }
                    ) {
                        Text("Search")
                    }
                }
            }

            item {
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { filter = !filter },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (filter) "Hide filters" else "Filter")
                }
            }

            // filter options
            if (filter) {
                item {
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
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
                                shape = MaterialTheme.shapes.small,
                                onClick = {
                                    if (filter_my_campsites) {
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
            }

            // results
            if (hasSearched) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            text = if (filter_my_campsites) "You own no campsites" else "No campsites found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(results, key = { it.id }) { campsite ->
                        CampsiteRow(
                            campsite = campsite,
                            onClick = { onCampsiteClick(campsite.id) }
                        )
                    }
                }
            }
        }
    }
}
