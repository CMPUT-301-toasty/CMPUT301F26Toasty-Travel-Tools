/*
* CampsiteRepository.kt
* Purpose: Maintains a live list of campsites, handling add, update and deletion
* of campsites. As well as the deletion of campsite's corresponding
* reservations and lotteries
*
* Issues:
* - Reservations should probably be matched by ID, not name
 */
package com.example.toastytraveltools.controller

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.view.toCampsiteSafely
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.DocumentReference
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import android.util.Log
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID
/**
 * Repository responsible for managing campsite data in Firebase Firestore.
 *
 * Maintains a live list of campsites and provides operations to add,
 * update, and delete campsite records. It also handles related reservation
 * and lottery references when a campsite is removed.
 *
 * @property campsites Current list of loaded campsites.
 * @property isLoading Whether the initial campsite data is loading.
 * @property errorMessage Error message from the latest loading operation,
 * or null if no error occurred.
 */

class CampsiteRepository {

    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")
    private val storage = FirebaseStorage.getInstance()

    /** Storage folder that holds every photo for one campsite. */
    private fun imagesFolder(campsiteId: String): StorageReference =
        storage.reference.child("$CAMPSITE_IMAGES_FOLDER/$campsiteId")

    /**
     * Uploads photos to campsite_images/{campsiteId}/ and returns their download URLs,
     * in the same order as [imageUris]. Completes immediately with an empty list if
     * there are no photos.
     */
    private fun uploadImages(campsiteId: String, imageUris: List<Uri>): Task<List<String>> {
        if (imageUris.isEmpty()) return Tasks.forResult(emptyList())
        val folder = imagesFolder(campsiteId)
        val uploads = imageUris.map { uri ->
            val ref = folder.child("${UUID.randomUUID()}.jpg")
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Image upload failed")
                ref.downloadUrl
            }.continueWith { it.result.toString() }
        }
        return Tasks.whenAllSuccess<String>(uploads)
    }

    /**
     * Best-effort deletion of the given photos from Storage. Failures are logged,
     * not reported, since the Firestore document is the source of truth.
     */
    private fun deleteImagesByUrl(imageUrls: List<String>) {
        imageUrls.forEach { url ->
            try {
                storage.getReferenceFromUrl(url).delete()
                    .addOnFailureListener { Log.w(TAG, "Could not delete image $url", it) }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Not a Firebase Storage URL, skipping: $url")
            }
        }
    }

    /**
     * Deletes every photo stored for a campsite, including any not referenced by
     * the document (e.g. left behind by an interrupted upload).
     */
    private fun deleteAllImages(campsite: Campsite) {
        val folder = imagesFolder(campsite.id)
        // photos outside the campsite's folder (e.g. legacy uploads) are deleted by URL
        deleteImagesByUrl(campsite.imageUrls.filterNot { url ->
            runCatching { storage.getReferenceFromUrl(url).path.startsWith(folder.path + "/") }
                .getOrDefault(false)
        })
        folder.listAll()
            .addOnSuccessListener { result -> result.items.forEach { it.delete() } }
            .addOnFailureListener { Log.w(TAG, "Could not list images for ${campsite.id}", it) }
    }

    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite>
        get() = _campsites

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Add a new campsite to Firebase
     *
     * new documentId is created and stored in the 'id' field
     *
     * @param campsite is the campsite we add
     * @param onSuccess called when the campsite is saved
     * @param onFailure called along with the exception if the save doesn't work
     */

    /**
     * Adds a new campsite to Firestore using an automatically generated document ID.
     *
     * Any [imageUris] are uploaded to campsite_images/{newId}/ first and their
     * download URLs saved in the campsite's imageUrls. Zero photos is allowed.
     *
     * @param campsite Campsite information to save.
     * @param imageUris Local photos to attach to the campsite.
     * @param onSuccess Callback invoked when the campsite is saved.
     * @param onFailure Callback invoked if saving fails.
     */
    fun addCampsite(
        campsite: Campsite,
        imageUris: List<Uri> = emptyList(),
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val documentRef = campsitesRef.document()

        uploadImages(documentRef.id, imageUris)
            .addOnSuccessListener { uploadedUrls ->
                val newCampsite = campsite.copy(
                    id = documentRef.id,
                    imageUrls = campsite.imageUrls + uploadedUrls
                )
                documentRef.set(newCampsite)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener {
                        deleteImagesByUrl(uploadedUrls)
                        onFailure(it)
                    }
            }
            .addOnFailureListener {
                // clean up whichever uploads did succeed
                imagesFolder(documentRef.id).listAll()
                    .addOnSuccessListener { r -> r.items.forEach { item -> item.delete() } }
                onFailure(it)
            }
    }

    /**
     * Updates an already existing campsite with given data
     *
     * Find document through campsite.id, a new one is created
     * if there is no document with that id
     *
     * [newImageUris] are uploaded and appended to campsite.imageUrls. After the
     * document is saved, [removedImageUrls] are deleted from Storage.
     *
     * @param campsite The updated campsite, imageUrls should hold the photos to keep
     * @param newImageUris local photos to upload and attach
     * @param removedImageUrls previously stored photos the owner removed
     * @param onSuccess called after the update works
     * @param onFailure called with the exception when the update fails
     */
    // Update campsite
    fun updateCampsite(
        campsite: Campsite,
        newImageUris: List<Uri> = emptyList(),
        removedImageUrls: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        uploadImages(campsite.id, newImageUris)
            .addOnSuccessListener { uploadedUrls ->
                val updated = campsite.copy(
                    imageUrls = (campsite.imageUrls - removedImageUrls.toSet()) + uploadedUrls
                )
                campsitesRef.document(campsite.id).set(updated)
                    .addOnSuccessListener {
                        deleteImagesByUrl(removedImageUrls)
                        onSuccess()
                    }
                    .addOnFailureListener {
                        deleteImagesByUrl(uploadedUrls)
                        onFailure(it)
                    }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Removes a single photo from a campsite: deletes it from Storage and from
     * the campsite's imageUrls list.
     *
     * @param campsiteId ID of the campsite the photo belongs to
     * @param imageUrl download URL of the photo to remove
     */
    fun removeCampsiteImage(
        campsiteId: String,
        imageUrl: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        campsitesRef.document(campsiteId)
            .update("imageUrls", FieldValue.arrayRemove(imageUrl))
            .addOnSuccessListener {
                deleteImagesByUrl(listOf(imageUrl))
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Delete the campsite document
     *
     * @param campsite the campsite we want to delete, use id to reference
     */
    // Simple campsite deletion
    fun deleteCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.id).delete()
            .addOnSuccessListener { deleteAllImages(campsite) }
    }

    /**
     * Delete a campsite and its data
     *
     * Delete reservation where campsiteName matches the campsite's name
     * Remove campsite ID from all the lotteries that contain that ID
     * Delete the campsite document itself, then its photos in Storage
     *
     * All three steps must be completed
     * @param campsite the campsite we delete, id and name cannot be blank
     * @param onSuccess called after all deletions and updates are finished
     * @param onFailure called with exception if any of the steps fail
     */
    // Delete campsite and associated reservations and lottery references

    fun deleteCampsiteAndRelatedData(
        campsite: Campsite,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (campsite.id.isBlank() || campsite.name.isBlank()) {
            onFailure(IllegalArgumentException("Campsite ID or name is missing."))
            return
        }

        val reservationsRef = db.collection("reservations")
        val lotteriesRef = db.collection("lotteries")

        reservationsRef
            .whereEqualTo("campsiteName", campsite.name)
            .get()
            .addOnSuccessListener { reservationSnapshot ->

                val entrantIds = reservationSnapshot.documents
                    .mapNotNull { doc ->
                        (doc.getString("entrant_id")
                            ?: doc.getString("entrantId"))
                            ?.takeIf { it.isNotBlank() }
                    }
                    .distinct()

                val entrantReads = entrantIds.map { entrantId ->
                    db.collection("entrants").document(entrantId).get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(entrantReads)
                    .addOnSuccessListener { entrantSnapshots ->

                        lotteriesRef
                            .whereArrayContains("campsite_id", campsite.id)
                            .get()
                            .addOnSuccessListener { snakeCaseSnapshot ->

                                lotteriesRef
                                    .whereArrayContains("campsiteIds", campsite.id)
                                    .get()
                                    .addOnSuccessListener { camelCaseSnapshot ->

                                        try {
                                            val lotteryDocuments =
                                                (snakeCaseSnapshot.documents +
                                                        camelCaseSnapshot.documents)
                                                    .distinctBy { it.id }

                                            val lotteryUpdates = mutableListOf<
                                                    Pair<DocumentReference, Map<String, List<String>>?>
                                                    >()

                                            lotteryDocuments.forEach { document ->
                                                val fields = listOf(
                                                    "campsite_id",
                                                    "campsiteIds"
                                                ).filter { document.contains(it) }

                                                if (fields.isEmpty()) {
                                                    throw IllegalStateException(
                                                        "Lottery ${document.id} has no campsite reference field."
                                                    )
                                                }

                                                val updatedFields = fields.associateWith { field ->
                                                    val values = document.get(field) as? List<*>
                                                        ?: throw IllegalStateException(
                                                            "Invalid campsite list in lottery ${document.id}."
                                                        )

                                                    values.map { value ->
                                                        value as? String
                                                            ?: throw IllegalStateException(
                                                                "Invalid campsite ID in lottery ${document.id}."
                                                            )
                                                    }.filterNot { it == campsite.id }
                                                }

                                                val deleteLottery =
                                                    updatedFields.values.all { it.isEmpty() }

                                                lotteryUpdates.add(
                                                    document.reference to
                                                            if (deleteLottery) null else updatedFields
                                                )
                                            }

                                            val eligibleEntrants = entrantSnapshots.filter {
                                                it.getBoolean("notificationsEnabled") != false
                                            }

                                            val totalWrites =
                                                reservationSnapshot.documents.size +
                                                        lotteryUpdates.sumOf { (_, updates) ->
                                                            if (updates == null) 1 else updates.size
                                                        } +
                                                        eligibleEntrants.size + 1

                                            if (totalWrites > 500) {
                                                throw IllegalStateException(
                                                    "Too many related records to delete in one operation."
                                                )
                                            }

                                            val batch = db.batch()

                                            // Notify affected entrants in their existing profile documents.
                                            eligibleEntrants.forEach { entrant ->
                                                batch.set(
                                                    entrant.reference,
                                                    mapOf(
                                                        "campsiteDeletionNotice" to
                                                                "A reservation for ${campsite.name} was cancelled because the campsite was removed."
                                                    ),
                                                    SetOptions.merge()
                                                )
                                            }

                                            // Delete associated reservations.
                                            reservationSnapshot.documents.forEach { document ->
                                                batch.delete(document.reference)
                                            }

                                            // Update or delete associated lotteries.
                                            lotteryUpdates.forEach { (reference, updates) ->
                                                if (updates == null) {
                                                    batch.delete(reference)
                                                } else {
                                                    updates.forEach { (field, values) ->
                                                        batch.update(reference, field, values)
                                                    }
                                                }
                                            }

                                            // Delete the campsite.
                                            batch.delete(campsitesRef.document(campsite.id))

                                            batch.commit()
                                                .addOnSuccessListener {
                                                    // Remove the campsite's photos from Storage.
                                                    deleteAllImages(campsite)
                                                    onSuccess()
                                                }
                                                .addOnFailureListener { onFailure(it) }

                                        } catch (e: Exception) {
                                            onFailure(e)
                                        }
                                    }
                                    .addOnFailureListener { onFailure(it) }
                            }
                            .addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }


    /**
     * Starts a Firestore snapshot listener to keep the campsite list updated.
     *
     * Updates the loading state and error message as snapshots are received.
     */
    init {
        campsitesRef.addSnapshotListener { snapshot, error ->

            if (error != null) {
                errorMessage = error.localizedMessage
                    ?: "Failed to load campsites."
                isLoading = false
                return@addSnapshotListener
            }

            _campsites.clear()
            errorMessage = null

            snapshot?.documents?.forEach { document ->
                try {
                    Log.d("CAMPSITE_CHECK", "ID: ${document.id}")
                    Log.d("CAMPSITE_CHECK", "DATA: ${document.data}")
                    val campsite = document.toCampsiteSafely()
                    Log.d("CAMPSITE_CHECK", "CONVERTED NAME: ${campsite.name}")
                    _campsites.add(campsite)
                } catch (e: Exception) {
                    errorMessage =
                        "Campsite ${document.id} failed: " +
                                (e.localizedMessage ?: e.javaClass.simpleName)
                }
            }

            isLoading = false
        }
    }

    companion object {
        private const val TAG = "CampsiteRepository"

        /** Root folder in Firebase Storage for campsite photos. */
        const val CAMPSITE_IMAGES_FOLDER = "campsite_images"
    }
}
