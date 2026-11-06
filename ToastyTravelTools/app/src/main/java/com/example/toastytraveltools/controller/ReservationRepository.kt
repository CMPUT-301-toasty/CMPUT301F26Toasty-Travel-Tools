
package com.example.toastytraveltools.controller

import androidx.compose.runtime.mutableStateListOf
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
