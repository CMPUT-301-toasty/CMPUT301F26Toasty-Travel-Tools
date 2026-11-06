
package com.example.toastytraveltools

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Any?.asReservationDate(): String {
    return when (this) {
        is String -> this
        is Timestamp -> SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(toDate())
        is Date -> SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(this)
        else -> ""
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
                        startDate = document.get("startDate").asReservationDate(),
                        endDate = document.get("endDate").asReservationDate(),
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
        reservationsRef.add(reservation)
            .addOnSuccessListener { document ->
                onSaved(document.id)
            }
    }
}
