package com.example.toastytraveltools.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

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
    var status: String = ""
)