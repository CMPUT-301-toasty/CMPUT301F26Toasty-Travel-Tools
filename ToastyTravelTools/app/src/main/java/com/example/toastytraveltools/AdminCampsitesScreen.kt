
package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AdminCampsitesScreen(
    repository: CampsiteRepository,
    onBack: () -> Unit
) {
    var selectedCampsite by remember {
        mutableStateOf<Campsite?>(null)
    }

    val campsites = repository.campsites

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Button(onClick = onBack) {
            Text("Back to Dashboard")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Registered Campsites",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text("Review all registered campsites.")

        Spacer(Modifier.height(16.dp))

        if (selectedCampsite == null) {

            Text("All campsites (${campsites.size})")

            Spacer(Modifier.height(12.dp))

            when {
                repository.isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading campsites...")
                }

                repository.errorMessage != null -> {
                    Text(
                        text = repository.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                campsites.isEmpty() -> {
                    Text("No campsites available.")
                }

                else -> {
                    campsites.forEach { campsite ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = campsite.Name,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(campsite.Location)

                                Spacer(Modifier.height(8.dp))

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
            }

        } else {

            val campsite = selectedCampsite!!

            Text(
                text = "Campsite Details",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = campsite.Name,
                fontWeight = FontWeight.Bold
            )

            Text("Campsite ID: ${campsite.id}")
            Text("Location: ${campsite.Location}")
            Text("Rating: ${campsite.Rating}")
            Text("Maximum guests: ${campsite.OccupancyLimit}")

            Spacer(Modifier.height(12.dp))

            Text("Description:")
            Text(campsite.Description)

            Spacer(Modifier.height(20.dp))

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
