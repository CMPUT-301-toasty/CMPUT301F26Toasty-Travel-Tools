/**
 * ReservationRepository.kt
 *
 * Purpose: Controller repository between views and Firebase "reservations" collection
 * A live list of reservations and creating new ones
 *
 * Outstanding issues:
 * - No error message for loading errors
 * - Add a failure callback for addReservation
 * - Remove snapshot listener
 */
package com.example.toastytraveltools.controller

import androidx.compose.runtime.mutableStateListOf
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Converts a Firestore date value into a Timestamp
 *
 * Supports Timestamp, Date and YYYY-MM-DD string values
 * Anything that is not the above options becomes Timestamp(0, 0)
 *
 * @return the value as a Timestamp
 */
// Convert Firestore date values into Timestamp.
// Supports existing Timestamp, Date and yyyy-MM-dd String values.
private fun Any?.asReservationTimestamp(): Timestamp {
    return when (this) {
        is Timestamp -> this

        is Date -> Timestamp(this)

        is String -> {
            try {
                val date = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).parse(this)

                if (date != null) Timestamp(date)
                else Timestamp(0, 0)

            } catch (e: Exception) {
                Timestamp(0, 0)
            }
        }

        else -> Timestamp(0, 0)
    }
}


/**
 * Manages reservation data stored in the Firestore "reservations" collection
 *
 * Listens for changes in the collection and keeps it up to date
 *
 * @property reservations list of all reservations loaded
 */
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

                try {
                    val reservation = Reservation(
                        id = document.id,
                        campsiteName = document.getString("campsiteName") ?: "",
                        location = document.getString("location") ?: "",
                        entrantName = document.getString("entrantName") ?: "",
                        startDate = document.get("startDate")
                            .asReservationTimestamp(),
                        endDate = document.get("endDate")
                            .asReservationTimestamp(),
                        guests = (document.get("guests") as? Number)?.toInt() ?: 0
                    )

                    _reservations.add(reservation)

                } catch (e: Exception) {
                    // Skip malformed reservation records.
                }
            }
        }
    }

    /**
     * Adds a new reservation to the Firestore
     *
     * A new document ID is created and stored in the reservation's id field
     *
     * @param reservation the reservation to add
     * @param onSaved called with new document ID after the save works, not called
     * if the save doesn't work
     */
    fun addReservation(
        reservation: Reservation,
        onSaved: (String) -> Unit
    ) {
        val documentRef = reservationsRef.document()

        val newReservation = reservation.copy(
            id = documentRef.id
        )

        documentRef.set(newReservation)
            .addOnSuccessListener {
                onSaved(documentRef.id)
            }
    }
}
