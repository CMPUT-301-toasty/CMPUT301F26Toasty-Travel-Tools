package com.example.toastytraveltools

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.items


@Composable
fun DisplayCampsite(Campsite: Campsite) {
    Text("Results for: $Campsite")
}

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

@Composable
fun SearchScreen() {
    var searchClicked by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Campsite>()) }
    var hasSearched by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!searchClicked) {
            Button(onClick = { searchClicked = true }) {
                Text("Search")
            }
        } else {
            Row {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search for campsite") }
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

            if (hasSearched) {
                if (results.isEmpty()) {
                    Text("No campsites found")
                } else {
                    LazyColumn {
                        items(results) { campsite ->
                            DisplayCampsite(campsite)
                        }
                    }
                }
            }
        }
    }
}