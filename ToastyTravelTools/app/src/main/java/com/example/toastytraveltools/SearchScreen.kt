package com.example.toastytraveltools

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

// display of one campsite card after the search
@Composable
fun DisplayCampsite(Campsite: Campsite) {
    Text("Results for: $Campsite")

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
            val matches = snapshot.toObjects(Campsite::class.java)
                .filter { it.Name.contains(query, ignoreCase = true) }
            onResult(matches)
        }
        .addOnFailureListener { onError(it) }
}

// search for when the user wants to search with filters
fun filter_search(
    location: String,
    date: String,
    numGuests: Int,
    onResult: (List<Campsite>) -> Unit,
    onError: (Exception) -> Unit
) {
    // need to implement the search


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

@Composable
fun SearchScreen(
    onCampsiteClick: (String) -> Unit
) {
    var searchClicked by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Campsite>()) }
    var hasSearched by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(false) }
    var filter_location by remember { mutableStateOf("") }
    var filter_date by remember { mutableStateOf("") }
    var filter_num_guests by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        Button(
                            onClick = {
                                if (filter_location.isNotBlank() &&
                                    filter_date.isNotBlank() &&
                                    filter_num_guests.isNotBlank()
                                ) {
                                    filter_search(
                                        location = filter_location,
                                        date = filter_date,
                                        numGuests = filter_num_guests.toIntOrNull() ?: 0,
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
                if (results.isEmpty()) {
                    Text(
                        text = "No campsites found",
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