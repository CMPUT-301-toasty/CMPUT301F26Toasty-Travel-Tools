/*
 * Lottery.kt
 *
 * Purpose: Model class relating to the campsite lottery. Entrants join
 * a waitlist during a specified period of time. Winners drawn are given
 * the opportunity to book the campsite. Relates to the Firestore "lotteries"
 * collection
 *
 * Issues:
 * - Could enforce status to be either open or closed rather than any string
 * - Validation/Error checking for dates(registrationEnd after registrationStart)
 * same idea for numberOfWinners must be positive
 */
package com.example.toastytraveltools.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Represents a lottery that entrants can join for an opportunity to book a campsite
 *
 * @property id Firestore document ID, automatically made in collection creation
 * @property campsiteIds IDs of the campsites in that specific lottery
 * @property lotteryName display name of the lottery
 * @property ownerId user ID of the campsite owner
 * @property registrationStart when the entrants can begin joining
 * @property registrationEnd when registration stops
 * @property bookingStartDate first date when booking starts
 * @property bookingEndDate last day of booking
 * @property numberOfWinners how many people are selected to win
 * @property waitlist user IDs who have joined the lottery
 * @property waitlistCapacity max number of entrants allowed to join the lottery waitlist
 * @property status current state of the lottery
 *
 */
data class Lottery(
    @DocumentId var id: String = "",
    var campsiteIds: List<String> = emptyList(),
    val lotteryName: String = "",
    var ownerId: String = "",
    var registrationStart: Timestamp? = null,
    var registrationEnd: Timestamp? = null,
    var bookingStartDate: Timestamp? = null,
    var bookingEndDate: Timestamp? = null,
    var numberOfWinners: Int = 0,
    var waitlist: List<String> = emptyList(),
    var waitlistCapacity: Int? = null,
    var status: String = "",
    var winners: List<String> = emptyList(),
    var notifiedWinners: List<String> = emptyList(),
    var spotsRemaining: Int = 0,
    var reservations: List<Reservation> = emptyList()
)