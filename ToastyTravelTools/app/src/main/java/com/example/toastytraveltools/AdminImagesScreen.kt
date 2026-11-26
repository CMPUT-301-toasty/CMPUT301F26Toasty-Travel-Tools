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
 * Campsite photos live in Firebase Storage under campsite_images/{campsiteId}/
 * and are referenced from the campsite document's imageUrls list. Each photo
 * is listed individually so an admin can remove one without touching the others.
 *
 * Outstanding issues:
 * Profile image management is not currently implemented.
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
/** One stored photo and the campsite it belongs to. */
private data class CampsitePhoto(val campsite: Campsite, val url: String)

/**
 * Displays campsite images that have been uploaded to the application.
 *
 * Administrators can select an image for removal and confirm the action.
 * The image is removed from the campsite's imageUrls and deleted from Storage.
 *
 * @param onBack Callback used to return to the Admin Dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminImagesScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    val photos = remember { mutableStateListOf<CampsitePhoto>() }
    var photoToRemove by remember { mutableStateOf<CampsitePhoto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRemoving by remember { mutableStateOf(false) }

    /**
     * Loads every photo of every campsite.
     */
    fun loadImages() {
        isLoading = true
        db.collection("campsites")
            .get()
            .addOnSuccessListener { snapshot ->
                photos.clear()
                snapshot.documents.forEach { document ->
                    val campsite = document.toCampsiteSafely()
                    campsite.imageUrls.forEach { url ->
                        photos.add(CampsitePhoto(campsite, url))
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
     * Removes the photo's reference from its campsite, then deletes the file from Storage.
     *
     * @param photo Photo that should be removed.
     */
    fun removeImage(photo: CampsitePhoto) {
        isRemoving = true
        errorMessage = null

        val updates = mutableMapOf<String, Any>(
            "imageUrls" to FieldValue.arrayRemove(photo.url),
            // clear the old single-image fields too, in case this photo came from one
            "imagesUrl" to FieldValue.delete(),
            "ImagesUrl" to FieldValue.delete()
        )

        db.collection("campsites")
            .document(photo.campsite.id)
            .update(updates)
            .addOnSuccessListener {
                photos.remove(photo)
                photoToRemove = null
                isRemoving = false
                try {
                    storage.getReferenceFromUrl(photo.url).delete()
                        .addOnFailureListener {
                            errorMessage =
                                "Removed from campsite, but the file could not be deleted from Storage: ${it.message}"
                        }
                } catch (e: IllegalArgumentException) {
                    // not a Firebase Storage URL, nothing to delete
                }
            }
            .addOnFailureListener {
                errorMessage = "Could not remove image: ${it.message}"
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

                photos.isEmpty() -> Text("No uploaded campsite images found.")

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(photos, key = { "${it.campsite.id}|${it.url}" }) { photo ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    photo.campsite.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Campsite: ${photo.campsite.location}")
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = "Image of ${photo.campsite.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { photoToRemove = photo },
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

    photoToRemove?.let { photo ->
        AlertDialog(
            onDismissRequest = {
                if (!isRemoving) photoToRemove = null
            },
            title = { Text("Remove image?") },
            text = {
                Text("Remove this image from ${photo.campsite.name}? The campsite and its other images will remain.")
            },
            confirmButton = {
                Button(
                    onClick = { removeImage(photo) },
                    enabled = !isRemoving
                ) {
                    Text(if (isRemoving) "Removing..." else "Confirm Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { photoToRemove = null },
                    enabled = !isRemoving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
