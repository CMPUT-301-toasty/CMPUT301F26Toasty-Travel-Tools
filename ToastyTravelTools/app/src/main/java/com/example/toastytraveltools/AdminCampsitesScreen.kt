package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

data class AdminCampsite(
    val name: String,
    val location: String,
    val owner: String,
    val description: String
)

private val sampleCampsites = listOf(
    AdminCampsite(
        name = "Pine Creek Camp",
        location = "Asheville, NC",
        owner = "Maya Patel",
        description = "A peaceful campsite surrounded by forests."
    ),
    AdminCampsite(
        name = "Fern Valley Camp",
        location = "Boone, NC",
        owner = "Noah Reed",
        description = "A scenic campsite with beautiful mountain views."
    ),
    AdminCampsite(
        name = "Cedar Ridge Camp",
        location = "Brevard, NC",
        owner = "Evan Cole",
        description = "A quiet campsite surrounded by nature."
    )
)

@Composable
fun AdminCampsitesScreen(onBack: () -> Unit) {
    var selectedCampsite by remember {
        mutableStateOf<AdminCampsite?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back to Dashboard")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keep the rest of your existing screen code below
        if (selectedCampsite == null) {

            Text(
                text = "Registered Campsites",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Review all registered campsites.")

            Spacer(modifier = Modifier.height(16.dp))

            Text("All campsites (${sampleCampsites.size})")

            Spacer(modifier = Modifier.height(12.dp))

            if (sampleCampsites.isEmpty()) {
                Text("No campsites available.")
            } else {
                sampleCampsites.forEach { campsite ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                campsite.name,
                                fontWeight = FontWeight.Bold
                            )

                            Text(campsite.location)

                            Text("Owner: ${campsite.owner}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    selectedCampsite = campsite
                                }
                            ) {
                                Text("View campsite")
                            }
                        }
                    }
                }
            }

        } else {

            Text(
                text = "Campsite Details",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = selectedCampsite!!.name,
                fontWeight = FontWeight.Bold
            )

            Text("Location: ${selectedCampsite!!.location}")

            Text("Owner: ${selectedCampsite!!.owner}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("Description:")
            Text(selectedCampsite!!.description)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    selectedCampsite = null
                }
            ) {
                Text("Back to campsites")
            }
        }
    }
}