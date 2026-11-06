
package com.example.toastytraveltools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AdminImage(
    val id: Int,
    val title: String,
    val belongsTo: String,
    val category: String
)

@Composable
fun AdminImagesScreen(onBack: () -> Unit) {
    val images = remember {
        mutableStateListOf(
            // place holder images / campsites
            AdminImage(1, "Creekside tent", "Pine Creek Camp", "Campsite"),
            AdminImage(2, "Forest trail", "Pine Creek Camp", "Campsite"),
            AdminImage(3, "Meadow pitch", "Fern Valley Camp", "Campsite"),
            AdminImage(4, "Valley view", "Fern Valley Camp", "Campsite"),
            AdminImage(5, "Maya Patel profile", "Maya Patel", "Profile"),
            AdminImage(6, "Jordan Lee profile", "Jordan Lee", "Profile")
        )
    }

    var imageToRemove by remember {
        mutableStateOf<AdminImage?>(null)
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
        Text(
            "Uploaded Images",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Browse campsite and profile images")

        Spacer(modifier = Modifier.height(16.dp))

        if (images.isEmpty()) {
            Text("No uploaded images")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(images, key = { it.id }) { image ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.LightGray),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text("Image Preview")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                image.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text("${image.category}: ${image.belongsTo}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    imageToRemove = image
                                }
                            ) {
                                Text("Remove Image")
                            }
                        }
                    }
                }
            }
        }
    }

    if (imageToRemove != null) {
        AlertDialog(
            onDismissRequest = {
                imageToRemove = null
            },
            title = {
                Text("Remove image?")
            },
            text = {
                Text(
                    "Are you sure you want to remove " +
                            "${imageToRemove!!.title}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        images.remove(imageToRemove)
                        imageToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        imageToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
