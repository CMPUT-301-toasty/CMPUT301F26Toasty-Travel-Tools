package com.example.toastytraveltools

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class CampsiteRepository {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")

    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite>
        get() = _campsites

    fun addCampsite(campsite: Campsite, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val documentRef = campsitesRef.document()
        val newCampsite = campsite.copy(id = documentRef.id)
        documentRef.set(newCampsite)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCampsite(campsite: Campsite, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
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
                return@addSnapshotListener
            }

            _campsites.clear()
            snapshot?.documents?.forEach { document ->
                try {
                    val camp = document.toCampsiteSafely()
                    _campsites.add(camp)
                } catch (e: Exception) {}
            }
        }
    }
}
