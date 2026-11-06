
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

    // Add campsite
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

    // Simple campsite deletion
    fun deleteCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.id).delete()
    }

    // Delete campsite and associated reservations and lottery references
    fun deleteCampsiteAndRelatedData(
        campsite: Campsite,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (campsite.id.isBlank() || campsite.Name.isBlank()) {
            onFailure(
                IllegalArgumentException("Campsite ID or name is missing.")
            )
            return
        }

        val reservationsRef = db.collection("reservations")
        val lotteriesRef = db.collection("lotteries")

        reservationsRef
            .whereEqualTo("campsiteName", campsite.Name)
            .get()
            .addOnSuccessListener { reservationSnapshot ->

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
                                        (
                                                snakeCaseSnapshot.documents +
                                                        camelCaseSnapshot.documents
                                                ).distinctBy { it.id }

                                    val lotteryUpdates = mutableListOf<
                                            Pair<
                                                    DocumentReference,
                                                    Map<String, List<String>>?
                                                    >
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

                                        val updatedFields =
                                            fields.associateWith { field ->

                                                val values =
                                                    document.get(field) as? List<*>
                                                        ?: throw IllegalStateException(
                                                            "Invalid campsite list in lottery ${document.id}."
                                                        )

                                                values.map { value ->
                                                    value as? String
                                                        ?: throw IllegalStateException(
                                                            "Invalid campsite ID in lottery ${document.id}."
                                                        )
                                                }.filterNot {
                                                    it == campsite.id
                                                }
                                            }

                                        val deleteLottery =
                                            updatedFields.values.all { it.isEmpty() }

                                        lotteryUpdates.add(
                                            document.reference to
                                                    if (deleteLottery) null
                                                    else updatedFields
                                        )
                                    }

                                    val reservationCount =
                                        reservationSnapshot.documents.size

                                    val lotteryWriteCount =
                                        lotteryUpdates.sumOf { (_, updates) ->
                                            if (updates == null) 1
                                            else updates.size
                                        }

                                    val totalWrites =
                                        reservationCount +
                                                lotteryWriteCount + 1

                                    if (totalWrites > 500) {
                                        throw IllegalStateException(
                                            "Too many related records to delete in one operation."
                                        )
                                    }

                                    val batch = db.batch()

                                    // Delete associated reservations
                                    reservationSnapshot.documents.forEach { document ->
                                        batch.delete(document.reference)
                                    }

                                    // Update or delete associated lotteries
                                    lotteryUpdates.forEach { (reference, updates) ->

                                        if (updates == null) {
                                            batch.delete(reference)
                                        } else {
                                            updates.forEach { (field, values) ->
                                                batch.update(
                                                    reference,
                                                    field,
                                                    values
                                                )
                                            }
                                        }
                                    }

                                    // Delete the campsite
                                    batch.delete(
                                        campsitesRef.document(campsite.id)
                                    )

                                    batch.commit()
                                        .addOnSuccessListener {
                                            onSuccess()
                                        }
                                        .addOnFailureListener {
                                            onFailure(it)
                                        }

                                } catch (e: Exception) {
                                    onFailure(e)
                                }
                            }
                            .addOnFailureListener {
                                onFailure(it)
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it)
                    }
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    // Listen for campsite changes
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
                        "Some campsite records could not be loaded."
                }
            }

            isLoading = false
        }
    }
}
