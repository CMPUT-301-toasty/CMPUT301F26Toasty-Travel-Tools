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
 * A reservation an entrant holds for a campsite
 *
 * @property id Firestore identification for reservation
 * @property campsiteName name of the campsite being reserved
 * @property location where the reserved campsite is
 * @property entrantName name of the entrant who made the reservation
 * @property startDate first day of the reservation; default Timestamp(0, 0)
 * @property endDate last day of the reservation; defailt Timestamp(0, 0)
 * @property guests number of guests included in the reservation
 * @property reviewScore the entrant's review score of their experience, can be null
 *
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