package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.controller.ReservationRepository
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Lets an admin see every rating and written review left on campsites,
 * and remove ones that violate app policy.
 *
 * Ratings and written reviews are removed separately: removing a rating
 * recalculates the campsite's average, removing a written review keeps the rating.
 *
 * @param reservationRepository live list of all reservations (reviews are stored on them)
 * @param onBack navigates back to the admin dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewsScreen(
    reservationRepository: ReservationRepository,
    onBack: () -> Unit
) {
    // A reservation counts as reviewed if it has a rating or a written review
    val reviews = reservationRepository.reservations
        .filter { it.reviewScore != null || !it.reviewMessage.isNullOrBlank() }
        .sortedByDescending { it.endDate }

    // Which review is waiting for confirmation, and whether it's the "rating" or the "review"
    var pendingRemoval by remember { mutableStateOf<Pair<Reservation, String>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    pendingRemoval?.let { (review, kind) ->
        val isRating = kind == "rating"
        val name = review.entrantName.ifBlank { "this user" }
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(if (isRating) "Remove rating?" else "Remove written review?") },
            text = {
                Text(
                    if (isRating)
                        "Remove $name's ${review.reviewScore}/5 rating for ${review.campsiteName}? " +
                                "The campsite's average will be recalculated."
                    else
                        "Remove $name's written review for ${review.campsiteName}? " +
                                "Their rating will stay."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    if (isRating) {
                        reservationRepository.removeRating(
                            review,
                            onError = { errorMessage = "Could not remove rating: ${it.message}" }
                        )
                    } else {
                        reservationRepository.removeReview(
                            review,
                            onError = { errorMessage = "Could not remove review: ${it.message}" }
                        )
                    }
                    pendingRemoval = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { innerPadding ->
        if (reviews.isEmpty()) {
            Text(
                text = "No reviews yet",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                errorMessage?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }

                items(reviews, key = { it.id }) { review ->
                    val reviewText = rememberReviewText(review.reviewMessage)
                    val hasText = reviewText.isNotBlank() && reviewText != "N/A"

                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = review.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = review.reviewScore?.let { "$it/5" } ?: "No rating"
                                )
                            }

                            Text(
                                text = "By ${review.entrantName.ifBlank { "Anonymous" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (hasText) {
                                Text(reviewText)
                            } else {
                                Text(
                                    text = "No written review",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (review.reviewScore != null) {
                                    OutlinedButton(
                                        shape = MaterialTheme.shapes.small,
                                        onClick = { pendingRemoval = review to "rating" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove rating")
                                    }
                                }
                                if (hasText) {
                                    OutlinedButton(
                                        shape = MaterialTheme.shapes.small,
                                        onClick = { pendingRemoval = review to "review" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove review")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a review's text. Newer reviews store a "messages" document id,
 * older ones store the text directly.
 *
 * @param saved the reservation's reviewMessage value
 * @return the review text, or "" while loading or if there is none
 */
@Composable
fun rememberReviewText(saved: String?): String {
    var text by remember(saved) { mutableStateOf("") }
    LaunchedEffect(saved) {
        val value = saved ?: ""
        if (!isMessageId(value)) {
            text = value
            return@LaunchedEffect
        }
        FirebaseFirestore.getInstance()
            .collection("messages")
            .document(value)
            .get()
            .addOnSuccessListener { doc ->
                // if no message has that id, it's an older review that saved the text itself
                text = if (doc.exists()) doc.getString("text") ?: "" else value
            }
    }
    return text
}