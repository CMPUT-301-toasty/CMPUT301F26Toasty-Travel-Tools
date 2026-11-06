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
     * @param campsite Campsite information to save.
     * @param onSuccess Callback invoked when the campsite is saved.
     * @param onFailure Callback invoked if saving fails.
     */
    fun addCampsite(
        campsite: Campsite,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val documentRef = campsitesRef.document()
        val newCampsite = campsite.copy(id = documentRef.id)

        documentRef.set(newCampsite)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Updates an already existing campsite with given data
     *
     * Find document through campsite.id, a new one is created
     * if there is no document with that id
     *
     * @param campsite The updated campsite
     * @param onSuccess called after the update works
     * @param onFailure called with the exception when the update fails
     */
    // Update campsite
    fun updateCampsite(
        campsite: Campsite,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        campsitesRef.document(campsite.id).set(campsite)
            .addOnSuccessListener { onSuccess() }
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
    }

    /**
     * Delete a campsite and its data
     *
     * Delete reservation where campsiteName matches the campsite's name
     * Remove campsite ID from all the lotteries that contain that ID
     * Delete the campsite document itself
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
                                                .addOnSuccessListener { onSuccess() }
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
                    val campsite = document.toCampsiteSafely()
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
}
