
package com.example.toastytraveltools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CampsiteRating(
    campsiteId: String,
    entrantId: String,
    canRate: Boolean
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Text("Rate this campsite")

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { rating ->
            Button(
                onClick = { selectedRating = rating },
                enabled = canRate && !saving
            ) {
                Text(if (rating <= selectedRating) "★" else "☆")
            }
        }
    }

    Button(
        onClick = {
            if (selectedRating == 0) {
                message = "Please select a rating"
                return@Button
            }

            saving = true

            val db = FirebaseFirestore.getInstance()
            val campsiteRef = db.collection("campsites")
                .document(campsiteId)

            val ratingRef = campsiteRef.collection("ratings")
                .document(entrantId)

            db.runTransaction { transaction ->
                val existingRating = transaction.get(ratingRef)

                if (existingRating.exists()) {
                    throw Exception("You have already rated this campsite")
                }

                val campsite = transaction.get(campsiteRef)
                val oldRating = campsite.getDouble("Rating") ?: 0.0

                val count = campsite.getLong("ratingCount") ?: 0L
                val newCount = count + 1

                val newAverage =
                    ((oldRating * count) + selectedRating) / newCount

                transaction.set(
                    ratingRef,
                    mapOf(
                        "entrantId" to entrantId,
                        "rating" to selectedRating
                    )
                )

                transaction.update(
                    campsiteRef,
                    mapOf(
                        "Rating" to newAverage,
                        "ratingCount" to newCount
                    )
                )
            }.addOnSuccessListener {
                saving = false
                message = "Rating submitted!"
            }.addOnFailureListener {
                saving = false
                message = it.message ?: "Could not submit rating"
            }
        },
        enabled = canRate && !saving
    ) {
        Text(if (saving) "Submitting..." else "Submit Rating")
    }

    if (!canRate) {
        Text("You can only rate campsites you have previously reserved.")
    }

    if (message.isNotBlank()) {
        Text(message)
    }
}
