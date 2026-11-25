/**
 * AdminImagesScreen.kt
 *
 * Purpose:
 * Allows administrators to browse and remove uploaded campsite images.
 *
 * Design:
 * Uses Jetpack Compose for the interface and Firebase Firestore
 * and Storage for image management.
 *
 * Outstanding issues:
 * Profile image management is not currently implemented.
 * Firebase Storage deletion requires Storage access to be enabled
 * in the Firebase project.
 */
package com.example.toastytraveltools
import com.example.toastytraveltools.view.toCampsiteSafely
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.Campsite
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Displays campsite images that have been uploaded to the application.
 *
 * Administrators can select an image for removal and confirm the action.
 * The associated Firestore image reference is cleared after deletion.
 *
 * @param onBack Callback used to return to the Admin Dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminImagesScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    val campsites = remember { mutableStateListOf<Campsite>() }
    var imageToRemove by remember { mutableStateOf<Campsite?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRemoving by remember { mutableStateOf(false) }

    /**
     * Loads campsite records that have an image URL.
     */
    fun loadImages() {
        isLoading = true
        db.collection("campsites")
            .get()
            .addOnSuccessListener { snapshot ->
                campsites.clear()
                snapshot.documents.forEach { document ->
                    val campsite = document.toCampsiteSafely()
                    if (campsite != null) {
                        campsite.id = document.id
                        if (campsite.imagesUrl.isNotBlank()) {
                            campsites.add(campsite)
                        }
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Could not load images: ${it.message}"
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadImages()
    }

    /**
     * Deletes the Storage image and clears its Firestore URL.
     *
     * @param campsite Campsite whose image should be removed.
     */
    fun removeImage(campsite: Campsite) {
        isRemoving = true
        errorMessage = null

        try {
            storage.getReferenceFromUrl(campsite.imagesUrl)
                .delete()
                .addOnSuccessListener {
                    db.collection("campsites")
                        .document(campsite.id)
                        .update("imagesUrl", "")
                        .addOnSuccessListener {
                            campsites.removeAll { it.id == campsite.id }
                            imageToRemove = null
                            isRemoving = false
                        }
                        .addOnFailureListener {
                            errorMessage =
                                "Image deleted, but its Firestore reference could not be cleared: ${it.message}"
                            isRemoving = false
                        }
                }
                .addOnFailureListener {
                    errorMessage = "Could not delete image: ${it.message}"
                    isRemoving = false
                }
        } catch (e: Exception) {
            errorMessage = "Invalid image URL: ${e.message}"
            isRemoving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Images") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "Uploaded Campsite Images",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            when {
                isLoading -> CircularProgressIndicator()

                campsites.isEmpty() -> Text("No uploaded campsite images found.")

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(campsites, key = { it.id }) { campsite ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    campsite.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Campsite: ${campsite.location}")
                                Text(
                                    "Image URL: ${campsite.imagesUrl}",
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { imageToRemove = campsite },
                                    enabled = !isRemoving
                                ) {
                                    Text("Remove Image")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Removes a campsite image from Firebase Storage and clears its
     * image URL from the corresponding Firestore document.
     *
     * The campsite document itself is preserved.
     *
     * @param campsite Campsite whose image should be removed.
     */
    imageToRemove?.let { campsite ->
        AlertDialog(
            onDismissRequest = {
                if (!isRemoving) imageToRemove = null
            },
            title = { Text("Remove image?") },
            text = {
                Text("Remove the image for ${campsite.name}? The campsite itself will remain.")
            },
            confirmButton = {
                Button(
                    onClick = { removeImage(campsite) },
                    enabled = !isRemoving
                ) {
                    Text(if (isRemoving) "Removing..." else "Confirm Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { imageToRemove = null },
                    enabled = !isRemoving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
