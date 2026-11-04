package com.example.toastytraveltools

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

public class CampsiteRepository {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("Campsites")

    private val _campsites = mutableStateListOf(Campsite(
        Name = TODO(),
        Description = TODO(),
        Location = TODO(),
        OccupancyLimit = TODO(),
        ImagesUrl = TODO(),
        Rating = TODO()
    ))

    val campsites: List<Campsite>
        get() = _campsites

    fun addCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.Name).set(campsite)
    }

    fun updateCity(oldCampsite: Campsite, updatedCampsite: Campsite) {
        campsitesRef.document(oldCampsite.Name).set(updatedCampsite)
    }

    fun deleteCity(campsite: Campsite) {
        campsitesRef.document(campsite.Name).delete()
    }

    init {
        campsitesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            _campsites.clear()
            snapshot?.documents?.forEach { document ->
                val city = document.toObject(Campsite::class.java)
                if (city != null) {
                    _campsites.add(city)
                }
            }
        }
    }
}