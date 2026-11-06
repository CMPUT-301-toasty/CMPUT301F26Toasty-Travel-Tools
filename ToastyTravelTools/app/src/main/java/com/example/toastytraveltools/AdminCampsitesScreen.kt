
package com.example.toastytraveltools

import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.model.Campsite
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

    var campsiteToDelete by remember {
        mutableStateOf<Campsite?>(null)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    var deleteError by remember {
        mutableStateOf<String?>(null)
    }

    var deleteMessage by remember {
        mutableStateOf<String?>(null)
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

        deleteMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

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
                                    text = campsite.name,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(campsite.location)

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    Button(
                                        onClick = {
                                            selectedCampsite = campsite
                                        }
                                    ) {
                                        Text("View campsite")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            deleteError = null
                                            deleteMessage = null
                                            campsiteToDelete = campsite
                                        }
                                    ) {
                                        Text("Remove")
                                    }
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
                text = campsite.name,
                fontWeight = FontWeight.Bold
            )

            Text("Campsite ID: ${campsite.id}")
            Text("Location: ${campsite.location}")
            Text("Rating: ${campsite.rating}")
            Text("Maximum guests: ${campsite.occupancyLimit}")

            Spacer(Modifier.height(12.dp))

            Text("Description:")
            Text(campsite.description)

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    selectedCampsite = null
                }
            ) {
                Text("Back to campsites")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    deleteError = null
                    deleteMessage = null
                    campsiteToDelete = campsite
                }
            ) {
                Text("Remove Campsite")
            }
        }
    }

    // Confirmation dialog
    campsiteToDelete?.let { campsite ->

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    campsiteToDelete = null
                }
            },

            title = {
                Text("Remove campsite?")
            },

            text = {
                Column {

                    Text(
                        "Are you sure you want to remove ${campsite.name}? " +
                                "Its associated reservations and lottery references " +
                                "will also be handled."
                    )

                    deleteError?.let {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (isDeleting) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator()
                        Text("Removing campsite...")
                    }
                }
            },

            confirmButton = {

                Button(
                    enabled = !isDeleting,
                    onClick = {

                        isDeleting = true
                        deleteError = null

                        repository.deleteCampsiteAndRelatedData(
                            campsite = campsite,

                            onSuccess = {
                                isDeleting = false

                                if (selectedCampsite?.id == campsite.id) {
                                    selectedCampsite = null
                                }

                                campsiteToDelete = null
                                deleteMessage =
                                    "${campsite.name} was successfully removed."
                            },

                            onFailure = { error ->
                                isDeleting = false

                                deleteError = error.localizedMessage
                                    ?: "Failed to remove campsite."
                            }
                        )
                    }
                ) {
                    Text(
                        if (isDeleting) "Removing..."
                        else "Confirm Remove"
                    )
                }
            },

            dismissButton = {

                OutlinedButton(
                    enabled = !isDeleting,
                    onClick = {
                        campsiteToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
