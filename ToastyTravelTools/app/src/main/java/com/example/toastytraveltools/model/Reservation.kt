/*
 * Reservation.kt
 *
 * Purpose: Model class that handles booking of a campsite
 * that is within a range. Relates to Firestore
 * "reservations" collection
 *
 * Issues:
 * - References by campsite name instead of ID, so duplicates may break
 * - Should stick with @DocumentId for consistency with Campsite and Lottery
 * - No validation that endDate is after startDate, guests is positive
 * integer
 */

package com.example.toastytraveltools.model

import com.google.firebase.Timestamp

/**
 * Represents a reservation made by an entrant for a campsite.
 *
 * Reservation information is stored in Firestore and is used
 * by reservation and lottery features.
 *
 * @property id Unique reservation document ID.
 * @property campsiteName Name of the reserved campsite.
 * @property location Location of the campsite.
 * @property entrantName Name of the entrant who made the reservation.
 * @property entrantId ID of the entrant who made the reservation.
 * @property startDate Beginning date of the reservation.
 * @property endDate Ending date of the reservation.
 * @property guests Number of guests included in the reservation.
 * @property reviewScore Optional score submitted for the reservation.
 */
data class Reservation (
    val id: String = "",
    val campsiteName: String = "",
    val location: String = "",
    val entrantName: String = "",
    val entrantId: String = "",
    val startDate: Timestamp = Timestamp(0, 0),
    val endDate: Timestamp = Timestamp(0, 0),
    val guests: Int = 0,
    var reviewScore: Int? = null
)