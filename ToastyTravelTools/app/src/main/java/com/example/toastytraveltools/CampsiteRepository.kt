
package com.example.toastytraveltools

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

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

    fun updateCampsite(
        campsite: Campsite,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        campsitesRef.document(campsite.id).set(campsite)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.id).delete()
    }

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
                    errorMessage = "Some campsite records could not be loaded."
                }
            }

            isLoading = false
        }
    }
}
