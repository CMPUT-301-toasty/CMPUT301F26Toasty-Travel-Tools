package com.example.toastytraveltools.controller

import androidx.compose.runtime.mutableStateListOf
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ReservationRepository {
    private val db = Firebase.firestore
    private val reservationsRef = db.collection("reservations")

    private val _reservations = mutableStateListOf<Reservation>()

    val reservations: List<Reservation>
        get() = _reservations

    init {
        reservationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            _reservations.clear()

            snapshot?.documents?.forEach { document ->
                val reservation = document.toObject(Reservation::class.java)
                if (reservation != null) {
                    // the doc id is the reservation id
                    _reservations.add(reservation.copy(id = document.id))
                }
            }
        }
    }

    // onSaved gets the new document id so the caller can navigate to the confirmation screen
    fun addReservation(reservation: Reservation, onSaved: (String) -> Unit) {
        reservationsRef.add(reservation).addOnSuccessListener { docRef ->
            onSaved(docRef.id)
        }
    }
}